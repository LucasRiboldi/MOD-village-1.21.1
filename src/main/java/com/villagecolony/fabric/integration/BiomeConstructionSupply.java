package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * A fronteira entre a economia local e a peça que a construção recebe.
 *
 * <p>A estrutura Vanilla pode pedir qualquer item, inclusive uma peça cuja
 * receita termina no Nether ou em flora que não existe no mundo. A colônia
 * continua produzindo tudo que alguma profissão consegue obter ou fabricar;
 * quando não há essa rota, a quarta tentativa (ADR-036 item 6) coloca a peça no baú do
 * construtor — de manufatura ou da natureza, desde 2026-10-03. Ingrediente de drop (corante, linha, pó de osso,
 * drop de bicho) conta como rota: ele aparece no baú sem espera
 * ({@link DropIngredients}, 2026-09-30). Se ele estiver ausente ou cheio, usa outro baú livre
 * da colônia. Assim uma casa não fica em espera infinita por um ingrediente
 * que nenhum trabalhador pode obter.
 */
public final class BiomeConstructionSupply {

    static {
        ServerMemory.register(BiomeConstructionSupply.class, BiomeConstructionSupply::clearAll);
    }

    private static final int RECIPE_DEPTH = 5;

    private BiomeConstructionSupply() {
    }

    /** A peça tem alguma rota de produção que esta vila pode executar? */
    public static boolean hasRouteInBiome(ServerWorld world, UUID colonyId, Item item) {
        return VillageColonyMod.COLONIES.find(colonyId)
                .flatMap(colony -> {
                    BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
                    // Areia de beira d'água ao alcance — decisão do autor,
                    // 2026-09-30. Só é perguntada se a receita chegar à areia.
                    BooleanSupplier sandNearWater = () -> SandNearWater.around(
                            world, colonyId, center,
                            com.villagecolony.core.coordination.GatheringReach.radius(
                                    colony.observedBeds(), SandNearWater.RADIUS));

                    return world.getBiome(center).getKey().map(biome -> hasRouteInBiome(
                            world, biome, item, new HashSet<>(), RECIPE_DEPTH, sandNearWater));
                })
                .orElse(false);
    }

    /**
     * A mesma pergunta, para um bioma explicito.
     *
     * <p>O caminho normal descobre o bioma a partir do centro da colonia.
     * Esta sobrecarga permite auditar todos os cinco estilos de vila contra
     * o mesmo livro de receitas do servidor, sem forcar uma colonia de teste
     * a nascer em cada um deles.
     */
    public static boolean hasRouteInBiome(
            ServerWorld world, RegistryKey<Biome> biome, Item item) {

        return hasRouteInBiome(world, biome, item, new HashSet<>(), RECIPE_DEPTH, () -> false);
    }

    /**
     * Coloca uma peça sem rota local no baú que atende a obra.
     *
     * @return {@code true} quando a peça já estava ou foi depositada
     */
    public static boolean stockIfUnobtainable(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item) {

        if (hasRouteInBiome(world, colonyId, item)) {
            return false;
        }

        return stock(world, colonyId, near, item);
    }

    /**
     * Quantas tentativas sem sucesso antes de o que falta aparecer no baú da
     * profissão — 4 desde a ADR-036 item 6 (eram 3, decisão de 2026-09-30).
     * Para peça que um artesão faz, o que aparece é o ingrediente sem rota (a
     * linha do tear), e não a peça pronta.
     */
    public static final int ATTEMPTS_BEFORE_STOCKING = 4;

    /** Esperas da obra antes de pedir ao fazendeiro uma muda específica. */
    private static final int TREE_WAITS_BEFORE_PLANTING = 20;

    private static final Map<String, Integer> FAILED_PROFESSION_ATTEMPTS = new HashMap<>();

    /** A terceira falta da mesma peça sem rota profissional libera o depósito. */
    public static boolean failedProfessionAttempt(UUID colonyId, Item item) {
        return FAILED_PROFESSION_ATTEMPTS.merge(key(colonyId, item), 1, Integer::sum)
                >= ATTEMPTS_BEFORE_STOCKING;
    }

    /** A vigésima espera pela madeira desta espécie libera o plantio. */
    public static boolean treeWaitReached(UUID colonyId, TreeSpecies species) {
        return FAILED_PROFESSION_ATTEMPTS.merge(treeKey(colonyId, species), 1, Integer::sum)
                >= TREE_WAITS_BEFORE_PLANTING;
    }

    /** A muda foi plantada; uma futura falta recomeça na primeira espera. */
    public static void treePlanted(UUID colonyId, TreeSpecies species) {
        FAILED_PROFESSION_ATTEMPTS.remove(treeKey(colonyId, species));
    }

    /**
     * Tentativas sem rota profissional em curso, para o save.
     */
    public static Map<String, Integer> failedProfessionAttempts() {
        return Map.copyOf(FAILED_PROFESSION_ATTEMPTS);
    }

    /** Devolve as tentativas lidas do save. */
    public static void restoreFailedProfessionAttempts(Map<String, Integer> saved) {
        FAILED_PROFESSION_ATTEMPTS.putAll(saved);
    }

    /** A rota entregou: a contagem daquela peça recomeça. */
    public static void routeDelivered(UUID colonyId, Item item) {
        FAILED_PROFESSION_ATTEMPTS.remove(key(colonyId, item));
    }

    /**
     * Se o item é da natureza — decisão do autor, 2026-09-26: <i>"peças
     * fabricadas do nada pela regra de peça sem rota devem ser somente para
     * blocos de manufatura (blocos que não são localizados na natureza do jogo
     * naturalmente)"</i>.
     *
     * <p>Na sessão longa daquele dia a regra fabricou 254 peças, entre elas 22
     * toras, 22 grama e 15 terra: o que o lenhador e o fazendeiro deviam trazer
     * nasceu no baú, e a falta das profissões ficou escondida. Natureza é o
     * recurso que o mod declara como coletado ({@code ResourceCategory.NATURAL}
     * — tora, pedra, terra, areia, lã tosquiada, trigo) e o bloco que o mundo
     * gera sozinho: terreno, pedra de base, tronco, folha, muda e flor.
     */
    public static boolean isNatural(Item item) {
        Optional<com.villagecolony.core.type.ResourceType> resource = MinecraftTypeAdapter.toResourceType(item);

        if (resource.isPresent()
                && resource.get().category() == com.villagecolony.core.type.ResourceCategory.NATURAL) {
            return true;
        }

        net.minecraft.block.BlockState state = net.minecraft.block.Block.getBlockFromItem(item).getDefaultState();

        // Terreno e pedra de base são da natureza mesmo quando o mod os tem
        // como produto — a pedra lisa de fornalha é o bloco que o mundo gera.
        if (state.isIn(net.minecraft.registry.tag.BlockTags.DIRT)
                || state.isIn(net.minecraft.registry.tag.BlockTags.SAND)
                || state.isIn(net.minecraft.registry.tag.BlockTags.BASE_STONE_OVERWORLD)
                || state.isOf(net.minecraft.block.Blocks.GRAVEL)
                || state.isOf(net.minecraft.block.Blocks.CLAY)) {
            return true;
        }

        // O resto das tags só vale para o que o mod não conta: o tronco
        // descascado está na tag de troncos do jogo e é manufatura.
        if (resource.isPresent()) {
            return false;
        }

        return state.isIn(net.minecraft.registry.tag.BlockTags.LOGS)
                || state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)
                || state.isIn(net.minecraft.registry.tag.BlockTags.SAPLINGS)
                || state.isIn(net.minecraft.registry.tag.BlockTags.FLOWERS);
    }

    /** Põe a peça no baú que atende a obra, sem perguntar por rota — só peça de manufatura. */
    public static boolean stock(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item) {

        return stock(world, ColonyChests.nearestFirst(world, colonyId, near), item);
    }

    /**
     * Prioriza os baús dos construtores. Baú ausente ou cheio deixa a peça no
     * primeiro outro baú livre da colônia.
     */
    public static boolean stockForConstruction(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item) {

        return stock(world, chestsOf(world, colonyId, near, ProfessionType.BUILDER), item);
    }

    /**
     * O ingrediente sem rota aparece no baú de quem fabrica a peça — decisão
     * do autor, 2026-09-30.
     *
     * <p>O tear pede linha, e nenhuma profissão da colônia a obtém. Em vez de
     * a peça pronta aparecer, aparecem as linhas, no baú do artesão, e ele
     * fabrica o tear pelo caminho de sempre. Vale para todo item que não é
     * natural do bioma nem bloco das estruturas; natural continua sendo
     * trazido pela profissão, e nunca aparece.
     *
     * @param count quanto a receita pede desse ingrediente
     * @return se o baú já tem, ou passou a ter, a quantidade pedida
     */
    public static boolean stockForCraftsman(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item, int count,
            ProfessionType craftsman) {

        return stockFor(world, colonyId, near, item, count, craftsman,
                "after four failed attempts to gather it — no profession can obtain it in this biome");
    }

    /**
     * O material que a busca não achou três vezes aparece no baú de quem o
     * usa — pedido do autor, 2026-10-03.
     *
     * <p><i>"Se não há material necessário depois de 3 tentativas de
     * localizá-lo, o material necessário deve aparecer no baú da profissão
     * que precisou dele; se for localizado no bioma alcançável da vila, o
     * aldeão vai buscá-lo."</i> Vale também para o material da natureza, e é
     * o que revê a decisão de 26-09: ela recusava natureza porque a regra
     * fabricava tora e terra que a profissão <b>achava</b>. Aqui só chega o
     * que a busca no mundo, de verdade, não achou três vezes seguidas.
     *
     * @return se o baú já tem, ou passou a ter, a quantidade pedida
     */
    public static boolean stockAfterEmptySearches(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item, int count, ProfessionType user) {

        return stockFor(world, colonyId, near, item, count, user,
                "after four searches of the village's reach found none");
    }

    private static boolean stockFor(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item, int count, ProfessionType craftsman,
            String why) {

        List<ColonyPos> chests = chestsOf(world, colonyId, near, craftsman);
        int have = ColonyChests.countIn(world, chests, item);

        if (have >= count) {
            return true;
        }

        Optional<ColonyPos> chest = ColonyChests.firstWithRoomFor(world, chests, item, count - have);

        if (chest.isEmpty()
                || ChestDepositor.deposit(world, chest.get(), item, count - have) != 0) {
            VillageColonyMod.LOGGER.info(
                    "The colony could stock {} for the {} but every colony chest is full",
                    item, craftsman);

            return false;
        }

        VillageColonyMod.LOGGER.info("The colony stocked {} x{} for the {} {}", item, count - have, craftsman, why);
        logSupplyError(item, craftsman, why);

        return true;
    }

    /** Os baús da profissão primeiro, depois os outros baús da colônia. */
    private static List<ColonyPos> chestsOf(
            ServerWorld world, UUID colonyId, ColonyPos near, ProfessionType profession) {

        List<ColonyPos> chests = new ArrayList<>();

        for (var worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            if (worker.profession().filter(profession::equals).isEmpty()) {
                continue;
            }

            VillageColonyMod.STORAGES.of(worker.villagerId())
                    .map(WorkerStorage::chestPosition)
                    .filter(chest -> !chests.contains(chest))
                    .ifPresent(chests::add);
        }

        for (ColonyPos chest : ColonyChests.nearestFirst(world, colonyId, near)) {
            if (!chests.contains(chest)) {
                chests.add(chest);
            }
        }

        return chests;
    }

    private static boolean stock(ServerWorld world, List<ColonyPos> chests, Item item) {
        // Natureza também, desde 2026-10-03: quem chega aqui não tem rota no
        // bioma e falhou quatro vezes (ADR-036 item 6).
        if (ColonyChests.countIn(world, chests, item) > 0) {
            return true;
        }

        Optional<ColonyPos> chest = ColonyChests.firstWithRoomFor(world, chests, item, 1);

        if (chest.isEmpty()) {
            VillageColonyMod.LOGGER.info(
                    "The colony could stock {} for construction but every colony chest is full",
                    item);
            return false;
        }

        if (ChestDepositor.deposit(world, chest.get(), item, 1) != 0) {
            return false;
        }

        VillageColonyMod.LOGGER.info(
                "The colony stocked {} for construction after four failed attempts"
                        + " — it has no recipe and no profession can obtain it in this biome",
                item);
        logSupplyError(item, ProfessionType.BUILDER, "no profession can obtain it in this biome");
        return true;
    }

    /**
     * A linha de erro de suprimento — ADR-036 item 6: toda peça que aparece
     * sem a profissão a ter obtido fica registrada, para o playtest contar.
     */
    private static void logSupplyError(Item item, ProfessionType profession, String why) {
        VillageColonyMod.LOGGER.warn("VC_SUPPLY_ERROR version=1 piece={} profession={} attempts={} reason={}",
                Registries.ITEM.getId(item), profession, ATTEMPTS_BEFORE_STOCKING, why);
    }

    private static String key(UUID colonyId, Item item) {
        return colonyId + "/" + Registries.ITEM.getId(item);
    }

    private static String treeKey(UUID colonyId, TreeSpecies species) {
        return colonyId + "/tree/" + species.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Esquece as tentativas. Chamado ao parar o servidor. */
    public static void clearAll() {
        FAILED_PROFESSION_ATTEMPTS.clear();
    }

    private static boolean hasRouteInBiome(
            ServerWorld world, RegistryKey<Biome> biome, Item item, Set<Item> visiting, int depth,
            BooleanSupplier sandNearWater) {

        if (depth < 0 || !visiting.add(item)) {
            return false;
        }

        // Corante, linha, pó de osso e drop de bicho sempre têm rota: eles
        // aparecem no baú — 2026-09-30, ver DropIngredients.
        if (DropIngredients.isAutomatic(item)) {
            return true;
        }

        try {
            Optional<ResourceType> resource = MinecraftTypeAdapter.toResourceType(item);

            if (resource.isPresent() && isDirectBiomeResource(biome, resource.get(), sandNearWater)) {
                return true;
            }

            if (depth == 0) {
                return false;
            }

            if (CraftingLookup.billFor(
                    world,
                    item,
                    ingredient -> hasRouteInBiome(
                            world, biome, ingredient, visiting, depth - 1, sandNearWater))
                    .isPresent()) {

                return true;
            }

            return CraftingLookup.smeltingInputsFor(world, item).stream()
                    .anyMatch(input -> hasRouteInBiome(
                            world, biome, input, visiting, depth - 1, sandNearWater));
        } finally {
            visiting.remove(item);
        }
    }

    private static boolean isDirectBiomeResource(
            RegistryKey<Biome> biome, ResourceType resource, BooleanSupplier sandNearWater) {

        return switch (resource.production()) {
            case HARVESTED -> isVillageWood(biome, resource);
            case MINED -> isMineResource(biome, resource);
            case SURFACE_GATHERED -> isSurfaceResource(biome, resource)
                    || (resource == ResourceType.SAND && sandNearWater.getAsBoolean());
            case SOIL_GATHERED -> !isDesert(biome);
            case SHEARED -> !isDesert(biome);
            case FARMED -> true;
            case CRAFTED_WOOD, CRAFTED_STONE, SMELTED -> false;
        };
    }

    private static boolean isVillageWood(RegistryKey<Biome> biome, ResourceType resource) {
        Optional<String> species = VillageBiomes.woodFor(biome)
                .map(ResourceId::path)
                .map(path -> path.substring(0, path.length() - "_planks".length()));

        String path = MinecraftTypeAdapter.toItem(resource)
                .map(item -> Registries.ITEM.getId(item).getPath())
                .orElse("");

        return species.map(name -> path.startsWith(name + "_")).orElse(false)
                && !isDesert(biome);
    }

    private static boolean isMineResource(RegistryKey<Biome> biome, ResourceType resource) {
        if (resource == ResourceType.COAL || resource == ResourceType.RAW_IRON) {
            return true;
        }

        return VillageBiomes.paletteFor(biome)
                .map(VillagePalette::stone)
                .map(MinecraftTypeAdapter::toIdentifier)
                .filter(Registries.ITEM::containsId)
                .map(Registries.ITEM::get)
                .flatMap(MinecraftTypeAdapter::toResourceType)
                .filter(resource::equals)
                .isPresent();
    }

    private static boolean isSurfaceResource(RegistryKey<Biome> biome, ResourceType resource) {
        return switch (resource) {
            case CACTUS, SAND -> isDesert(biome);
            case GRASS_BLOCK, CLAY_BALL, CLAY -> !isDesert(biome);
            default -> false;
        };
    }

    private static boolean isDesert(RegistryKey<Biome> biome) {
        return VillageBiomes.paletteFor(biome)
                .map(VillagePalette::style)
                .filter("desert"::equals)
                .isPresent();
    }
}
