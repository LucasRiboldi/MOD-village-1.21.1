package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O alcance do fazendeiro inclui a borda: a exatamente 3 blocos (2, 1, 2) ele colhe. */
class FarmerReachTest {

    private static final BlockPos CROP = new BlockPos(158, 77, -491);

    @Test
    void exactlyThreeBlocksAwayStillReachesTheCrop() {
        assertTrue(FarmerWork.reaches(new BlockPos(160, 76, -493), CROP));
    }

    @Test
    void moreThanThreeBlocksAwayDoesNotReach() {
        assertFalse(FarmerWork.reaches(new BlockPos(161, 76, -493), CROP));
    }
}
