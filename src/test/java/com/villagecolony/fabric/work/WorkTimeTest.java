package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O tempo dos aldeões — Regra 50, 2026-10-02.
 */
class WorkTimeTest {

    @Test
    void strandedComesBeforeEverything() {
        assertEquals(WorkTime.State.STRANDED, WorkTime.classify(true, true, true, true, true, 9));
    }

    @Test
    void withoutATaskItIsIdleWhateverItDoes() {
        assertEquals(WorkTime.State.IDLE, WorkTime.classify(false, false, true, true, true, 0));
    }

    @Test
    void theSwingingArmIsWork() {
        assertEquals(WorkTime.State.WORK, WorkTime.classify(false, true, true, false, false, 7));
    }

    @Test
    void movingIsWalking() {
        assertEquals(WorkTime.State.WALK, WorkTime.classify(false, true, false, true, true, 0));
    }

    @Test
    void standingWithATargetIsBlockedOnlyAfterThreeSeconds() {
        assertEquals(WorkTime.State.WALK, WorkTime.classify(false, true, false, false, true, 2));
        assertEquals(WorkTime.State.BLOCKED,
                WorkTime.classify(false, true, false, false, true, WorkTime.BLOCKED_AFTER));
    }

    @Test
    void standingWithATaskAndNoTargetIsWaiting() {
        assertEquals(WorkTime.State.WAIT, WorkTime.classify(false, true, false, false, false, 30));
    }

    @Test
    void theLineGivesTheShareOfEachState() {
        Map<WorkTime.State, Integer> seconds = new EnumMap<>(WorkTime.State.class);

        seconds.put(WorkTime.State.WORK, 30);
        seconds.put(WorkTime.State.WALK, 10);
        seconds.put(WorkTime.State.IDLE, 10);

        assertEquals("MINER[s=50 work=60 walk=20 wait=0 blocked=0 idle=20 stranded=0]",
                WorkTime.describe("MINER", seconds));
    }
}
