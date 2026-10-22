package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * As partidas da escada que insistem no nível (0.3.87): a entrada original deslocada para leste, oeste, sul
 * e norte, e depois de novo, duas vezes mais longe.
 */
class MineStairPointsTest {

    private static final ColonyPos ORIGIN = new ColonyPos(100, 40, -200);

    @Test
    void theFirstFourShiftsGoEastWestSouthAndNorth() {
        int step = MineStairPoints.SHIFT;

        assertEquals(new ColonyPos(100 + step, 40, -200), MineStairPoints.shiftedEntry(ORIGIN, 1));
        assertEquals(new ColonyPos(100 - step, 40, -200), MineStairPoints.shiftedEntry(ORIGIN, 2));
        assertEquals(new ColonyPos(100, 40, -200 + step), MineStairPoints.shiftedEntry(ORIGIN, 3));
        assertEquals(new ColonyPos(100, 40, -200 - step), MineStairPoints.shiftedEntry(ORIGIN, 4));
    }

    @Test
    void theNextRoundGoesTwiceAsFar() {
        int step = MineStairPoints.SHIFT;

        assertEquals(new ColonyPos(100 + 2 * step, 40, -200), MineStairPoints.shiftedEntry(ORIGIN, 5));
        assertEquals(new ColonyPos(100, 40, -200 - 2 * step), MineStairPoints.shiftedEntry(ORIGIN, 8));
    }
}
