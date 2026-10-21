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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A fronteira entre a economia local e a peça que a construção recebe.
 *
 * <p>A estrutura Vanilla pode pedir qualquer item, inclusive uma peça cuja
 * receita termina no Nether ou em flora que não existe no mundo. A colônia
 * continua produzindo tudo que alguma profissão consegue obter ou fabricar;
 * quando não há essa rota, o quarto despacho (ADR-040) coloca a peça no baú do
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

    private BiomeConstructionSupply() {
    }

    /** A peça tem alguma rota de produção que esta vila pode executar? */
    public static boolean hasRouteInBiome(ServerWorld world, UUID colonyId, Item item) {
        return routeInBiome(world, colonyId, item).available();
    }

    /**
     * A rota explicita da peça, para log e auditoria das dependencias da obra — com as provas do
     * mundo em volta da colônia ({@link WorldEvidence}: areia de beira d'água, árvore, argila).
     */
    public static SupplyRoute routeInBiome(ServerWorld world, UUID colonyId, Item item) {
        return VillageColonyMod.COLONIES.find(colonyId)
                .map(colony -> SupplyRoutes.inBiome(world, VillageStyle.biomeAt(world, colony.center()), item,
                        WorldEvidence.around(world, colony)))
                .orElseGet(() -> SupplyRoute.unavailable(item, SupplySource.NO_COLONY, List.of()));
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

        return SupplyRoutes.inBiome(world, biome, item, WorldEvidence.NONE).available();
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

        return stockForConstruction(world, colonyId, near, item);
    }

    /**
     * Quantos despachos sem entrega antes de o que falta aparecer no baú da
     * profissão — 4, Regra 40 e ADR-040 (eram 3, decisão de 2026-09-30).
     * Para peça que um artesão faz, o que aparece é o ingrediente sem rota (a
     * linha do tear), e não a peça pronta.
     */
    public static final int ATTEMPTS_BEFORE_STOCKING = 4;

    /**
     * Ciclos com a rota da obra aberta e sem entrega antes de a peça aparecer no
     * baú do construtor — regra do autor, 2026-10-11 (depois do playtest da
     * 0.3.25): se em um minuto nenhuma rota avançou, o bloco surge. São 2 ciclos
     * de 30 s sem progresso, por peça, com ou sem tarefa aberta (G2, 2026-10-20;
     * ADR-040). A busca vazia ({@code LocateFallback}) segue com
     * {@link #ATTEMPTS_BEFORE_STOCKING}.
     */
    public static final int DISPATCHES_BEFORE_STOCKING = 2;

    /** Esperas da obra antes de pedir ao fazendeiro uma muda específica. */
    private static final int TREE_WAITS_BEFORE_PLANTING = 20;

    private static final Map<String, Integer> TREE_WAIT_ATTEMPTS = new HashMap<>();

    /** A vigésima espera pela madeira desta espécie libera o plantio. */
    public static boolean treeWaitReached(UUID colonyId, TreeSpecies species) {
        return TREE_WAIT_ATTEMPTS.merge(treeKey(colonyId, species), 1, Integer::sum)
                >= TREE_WAITS_BEFORE_PLANTING;
    }

    /** A muda foi plantada; uma futura falta recomeça na primeira espera. */
    public static void treePlanted(UUID colonyId, TreeSpecies species) {
        TREE_WAIT_ATTEMPTS.remove(treeKey(colonyId, species));
    }

    /** Esperas de muda em curso, para o save. */
    public static Map<String, Integer> treeWaitAttempts() {
        return Map.copyOf(TREE_WAIT_ATTEMPTS);
    }

    /** Devolve as esperas de muda lidas do save. */
    public static void restoreTreeWaitAttempts(Map<String, Integer> saved) {
        saved.forEach((key, value) -> {
            if (key.contains("/tree/")) {
                TREE_WAIT_ATTEMPTS.put(key, value);
            }
        });
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

        return stock(world, colonyId, ColonyChests.nearestFirst(world, colonyId, near), item,
                askedByTheBuild(colonyId, item), SupplyGapLedger.NO_ROUTE);
    }

    /**
     * Prioriza os baús dos construtores. Baú ausente ou cheio deixa a peça no
     * primeiro outro baú livre da colônia.
     */
    public static boolean stockForConstruction(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item) {

        return stockForConstruction(world, colonyId, near, item, SupplyGapLedger.STALLED);
    }

    /**
     * O mesmo, dizendo ao registro de faltas por que a peça surgiu ({@link SupplyGapLedger}) — G2,
     * 2026-10-20.
     */
    public static boolean stockForConstruction(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item, String reason) {

        return stock(world, colonyId, chestsOf(world, colonyId, near, ProfessionType.BUILDER), item,
                askedByTheBuild(colonyId, item), reason);
    }

    /** Quanto da peça a obra ainda pede (mínimo 1): surge tudo de uma vez — autor, 2026-10-11. */
    static int askedByTheBuild(UUID colonyId, Item item) {
        ResourceId id = MinecraftTypeAdapter.toResourceId(Block.getBlockFromItem(item));

        return VillageColonyMod.CONSTRUCTIONS.openOf(colonyId)
                .map(project -> project.remainingMaterials().getOrDefault(id, 0))
                .filter(asked -> asked > 0)
                .orElse(1);
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

        return stockFor(world, colonyId, near, item, count, craftsman, SupplyGapLedger.NO_ROUTE,
                "after four failed attempts to gather it — no profession can obtain it in this biome");
    }

    /**
     * O material que a busca não achou quatro vezes vai ao baú de quem o usa — pedido
     * do autor, 2026-10-03, natureza inclusive. O que a vila já tem em outro baú é
     * levado para lá; só o que ela não tem em baú nenhum aparece (Regra 51).
     *
     * @return se a vila já tem, ou passou a ter, a quantidade pedida
     */
    public static boolean stockAfterEmptySearches(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item, int count, ProfessionType user) {
        return stockFor(world, colonyId, near, item, count, user, SupplyGapLedger.NOT_FOUND,
                "after four searches of the village's reach found none");
    }

    private static boolean stockFor(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item, int count, ProfessionType craftsman,
            String reason, String why) {
        List<ColonyPos> professionChests = professionChestsOf(colonyId, craftsman);
        int professionHave = ColonyChests.countIn(world, professionChests, item);
        if (professionHave >= count) {
            return true;
        }
        if (!professionChests.isEmpty()) {
            int need = count - professionHave;
            Optional<ColonyPos> professionChest = ColonyChests.firstWithRoomFor(world, professionChests, item, need);
            // O que a vila tem em outro baú vem antes de surgir (ChestTransfer).
            int missing = professionChest.map(chest -> need - ChestTransfer.bring(
                    world, chestsOf(world, colonyId, near, craftsman), chest, item, need)).orElse(need);
            if (missing == 0) {
                return true;
            }
            if (professionChest.isPresent()
                    && ChestDepositor.deposit(world, professionChest.get(), item, missing) == 0) {
                VillageColonyMod.LOGGER.info("The colony stocked {} x{} for the {} {}", item, missing, craftsman, why);
                SupplyGapLedger.report(world, colonyId, item, missing, craftsman, ATTEMPTS_BEFORE_STOCKING, reason, why);
                return true;
            }
        }
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
        SupplyGapLedger.report(world, colonyId, item, count - have, craftsman, ATTEMPTS_BEFORE_STOCKING, reason, why);
        return true;
    }

    private static List<ColonyPos> chestsOf(
            ServerWorld world, UUID colonyId, ColonyPos near, ProfessionType profession) {
        List<ColonyPos> chests = professionChestsOf(colonyId, profession);
        for (ColonyPos chest : ColonyChests.nearestFirst(world, colonyId, near)) {
            if (!chests.contains(chest)) {
                chests.add(chest);
            }
        }
        return chests;
    }

    private static List<ColonyPos> professionChestsOf(UUID colonyId, ProfessionType profession) {
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
        return chests;
    }

    private static boolean stock(
            ServerWorld world, UUID colonyId, List<ColonyPos> chests, Item item, int asked, String reason) {
        // Natureza também, desde 2026-10-03: quem chega aqui não tem rota no
        // bioma e falhou quatro vezes (ADR-036 item 6). Surge o que falta até o
        // que a obra pede, de uma vez.
        int missing = asked - ColonyChests.countIn(world, chests, item);

        if (missing <= 0) {
            return true;
        }

        int left = ColonyChests.deposit(world, chests, item, missing);

        if (left == missing) {
            VillageColonyMod.LOGGER.info(
                    "The colony could stock {} for construction but every colony chest is full",
                    item);
            return false;
        }

        VillageColonyMod.LOGGER.info(
                "The colony stocked {} x{} for construction after {} cycles without a delivery"
                        + " — no route delivered it (none in this biome, or one only on paper)",
                item, missing - left, DISPATCHES_BEFORE_STOCKING);
        SupplyGapLedger.report(world, colonyId, item, missing - left, ProfessionType.BUILDER, DISPATCHES_BEFORE_STOCKING,
                reason, "no route delivered it");
        return true;
    }

    private static String treeKey(UUID colonyId, TreeSpecies species) {
        return colonyId + "/tree/" + species.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Esquece as esperas de muda. Chamado ao parar o servidor. */
    public static void clearAll() {
        TREE_WAIT_ATTEMPTS.clear();
    }

    static Optional<SupplySource> directSourceInBiome(
            RegistryKey<Biome> biome, Item item, WorldEvidence evidence) {

        if (DropIngredients.isAutomatic(item)) {
            return Optional.of(SupplySource.AUTOMATIC_DROP);
        }

        return MinecraftTypeAdapter.toResourceType(item)
                .filter(resource -> isDirectBiomeResource(biome, resource, evidence))
                .map(resource -> SupplySource.BIOME_RESOURCE);
    }

    private static boolean isDirectBiomeResource(
            RegistryKey<Biome> biome, ResourceType resource, WorldEvidence evidence) {

        return switch (resource.production()) {
            case HARVESTED -> isVillageWood(biome, resource, evidence);
            case MINED -> isMineResource(biome, resource);
            // A prova do mundo abre o que a regra do bioma fecha: areia de beira d'água fora do
            // deserto, argila ao alcance no deserto (autor, 2026-10-20).
            case SURFACE_GATHERED -> isSurfaceResource(biome, resource)
                    || (resource == ResourceType.SAND && evidence.sandNearWater().getAsBoolean())
                    || ((resource == ResourceType.CLAY || resource == ResourceType.CLAY_BALL)
                            && evidence.clayNearby().getAsBoolean());
            case SOIL_GATHERED -> !isDesert(biome);
            case SHEARED -> !isDesert(biome);
            case FARMED -> true;
            case CRAFTED_WOOD, CRAFTED_STONE, SMELTED -> false;
        };
    }

    private static boolean isVillageWood(RegistryKey<Biome> biome, ResourceType resource, WorldEvidence evidence) {
        Optional<String> species = VillageBiomes.woodFor(biome)
                .map(ResourceId::path)
                .map(path -> path.substring(0, path.length() - "_planks".length()));

        String path = MinecraftTypeAdapter.toItem(resource)
                .map(item -> Registries.ITEM.getId(item).getPath())
                .orElse("");

        // O deserto não tem madeira pela regra do bioma; com árvore em pé ao alcance (o bosque
        // fundacional, o viveiro), tem — a tocha e a cama dependiam disso (autor, 2026-10-20).
        return species.map(name -> path.startsWith(name + "_")).orElse(false)
                && (!isDesert(biome) || evidence.woodNearby().getAsBoolean());
    }

    private static boolean isMineResource(RegistryKey<Biome> biome, ResourceType resource) {
        if (resource == ResourceType.COAL
                || resource == ResourceType.RAW_IRON
                || resource == ResourceType.CALCITE) {
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
            case ICE, SNOWBALL -> VillageBiomes.paletteFor(biome).map(VillagePalette::style)
                    .filter("snowy"::equals).isPresent();
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
