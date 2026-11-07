package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O caminho traçado não é esquecido porque uma releitura deixou de pedi-lo — Regra 214. */
@DisplayName("PlannedPaths")
class PlannedPathsTest {

    private static final ColonyPos A = new ColonyPos(1, 64, 1);
    private static final ColonyPos B = new ColonyPos(2, 64, 1);

    @Test
    @DisplayName("o trecho que uma releitura não pediu continua no plano")
    void aReadThatDoesNotAskKeepsThePlan() {
        PlannedPaths plan = new PlannedPaths();

        plan.read(1, List.of(A, B), at -> false);
        plan.read(2, List.of(A), at -> false);

        assertEquals(Set.of(A, B), plan.cells());
    }

    @Test
    @DisplayName("calçado ou recusado, sai")
    void aFinishedColumnLeaves() {
        PlannedPaths plan = new PlannedPaths();

        plan.read(1, List.of(A, B), at -> false);
        plan.read(2, List.of(), A::equals);

        assertEquals(Set.of(B), plan.cells());
    }

    @Test
    @DisplayName("sem ser pedido por tempo demais, sai, e pedir de novo renova")
    void itAgesOutExactlyAfterTheLimit() {
        PlannedPaths plan = new PlannedPaths();

        plan.read(1, List.of(A), at -> false);
        plan.read(1 + PlannedPaths.KEEP_READS, List.of(), at -> false);

        assertTrue(plan.cells().contains(A), "no limite ainda fica");

        plan.read(1 + PlannedPaths.KEEP_READS, List.of(A), at -> false);
        plan.read(1 + 2 * PlannedPaths.KEEP_READS, List.of(), at -> false);

        assertTrue(plan.cells().contains(A), "pedir de novo renova");

        plan.read(2 + 2 * PlannedPaths.KEEP_READS, List.of(), at -> false);

        assertFalse(plan.cells().contains(A), "passou do limite");
    }

    @Test
    @DisplayName("quem lê o plano não mexe nele")
    void readingTheCellsDoesNotChangeThePlan() {
        PlannedPaths plan = new PlannedPaths();

        plan.read(1, List.of(A), at -> false);
        plan.cells().clear();

        assertEquals(Set.of(A), plan.cells());
    }
}
