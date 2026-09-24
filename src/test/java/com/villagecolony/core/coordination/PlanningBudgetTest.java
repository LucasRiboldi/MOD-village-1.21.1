package com.villagecolony.core.coordination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A cota de colônias que planejam se ajusta pelo custo do ciclo anterior. */
class PlanningBudgetTest {

    @Test
    void anExpensiveCycleHalvesTheTurns() {
        // O caso medido em 24-09: oito colônias, 255 ms de planejador.
        assertEquals(4, PlanningBudget.nextTurns(8, 255));
        assertEquals(2, PlanningBudget.nextTurns(4, 60));
    }

    @Test
    void atLeastOneColonyAlwaysPlans() {
        assertEquals(1, PlanningBudget.nextTurns(1, 500));
        assertEquals(1, PlanningBudget.nextTurns(0, 500));
    }

    @Test
    void aCheapCycleGivesOneMoreTurnUpToTheCeiling() {
        assertEquals(3, PlanningBudget.nextTurns(2, 1));
        assertEquals(PlanningBudget.MAX_TURNS, PlanningBudget.nextTurns(PlanningBudget.MAX_TURNS, 0));
    }

    @Test
    void nearTheTargetTheTurnsHold() {
        assertEquals(3, PlanningBudget.nextTurns(3, PlanningBudget.TARGET_MS));
        assertEquals(3, PlanningBudget.nextTurns(3, PlanningBudget.TARGET_MS / 4));
    }
}
