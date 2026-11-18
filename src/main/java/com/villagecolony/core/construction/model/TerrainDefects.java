package com.villagecolony.core.construction.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Os defeitos do chão de um quadrado da vila — Regra 323 (autor, 2026-11-18: "todo terreno sendo arrumado pelos aldeões
 * para não ter buracos isolados, ou blocos únicos isolados ... diferença máxima de um bloco se possível").
 *
 * <p>Três classes, nesta ordem de prioridade: a coluna isolada (buraco ou bloco único, mais baixa ou mais alta que os quatro
 * vizinhos), o degrau maior que 1 que toca caminho (aresta calçada e frente de entrada) e o degrau maior que 1 no resto do
 * chão. Só conta chão que o aldeão pode mexer. Puro.
 */
public final class TerrainDefects {

    /** Colunas que os aldeões mudam por dia em cada colônia. */
    public static final int DAILY_COLUMNS = 256;

    private static final int[][] NEIGHBORS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * O que a leitura de um quadrado achou.
     *
     * @param isolated cada coluna isolada: {dx, dz, altura alvo}, com dx e dz dentro do quadrado (de 0)
     */
    public record Survey(List<int[]> isolated, int pathSteps, int steps) {

        public Survey {
            isolated = List.copyOf(isolated);
        }

        /** A prioridade do quadrado: coluna isolada antes de degrau no caminho, e este antes do resto. */
        public long score() {
            return isolated.size() * 1_000_000L + pathSteps * 1_000L + steps;
        }
    }

    /** O teto diário de uma colônia. */
    public static final class Daily {

        private long day = Long.MIN_VALUE;
        private int used;

        /** Quanto ainda cabe hoje, até {@code wanted}. */
        public int allowance(long today, int wanted) {
            if (today != day) {
                day = today;
                used = 0;
            }

            return Math.max(0, Math.min(wanted, DAILY_COLUMNS - used));
        }

        /** Quanto já foi feito hoje. */
        public int used(long today) {
            allowance(today, 0);

            return used;
        }

        /** Conta o que foi feito hoje. */
        public void spend(long today, int done) {
            allowance(today, 0);
            used += done;
        }
    }

    private TerrainDefects() {
    }

    /**
     * Lê o quadrado. As matrizes têm uma borda de 1 em volta dele: o quadrado vai de 1 a {@code length - 2}.
     *
     * @param top    o topo de cada coluna
     * @param ground se a coluna é chão que o aldeão pode mexer
     * @param path   se a coluna é caminho (aresta calçada ou frente de entrada)
     */
    public static Survey survey(int[][] top, boolean[][] ground, boolean[][] path) {
        List<int[]> isolated = new ArrayList<>();
        int pathSteps = 0;
        int steps = 0;
        int last = top.length - 2;

        for (int x = 1; x <= last; x++) {
            for (int z = 1; z <= last; z++) {
                if (!ground[x][z]) {
                    continue;
                }

                java.util.OptionalInt target = isolation(top, ground, x, z);

                if (target.isPresent()) {
                    isolated.add(new int[] {x - 1, z - 1, target.getAsInt()});
                }

                // Cada par uma vez: o vizinho de leste e o de sul (o da borda conta, ele é chão do quadrado ao lado).
                for (int[] step : new int[][] {{1, 0}, {0, 1}}) {
                    int nx = x + step[0];
                    int nz = z + step[1];

                    if (ground[nx][nz] && Math.abs(top[nx][nz] - top[x][z]) > 1) {
                        if (path[x][z] || path[nx][nz]) {
                            pathSteps++;
                        } else {
                            steps++;
                        }
                    }
                }
            }
        }

        return new Survey(isolated, pathSteps, steps);
    }

    /** A altura alvo da coluna isolada: o vizinho mais baixo para o bloco único, o mais alto para o buraco. */
    private static java.util.OptionalInt isolation(int[][] top, boolean[][] ground, int x, int z) {
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;

        for (int[] near : NEIGHBORS) {
            int nx = x + near[0];
            int nz = z + near[1];

            if (!ground[nx][nz]) {
                return java.util.OptionalInt.empty();
            }

            lowest = Math.min(lowest, top[nx][nz]);
            highest = Math.max(highest, top[nx][nz]);
        }

        if (lowest > top[x][z]) {
            return java.util.OptionalInt.of(lowest);
        }

        return highest < top[x][z] ? java.util.OptionalInt.of(highest) : java.util.OptionalInt.empty();
    }
}
