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

    /** Regra 49 (10-02): sem falta de cama, a casa do ofício que falta vem antes de tudo. */
    @Test
    void aMissingWorkshopComesBeforeTheRotationAndTheFirstHouse() {
        assertEquals(ConstructionPriority.WORKSHOP, ConstructionPriority.decide(true, false, 6, 6, true));
        assertEquals(ConstructionPriority.WORKSHOP, ConstructionPriority.decide(false, false, 0, 0, true));
        assertFalse(ConstructionPriority.WORKSHOP.requiresHouse());
    }

    /** Regra 49: faltando cama, a casa vence a oficina. */
    @Test
    void aBedShortageStillComesFirst() {
        assertEquals(ConstructionPriority.HOUSING_DEFICIT, ConstructionPriority.decide(true, false, 8, 6, true));
    }

    /** Regra 49: com todas as oficinas de pé, o rodízio de sempre volta. */
    @Test
    void withEveryWorkshopStandingTheRotationIsBack() {
        assertEquals(ConstructionPriority.ROTATION_HOUSE, ConstructionPriority.decide(true, false, 6, 6, false));
        assertEquals(ConstructionPriority.ROTATION_NON_RESIDENTIAL,
                ConstructionPriority.decide(true, true, 6, 6, false));
    }
}
