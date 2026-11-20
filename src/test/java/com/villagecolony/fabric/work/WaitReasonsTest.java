package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regra 339: a espera sai dividida por motivo, o pedido que o aldeão segura parado (linha {@code VC_TIME_WAIT}). */
class WaitReasonsTest {

    private static final UUID COLONY = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void forget() {
        VillageColonyMod.TASKS.clear();
    }

    @Test
    void theReasonIsTheRequestTheWorkerHolds() {
        UUID smelter = UUID.randomUUID();
        Task iron = VillageColonyMod.TASKS.create(
                COLONY, TaskType.SMELT_MATERIAL, TaskPriority.PRODUCTION, ResourceType.IRON_INGOT, 8);

        iron.reserveFor(smelter);

        assertEquals("SMELT_MATERIAL:IRON_INGOT", WaitReasons.reasonOf(smelter));
    }

    @Test
    void withoutARequestTheWaitIsAHelp() {
        assertEquals("HELP", WaitReasons.reasonOf(UUID.randomUUID()));
    }

    @Test
    void theLineKeepsTheThreeLongestReasonsInSeconds() {
        Map<String, Integer> reasons = new LinkedHashMap<>();
        reasons.put("HELP", 2);
        reasons.put("SMELT_MATERIAL:IRON_INGOT", 80);
        reasons.put("SMELT_MATERIAL:GLASS", 5);
        reasons.put("SMELT_MATERIAL:STONE", 1);

        assertEquals("SMELTER[s=88 SMELT_MATERIAL:IRON_INGOT=80 SMELT_MATERIAL:GLASS=5 HELP=2]",
                WaitReasons.describe("SMELTER", reasons));
    }
}
