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

    /** Casa só com falta de cama ou a primeira — autor, 2026-10-12. */
    @Test
    void aHouseIsBuiltOnlyWhenBedsAreShort() {
        assertTrue(ConstructionPriority.decide(true, true, 7, 6, false).buildsHouse(), "faltam camas");
        assertTrue(ConstructionPriority.decide(false, false, 6, 6, false).buildsHouse(), "a primeira casa");
        assertFalse(ConstructionPriority.decide(true, false, 6, 6, false).buildsHouse(),
                "camas de sobra: a vez da casa do rodízio não levanta casa");
        assertFalse(ConstructionPriority.WORKSHOP.buildsHouse());
    }

    /** O rodízio casa ↔ outro tipo voltou com teto: camas abaixo de adultos + 4 (1A, 2026-10-13). */
    @Test
    void theRotationBuildsAHouseUntilThereAreFourBedsToSpare() {
        ConstructionPriority houseTurn = ConstructionPriority.decide(true, false, 6, 6, false);

        assertTrue(houseTurn.buildsHouse(6, 6), "camas iguais aos adultos: a vez da casa levanta casa");
        assertTrue(houseTurn.buildsHouse(6, 6 + ConstructionPriority.HOUSE_SURPLUS - 1), "uma cama a menos que o teto");
        assertFalse(houseTurn.buildsHouse(6, 6 + ConstructionPriority.HOUSE_SURPLUS), "no teto de camas");
        assertFalse(ConstructionPriority.decide(true, true, 6, 6, false).buildsHouse(6, 6),
                "a vez de outro tipo não levanta casa");
    }
}
