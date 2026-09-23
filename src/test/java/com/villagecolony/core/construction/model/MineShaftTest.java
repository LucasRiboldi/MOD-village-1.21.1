package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contratos da sequência finita de uma mina. */
class MineShaftTest {

    private static final ColonyPos ENTRY = new ColonyPos(100, 64, 200);

    private static MineShaft shaft() {
        return MineShaft.from(ENTRY, Side.NORTH);
    }

    @Test
    void theSharedSpiralHasExactlyTenSteps() {
        MineShaft shaft = shaft();
        int blocksPerStep = MineShaft.STAIR_HEADROOM * MineShaft.STAIR_LANES;

        assertEquals(10, MineShaft.DESCENT);
        assertEquals(10 * blocksPerStep, MineShaft.CARVED);
        assertEquals(ENTRY.y() - MineShaft.DESCENT + 1,
                shaft.positionAt(MineShaft.CARVED - blocksPerStep).y());
    }

    @Test
    void theSharedSearchAreaHasFiftyDistinctBlocks() {
        MineShaft shaft = shaft();
        Set<ColonyPos> area = new HashSet<>();

        for (int index = MineShaft.CARVED; index < MineShaft.SHARED_BLOCKS; index++) {
            area.add(shaft.positionAt(index));
        }

        assertEquals(50, MineShaft.SEARCH_AREA_BLOCKS);
        assertEquals(MineShaft.SEARCH_AREA_BLOCKS, area.size());
    }

    @Test
    void allArmsShareTheSpiralAndSearchBeforeTheySplit() {
        MineShaft first = shaft();
        MineShaft turned = first.turned();

        for (int index = 0; index < MineShaft.SHARED_BLOCKS; index++) {
            assertEquals(first.positionAt(index), turned.positionAt(index));
        }

        assertNotEquals(first.positionAt(MineShaft.SHARED_BLOCKS),
                turned.positionAt(MineShaft.SHARED_BLOCKS));
    }

    @Test
    void eachArmHasTenStairStepsAndFiftySearchBlocks() {
        MineShaft shaft = shaft();
        int blocksPerStep = MineShaft.STAIR_HEADROOM * MineShaft.STAIR_LANES;
        int armStairBlocks = MineShaft.ARM_STAIRS * blocksPerStep;

        assertEquals(10, MineShaft.ARM_STAIRS);
        assertEquals(50, MineShaft.ARM_AREA_BLOCKS);
        assertFalse(shaft.beyondTheArm(MineShaft.SHARED_BLOCKS + MineShaft.ARM_BLOCKS - 1));
        assertTrue(shaft.beyondTheArm(MineShaft.SHARED_BLOCKS + MineShaft.ARM_BLOCKS));
        assertTrue(shaft.positionAt(MineShaft.SHARED_BLOCKS + armStairBlocks).y()
                        < shaft.positionAt(MineShaft.SHARED_BLOCKS).y(),
                "a área do ramal precisa começar abaixo da sua escada");
    }

    @Test
    void aDeeperCycleStartsTenBlocksBelowThePreviousOne() {
        MineShaft shaft = shaft();
        MineShaft deeper = shaft.deepened();

        assertEquals(shaft.positionAt(MineShaft.CARVED).y() - MineShaft.DESCENT,
                deeper.positionAt(MineShaft.CARVED).y());
        assertEquals(shaft.descent(), deeper.descent());
    }

    @Test
    void theLastSafeCycleDoesNotPlanBelowTheWorldBottom() {
        MineShaft last = MineShaft.from(
                new ColonyPos(0, MineShaft.DEEPEST + MineShaft.DESCENT, 0), Side.EAST);

        assertFalse(last.mayDeepen());
    }
}
