package com.villagecolony.fabric.integration;

import com.villagecolony.core.resource.model.ColonyResources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O aviso de armazém é estado de transição, não um bloqueio do ciclo. */
class WarehouseHealthLogTest {

    @AfterEach
    void clearMemory() {
        WarehouseHealthLog.clearAll();
    }

    @Test
    void anUnreachableChestEntersAndLeavesDegradedMode() {
        UUID colonyId = UUID.randomUUID();
        ChestInventoryReader.ChestSurvey degraded = new ChestInventoryReader.ChestSurvey(
                ColonyResources.empty(), Map.of(), 1, 1, 0);
        ChestInventoryReader.ChestSurvey complete = new ChestInventoryReader.ChestSurvey(
                ColonyResources.empty(), Map.of(), 2, 0, 0);

        WarehouseHealthLog.observe(colonyId, degraded);
        assertTrue(WarehouseHealthLog.isDegraded(colonyId));

        WarehouseHealthLog.observe(colonyId, complete);
        assertFalse(WarehouseHealthLog.isDegraded(colonyId));
    }
}
