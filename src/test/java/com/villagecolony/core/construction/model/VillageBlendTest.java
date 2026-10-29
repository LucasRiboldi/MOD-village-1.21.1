package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O chão em volta da vila — autor, 2026-10-29 (jogo da 0.3.108: "quebrou uma montanha no meio"): dentro da vila o
 * degrau de 1; fora, a mistura suave entre o nível da vila e o terreno natural, numa faixa de 4 vezes o desnível,
 * de 16 a 64 blocos.
 */
class VillageBlendTest {

    /** A faixa: x de 0 a {@code length - 1}, z de 0 a 8; a vila nas colunas x de 0 a 9. */
    private static final int CORE_END = 9;

    private static final VillageBlend.Area EVERYWHERE = new VillageBlend.Area(-1000, -1000, 1000, 1000);

    private static int[][] strip(int length, int village, int outside) {
        int[][] natural = new int[length][9];

        for (int x = 0; x < length; x++) {
            Arrays.fill(natural[x], x <= CORE_END ? village : outside);
        }

        return natural;
    }

    private static VillageSurface.Kind[][] free(int[][] natural) {
        VillageSurface.Kind[][] kinds = new VillageSurface.Kind[natural.length][natural[0].length];

        for (VillageSurface.Kind[] row : kinds) {
            Arrays.fill(row, VillageSurface.Kind.FREE);
        }

        return kinds;
    }

    private static int[][] blend(int[][] natural, VillageSurface.Kind[][] kinds, VillageBlend.Area reach) {
        return VillageBlend.offsets(natural, kinds, new VillageBlend.Area(0, 0, CORE_END, natural[0].length - 1), reach);
    }

    @Test
    void flatGroundIsNotTouched() {
        int[][] natural = strip(80, 70, 70);
        int[][] offsets = blend(natural, free(natural), EVERYWHERE);

        for (int[] row : offsets) {
            assertTrue(Arrays.stream(row).allMatch(offset -> offset == 0), Arrays.toString(row));
        }
    }

    /** Montanha 30 acima da vila: o corte começa inteiro na borda e some aos 64 blocos, sem degrau maior que 2. */
    @Test
    void aMountainBesideTheVillageBecomesAGentleSlope() {
        int[][] natural = strip(120, 70, 100);
        int[][] offsets = blend(natural, free(natural), EVERYWHERE);

        assertEquals(-30, offsets[CORE_END + 1][4], 1, "colada na vila, a coluna desce ao nível dela");
        assertEquals(-15, offsets[CORE_END + 32][4], 1, "na metade da faixa de 64, a metade do corte");
        assertEquals(0, offsets[CORE_END + 64][4], "a faixa de 4 × 30 vai até o teto de 64");
        assertEquals(0, offsets[CORE_END][4], "a vila plana fica como está");

        for (int x = CORE_END; x < 119; x++) {
            int step = Math.abs(natural[x + 1][4] + offsets[x + 1][4] - natural[x][4] - offsets[x][4]);
            assertTrue(step <= 2, "degrau de " + step + " entre x " + x + " e " + (x + 1));
        }
    }

    /** A largura é 4 vezes o desnível: 8 blocos de diferença, 32 de faixa. */
    @Test
    void theBandIsFourTimesTheStep() {
        int[][] natural = strip(80, 70, 78);
        int[][] offsets = blend(natural, free(natural), EVERYWHERE);

        assertEquals(-4, offsets[CORE_END + 16][4], 1, "na metade da faixa de 32, a metade do corte");
        assertEquals(0, offsets[CORE_END + 32][4], "a faixa de 32 acabou");
        assertTrue(offsets[CORE_END + 8][4] <= -6, "a três quartos do corte, perto da vila: " + offsets[CORE_END + 8][4]);
    }

    /** Desnível pequeno usa a faixa mínima de 16, não 4 vezes 2 = 8. */
    @Test
    void aSmallStepUsesTheNarrowestBand() {
        int[][] natural = strip(60, 70, 74);
        int[][] offsets = blend(natural, free(natural), EVERYWHERE);

        assertEquals(-2, offsets[CORE_END + 8][4], 1, "na metade da faixa de 16");
        assertEquals(0, offsets[CORE_END + 16][4], "a faixa de 16 acabou");
        assertTrue(offsets[CORE_END + 3][4] <= -3, "perto da vila, quase todo o corte: " + offsets[CORE_END + 3][4]);
    }

    /** Vale ao lado da vila: o chão sobe para ela, com a mesma conta. */
    @Test
    void aValleyIsFilledTowardsTheVillage() {
        int[][] natural = strip(80, 70, 62);
        int[][] offsets = blend(natural, free(natural), EVERYWHERE);

        assertEquals(8, offsets[CORE_END + 1][4], 1);
        assertEquals(4, offsets[CORE_END + 16][4], 1);
        assertEquals(0, offsets[CORE_END + 40][4]);
    }

    /** A água não se move, e a margem dela fica natural: a mistura some em 8 blocos até a água. */
    @Test
    void waterIsNeverMovedAndItsBankStaysNatural() {
        int[][] natural = strip(60, 70, 90);
        VillageSurface.Kind[][] kinds = free(natural);
        kinds[CORE_END + 5][4] = VillageSurface.Kind.WATER;

        int[][] offsets = blend(natural, kinds, EVERYWHERE);

        assertEquals(0, offsets[CORE_END + 5][4], "a água");
        assertTrue(offsets[CORE_END + 5][3] >= -2, "a margem colada na água quase não muda: " + offsets[CORE_END + 5][3]);
        assertTrue(offsets[CORE_END + 15][4] < -10, "a 10 da água a mistura volta: " + offsets[CORE_END + 15][4]);
    }

    /** A conta parte do topo de verdade: colada na vila dá o nível dela, longe dá o próprio topo. */
    @Test
    void theRealTopKeepsItsReliefAndMeetsTheVillage() {
        int[][] natural = strip(120, 70, 100);
        VillageBlend.Shape shape = VillageBlend.shape(natural, free(natural),
                new VillageBlend.Area(0, 0, CORE_END, 8), EVERYWHERE);

        assertEquals(70, shape.target(CORE_END + 1, 4, 104), 1, "colada na vila, o nível dela, seja qual for o topo");
        assertEquals(103, shape.target(CORE_END + 80, 4, 103), "fora da faixa, o próprio topo");
        assertEquals(1, shape.target(CORE_END + 32, 4, 101) - shape.target(CORE_END + 32, 4, 99), 1,
                "na metade da faixa, 2 blocos de relevo viram 1");
        assertTrue(shape.shapes(2, 2) && !shape.shapes(CORE_END + 80, 4));
    }

    /** A distância até a água conta as 8 vizinhas. */
    @Test
    void theDistanceToWaterCountsDiagonals() {
        int[][] natural = strip(12, 70, 70);
        VillageSurface.Kind[][] kinds = free(natural);
        kinds[0][0] = VillageSurface.Kind.WATER;
        kinds[11][8] = VillageSurface.Kind.WATER;

        int[][] distance = VillageBlend.waterDistance(kinds);

        assertEquals(0, distance[0][0]);
        assertEquals(3, distance[3][3]);
        assertEquals(4, distance[4][1]);
        assertEquals(2, distance[9][6]);
        assertEquals(6, distance[5][8]);
    }

    /** O que a vila não alcança fica natural, e a mistura amacia até lá em 8 blocos. */
    @Test
    void groundOutOfReachStaysNaturalAndTheBlendFadesBeforeIt() {
        int[][] natural = strip(80, 70, 100);
        int[][] offsets = blend(natural, free(natural), new VillageBlend.Area(-1000, -1000, CORE_END + 20, 1000));
        int[][] open = blend(natural, free(natural), EVERYWHERE);

        assertEquals(0, offsets[CORE_END + 21][4], "fora do alcance");
        assertEquals(0, offsets[CORE_END + 20][4], "na beira do alcance");
        assertTrue(Math.abs(offsets[CORE_END + 16][4]) < Math.abs(open[CORE_END + 16][4]) - 2,
                "a 4 da beira a mistura já amacia: " + offsets[CORE_END + 16][4] + " contra " + open[CORE_END + 16][4]);
        assertEquals(open[CORE_END + 2][4], offsets[CORE_END + 2][4], 1, "longe da beira, a mistura é a mesma");
    }

    /** Dentro da vila vale a superfície de degrau 1: a cratera do miolo é tampada. */
    @Test
    void insideTheVillageTheCraterIsFilled() {
        int[][] natural = strip(40, 70, 70);
        natural[5][4] = 60;

        int[][] offsets = blend(natural, free(natural), EVERYWHERE);

        assertEquals(10, offsets[5][4], "a cratera sobe ao chão em volta");
        assertEquals(0, offsets[2][2]);
    }

    /** A vila num canto: a distância é até o canto, em linha reta. */
    @Test
    void theDistanceToTheVillageIsStraightToItsCorner() {
        int[][] natural = new int[80][80];

        for (int x = 0; x < 80; x++) {
            Arrays.fill(natural[x], 100);
            Arrays.fill(natural[x], 0, 10, x <= 9 ? 70 : 100);
        }

        VillageSurface.Kind[][] kinds = free(natural);
        int[][] offsets = VillageBlend.offsets(natural, kinds, new VillageBlend.Area(0, 0, 9, 9), EVERYWHERE);

        // A 32 em linha reta do canto (9, 9): a metade do corte.
        assertEquals(-15, offsets[9 + 23][9 + 23], 1);
        assertEquals(0, offsets[9 + 46][9 + 46], "a 65 do canto, fora da faixa");
    }

    /** A vila longe do canto da grade: as contas são feitas a partir dela, não da coluna 0. */
    @Test
    void theVillageNeedNotStartAtTheCornerOfTheGrid() {
        int[][] natural = new int[90][20];

        for (int x = 0; x < 90; x++) {
            Arrays.fill(natural[x], x >= 10 && x <= 19 ? 70 : 100);
        }

        natural[14][9] = 60;
        VillageBlend.Area core = new VillageBlend.Area(10, 5, 19, 14);
        int[][] offsets = VillageBlend.offsets(natural, free(natural), core, EVERYWHERE);

        assertEquals(10, offsets[14][9], "a cratera do miolo deslocado sobe");
        assertEquals(0, offsets[12][7], "o miolo plano fica");
        assertEquals(-30, offsets[20][9], 1, "colada na vila, pelo lado de x");
        assertEquals(-15, offsets[19 + 32][9], 1, "a 32 da vila");
        assertEquals(-30, offsets[9][9], 1, "colada na vila, pelo lado de menor x");
        assertEquals(-27, offsets[20][16], 2, "na diagonal, perto do canto da vila");
    }

    /** O desfoque é simétrico: dois picos iguais, um de cada lado da vila, descem igual. */
    @Test
    void twoMirroredPeaksComeDownAlike() {
        int[][] natural = new int[61][21];

        for (int[] row : natural) {
            Arrays.fill(row, 70);
        }

        natural[30 - 5 - 10][10] = 110;
        natural[30 + 5 + 10][10] = 110;
        natural[30 - 5 - 10][11] = 96;
        natural[30 + 5 + 10][11] = 96;
        VillageBlend.Area core = new VillageBlend.Area(25, 5, 35, 15);

        int[][] offsets = VillageBlend.offsets(natural, free(natural), core, EVERYWHERE);

        assertTrue(offsets[15][10] < -10, "o pico desce: " + offsets[15][10]);
        assertEquals(offsets[15][10], offsets[45][10], "o pico da esquerda e o da direita");
        assertEquals(offsets[15][11], offsets[45][11]);
        assertEquals(offsets[13][10], offsets[47][10]);
    }
}
