package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinerSearchScheduleTest {

    @Test
    void rotatesTheSearchStartAfterTheLastWorker() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        List<UUID> candidates = List.of(first, second, third);

        assertEquals(0, MinerWork.searchStartIndex(candidates, null));
        assertEquals(1, MinerWork.searchStartIndex(candidates, first));
        assertEquals(2, MinerWork.searchStartIndex(candidates, second));
        assertEquals(0, MinerWork.searchStartIndex(candidates, third));
    }

    @Test
    void startsAtTheFirstCandidateWhenThePreviousWorkerIsNoLongerSearching() {
        UUID remaining = UUID.randomUUID();
        UUID finished = UUID.randomUUID();

        assertEquals(0, MinerWork.searchStartIndex(List.of(remaining), finished));
    }
}
