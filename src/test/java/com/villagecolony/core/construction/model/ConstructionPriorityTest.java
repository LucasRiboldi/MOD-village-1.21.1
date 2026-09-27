package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contrato puro da prioridade da próxima construção da colônia. */
class ConstructionPriorityTest {

    @Test
    void housingDeficitOverridesTheRotation() {
        ConstructionPriority priority = ConstructionPriority.decide(true, true, 8, 7);

        assertEquals(ConstructionPriority.HOUSING_DEFICIT, priority);
        assertTrue(priority.requiresHouse());
    }

    @Test
    void firstConstructionIsAHouseWhenBedsAlreadyCoverTheAdults() {
        ConstructionPriority priority = ConstructionPriority.decide(false, false, 0, 0);

        assertEquals(ConstructionPriority.FIRST_HOUSE, priority);
        assertTrue(priority.requiresHouse());
    }

    @Test
    void aHouseHandsTheTurnToInfrastructureWhenBedsAreEnough() {
        ConstructionPriority priority = ConstructionPriority.decide(true, true, 6, 6);

        assertEquals(ConstructionPriority.ROTATION_NON_RESIDENTIAL, priority);
        assertFalse(priority.requiresHouse());
    }

    @Test
    void infrastructureHandsTheTurnBackToAHouse() {
        ConstructionPriority priority = ConstructionPriority.decide(true, false, 6, 6);

        assertEquals(ConstructionPriority.ROTATION_HOUSE, priority);
        assertTrue(priority.requiresHouse());
    }
}
