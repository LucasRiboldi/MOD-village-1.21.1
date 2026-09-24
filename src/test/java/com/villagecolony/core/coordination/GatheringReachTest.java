package com.villagecolony.core.coordination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** O alcance da colheita cresce com as camas da vila — N11, 2026-09-24. */
class GatheringReachTest {

    @Test
    void aYoungVillageGathersCloseToHome() {
        assertEquals(24 + 2 * 6, GatheringReach.radius(6, 64));
    }

    @Test
    void theReachGrowsWithEveryBed() {
        assertEquals(GatheringReach.radius(10, 64) + 2, GatheringReach.radius(11, 64));
    }

    @Test
    void theReachNeverPassesTheTradesCeiling() {
        assertEquals(64, GatheringReach.radius(40, 64));
        assertEquals(48, GatheringReach.radius(40, 48));
    }

    @Test
    void anUnmeasuredVillageKeepsTheFullReach() {
        assertEquals(64, GatheringReach.radius(0, 64));
    }

    @Test
    void aCeilingOfZeroIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> GatheringReach.radius(5, 0));
    }
}
