package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O mapa de interesse do lote — a vila adensa antes de esticar e foge do morro (autor, 2026-10-28). */
class LotScoreTest {

    @Test
    void theCloserLotWins() {
        double near = LotScore.of(0, 64, 0, 10, 64, 0, 5, 5, 0);
        double far = LotScore.of(0, 64, 0, 60, 64, 0, 5, 5, 0);

        assertTrue(near < far, "o lote perto do centro devia ter nota melhor");
    }

    @Test
    void aLotUpTheHillLosesToAFartherOneAtTheVillageHeight() {
        // O caso do jogo da 0.3.102: 24 blocos acima do centro pesa mais que 30 blocos de distância a mais.
        double hill = LotScore.of(0, 73, 0, 20, 97, 0, 5, 5, 0);
        double level = LotScore.of(0, 73, 0, 50, 73, 0, 5, 5, 0);

        assertTrue(level < hill, "o lote no morro devia perder para o lote na altura da vila");
    }

    @Test
    void theLotAgainstTheStreetBeatsOneWithALongPath() {
        assertTrue(LotScore.of(0, 64, 0, 10, 64, 0, 5, 5, 0) < LotScore.of(0, 64, 0, 10, 64, 0, 5, 5, 8));
    }

    @Test
    void theStreetColumnsAreVisitedFromTheCenterOutwards() {
        List<Long> columns = List.of(ColonyRoads.column(50, 0), ColonyRoads.column(2, 2), ColonyRoads.column(-20, 5),
                ColonyRoads.column(0, 1));

        assertArrayEquals(new int[] {3, 1, 2, 0}, LotScore.nearestFirst(columns, 0, 0));
    }
}
