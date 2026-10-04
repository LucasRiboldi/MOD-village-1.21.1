package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A borda mantém pelo menos cinco árvores por lenhador. */
class LumberjackNurseryTargetTest {

    @Test
    void theForestTargetScalesAfterTheBaselineForEachLumberjack() {
        assertEquals(10, LumberjackNursery.targetTreesFor(0));
        assertEquals(10, LumberjackNursery.targetTreesFor(1));
        assertEquals(10, LumberjackNursery.targetTreesFor(2));
        assertEquals(15, LumberjackNursery.targetTreesFor(3));
        assertEquals(20, LumberjackNursery.targetTreesFor(4));
    }
}
