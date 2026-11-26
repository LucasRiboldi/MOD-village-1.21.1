package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.fabric.integration.ProfessionChestOverflow;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * O que fura a fila de obras antes do rodízio: o armazém, quando não há baú livre na vila (ADR-036
 * item 9, antes de tudo); o curral grande e a plantação grande que a vila deve (Regra 374); a plantação grande com o
 * fazendeiro parado (Regras 241 e 267); depois, com cama para todos (E48), o curral novo, quando todos os currais estão
 * cheios (autor, 2026-10-22).
 */
final class UrgentPlans {

    /** A cada quantas construções prontas a vila deve mais uma plantação grande (Regra 374). */
    static final int FARM_EVERY = 10;

    /** A obra que a vila deve antes de qualquer outra: o curral grande, a plantação grande ou nenhuma. */
    enum First { PEN, FARM, NONE }

    private UrgentPlans() {
    }

    /**
     * Regra 374 (autor 26-11, J2 🅲 do jogo da 0.4.145): o curral grande é a primeira obra da vila, a plantação grande a
     * segunda, e a cada {@value #FARM_EVERY} construções prontas a vila deve mais uma plantação grande.
     */
    static First firstBuild(boolean hasLargePen, int largeFarms, int finished) {
        if (!hasLargePen) {
            return First.PEN;
        }

        return largeFarms < 1 + finished / FARM_EVERY ? First.FARM : First.NONE;
    }

    /** As plantas da obra urgente, ou nenhuma. */
    static List<Blueprint> of(ServerWorld world, Colony colony, List<Building> buildings, boolean bedsForAll) {
        if (ProfessionChestOverflow.needsStorage(colony.id())) {
            List<Blueprint> storage = StoragePlans.forColony(world, colony, buildings);

            if (!storage.isEmpty()) {
                return storage;
            }
        }

        // O curral grande e a plantação grande que a vila deve vêm antes do resto (Regra 374).
        First first = firstBuild(PenPlans.hasLargePen(buildings), LargeFarmPlans.count(buildings),
                (int) buildings.stream().filter(Building::finished).count());
        List<Blueprint> owed = first == First.PEN ? PenPlans.forColony(world, colony, buildings)
                : first == First.FARM ? LargeFarmPlans.model(world, colony) : List.of();

        if (!owed.isEmpty()) {
            return owed;
        }

        // O fazendeiro parado pede a plantação grande, em todo bioma (Regras 241 e 267).
        List<Blueprint> farm = LargeFarmPlans.forColony(world, colony, buildings);

        if (!farm.isEmpty()) {
            return farm;
        }

        // O poste com 5 animais fura até a fila das camas: o curral entra como prioridade (Regra 130).
        return bedsForAll || PenPlans.crowdedPost(world, colony) ? PenPlans.forColony(world, colony, buildings) : List.of();
    }
}
