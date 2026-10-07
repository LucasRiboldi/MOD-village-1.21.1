package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** No fundo, a mina sobe em rampa para longe do centro da vila (ADR-036 17). */
class MineRampTest {

    private static final ColonyPos FLOOR = new ColonyPos(100, -50, 200);

    @Test
    void eachRampStepGoesOneForwardAndOneUp() {
        MineShaft ramp = MineShaft.ramp(FLOOR, Side.EAST, 20);

        for (int step = 1; step <= 20; step++) {
            ColonyPos feet = ramp.positionAt((step - 1) * MineShaft.STAIR_STEP_BLOCKS);

            assertEquals(FLOOR.x() + step, feet.x(), "degrau " + step + " anda um para leste");
            assertEquals(FLOOR.y() + step, feet.y(), "degrau " + step + " sobe um");
            assertEquals(FLOOR.z(), feet.z());
        }
    }

    @Test
    void theFirstStepIsAtTheHallLevel() {
        // O salão tem os pés em floor.y + 1, como em MineShaft.search.
        assertEquals(FLOOR.y() + 1, MineShaft.ramp(FLOOR, Side.NORTH, 5).positionAt(0).y());
    }

    @Test
    void theRampEndsAfterItsStepsAndNeverDeepens() {
        MineShaft ramp = MineShaft.ramp(FLOOR, Side.SOUTH, 7);
        int blocks = 7 * MineShaft.STAIR_STEP_BLOCKS;

        assertFalse(ramp.beyondTheArm(blocks - 1));
        assertTrue(ramp.beyondTheArm(blocks));
        assertFalse(ramp.mayDeepen());
        assertEquals(blocks, ramp.plannedCells().size());
    }

    @Test
    void theRampPointsAwayFromTheVillageCentre() {
        ColonyPos centre = new ColonyPos(0, 64, 0);

        assertEquals(Side.EAST, Side.awayFrom(centre, new ColonyPos(30, -40, 5), Side.NORTH));
        assertEquals(Side.NORTH, Side.awayFrom(centre, new ColonyPos(3, -40, -30), Side.EAST));
        assertEquals(Side.WEST, Side.awayFrom(centre, new ColonyPos(0, -40, 0), Side.WEST),
                "sem rumo, vale o de reserva");
    }

    @Test
    void anExhaustedMineTurnsIntoARampWithOneBranch() {
        Mine mine = Mine.open(UUID.randomUUID(), MineShaft.from(new ColonyPos(0, 60, 0), Side.NORTH));

        mine.climbOut(FLOOR, Side.WEST, 12);

        assertTrue(mine.shaft().isRamp());
        assertEquals(1, mine.branchesOpenNow());
        assertEquals(0, mine.arm(0).cut());

        mine.arm(0).finish();

        assertEquals(Mine.LevelAdvance.EXHAUSTED, mine.advanceIfEveryOpenArmIsDone(),
                "a rampa acabada não desce: o fundo segue para a boca do outro lado");
    }
}
