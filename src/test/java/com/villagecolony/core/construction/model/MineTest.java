package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contratos de avanço e divisão dos quatro ramais. */
class MineTest {

    private static final ColonyPos ENTRY = new ColonyPos(100, 64, 200);

    private static Mine opened() {
        return Mine.open(UUID.randomUUID(), MineShaft.from(ENTRY, Side.NORTH));
    }

    @Test
    void onlyOneMinerGetsTheSharedSpiralAndSearchArea() {
        Mine mine = opened();

        assertEquals(1, mine.branchesOpenNow());

        for (int index = 0; index < MineShaft.SHARED_BLOCKS; index++) {
            mine.arm(0).nextPosition();
        }

        assertEquals(Mine.ARMS, mine.branchesOpenNow());
    }

    @Test
    void fourArmsStartAtDifferentStaircasesAfterTheSharedArea() {
        Mine mine = opened();

        for (int index = 0; index < MineShaft.SHARED_BLOCKS; index++) {
            mine.arm(0).nextPosition();
        }

        for (int arm = 1; arm < Mine.ARMS; arm++) {
            assertNotEquals(mine.arm(0).shaft().positionAt(MineShaft.SHARED_BLOCKS),
                    mine.arm(arm).shaft().positionAt(MineShaft.SHARED_BLOCKS));
        }
    }

    @Test
    void closingTheSharedPathAdvancesTheWholeMineTogether() {
        Mine mine = opened();
        int before = mine.shaft().positionAt(MineShaft.CARVED).y();

        mine.arm(0).finish();

        assertEquals(Mine.LevelAdvance.DEEPENED, mine.advanceIfEveryOpenArmIsDone());
        assertTrue(mine.shaft().positionAt(MineShaft.CARVED).y() < before);

        for (MineArm arm : mine.arms()) {
            assertFalse(arm.isDone());
            assertEquals(0, arm.cut());
        }
    }

    @Test
    void theFourthFinishedArmStartsTheNextCycle() {
        Mine mine = opened();

        for (int index = 0; index < MineShaft.SHARED_BLOCKS; index++) {
            mine.arm(0).nextPosition();
        }

        for (MineArm arm : mine.arms()) {
            arm.finish();
        }

        assertEquals(Mine.LevelAdvance.DEEPENED, mine.advanceIfEveryArmIsDone());
    }

    @Test
    void theLowestCycleIsExhaustedWithoutRestartingAtTheSameMouth() {
        Mine mine = Mine.open(
                UUID.randomUUID(),
                MineShaft.from(
                        new ColonyPos(40, MineShaft.DEEPEST + MineShaft.DESCENT, 0),
                        Side.EAST));
        Side descent = mine.shaft().descent();

        for (MineArm arm : mine.arms()) {
            arm.finish();
        }

        assertEquals(Mine.LevelAdvance.EXHAUSTED, mine.advanceIfEveryArmIsDone());
        assertEquals(descent, mine.shaft().descent());
        assertTrue(mine.everyArmIsDone());
    }

    @Test
    void eachArmStopsAfterItsConfiguredFiniteWork() {
        MineArm arm = opened().arm(0);

        while (!arm.reachedTheEndOfTheArm()) {
            arm.nextPosition();
        }

        assertEquals(MineShaft.SHARED_BLOCKS + MineShaft.ARM_BLOCKS, arm.cut());
    }

    @Test
    void aSavedMineKeepsEveryArmCursor() {
        int[] cuts = {50, 40, 30, 20};
        Mine mine = Mine.restore(UUID.randomUUID(), MineShaft.from(ENTRY, Side.NORTH), cuts);

        assertEquals(4, mine.cuts().length);
        for (int arm = 0; arm < cuts.length; arm++) {
            assertEquals(cuts[arm], mine.arm(arm).cut());
        }
    }
}
