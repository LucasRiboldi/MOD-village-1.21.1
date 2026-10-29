package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contratos da sequência finita de uma mina. */
class MineShaftTest {

    /** Metade do lado do salão: as coordenadas do traçado crescem com ela. */
    private static final int H = MineShaft.HALL_SIDE / 2;

    private static final ColonyPos ENTRY = new ColonyPos(100, 64, 200);

    private static MineShaft shaft() {
        return MineShaft.from(ENTRY, Side.NORTH);
    }

    @Test
    void theSharedSpiralIsOneTurnOfFourShortFlights() {
        MineShaft shaft = shaft();
        int blocksPerStep = MineShaft.STAIR_HEADROOM * MineShaft.STAIR_LANES;

        assertEquals(4, MineShaft.HELIX_FLIGHTS);
        assertEquals(3, MineShaft.HELIX_SIDE);
        assertEquals(12, MineShaft.DESCENT);
        assertEquals(12 * blocksPerStep, MineShaft.CARVED);
        assertEquals(ENTRY.y() - MineShaft.DESCENT + 1,
                shaft.positionAt(MineShaft.CARVED - blocksPerStep).y());
    }

    @Test
    void eachStairStepHasThreeLanesAndThreeLayers() {
        MineShaft shaft = shaft();
        Set<ColonyPos> firstStep = new HashSet<>();

        for (int index = 0; index < 9; index++) {
            firstStep.add(shaft.positionAt(index));
        }

        assertEquals(3, MineShaft.STAIR_LANES);
        assertEquals(Set.of(
                new ColonyPos(100, 64, 199),
                new ColonyPos(100, 65, 199),
                new ColonyPos(100, 66, 199),
                new ColonyPos(99, 64, 199),
                new ColonyPos(99, 65, 199),
                new ColonyPos(99, 66, 199),
                new ColonyPos(98, 64, 199),
                new ColonyPos(98, 65, 199),
                new ColonyPos(98, 66, 199)), firstStep);
        assertFalse(firstStep.contains(new ColonyPos(97, 64, 199)));
    }

    /**
     * Um nível inteiro — o caracol e o salão do nível, e o caracol e o salão de cada um dos quatro ramais — cava no
     * máximo 1.800 blocos (autor, 2026-10-28: com o salão de 17 eram 4.875, e a mina descia um nível em 10 dias).
     */
    @Test
    void aWholeLevelDigsAtMostEighteenHundredBlocks() {
        int level = MineShaft.SHARED_BLOCKS + Mine.ARMS * MineShaft.ARM_BLOCKS;

        assertTrue(level <= 1_800, "um nível cava " + level + " blocos");
    }

    @Test
    void theSharedHallIsNineByNineByThree() {
        MineShaft shaft = shaft();
        Set<ColonyPos> area = new HashSet<>();

        for (int index = MineShaft.CARVED; index < MineShaft.SHARED_BLOCKS; index++) {
            area.add(shaft.positionAt(index));
        }

        assertEquals(9 * 9 * 3, MineShaft.SEARCH_AREA_BLOCKS);
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
    void eachArmHasASpiralAndAHallAsLargeAsTheLevels() {
        MineShaft shaft = shaft();
        int blocksPerStep = MineShaft.STAIR_HEADROOM * MineShaft.STAIR_LANES;
        int armStairBlocks = MineShaft.ARM_STAIRS * blocksPerStep;

        assertEquals(12, MineShaft.ARM_STAIRS);
        assertEquals(MineShaft.SEARCH_AREA_BLOCKS, MineShaft.ARM_AREA_BLOCKS);
        assertFalse(shaft.beyondTheArm(MineShaft.SHARED_BLOCKS + MineShaft.ARM_BLOCKS - 1));
        assertTrue(shaft.beyondTheArm(MineShaft.SHARED_BLOCKS + MineShaft.ARM_BLOCKS));
        assertTrue(shaft.positionAt(MineShaft.SHARED_BLOCKS + armStairBlocks).y()
                        < shaft.positionAt(MineShaft.SHARED_BLOCKS).y(),
                "a área do ramal precisa começar abaixo da sua escada");
    }

    /** O nível seguinte desce do meio do salão do ramal zero: duas escadas abaixo. */
    @Test
    void aDeeperCycleStartsTwoSpiralsBelowThePreviousOne() {
        MineShaft shaft = shaft();
        MineShaft deeper = shaft.deepened();

        assertEquals(shaft.positionAt(MineShaft.CARVED).y() - MineShaft.DESCENT - MineShaft.ARM_STAIRS,
                deeper.positionAt(MineShaft.CARVED).y());
        assertEquals(new ColonyPos(100 + H, 40, 200 + 2 * H), deeper.entry());
        assertEquals(shaft.descent(), deeper.descent());
    }

    /**
     * Onde cada índice cai — o cursor da mina é salvo como índice, então {@code positionAt} é contrato
     * com o save. Valores tirados à mão da geometria (Regra 119): entrada (100, 64, 200), descida ao
     * norte; degrau = um à frente e um abaixo; pistas à esquerda de quem desce; vira à direita a cada
     * três degraus e fecha a volta doze blocos abaixo, na coluna da entrada.
     */
    @Test
    void theSpiralLandsWhereTheGeometrySays() {
        MineShaft shaft = shaft();

        // Norte: pistas a oeste.
        assertEquals(new ColonyPos(100, 64, 199), shaft.positionAt(0));
        assertEquals(new ColonyPos(100, 66, 199), shaft.positionAt(2));
        assertEquals(new ColonyPos(99, 64, 199), shaft.positionAt(3));
        assertEquals(new ColonyPos(100, 63, 198), shaft.positionAt(9));
        assertEquals(new ColonyPos(98, 62, 197), shaft.positionAt(24));
        // Leste, de (100, 61, 197): pistas ao norte.
        assertEquals(new ColonyPos(101, 61, 197), shaft.positionAt(27));
        assertEquals(new ColonyPos(101, 61, 196), shaft.positionAt(30));
        assertEquals(new ColonyPos(103, 59, 195), shaft.positionAt(51));
        // Sul, de (103, 58, 197): pistas a leste.
        assertEquals(new ColonyPos(103, 58, 198), shaft.positionAt(54));
        assertEquals(new ColonyPos(103, 56, 200), shaft.positionAt(72));
        // Oeste, de (103, 55, 200): o último degrau cai na coluna da entrada.
        assertEquals(new ColonyPos(102, 55, 200), shaft.positionAt(81));
        assertEquals(new ColonyPos(100, 53, 200), shaft.positionAt(99));
    }

    /**
     * O salão do nível, com o meio em (91, 52, 200): a primeira fileira encosta no pé do caracol, e
     * cada fileira alterna os lados do meio em três camadas.
     */
    @Test
    void theSharedRoomAlternatesSidesFromTheCentre() {
        MineShaft shaft = shaft();
        int room = MineShaft.CARVED;

        assertEquals(new ColonyPos(99, 53, 200), shaft.positionAt(room));
        assertEquals(new ColonyPos(99, 54, 200), shaft.positionAt(room + 1));
        assertEquals(new ColonyPos(99, 55, 200), shaft.positionAt(room + 2));
        assertEquals(new ColonyPos(99, 53, 199), shaft.positionAt(room + 3));
        assertEquals(new ColonyPos(99, 53, 201), shaft.positionAt(room + 6));
        assertEquals(new ColonyPos(98, 53, 200), shaft.positionAt(room + 3 * MineShaft.HALL_SIDE));
        assertEquals(new ColonyPos(99 - 2 * H, 55, 200 + H), shaft.positionAt(room + MineShaft.SEARCH_AREA_BLOCKS - 1));
    }

    /**
     * Cada ramal sai de um canto do salão, desce o caracol dele para fora e abre o salão no quadrante
     * diagonal daquele canto. Dois ramais, um em cada eixo.
     */
    @Test
    void eachArmLeavesFromACornerOfTheRoom() {
        MineShaft south = shaft();
        MineShaft west = south.turned();
        int arm = MineShaft.SHARED_BLOCKS;
        int armRoom = arm + MineShaft.ARM_STAIRS * MineShaft.STAIR_STEP_BLOCKS;
        int last = armRoom + MineShaft.ARM_AREA_BLOCKS - 1;

        assertEquals(Side.SOUTH, south.gallery());
        assertEquals(Side.WEST, west.gallery());

        // Sul: canto (99, 52, 200 + H), pistas a leste, salão a leste e ao sul.
        assertEquals(new ColonyPos(99, 52, 201 + H), south.positionAt(arm));
        assertEquals(new ColonyPos(100, 52, 201 + H), south.positionAt(arm + 3));
        assertEquals(new ColonyPos(99, 43, 198 + H), south.positionAt(armRoom - 1));
        assertEquals(new ColonyPos(100, 41, 200 + H), south.positionAt(armRoom));
        assertEquals(new ColonyPos(99 + MineShaft.HALL_SIDE, 43, 200 + 3 * H), south.positionAt(last));

        // Oeste: canto (99 - 2H, 52, 200 + H), pistas ao sul, salão ao sul e a oeste.
        assertEquals(new ColonyPos(98 - 2 * H, 52, 200 + H), west.positionAt(arm));
        assertEquals(new ColonyPos(98 - 2 * H, 52, 201 + H), west.positionAt(arm + 3));
        assertEquals(new ColonyPos(99 - 2 * H, 41, 201 + H), west.positionAt(armRoom));
        assertEquals(new ColonyPos(99 - 4 * H, 43, 201 + 3 * H), west.positionAt(last));
    }

    @Test
    void thereIsNoPositionPastTheEndOfTheArm() {
        MineShaft shaft = shaft();
        int end = MineShaft.SHARED_BLOCKS + MineShaft.ARM_BLOCKS;

        assertThrows(IllegalArgumentException.class, () -> shaft.positionAt(end));
        assertThrows(IllegalArgumentException.class, () -> shaft.positionAt(-1));
    }

    @Test
    void theLastSafeCycleDoesNotPlanBelowTheWorldBottom() {
        MineShaft last = MineShaft.from(
                new ColonyPos(0, MineShaft.DEEPEST + MineShaft.DESCENT, 0), Side.EAST);

        assertFalse(last.mayDeepen());
        assertTrue(MineShaft.from(new ColonyPos(0, 0, 0), Side.EAST).mayDeepen());
    }

    // --- as células que a mina planeja abrir, 2026-09-25 (ADR-025) ---

    /**
     * O conjunto de células planejadas tem o caracol, a sala e o ramal — e
     * não tem o piso embaixo deles, que é onde um vão vira queda.
     */
    @Test
    void thePlannedCellsAreTheShaftAndNotTheFloorUnderIt() {
        java.util.Set<ColonyPos> cells = shaft().plannedCells();

        assertTrue(cells.contains(new ColonyPos(100, 64, 199)), "o primeiro degrau");
        assertTrue(cells.contains(new ColonyPos(99, 53, 200)), "o salão comum");
        assertTrue(cells.contains(new ColonyPos(99 + MineShaft.HALL_SIDE, 43, 200 + 3 * H)), "o salão do ramal");
        assertFalse(cells.contains(new ColonyPos(100, 63, 199)), "a rocha sob o primeiro degrau");
        assertFalse(cells.contains(new ColonyPos(99, 52, 200)), "o piso do salão");
        // O caracol fica fora das colunas do salão, e o ramal fora das dele: nada se repete.
        assertEquals(MineShaft.SHARED_BLOCKS + MineShaft.ARM_BLOCKS, cells.size(),
                "o conjunto não tem repetição e não perde célula");
    }
}
