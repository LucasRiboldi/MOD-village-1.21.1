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
     * a segunda pista fica à esquerda de quem desce; três alturas por degrau.
     * O caracol vira em sentido horário com lados 3, 2, 3, 2 e fecha dez blocos
     * abaixo da entrada, em (100, 54, 200).
     */
    @Test
    void theSpiralLandsWhereTheGeometrySays() {
        MineShaft shaft = shaft();

        // Primeira volta: norte, três degraus; segunda pista a oeste.
        assertEquals(new ColonyPos(100, 64, 199), shaft.positionAt(0));
        assertEquals(new ColonyPos(100, 66, 199), shaft.positionAt(2));
        assertEquals(new ColonyPos(99, 64, 199), shaft.positionAt(3));
        assertEquals(new ColonyPos(100, 63, 198), shaft.positionAt(6));
        assertEquals(new ColonyPos(99, 64, 197), shaft.positionAt(17));

        // Segunda: leste, dois degraus, a partir de (100, 61, 197); pista ao norte.
        assertEquals(new ColonyPos(101, 61, 197), shaft.positionAt(18));
        assertEquals(new ColonyPos(101, 61, 196), shaft.positionAt(21));
        assertEquals(new ColonyPos(102, 62, 196), shaft.positionAt(29));

        // Terceira: sul, três degraus, a partir de (102, 59, 197); pista a leste.
        assertEquals(new ColonyPos(102, 59, 198), shaft.positionAt(30));
        assertEquals(new ColonyPos(103, 59, 198), shaft.positionAt(33));

        // Quarta: oeste, dois degraus, a partir de (102, 56, 200); pista ao sul.
        assertEquals(new ColonyPos(101, 56, 200), shaft.positionAt(48));
        assertEquals(new ColonyPos(101, 56, 201), shaft.positionAt(51));
        assertEquals(new ColonyPos(100, 57, 201), shaft.positionAt(59));
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
        assertEquals(new ColonyPos(99, 55, 200), shaft.positionAt(room + 2));
        assertEquals(new ColonyPos(101, 55, 200), shaft.positionAt(room + 4));
        assertEquals(new ColonyPos(98, 55, 200), shaft.positionAt(room + 6));
        assertEquals(new ColonyPos(102, 55, 200), shaft.positionAt(room + 8));
        assertEquals(new ColonyPos(100, 55, 201), shaft.positionAt(room + 10));
        assertEquals(new ColonyPos(102, 56, 204), shaft.positionAt(room + 49));
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
        int armRoom = arm + MineShaft.ARM_STAIRS * 6;

        assertEquals(Side.SOUTH, south.gallery());
        assertEquals(Side.WEST, west.gallery());

        // Sul: topo em (100, 55, 205), pista a leste, sala em (100, 45, 215).
        assertEquals(new ColonyPos(100, 55, 206), south.positionAt(arm));
        assertEquals(new ColonyPos(101, 55, 206), south.positionAt(arm + 3));
        assertEquals(new ColonyPos(101, 48, 215), south.positionAt(armRoom - 1));
        assertEquals(new ColonyPos(100, 46, 215), south.positionAt(armRoom));
        assertEquals(new ColonyPos(102, 47, 219), south.positionAt(armRoom + 49));

        // Oeste: topo em (95, 55, 200), pista ao sul, sala em (85, 45, 200).
        assertEquals(new ColonyPos(94, 55, 200), west.positionAt(arm));
        assertEquals(new ColonyPos(94, 55, 201), west.positionAt(arm + 3));
        assertEquals(new ColonyPos(85, 46, 200), west.positionAt(armRoom));
        assertEquals(new ColonyPos(81, 47, 202), west.positionAt(armRoom + 49));
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
}
