package com.villagecolony.fabric.integration;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoundationLotSearchTest {

    private static final int SIDE = 9;

    private static int[] flat(int height) {
        int[] heights = new int[SIDE * SIDE];
        Arrays.fill(heights, height);

        return heights;
    }

    private static void set(int[] heights, int x, int z, int height) {
        heights[z * SIDE + x] = height;
    }

    @Test
    void anExactlyFlatLotIsPreferredEvenFartherThanALevelledOneNearTheCenter() {
        // Duna: tudo sobe de 1 em 1, menos um canto plano.
        int[] heights = new int[SIDE * SIDE];

        for (int z = 0; z < SIDE; z++) {
            for (int x = 0; x < SIDE; x++) {
                heights[z * SIDE + x] = 70 + (x + z) / 3;
            }
        }

        for (int z = 0; z < 3; z++) {
            for (int x = 0; x < 3; x++) {
                set(heights, x, z, 50);
            }
        }

        List<FoundationLotSearch.Lot> lots = FoundationLotSearch.candidates(heights, SIDE, SIDE, 3, 3);
        FoundationLotSearch.Lot first = lots.get(0);

        assertEquals(0, first.spread());
        assertEquals(0, first.offsetX());
        assertEquals(0, first.offsetZ());
        assertEquals(50, first.floor());
    }

    @Test
    void withoutAFlatLotTheMostLevelOneWinsAndItsFloorIsTheMedian() {
        int[] heights = new int[SIDE * SIDE];

        for (int z = 0; z < SIDE; z++) {
            for (int x = 0; x < SIDE; x++) {
                heights[z * SIDE + x] = 70 + x % 2; // ondulação de 1 bloco
            }
        }

        FoundationLotSearch.Lot first = FoundationLotSearch.candidates(heights, SIDE, SIDE, 3, 3).get(0);

        assertEquals(1, first.spread());
        assertTrue(first.floor() == 70 || first.floor() == 71);
        assertEquals(0, first.ring(), "entre lotes de mesmo desnível, o do centro vem primeiro");
    }

    @Test
    void aSlopeSteeperThanTwoBlocksIsNeverACandidate() {
        int[] heights = new int[SIDE * SIDE];

        for (int z = 0; z < SIDE; z++) {
            for (int x = 0; x < SIDE; x++) {
                heights[z * SIDE + x] = 70 + 2 * x;
            }
        }

        assertTrue(FoundationLotSearch.candidates(heights, SIDE, SIDE, 3, 3).isEmpty());
    }

    @Test
    void aLotTouchingAnUnloadedColumnIsNotACandidate() {
        int[] heights = flat(64);
        set(heights, 4, 4, FoundationLotSearch.UNKNOWN);

        List<FoundationLotSearch.Lot> lots = FoundationLotSearch.candidates(heights, SIDE, SIDE, 3, 3);

        assertTrue(lots.stream().noneMatch(lot -> lot.offsetX() <= 4 && lot.offsetX() + 2 >= 4
                && lot.offsetZ() <= 4 && lot.offsetZ() + 2 >= 4));
        assertEquals(49 - 9, lots.size());
    }

    @Test
    void onFlatGroundTheLotInTheMiddleComesFirst() {
        FoundationLotSearch.Lot first = FoundationLotSearch.candidates(flat(64), SIDE, SIDE, 3, 3).get(0);

        assertEquals(0, first.ring());
        assertEquals(3, first.offsetX());
        assertEquals(3, first.offsetZ());
    }
}
