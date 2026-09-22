package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A fronteira entre a economia local e a peça que a construção recebe.
 *
 * <p>A estrutura Vanilla pode pedir qualquer item, inclusive uma peça cuja
 * receita termina no Nether ou em flora que não existe no bioma. A colônia
 * continua produzindo tudo que tem rota local; quando a árvore de receitas
 * não alcança uma fonte que o perfil do bioma oferece, a peça final entra no
 * baú mais próximo da obra. Assim uma casa não fica em espera infinita por um
 * ingrediente que nenhum trabalhador pode obter.
 */
public final class BiomeConstructionSupply {

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

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, near);

        if (ColonyChests.countIn(world, chests, item) > 0) {
            return true;
        }

        Optional<ColonyPos> chest = ColonyChests.firstWithRoomFor(world, chests, item, 1);

        if (chest.isEmpty()) {
            VillageColonyMod.LOGGER.info(
                    "The colony could stock {} for construction but every builder chest is full",
                    item);
            return false;
        }

        if (ChestDepositor.deposit(world, chest.get(), item, 1) != 0) {
            return false;
        }

        VillageColonyMod.LOGGER.info(
                "The colony stocked {} for construction because this biome has no production route",
                item);
        return true;
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
