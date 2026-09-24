package com.villagecolony.fabric.integration;

import com.villagecolony.core.coordination.ScanReport;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColonyScanSchedulerTest {

    @Test
    void schedulerVisitsColoniesRoundRobinWithinBudget() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        List<UUID> visited = new ArrayList<>();
        ColonyScanScheduler scheduler = ColonyScanScheduler.withBudget(2, colonyId -> {
            visited.add(colonyId);
            return new ScanReport(colonyId, 1, Map.of(), true);
        });

        assertEquals(2, scheduler.runCycle(List.of(first, second, third)));
        assertEquals(List.of(first, second), visited);

        assertEquals(2, scheduler.runCycle(List.of(first, second, third)));
        assertEquals(List.of(first, second, third, first), visited);
    }
}
