package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.TaskType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Quais tarefas têm trava antes da reserva — ADR-035 §4. A decisão é pura; a
 * camada Fabric só responde, lendo o mundo, a trava que este enum aponta.
 */
class ReservationGateTest {

    private static final Set<TaskType> SWEPT =
            EnumSet.of(TaskType.COLLECT_SURFACE_RESOURCE, TaskType.COLLECT_SOIL, TaskType.COLLECT_WOOL);

    @Test
    void surfaceSoilAndWoolWaitForAnEmptySweepToCoolDown() {
        for (TaskType type : SWEPT) {
            assertEquals(ReservationGate.EMPTY_SWEEP, ReservationGate.of(type), type.name());
        }
    }

    @Test
    void buildingWaitsForAPlaceToStandAtTheNextBlock() {
        assertEquals(ReservationGate.BUILD_SITE, ReservationGate.of(TaskType.BUILD));
    }

    @Test
    void smeltingWaitsForTheRawInAChest() {
        assertEquals(ReservationGate.SMELT_INPUT, ReservationGate.of(TaskType.SMELT_MATERIAL));
    }

    @Test
    void everyOtherTaskHasNoGate() {
        for (TaskType type : TaskType.values()) {
            if (type != TaskType.BUILD && type != TaskType.SMELT_MATERIAL && !SWEPT.contains(type)) {
                assertEquals(ReservationGate.NONE, ReservationGate.of(type), type.name());
            }
        }
    }
}
