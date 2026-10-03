package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    // --- o que o save devolve e a troca de rota, 2026-09-25 (sobreviventes do PIT) ---

    private static final MineShaft SHAFT = MineShaft.from(ENTRY, Side.NORTH);

    /** Mina gravada antes de a primeira picareta cair começa em zero, e isso vale. */
    @Test
    void aMineSavedBeforeTheFirstBlockRestores() {
        assertEquals(0, Mine.restore(UUID.randomUUID(), SHAFT, 0).arm(0).cut());
        assertThrows(IllegalArgumentException.class,
                () -> Mine.restore(UUID.randomUUID(), SHAFT, -1));
    }

    /** A fronteira de cada ramal volta ao disco como veio dele. */
    @Test
    void theCutsGoBackToDiskAsTheyCame() {
        int[] saved = {70, 5, 6, 7};

        assertArrayEquals(saved, Mine.restore(UUID.randomUUID(), SHAFT, saved).cuts());
    }

    /** Duas voltas sem picareta são azar; a terceira manda trocar de rota. */
    @Test
    void theThirdTurnWithoutAPickaxeAsksForANewRoute() {
        Mine mine = opened();

        assertFalse(mine.turnedWithoutAPickaxe());
        assertFalse(mine.turnedWithoutAPickaxe());
        assertTrue(mine.turnedWithoutAPickaxe());
    }

    /**
     * Trocar de rota reabre os quatro ramais do zero na hélice nova, cada
     * um virado um passo em relação ao anterior, e conta a tentativa.
     */
    @Test
    void reroutingRestartsEveryArmOnTheNewHelix() {
        Mine mine = Mine.restore(UUID.randomUUID(), SHAFT, new int[] {70, 5, 6, 7});
        mine.turnedWithoutAPickaxe();

        mine.reroute();

        MineShaft heading = SHAFT.rerouted();

        for (int index = 0; index < 4; index++) {
            assertEquals(0, mine.arm(index).cut(), "ramal " + index + " não recomeçou");
            assertEquals(heading, mine.arm(index).shaft(), "ramal " + index + " na hélice errada");
            heading = heading.turned();
        }

        assertEquals(1, mine.helicesTried());
        assertEquals(0, mine.turnsWithoutAPickaxe());
    }

    /** Mina recém-aberta não desce por nenhuma das duas portas. */
    @Test
    void aFreshMineDoesNotDeepen() {
        Mine mine = opened();
        MineShaft before = mine.shaft();

        assertEquals(Mine.LevelAdvance.WAITING, mine.advanceIfEveryArmIsDone());
        assertFalse(mine.deepenIfEveryArmIsDone());
        assertEquals(Mine.LevelAdvance.WAITING, mine.advanceIfEveryOpenArmIsDone());
        assertEquals(before, mine.shaft());
    }

    /** As células planejadas da mina somam os quatro ramais. */
    @Test
    void thePlannedCellsCoverEveryArm() {
        java.util.Set<ColonyPos> cells = opened().plannedCells();

        assertTrue(cells.contains(new ColonyPos(100, 55, 206)), "o ramal do sul");
        assertTrue(cells.contains(new ColonyPos(94, 55, 200)), "o ramal do oeste");
        assertTrue(cells.contains(new ColonyPos(100, 64, 199)), "o caracol comum");
    }
}
