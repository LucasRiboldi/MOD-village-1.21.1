package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.fabric.integration.ProfessionChestOverflow;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * O que fura a fila de obras antes do rodízio: o armazém, quando não há baú livre na vila (ADR-036
 * item 9, antes de tudo); depois, com cama para todos (E48), o curral novo, quando todos os currais
 * estão cheios (autor, 2026-10-22).
 */
final class UrgentPlans {

    private UrgentPlans() {
    }

    /** As plantas da obra urgente, ou nenhuma. */
    static List<Blueprint> of(ServerWorld world, Colony colony, List<Building> buildings, boolean bedsForAll) {
        if (ProfessionChestOverflow.needsStorage(colony.id())) {
            List<Blueprint> storage = StoragePlans.forColony(world, colony, buildings);

            if (!storage.isEmpty()) {
                return storage;
            }
        }

        // O poste com 5 animais fura até a fila das camas: o curral entra como prioridade (Regra 130).
        return bedsForAll || PenPlans.crowdedPost(world, colony) ? PenPlans.forColony(world, colony, buildings) : List.of();
    }
}
