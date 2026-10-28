package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A repetição vertical preserva a forma e desce pelo salão do ramal zero (Regra 119). */
class MineSecondLevelTest {

    /** Metade do lado do salão: as coordenadas do traçado crescem com ela. */
    private static final int H = MineShaft.HALL_SIDE / 2;

    @Test
    void theNextCyclePreservesEveryArmDirection() {
        MineShaft first = MineShaft.from(new ColonyPos(0, 64, 0), Side.WEST);
        MineShaft second = first.deepened();

        assertEquals(first.gallery(), second.gallery());
        assertEquals(first.descent(), second.descent());
        // Regra 119: o nível seguinte desce do meio do salão do ramal zero, duas escadas abaixo.
        assertEquals(MineShaft.DESCENT + MineShaft.ARM_STAIRS, first.entry().y() - second.entry().y());
        assertEquals(new ColonyPos(2 * H, 40, -H), second.entry());
    }

    @Test
    void aCycleThatStillFitsCanDescendOnce() {
        MineShaft shaft = MineShaft.from(
                new ColonyPos(0, MineShaft.DEEPEST + 2 * MineShaft.DESCENT + MineShaft.ARM_STAIRS, 0), Side.SOUTH);
        MineShaft tooDeep = MineShaft.from(
                new ColonyPos(0, MineShaft.DEEPEST + 2 * MineShaft.DESCENT + MineShaft.ARM_STAIRS - 1, 0), Side.SOUTH);

        assertTrue(shaft.mayDeepen());
        org.junit.jupiter.api.Assertions.assertFalse(tooDeep.mayDeepen());
    }
}
