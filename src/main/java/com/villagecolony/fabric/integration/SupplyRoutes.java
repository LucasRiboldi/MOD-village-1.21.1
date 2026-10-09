package com.villagecolony.fabric.integration;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Resolve a rota de suprimento como grafo, nao so como sim/nao (ADR-036 item 6). */
final class SupplyRoutes {

    private static final int DEPTH = 5;

    private SupplyRoutes() {
    }

    static SupplyRoute inBiome(
            ServerWorld world, RegistryKey<Biome> biome, Item item, BooleanSupplier sandNearWater) {
        return inBiome(world, biome, item, sandNearWater, new HashSet<>(), DEPTH);
    }

    private static SupplyRoute inBiome(
            ServerWorld world, RegistryKey<Biome> biome, Item item, BooleanSupplier sandNearWater,
            Set<Item> visiting, int depth) {

        if (depth < 0) {
            return SupplyRoute.unavailable(item, SupplySource.DEPTH_LIMIT, List.of());
        }

        if (!visiting.add(item)) {
            return SupplyRoute.unavailable(item, SupplySource.CYCLE, List.of());
        }

        try {
            Optional<SupplySource> direct = BiomeConstructionSupply.directSourceInBiome(biome, item, sandNearWater);

            if (direct.isPresent()) {
                return SupplyRoute.available(item, direct.get(), List.of());
            }

            Optional<CraftingLookup.Bill> bill = CraftingLookup.billFor(
                    world,
                    item,
                    ingredient -> inBiome(world, biome, ingredient, sandNearWater, visiting, depth - 1).available());

            if (bill.isPresent()) {
                List<SupplyRoute> inputs = bill.get().ingredients().keySet().stream()
                        .map(input -> inBiome(world, biome, input, sandNearWater, visiting, depth - 1))
                        .toList();

                return SupplyRoute.available(item, SupplySource.CRAFTING, inputs);
            }

            for (Item input : CraftingLookup.smeltingInputsFor(world, item)) {
                SupplyRoute route = inBiome(world, biome, input, sandNearWater, visiting, depth - 1);

                if (route.available()) {
                    return SupplyRoute.available(item, SupplySource.SMELTING, List.of(route));
                }
            }

            return SupplyRoute.unavailable(item, SupplySource.NO_ROUTE, List.of());
        } finally {
            visiting.remove(item);
        }
    }
}
