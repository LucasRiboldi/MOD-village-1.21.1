package com.villagecolony.fabric.integration;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Resolve a rota de suprimento como grafo, nao so como sim/nao (ADR-036 item 6). */
final class SupplyRoutes {

    private static final int DEPTH = 5;

    private SupplyRoutes() {
    }

    static SupplyRoute inBiome(
            ServerWorld world, RegistryKey<Biome> biome, Item item, WorldEvidence evidence) {
        return inBiome(world, biome, item, evidence, new HashSet<>(), DEPTH);
    }

    private static SupplyRoute inBiome(
            ServerWorld world, RegistryKey<Biome> biome, Item item, WorldEvidence evidence,
            Set<Item> visiting, int depth) {

        if (depth < 0) {
            return SupplyRoute.unavailable(item, SupplySource.DEPTH_LIMIT, List.of());
        }

        if (!visiting.add(item)) {
            return SupplyRoute.unavailable(item, SupplySource.CYCLE, List.of());
        }

        try {
            Optional<SupplySource> direct = BiomeConstructionSupply.directSourceInBiome(biome, item, evidence);

            if (direct.isPresent()) {
                return SupplyRoute.available(item, direct.get(), List.of());
            }

            // Tronco descascado: o machado no tronco, que não é receita do jogo — o carpinteiro já
            // descasca (CraftingSteps.strip); a rota é a do tronco (autor, 2026-10-20).
            Optional<Item> log = unstripped(item);

            if (log.isPresent()) {
                SupplyRoute route = inBiome(world, biome, log.get(), evidence, visiting, depth - 1);

                if (route.available()) {
                    return SupplyRoute.available(item, SupplySource.STRIPPING, List.of(route));
                }
            }

            Optional<CraftingLookup.Bill> bill = CraftingLookup.billFor(
                    world,
                    item,
                    ingredient -> inBiome(world, biome, ingredient, evidence, visiting, depth - 1).available());

            if (bill.isPresent()) {
                List<SupplyRoute> inputs = bill.get().ingredients().keySet().stream()
                        .map(input -> inBiome(world, biome, input, evidence, visiting, depth - 1))
                        .toList();

                return SupplyRoute.available(item, SupplySource.CRAFTING, inputs);
            }

            for (Item input : CraftingLookup.smeltingInputsFor(world, item)) {
                SupplyRoute route = inBiome(world, biome, input, evidence, visiting, depth - 1);

                if (route.available()) {
                    return SupplyRoute.available(item, SupplySource.SMELTING, List.of(route));
                }
            }

            return SupplyRoute.unavailable(item, SupplySource.NO_ROUTE, List.of());
        } finally {
            visiting.remove(item);
        }
    }

    /** O tronco de que este descascado sai: {@code stripped_oak_log} → {@code oak_log}. */
    static Optional<Item> unstripped(Item item) {
        net.minecraft.util.Identifier id = net.minecraft.registry.Registries.ITEM.getId(item);

        if (!id.getPath().startsWith("stripped_")) {
            return Optional.empty();
        }

        net.minecraft.util.Identifier log = net.minecraft.util.Identifier.of(id.getNamespace(),
                id.getPath().substring("stripped_".length()));

        return net.minecraft.registry.Registries.ITEM.containsId(log)
                ? Optional.of(net.minecraft.registry.Registries.ITEM.get(log)) : Optional.empty();
    }
}
