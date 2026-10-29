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

    /** Metade do lado do salão: as coordenadas do traçado crescem com ela. */
    private static final int H = MineShaft.HALL_SIDE / 2;

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

    /** Aberta a escada, os outros três ramais entram pulando a sala comum — regra do autor, 2026-10-11. */
    @Test
    void theOtherBranchesOpenOnceTheStairIsCutAndSkipTheSharedHall() {
        Mine mine = opened();

        for (int index = 0; index < MineShaft.CARVED - 1; index++) {
            mine.arm(0).nextPosition();
        }

        assertEquals(1, mine.branchesOpenNow(), "a escada ainda é de um mineiro só");

        mine.arm(0).nextPosition();
        assertEquals(Mine.ARMS, mine.branchesOpenNow(), "aberta a escada, os quatro ramais abrem");

        mine.skipTheSharedHall();
        java.util.Set<ColonyPos> hall = new java.util.HashSet<>();

        for (int index = 0; index < MineShaft.SHARED_BLOCKS; index++) {
            hall.add(mine.arm(0).shaft().positionAt(index));
        }

        for (int arm = 1; arm < Mine.ARMS; arm++) {
            assertEquals(MineShaft.SHARED_BLOCKS, mine.arm(arm).cut(), "o ramal " + arm + " devia pular a sala");
            assertFalse(hall.contains(mine.arm(arm).nextPosition()), "o ramal " + arm + " recebeu bloco da sala comum");
        }
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

        for (int index = 0; index < MineShaft.SHARED_BLOCKS; index++) {
            mine.arm(0).nextPosition();
        }

        mine.arm(0).finish();

        assertEquals(Mine.LevelAdvance.DEEPENED, mine.advanceIfEveryOpenArmIsDone());
        assertTrue(mine.shaft().positionAt(MineShaft.CARVED).y() < before);

        for (MineArm arm : mine.arms()) {
            assertFalse(arm.isDone());
            assertEquals(0, arm.cut());
        }
    }

    /**
     * O ramal zero que acaba depois de cavar além do trecho comum não fecha os
     * outros três — P6, playtest de 2026-10-11: dois mineiros esperavam
     * "branches 0 open" com três ramais por cavar, e a mina descia sem eles.
     */
    @Test
    void theZeroArmFinishedPastTheSharedPathKeepsTheOtherBranchesOpen() {
        Mine mine = opened();

        for (int index = 0; index < MineShaft.SHARED_BLOCKS + 3; index++) {
            mine.arm(0).nextPosition();
        }

        mine.arm(0).finish();

        assertEquals(Mine.ARMS, mine.branchesOpenNow());
        assertEquals(Mine.LevelAdvance.WAITING, mine.advanceIfEveryOpenArmIsDone(),
                "a mina desceu com três ramais por cavar");
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

    /**
     * Uma volta completa sem passar do salão muda a escada de ponto — decisão do autor de 2026-10-22.
     * No deserto as dez hélices giraram em volta da mesma entrada e bateram na mesma água.
     */
    @Test
    void aFullTurnWithoutTheHallMovesTheStairToAnotherPoint() {
        Mine mine = Mine.open(UUID.randomUUID(), SHAFT);

        for (int turn = 1; turn < MineStairPoints.TURNS; turn++) {
            mine.reroute();
            assertEquals(ENTRY, mine.entry(), "a hélice " + turn + " devia girar no mesmo ponto");
        }

        mine.reroute();

        assertEquals(1, mine.stairShifts());
        assertEquals(MineStairPoints.SHIFT, Math.abs(mine.entry().x() - ENTRY.x()) + Math.abs(mine.entry().z() - ENTRY.z()),
                "a volta completa devia levar a escada a " + MineStairPoints.SHIFT + " blocos, e foi para " + mine.entry());
        assertEquals(ENTRY.y(), mine.entry().y(), "a escada nova começa no mesmo nível");
    }

    /** Os pontos novos são todos diferentes, e só acabam junto com as partidas. */
    @Test
    void everyShiftIsANewPointUntilTheMouthIsBlamed() {
        Mine mine = Mine.open(UUID.randomUUID(), SHAFT);
        java.util.Set<ColonyPos> points = new java.util.HashSet<>(java.util.List.of(ENTRY));

        for (int helix = 1; helix <= Mine.HELICES_BEFORE_BLAMING_THE_MOUTH; helix++) {
            assertFalse(mine.mouthIsHopeless(), "culpou a boca antes da partida " + helix);
            mine.reroute();
            points.add(mine.entry());
        }

        assertTrue(mine.mouthIsHopeless());
        assertEquals(MineStairPoints.SHIFTS, mine.stairShifts());
        assertEquals(MineStairPoints.SHIFTS + 1, points.size(), "pontos repetidos: " + points);
    }

    /** A rampa não muda de ponto: ela já é a saída do fundo. */
    @Test
    void aRampOnlyTurns() {
        Mine mine = Mine.open(UUID.randomUUID(), MineShaft.ramp(ENTRY, Side.NORTH, 5));

        for (int helix = 0; helix < MineStairPoints.TURNS * 2; helix++) {
            mine.reroute();
        }

        assertEquals(0, mine.stairShifts());
        assertEquals(ENTRY, mine.entry());
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

        assertTrue(cells.contains(new ColonyPos(99, 52, 201 + H)), "o ramal do sul");
        assertTrue(cells.contains(new ColonyPos(98 - 2 * H, 52, 200 + H)), "o ramal do oeste");
        assertTrue(cells.contains(new ColonyPos(100, 64, 199)), "o caracol comum");
    }

    /** A escada que fecha antes do salão não é nível feito: a mina não desce (ADR-037 M1). */
    @Test
    void aStairClosedBeforeTheHallDoesNotDeepen() {
        Mine mine = opened();
        MineShaft before = mine.shaft();

        mine.arm(0).nextPosition();
        mine.arm(0).finish();

        assertEquals(Mine.LevelAdvance.BLOCKED, mine.advanceIfEveryOpenArmIsDone());
        assertEquals(before, mine.shaft(), "a mina desceu sem ter cavado a escada");
    }
}
