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

    @Test
    void theSharedSearchAreaHasFortyFiveDistinctBlocks() {
        MineShaft shaft = shaft();
        Set<ColonyPos> area = new HashSet<>();

        for (int index = MineShaft.CARVED; index < MineShaft.SHARED_BLOCKS; index++) {
            area.add(shaft.positionAt(index));
        }

        assertEquals(45, MineShaft.SEARCH_AREA_BLOCKS, "cinco fileiras de 3 × 3");
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
    void eachArmHasTenStairStepsAndFortyFiveSearchBlocks() {
        MineShaft shaft = shaft();
        int blocksPerStep = MineShaft.STAIR_HEADROOM * MineShaft.STAIR_LANES;
        int armStairBlocks = MineShaft.ARM_STAIRS * blocksPerStep;

        assertEquals(10, MineShaft.ARM_STAIRS);
        assertEquals(45, MineShaft.ARM_AREA_BLOCKS, "cinco fileiras de 3 × 3");
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

    /**
     * Onde cada índice cai — 2026-09-25.
     *
     * <p>O cursor da mina é salvo como índice, então {@code positionAt} é
     * contrato com o save: mudar a conta muda onde uma mina já aberta cava.
     * Os testes acima só contavam posições, e o PIT mostrou 37 mutações de
     * coordenada passando por eles.
     *
     * <p>Os valores foram tirados à mão da geometria, não da fórmula: entrada
     * (100, 64, 200), descida ao norte. Degrau = um passo à frente e um abaixo;
     * as segunda e terceira pistas ficam à esquerda de quem desce; três alturas
     * por degrau.
     * O caracol vira em sentido horário com lados 3, 2, 3, 2 e fecha dez blocos
     * abaixo da entrada, em (100, 54, 200).
     */
    @Test
    void theSpiralLandsWhereTheGeometrySays() {
        MineShaft shaft = shaft();

        // Primeira volta: norte, três degraus; pistas adicionais a oeste.
        assertEquals(new ColonyPos(100, 64, 199), shaft.positionAt(0));
        assertEquals(new ColonyPos(100, 66, 199), shaft.positionAt(2));
        assertEquals(new ColonyPos(99, 64, 199), shaft.positionAt(3));
        assertEquals(new ColonyPos(98, 64, 199), shaft.positionAt(6));
        assertEquals(new ColonyPos(100, 63, 198), shaft.positionAt(9));
        assertEquals(new ColonyPos(98, 62, 197), shaft.positionAt(24));

        // Segunda: leste, dois degraus, a partir de (100, 61, 197); pistas ao norte.
        assertEquals(new ColonyPos(101, 61, 197), shaft.positionAt(27));
        assertEquals(new ColonyPos(101, 61, 196), shaft.positionAt(30));
        assertEquals(new ColonyPos(102, 60, 195), shaft.positionAt(42));

        // Terceira: sul, três degraus, a partir de (102, 59, 197); pistas a leste.
        assertEquals(new ColonyPos(102, 59, 198), shaft.positionAt(45));
        assertEquals(new ColonyPos(103, 59, 198), shaft.positionAt(48));
        assertEquals(new ColonyPos(104, 57, 200), shaft.positionAt(69));

        // Quarta: oeste, dois degraus, a partir de (102, 56, 200); pistas ao sul.
        assertEquals(new ColonyPos(101, 56, 200), shaft.positionAt(72));
        assertEquals(new ColonyPos(101, 56, 201), shaft.positionAt(75));
        assertEquals(new ColonyPos(100, 55, 202), shaft.positionAt(87));
    }

    /**
     * A sala comum, a partir do piso (100, 54, 200), avança para o sul e
     * alterna os lados do centro: 0, oeste, leste, dois a oeste, dois a leste.
     */
    @Test
    void theSharedRoomAlternatesSidesFromTheCentre() {
        MineShaft shaft = shaft();
        int room = MineShaft.CARVED;

        assertEquals(new ColonyPos(100, 55, 200), shaft.positionAt(room));
        assertEquals(new ColonyPos(100, 56, 200), shaft.positionAt(room + 1));
        assertEquals(new ColonyPos(100, 57, 200), shaft.positionAt(room + 2), "três de altura");
        assertEquals(new ColonyPos(99, 55, 200), shaft.positionAt(room + 3));
        assertEquals(new ColonyPos(101, 55, 200), shaft.positionAt(room + 6));
        assertEquals(new ColonyPos(100, 55, 201), shaft.positionAt(room + 9), "três de largura: a fileira seguinte");
        assertEquals(new ColonyPos(101, 57, 204), shaft.positionAt(room + 44));
    }

    /**
     * O ramal sai da ponta da sala, cinco blocos adiante do piso, desce dez
     * degraus e abre a sala dele. Dois ramais, um em cada eixo: com só o do
     * sul, trocar a conta do x não mudaria nada.
     */
    @Test
    void eachArmLeavesFromTheFarEndOfTheRoom() {
        MineShaft south = shaft();
        MineShaft west = south.turned();
        int arm = MineShaft.SHARED_BLOCKS;
        int armRoom = arm + MineShaft.ARM_STAIRS * MineShaft.STAIR_STEP_BLOCKS;

        assertEquals(Side.SOUTH, south.gallery());
        assertEquals(Side.WEST, west.gallery());

        // Sul: topo em (100, 55, 205), pista a leste, sala em (100, 45, 215).
        assertEquals(new ColonyPos(100, 55, 206), south.positionAt(arm));
        assertEquals(new ColonyPos(101, 55, 206), south.positionAt(arm + 3));
        assertEquals(new ColonyPos(102, 48, 215), south.positionAt(armRoom - 1));
        assertEquals(new ColonyPos(100, 46, 215), south.positionAt(armRoom));
        assertEquals(new ColonyPos(101, 48, 219), south.positionAt(armRoom + 44));

        // Oeste: topo em (95, 55, 200), pista ao sul, sala em (85, 45, 200).
        assertEquals(new ColonyPos(94, 55, 200), west.positionAt(arm));
        assertEquals(new ColonyPos(94, 55, 201), west.positionAt(arm + 3));
        assertEquals(new ColonyPos(85, 46, 200), west.positionAt(armRoom));
        assertEquals(new ColonyPos(81, 48, 201), west.positionAt(armRoom + 44));
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
        assertTrue(cells.contains(new ColonyPos(100, 55, 200)), "a sala comum");
        assertTrue(cells.contains(new ColonyPos(101, 47, 219)), "a sala do ramal");
        assertFalse(cells.contains(new ColonyPos(100, 63, 199)), "a rocha sob o primeiro degrau");
        assertFalse(cells.contains(new ColonyPos(100, 54, 200)), "o piso da sala");
        // Os últimos degraus de cada escada caem dentro da sala que ela abre;
        // com a sala de 3 × 3 (03-10) são 21 índices repetindo células.
        assertEquals(MineShaft.SHARED_BLOCKS + MineShaft.ARM_BLOCKS - 21, cells.size(),
                "o conjunto não tem repetição e não perde célula");
    }

    /** Toda sala da mina é cortada em 3 × 3 — autor, 2026-10-03 (eram 5 de largura por 2 de altura). */
    @Test
    void everySearchRoomIsThreeWideAndThreeHigh() {
        MineShaft shaft = MineShaft.from(new ColonyPos(0, 60, 0), Side.NORTH);
        java.util.Set<Integer> heights = new java.util.HashSet<>();
        java.util.Set<Integer> across = new java.util.HashSet<>();

        for (int index = MineShaft.CARVED; index < MineShaft.SHARED_BLOCKS; index++) {
            ColonyPos cell = shaft.positionAt(index);

            heights.add(cell.y());
            across.add(cell.x());
        }

        assertEquals(3, heights.size(), "três de altura");
        assertEquals(3, across.size(), "três de largura (a sala anda para o sul, então a largura é em x)");
    }
}
