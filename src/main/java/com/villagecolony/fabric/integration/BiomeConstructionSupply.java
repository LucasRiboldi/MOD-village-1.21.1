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
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A fronteira entre a economia local e a peça que a construção recebe.
 *
 * <p>A estrutura Vanilla pode pedir qualquer item, inclusive uma peça cuja
 * receita termina no Nether ou em flora que não existe no mundo. A colônia
 * continua produzindo tudo que alguma profissão consegue obter ou fabricar;
 * quando não há essa rota, a terceira tentativa coloca a peça de manufatura
 * no baú do construtor. Se ele estiver ausente ou cheio, usa outro baú livre
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
                .flatMap(colony -> world.getBiome(MinecraftTypeAdapter.toBlockPos(colony.center()))
                        .getKey()
                        .map(biome -> hasRouteInBiome(world, biome, item)))
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

        return hasRouteInBiome(world, biome, item, new HashSet<>(), RECIPE_DEPTH);
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

    private static final int ATTEMPTS_BEFORE_STOCKING = 3;

    private static final Map<String, Integer> FAILED_PROFESSION_ATTEMPTS = new HashMap<>();

    /** A terceira falta da mesma peça sem rota profissional libera o depósito. */
    public static boolean failedProfessionAttempt(UUID colonyId, Item item) {
        return FAILED_PROFESSION_ATTEMPTS.merge(key(colonyId, item), 1, Integer::sum)
                >= ATTEMPTS_BEFORE_STOCKING;
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

    /** Há alguma profissão capaz de alcançar este item pela cadeia de produção do mod? */
    public static boolean hasProfessionRoute(ServerWorld world, Item item) {
        return hasProfessionRoute(world, item, new HashSet<>(), RECIPE_DEPTH);
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

    private static final java.util.Set<Item> REFUSED_NATURAL = new java.util.HashSet<>();

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

        List<ColonyPos> chests = new ArrayList<>();

        for (var worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            if (worker.profession().filter(ProfessionType.BUILDER::equals).isEmpty()) {
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

        return stock(world, chests, item);
    }

    private static boolean stock(ServerWorld world, List<ColonyPos> chests, Item item) {

        if (isNatural(item)) {
            // A obra espera: quem traz é a profissão, com prioridade para
            // o que a obra pede. Uma linha por item, não por ciclo.
            if (REFUSED_NATURAL.add(item)) {
                VillageColonyMod.LOGGER.info(
                        "The colony will not conjure {} for construction — it is found in nature,"
                                + " and a profession brings it",
                        item);
            }

            return false;
        }

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
                "The colony stocked {} for construction after three failed profession attempts",
                item);
        return true;
    }

    private static String key(UUID colonyId, Item item) {
        return colonyId + "/" + Registries.ITEM.getId(item);
    }

    /** Esquece as tentativas. Chamado ao parar o servidor. */
    public static void clearAll() {
        FAILED_PROFESSION_ATTEMPTS.clear();
        REFUSED_NATURAL.clear();
    }

    private static boolean hasProfessionRoute(
            ServerWorld world, Item item, Set<Item> visiting, int depth) {

        if (depth < 0 || !visiting.add(item)) {
            return false;
        }

        try {
            if (MinecraftTypeAdapter.toResourceType(item).isPresent()) {
                return true;
            }

            if (depth == 0) {
                return false;
            }

            if (CraftingLookup.billFor(
                    world,
                    item,
                    ingredient -> hasProfessionRoute(world, ingredient, visiting, depth - 1))
                    .isPresent()) {
                return true;
            }

            return CraftingLookup.smeltingInputsFor(world, item).stream()
                    .anyMatch(input -> hasProfessionRoute(world, input, visiting, depth - 1));
        } finally {
            visiting.remove(item);
        }
    }

    private static boolean hasRouteInBiome(
            ServerWorld world, RegistryKey<Biome> biome, Item item, Set<Item> visiting, int depth) {

        if (depth < 0 || !visiting.add(item)) {
            return false;
        }

        try {
            Optional<ResourceType> resource = MinecraftTypeAdapter.toResourceType(item);

            if (resource.isPresent() && isDirectBiomeResource(biome, resource.get())) {
                return true;
            }

            if (depth == 0) {
                return false;
            }

            if (CraftingLookup.billFor(
                    world,
                    item,
                    ingredient -> hasRouteInBiome(world, biome, ingredient, visiting, depth - 1))
                    .isPresent()) {

                return true;
            }

            return CraftingLookup.smeltingInputsFor(world, item).stream()
                    .anyMatch(input -> hasRouteInBiome(world, biome, input, visiting, depth - 1));
        } finally {
            visiting.remove(item);
        }
    }

    private static boolean isDirectBiomeResource(RegistryKey<Biome> biome, ResourceType resource) {

        return switch (resource.production()) {
            case HARVESTED -> isVillageWood(biome, resource);
            case MINED -> isMineResource(biome, resource);
            case SURFACE_GATHERED -> isSurfaceResource(biome, resource);
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
