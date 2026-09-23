package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A repetição vertical preserva a forma e desce só pelo caracol central. */
class MineSecondLevelTest {

    @Test
    void theNextCyclePreservesEveryArmDirection() {
        MineShaft first = MineShaft.from(new ColonyPos(0, 64, 0), Side.WEST);
        MineShaft second = first.deepened();

        assertEquals(first.gallery(), second.gallery());
        assertEquals(first.descent(), second.descent());
        assertEquals(first.positionAt(MineShaft.SHARED_BLOCKS).x(),
                second.positionAt(MineShaft.SHARED_BLOCKS).x());
        assertEquals(first.positionAt(MineShaft.SHARED_BLOCKS).z(),
                second.positionAt(MineShaft.SHARED_BLOCKS).z());
    }

    @Test
    void aCycleThatStillFitsCanDescendOnce() {
        MineShaft shaft = MineShaft.from(
                new ColonyPos(0, MineShaft.DEEPEST + 2 * MineShaft.DESCENT, 0), Side.SOUTH);

        assertTrue(shaft.mayDeepen());
    }
}
