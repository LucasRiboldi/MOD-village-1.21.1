package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regra 323: buraco e bloco isolados primeiro, depois degrau maior que 1 no caminho, depois o resto; teto por dia. */
class TerrainDefectsTest {

    private static final int SIDE = 6;

    private static int[][] flat() {
        int[][] top = new int[SIDE + 2][SIDE + 2];

        for (int[] row : top) {
            java.util.Arrays.fill(row, 64);
        }

        return top;
    }

    private static boolean[][] all(boolean value) {
        boolean[][] grid = new boolean[SIDE + 2][SIDE + 2];

        for (boolean[] row : grid) {
            java.util.Arrays.fill(row, value);
        }

        return grid;
    }

    @Test
    void aOneBlockHoleIsIsolatedAndFilledToItsNeighbors() {
        int[][] top = flat();

        top[3][3] = 63;

        TerrainDefects.Survey survey = TerrainDefects.survey(top, all(true), all(false));

        assertEquals(1, survey.isolated().size(), "o buraco de 1 bloco não é isolado");
        assertEquals(64, survey.isolated().get(0)[2], "o buraco não sobe ao nível dos vizinhos");
        assertEquals(0, survey.steps(), "degrau de 1 não é defeito de degrau");
    }

    @Test
    void aSingleBlockIsIsolatedAndCutToItsNeighbors() {
        int[][] top = flat();

        top[2][5] = 67;

        TerrainDefects.Survey survey = TerrainDefects.survey(top, all(true), all(false));

        assertEquals(1, survey.isolated().size());
        assertEquals(64, survey.isolated().get(0)[2], "o bloco único não desce ao nível dos vizinhos");
    }

    @Test
    void aColumnBesideWaterIsNotIsolated() {
        int[][] top = flat();
        boolean[][] ground = all(true);

        top[3][3] = 63;
        ground[4][3] = false;

        assertTrue(TerrainDefects.survey(top, ground, all(false)).isolated().isEmpty(),
                "a coluna ao lado da água foi tratada como isolada");
    }

    @Test
    void aStepOnThePathOutranksAStepElsewhereAndAnIsolatedHoleOutranksBoth() {
        int[][] wall = flat();
        boolean[][] path = all(false);

        for (int z = 0; z < SIDE + 2; z++) {
            for (int x = 4; x < SIDE + 2; x++) {
                wall[x][z] = 67;
            }
        }

        long elsewhere = TerrainDefects.survey(wall, all(true), path).score();

        path[3][3] = true;
        long onThePath = TerrainDefects.survey(wall, all(true), path).score();
        int[][] hole = flat();

        hole[3][3] = 62;
        long isolated = TerrainDefects.survey(hole, all(true), all(false)).score();

        assertTrue(onThePath > elsewhere, "o degrau no caminho não passou à frente");
        assertTrue(isolated > onThePath, "o buraco isolado não passou à frente do degrau no caminho");
    }

    @Test
    void theIsolatedColumnKeepsItsPlaceInsideTheSquare() {
        int[][] top = flat();

        top[2][5] = 67;
        top[SIDE][SIDE] = 61;

        TerrainDefects.Survey survey = TerrainDefects.survey(top, all(true), all(false));

        assertEquals(2, survey.isolated().size(), "a coluna da última linha do quadrado ficou de fora");
        assertTrue(survey.isolated().stream().anyMatch(at -> java.util.Arrays.equals(at, new int[] {1, 4, 64})),
                "o bloco único saiu fora do lugar");
        assertTrue(survey.isolated().stream().anyMatch(at -> java.util.Arrays.equals(at, new int[] {SIDE - 1, SIDE - 1, 64})),
                "o buraco do canto saiu fora do lugar");
    }

    @Test
    void theStepAgainstTheNextSquareCountsOncePerColumn() {
        int[][] east = flat();
        int[][] south = flat();

        for (int i = 0; i < SIDE + 2; i++) {
            east[SIDE + 1][i] = 67;
            south[i][SIDE + 1] = 67;
        }

        assertEquals(SIDE, TerrainDefects.survey(east, all(true), all(false)).steps(),
                "o degrau contra o quadrado de leste não contou uma vez por coluna");
        assertEquals(SIDE, TerrainDefects.survey(south, all(true), all(false)).steps(),
                "o degrau contra o quadrado de sul não contou uma vez por coluna");
    }

    @Test
    void theScoreWeighsIsolatedThenPathThenTheRest() {
        assertEquals(1_002_003L, new TerrainDefects.Survey(java.util.List.of(new int[] {0, 0, 0}), 2, 3).score());
    }

    @Test
    void theDailyCapStopsAndResetsTheNextDay() {
        TerrainDefects.Daily daily = new TerrainDefects.Daily();

        daily.spend(5, TerrainDefects.DAILY_COLUMNS - 3);

        assertEquals(3, daily.allowance(5, 32), "o teto do dia não segurou");
        daily.spend(5, 3);
        assertEquals(0, daily.allowance(5, 32));
        assertEquals(TerrainDefects.DAILY_COLUMNS, daily.used(5), "a medida do dia não somou o que foi feito");
        assertEquals(32, daily.allowance(6, 32), "o teto não voltou no dia seguinte");
        assertEquals(0, daily.used(6), "a medida não zerou no dia seguinte");
    }
}
