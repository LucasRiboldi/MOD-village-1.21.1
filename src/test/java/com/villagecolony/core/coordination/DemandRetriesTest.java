package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandRetriesTest {

    @Test
    void countsOnlyFailuresFromTheSameWorldDay() {
        DemandKey demand = new DemandKey(
                UUID.randomUUID(), TaskType.COLLECT_SURFACE_RESOURCE, ResourceType.SAND);
        DemandRetries retries = new DemandRetries();

        assertEquals(1, retries.recordFailure(demand, 12));
        assertEquals(2, retries.recordFailure(demand, 12));
        assertEquals(1, retries.recordFailure(demand, 13));
    }

    @Test
    void savesOnlyTheCurrentWorldDay() {
        DemandRetries retries = new DemandRetries();
        DemandKey yesterday = new DemandKey(UUID.randomUUID(), TaskType.SMELT_MATERIAL, ResourceType.GLASS);
        DemandKey today = new DemandKey(UUID.randomUUID(), TaskType.SMELT_MATERIAL, ResourceType.SAND);

        retries.recordFailure(yesterday, 12);
        retries.recordFailure(today, 13);

        assertEquals(1, retries.snapshot(13).size());
        assertEquals(today, retries.snapshot(13).getFirst().demand());
        assertFalse(retries.snapshot(13).stream().anyMatch(retry -> retry.demand().equals(yesterday)));
        assertTrue(retries.snapshot(12).stream().anyMatch(retry -> retry.demand().equals(yesterday)));
    }
}
