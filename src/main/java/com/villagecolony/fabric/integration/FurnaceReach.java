package com.villagecolony.fabric.integration;

import com.villagecolony.core.coordination.WorkDemand;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O fundidor só persegue o que o bioma dá — sessão longa de 2026-09-26.
 *
 * <p>A meta de "tudo o que a fornalha faz" tem piso de 16 para cada produto,
 * inclusive o arenito liso numa vila de planície. O fundidor procurou arenito e
 * areia 582 vezes naquela sessão sem nunca achar: trabalho que parece trabalho
 * e não rende. Produto de fornalha sem matéria-prima com rota no bioma sai das
 * metas — a menos que a obra o peça, e aí quem responde é a regra de peça sem
 * rota (ADR-022).
 */
public final class FurnaceReach {

    static {
        ServerMemory.register(FurnaceReach.class, FurnaceReach::clearAll);
    }

    /** A resposta por colônia e produto: a rota do bioma não muda durante a sessão. */
    private static final Map<String, Boolean> REACHABLE = new HashMap<>();

    private FurnaceReach() {
    }

    /** As metas sem os produtos de fornalha que o bioma não alimenta. */
    public static Map<ResourceType, Integer> withoutUnreachable(
            ServerWorld world, UUID colonyId, Map<ResourceType, Integer> goals, WorkDemand work) {

        Map<ResourceType, Integer> kept = new LinkedHashMap<>();

        goals.forEach((resource, amount) -> {
            if (resource.production() != com.villagecolony.core.type.Production.SMELTED
                    || work.smelted().getOrDefault(resource, 0) > 0
                    || (resource == ResourceType.GLASS && work.glass() > 0)
                    || (resource == ResourceType.IRON_INGOT && work.iron() > 0)
                    || isReachable(world, colonyId, resource)) {
                kept.put(resource, amount);
            }
        });

        return Map.copyOf(kept);
    }

    private static boolean isReachable(ServerWorld world, UUID colonyId, ResourceType resource) {
        return REACHABLE.computeIfAbsent(colonyId + "/" + resource, key ->
                MinecraftTypeAdapter.toItem(resource)
                        .map(item -> BiomeConstructionSupply.hasRouteInBiome(world, colonyId, item))
                        .orElse(true));
    }

    public static void clearAll() {
        REACHABLE.clear();
    }
}
