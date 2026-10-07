package com.villagecolony.core.coordination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Cinco árvores por dezena de aldeões, uma pedida a cada cinco (ADR-037 F1, F2). */
class ForestQuotaTest {

    @Test
    void theFoundingGetsFiveTreesPerTenVillagers() {
        assertEquals(5, ForestQuota.foundingTrees(0));
        assertEquals(5, ForestQuota.foundingTrees(9));
        assertEquals(5, ForestQuota.foundingTrees(10));
        assertEquals(10, ForestQuota.foundingTrees(24));
    }

    @Test
    void theMilestoneStartsFromTheTensTheFoundingCovered() {
        assertEquals(0, ForestQuota.foundingMilestone(9));
        assertEquals(20, ForestQuota.foundingMilestone(24));
    }

    @Test
    void theBuildAsksOneTreePerFiveVillagersAndAtLeastOne() {
        assertEquals(1, ForestQuota.requestedTrees(0));
        assertEquals(1, ForestQuota.requestedTrees(9));
        assertEquals(4, ForestQuota.requestedTrees(20));
    }
}
