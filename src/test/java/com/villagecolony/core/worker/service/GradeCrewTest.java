package com.villagecolony.core.worker.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ADR-063 §3: o orçamento do Zelador cresce com o atraso, e o atraso parado abre a segunda vaga. */
class GradeCrewTest {

    @AfterEach
    void forget() {
        GradeCrew.clearAll();
    }

    @Test
    void theBudgetGrowsWithThePendingColumnsUpToTheCap() {
        assertEquals(256, GradeCrew.perDay(0));
        assertEquals(256 + 250, GradeCrew.perDay(1_000));
        assertEquals(1_024, GradeCrew.perDay(10_000));
    }

    @Test
    void onlyAStallOfThreeDaysCounts() {
        assertFalse(GradeCrew.stalled(List.of(500, 510, 505)), "três dias não bastam para comparar com três atrás");
        assertFalse(GradeCrew.stalled(List.of(500, 480, 470, 490)), "o atraso caiu desde três dias atrás");
        assertTrue(GradeCrew.stalled(List.of(500, 510, 505, 520)), "o atraso não caiu em três dias");
        assertFalse(GradeCrew.stalled(List.of(0, 0, 0, 0)), "sem nada a mexer não há atraso");
    }

    @Test
    void aStalledVillageGetsASecondKeeperAndLosesItWhenTheGroundIsDone() {
        UUID colony = UUID.randomUUID();

        assertEquals(1, GradeCrew.keepers(colony));
        for (int pending : new int[] {900, 950, 940, 960}) {
            GradeCrew.endOfDay(colony, pending);
        }
        assertEquals(2, GradeCrew.keepers(colony), "o atraso parado não abriu a segunda vaga");
        GradeCrew.endOfDay(colony, 0);
        assertEquals(1, GradeCrew.keepers(colony), "com o chão pronto, a segunda vaga ficou");
        assertEquals(1, GradeCrew.keepers(null));
    }
}
