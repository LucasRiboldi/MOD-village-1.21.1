package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O solo da vila na geração — autor, 2026-10-29: degrau de no máximo 1 entre colunas vizinhas, buraco tampado, morro
 * virando encosta, casa no próprio piso, água no lugar e a borda encontrando o terreno natural.
 */
class VillageSurfaceTest {

    private static VillageSurface.Kind[][] kinds(int size) {
        VillageSurface.Kind[][] kinds = new VillageSurface.Kind[size][size];

        for (VillageSurface.Kind[] row : kinds) {
            java.util.Arrays.fill(row, VillageSurface.Kind.FREE);
        }

        return kinds;
    }

    private static int[][] flat(int size, int y) {
        int[][] grid = new int[size][size];

        for (int[] row : grid) {
            java.util.Arrays.fill(row, y);
        }

        return grid;
    }

    private static void assertSmooth(int[][] surface, VillageSurface.Kind[][] kinds) {
        for (int x = 0; x < surface.length; x++) {
            for (int z = 0; z < surface[x].length; z++) {
                if (x + 1 < surface.length && kinds[x][z] != VillageSurface.Kind.WATER
                        && kinds[x + 1][z] != VillageSurface.Kind.WATER) {
                    assertTrue(Math.abs(surface[x][z] - surface[x + 1][z]) <= 1,
                            "degrau maior que 1 entre " + x + "," + z + " e o vizinho: " + surface[x][z] + " / "
                                    + surface[x + 1][z]);
                }

                if (z + 1 < surface[x].length && kinds[x][z] != VillageSurface.Kind.WATER
                        && kinds[x][z + 1] != VillageSurface.Kind.WATER) {
                    assertTrue(Math.abs(surface[x][z] - surface[x][z + 1]) <= 1,
                            "degrau maior que 1 entre " + x + "," + z + " e o vizinho: " + surface[x][z] + " / "
                                    + surface[x][z + 1]);
                }
            }
        }
    }

    @Test
    void aCraterIsFilled() {
        int[][] natural = flat(21, 70);

        for (int x = 8; x <= 12; x++) {
            for (int z = 8; z <= 12; z++) {
                natural[x][z] = 50;
            }
        }

        int[][] surface = VillageSurface.smooth(natural, kinds(21));

        assertEquals(70, surface[10][10], "o fundo da cratera devia subir ao chão em volta");
        assertSmooth(surface, kinds(21));
    }

    @Test
    void aCliffBecomesASlopeOfOneBlockSteps() {
        int[][] natural = flat(30, 70);

        for (int x = 15; x < 30; x++) {
            for (int z = 0; z < 30; z++) {
                natural[x][z] = 90;
            }
        }

        int[][] surface = VillageSurface.smooth(natural, kinds(30));

        assertSmooth(surface, kinds(30));
        assertTrue(surface[0][15] <= 72 && surface[29][15] >= 88,
                "a encosta devia guardar o formato: em baixo " + surface[0][15] + ", em cima " + surface[29][15]);
    }

    @Test
    void aHouseKeepsItsFloorAndTheGroundMeetsIt() {
        int[][] natural = flat(21, 80);
        VillageSurface.Kind[][] kinds = kinds(21);

        for (int x = 8; x <= 12; x++) {
            for (int z = 8; z <= 12; z++) {
                natural[x][z] = 72;
                kinds[x][z] = VillageSurface.Kind.FIXED;
            }
        }

        int[][] surface = VillageSurface.smooth(natural, kinds);

        assertEquals(72, surface[10][10], "o piso da casa não pode mudar");
        assertEquals(72, surface[8][8], "o piso da casa não pode mudar");
        assertEquals(73, surface[7][10], "o chão encostado na casa fica a um degrau dela");
        assertSmooth(surface, kinds);
    }

    @Test
    void waterStaysAndItsBankComesDownToIt() {
        int[][] natural = flat(21, 75);
        VillageSurface.Kind[][] kinds = kinds(21);

        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z < 21; z++) {
                natural[x][z] = 62;
                kinds[x][z] = VillageSurface.Kind.WATER;
            }
        }

        int[][] surface = VillageSurface.smooth(natural, kinds);

        assertEquals(62, surface[3][10], "a água fica onde está");
        assertTrue(surface[6][10] <= 63, "a margem devia descer até a água, está em " + surface[6][10]);
        assertSmooth(surface, kinds);
    }

    @Test
    void aRoughVillageSizedTerrainComesOutSmoothInAFewHundredMilliseconds() {
        java.util.Random random = new java.util.Random(42);
        int size = 200;
        int[][] natural = new int[size][size];

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                natural[x][z] = 80 + (int) (20 * Math.sin(x / 9.0) * Math.cos(z / 13.0)) + random.nextInt(11) - 5;
            }
        }

        VillageSurface.Kind[][] kinds = kinds(size);

        for (int x = 90; x < 100; x++) {
            for (int z = 90; z < 100; z++) {
                kinds[x][z] = VillageSurface.Kind.FIXED;
                natural[x][z] = 80;
            }
        }

        long began = System.nanoTime();
        int[][] surface = VillageSurface.smooth(natural, kinds);
        long millis = (System.nanoTime() - began) / 1_000_000;

        assertSmooth(surface, kinds);
        assertEquals(80, surface[95][95]);
        assertTrue(millis < 2_000, "a superfície de uma vila levou " + millis + " ms");
    }

    /**
     * O que não é chão (casa, árvore, roça) não muda e não puxa as vizinhas: o telhado de uma casa a 85 no meio do chão
     * a 70 não faz o chão em volta subir até ele (pedreiro alisando a vila, autor 2026-10-29).
     */
    @Test
    void whatIsNotGroundNeitherChangesNorPullsItsNeighbours() {
        int[][] natural = flat(21, 70);
        VillageSurface.Kind[][] kinds = kinds(21);

        for (int x = 8; x <= 12; x++) {
            for (int z = 8; z <= 12; z++) {
                natural[x][z] = 85;
                kinds[x][z] = VillageSurface.Kind.SKIP;
            }
        }

        natural[3][3] = 74;

        int[][] surface = VillageSurface.smooth(natural, kinds);

        assertEquals(85, surface[10][10], "o telhado não muda");
        assertEquals(70, surface[7][10], "o chão colado na casa fica no nível dele");
        assertEquals(70, surface[3][3], "o calombo isolado desce ao nível em volta");
    }

    @Test
    void theOuterRingKeepsTheNaturalGround() {
        int[][] natural = flat(21, 70);
        VillageSurface.Kind[][] kinds = kinds(21);

        for (int i = 0; i < 21; i++) {
            natural[0][i] = 78;
            kinds[0][i] = VillageSurface.Kind.FIXED;
        }

        int[][] surface = VillageSurface.smooth(natural, kinds);

        assertEquals(78, surface[0][10], "a borda é o terreno natural");
        assertSmooth(surface, kinds);
    }

    /** O calombo de até 4 colunas desce exatamente ao nível em volta; o de 5 fica para a encosta. */
    @Test
    void aBumpOfFourIsCutToTheGroundAroundAndOneOfFiveIsNot() {
        int[][] natural = flat(15, 70);
        VillageSurface.Kind[][] kinds = kinds(15);

        for (int[] cell : new int[][] {{3, 3}, {3, 4}, {4, 4}, {4, 5}}) {
            natural[cell[0]][cell[1]] = 74;
        }

        for (int z = 3; z <= 7; z++) {
            natural[10][z] = 73;
        }

        int[][] cut = VillageSurface.cutSmallBumps(natural, kinds);

        assertEquals(70, cut[3][3]);
        assertEquals(70, cut[4][5]);
        assertEquals(73, cut[10][5], "o calombo de 5 colunas não é cortado aqui");
        assertEquals(70, cut[7][7], "o chão em volta fica");
    }

    /** O calombo na beira de um degrau desce ao nível mais alto em volta, não ao mais baixo. */
    @Test
    void aBumpComesDownToTheHighestGroundAroundIt() {
        int[][] natural = flat(9, 70);
        VillageSurface.Kind[][] kinds = kinds(9);

        for (int z = 0; z < 9; z++) {
            natural[5][z] = 72;
            natural[6][z] = 72;
            natural[7][z] = 72;
            natural[8][z] = 72;
        }

        natural[4][4] = 76;

        int[][] cut = VillageSurface.cutSmallBumps(natural, kinds);

        assertEquals(72, cut[4][4]);
        assertEquals(70, cut[3][4]);
    }

    /** Casa e água não são calombo: ficam como estão. */
    @Test
    void housesAndWaterAreNeverCutAsBumps() {
        int[][] natural = flat(9, 70);
        VillageSurface.Kind[][] kinds = kinds(9);
        natural[3][3] = 75;
        kinds[3][3] = VillageSurface.Kind.FIXED;
        natural[5][5] = 75;
        kinds[5][5] = VillageSurface.Kind.SKIP;

        int[][] cut = VillageSurface.cutSmallBumps(natural, kinds);

        assertEquals(75, cut[3][3]);
        assertEquals(75, cut[5][5]);
    }

    /** A vala na beira de longe da grade escoa para fora: não é tampada. */
    @Test
    void aDitchOnTheFarBordersDrainsAndStays() {
        int[][] natural = flat(9, 70);
        VillageSurface.Kind[][] kinds = kinds(9);

        for (int i = 2; i <= 6; i++) {
            natural[8][i] = 60;
            natural[i][8] = 60;
        }

        int[][] level = VillageSurface.filled(natural, kinds);

        assertEquals(60, level[8][4], "a vala na última coluna escoa para fora");
        assertEquals(60, level[4][8], "a vala na última linha escoa para fora");
        assertEquals(70, level[4][4]);
    }

    /** O buraco do meio sobe à beira mais baixa por onde escoaria, e não mais. */
    @Test
    void aHoleFillsUpToItsLowestRim() {
        int[][] natural = flat(9, 72);
        VillageSurface.Kind[][] kinds = kinds(9);

        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                natural[x][z] = 60;
            }
        }

        for (int x = 0; x <= 3; x++) {
            natural[x][4] = Math.min(natural[x][4], 68);
        }

        int[][] level = VillageSurface.filled(natural, kinds);

        assertEquals(68, level[4][4], "o buraco escoa pelo vão de 68 até a borda");
        assertEquals(72, level[7][7]);
    }
}
