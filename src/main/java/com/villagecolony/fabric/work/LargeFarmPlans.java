package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A plantação grande do mod no deserto — Regra 241 (autor, 2026-11-11): 16x16, a roça da planície com a terra arada toda
 * molhada ({@code scripts/gen_large_farm.py}). Entra na fila como obra urgente, uma por vila, quando o fazendeiro do
 * deserto não acha nada para colher, nem canteiro, nem semente para compostar, e trabalha menos de 30% do expediente.
 */
final class LargeFarmPlans {

    static final ResourceId MODEL = new ResourceId("villagecolony", "colony/large_farm");

    /** Acima desta parte do expediente sem trabalhar, o fazendeiro pede a plantação. */
    static final double IDLE_SHARE = 0.70;

    /** Por quanto tempo o "nada para fazer" do fazendeiro vale: cinco minutos. */
    static final long NOTHING_FOR = 6_000;

    private static final Map<UUID, Long> NOTHING = new HashMap<>();

    static {
        ServerMemory.register(LargeFarmPlans.class, NOTHING::clear);
    }

    private LargeFarmPlans() {
    }

    /** O fazendeiro não achou colheita, canteiro nem semente para compostar. */
    static void nothingToDo(UUID colonyId, long now) {
        NOTHING.put(colonyId, now);
    }

    /** Se o fazendeiro passou mais de {@value #IDLE_SHARE} do expediente sem trabalhar; sem amostra, não. */
    static boolean idleEnough(Map<WorkTime.State, Integer> farmer) {
        int total = farmer.values().stream().mapToInt(Integer::intValue).sum();

        return total > 0 && 1.0 - (double) farmer.getOrDefault(WorkTime.State.WORK, 0) / total > IDLE_SHARE;
    }

    /** A planta, se a vila do deserto pede a plantação agora; senão, nenhuma. */
    static List<Blueprint> forColony(ServerWorld world, Colony colony, List<Building> buildings) {
        Long nothing = NOTHING.get(colony.id());

        if (nothing == null || world.getTime() - nothing > NOTHING_FOR || !VillageGrove.isDesert(world, colony)
                || buildings.stream().anyMatch(building -> MODEL.equals(building.blueprint()))
                || !idleEnough(WorkTime.lastWindowOf(colony.id()).getOrDefault("FARMER", Map.of()))
                || PlanRefusals.skip(world, colony.id(), colony.center(), MODEL)) {
            return List.of();
        }

        return HousePlans.READ.computeIfAbsent(MODEL, missing -> StructureBlueprintReader.read(world, missing))
                .map(List::of).orElseGet(List::of);
    }
}
