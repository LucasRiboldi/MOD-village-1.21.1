package com.villagecolony.core.construction.model;

import java.util.PriorityQueue;

/**
 * O solo da vila na geração — autor, 2026-10-29 ("o terreno smooth, com diferenças de um bloco deixando tudo suave
 * de andar"; escolhas: encosta suave, água fica, borda de 16 até o terreno natural). Dada a altura natural de cada
 * coluna (o bloco sólido de cima) e o papel dela, devolve a altura do chão, com degrau de no máximo 1 entre colunas
 * vizinhas (4 vizinhos) onde as colunas fixas deixam:
 *
 * <ol>
 *   <li><b>Buraco tampado:</b> a depressão que não escoa para a borda nem para a água sobe até a beira
 *   (preenchimento por inundação, priority-flood).</li>
 *   <li><b>Faixa das fixas:</b> nenhuma coluna fica mais longe de uma coluna fixa do que a distância deixa — o piso
 *   das casas, a água e o anel da borda mandam no chão em volta (transformada de distância, duas passadas).</li>
 *   <li><b>Partida suave:</b> a média entre o maior e o menor relevo de degrau 1 que cabem no terreno tampado. A média
 *   de dois relevos de degrau 1 tem degrau 1, e nas fixas os dois coincidem.</li>
 *   <li><b>Corte mínimo:</b> cada coluna livre anda até a altura tampada, sem sair de vizinha mais alta − 1 e mais
 *   baixa + 1 — partindo de um relevo suave, o passo nunca quebra a suavidade. O paredão vira encosta de degraus de 1,
 *   cortando e aterrando o mínimo.</li>
 * </ol>
 *
 * <p>Onde duas fixas não cabem no degrau de 1 (casa colada num lago fundo), fica o meio entre elas. Puro e
 * determinístico: a mesma vila dá a mesma superfície em todos os chunks dela.
 */
public final class VillageSurface {

    /** O papel de cada coluna. */
    public enum Kind {
        /** Chão que a vila molda. */
        FREE,
        /** Altura que não muda: o piso das casas e o anel externo da borda. */
        FIXED,
        /** Lago ou rio: fica, e a margem desce até ele. */
        WATER
    }

    /** Passadas do corte mínimo, no máximo. */
    static final int MAX_PASSES = 400;

    private static final int FAR = 1 << 20;

    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private VillageSurface() {
    }

    /** A superfície alvo, do mesmo tamanho de {@code natural} ({@code [x][z]}). */
    public static int[][] smooth(int[][] natural, Kind[][] kinds) {
        int width = natural.length;
        int depth = width == 0 ? 0 : natural[0].length;
        int[][] level = filled(natural, kinds);

        // A faixa que as fixas deixam.
        int[][] high = new int[width][depth];
        int[][] low = new int[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean fixed = kinds[x][z] != Kind.FREE;
                high[x][z] = fixed ? natural[x][z] : FAR;
                low[x][z] = fixed ? natural[x][z] : -FAR;
            }
        }

        spread(high, true);
        spread(low, false);

        int[][] wanted = new int[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                int top = high[x][z];
                int bottom = low[x][z];

                if (bottom > top) {
                    // Fixas que não cabem no degrau de 1: o meio entre elas.
                    top = Math.floorDiv(top + bottom, 2);
                    bottom = top;
                }

                wanted[x][z] = kinds[x][z] == Kind.FREE ? Math.max(bottom, Math.min(top, level[x][z])) : natural[x][z];
            }
        }

        // A partida: a média do maior e do menor relevo de degrau 1 dentro do terreno tampado.
        int[][] below = copy(wanted);
        int[][] above = copy(wanted);

        spread(below, true);
        spread(above, false);

        int[][] surface = new int[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                surface[x][z] = kinds[x][z] == Kind.FREE
                        ? Math.floorDiv(below[x][z] + above[x][z], 2)
                        : natural[x][z];
            }
        }

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            boolean changed = false;

            for (int i = 0; i < width * depth; i++) {
                int cell = pass % 2 == 0 ? i : width * depth - 1 - i;
                changed |= pull(surface, wanted, kinds, cell / depth, cell % depth);
            }

            if (!changed) {
                break;
            }
        }

        return surface;
    }

    /** A coluna livre anda até a altura que quer, sem sair de vizinha mais alta − 1 e mais baixa + 1. */
    private static boolean pull(int[][] surface, int[][] wanted, Kind[][] kinds, int x, int z) {
        if (kinds[x][z] != Kind.FREE) {
            return false;
        }

        int low = Integer.MIN_VALUE;
        int high = Integer.MAX_VALUE;

        for (int[] side : SIDES) {
            int nx = x + side[0];
            int nz = z + side[1];

            if (nx >= 0 && nz >= 0 && nx < surface.length && nz < surface[nx].length) {
                low = Math.max(low, surface[nx][nz] - 1);
                high = Math.min(high, surface[nx][nz] + 1);
            }
        }

        if (low > high) {
            return false;
        }

        int target = Math.max(low, Math.min(high, wanted[x][z]));

        if (target == surface[x][z]) {
            return false;
        }

        surface[x][z] = target;
        return true;
    }

    /**
     * Espalha os valores com degrau de 1 (distância de quarteirão, duas passadas): com {@code fromBelow}, cada coluna
     * fica no máximo 1 acima da vizinha (o maior relevo de degrau 1 abaixo dos valores); sem, no mínimo 1 abaixo.
     */
    static void spread(int[][] grid, boolean fromBelow) {
        int width = grid.length;
        int depth = width == 0 ? 0 : grid[0].length;
        int step = fromBelow ? 1 : -1;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                if (x > 0) {
                    grid[x][z] = pick(grid[x][z], grid[x - 1][z] + step, fromBelow);
                }

                if (z > 0) {
                    grid[x][z] = pick(grid[x][z], grid[x][z - 1] + step, fromBelow);
                }
            }
        }

        for (int x = width - 1; x >= 0; x--) {
            for (int z = depth - 1; z >= 0; z--) {
                if (x < width - 1) {
                    grid[x][z] = pick(grid[x][z], grid[x + 1][z] + step, fromBelow);
                }

                if (z < depth - 1) {
                    grid[x][z] = pick(grid[x][z], grid[x][z + 1] + step, fromBelow);
                }
            }
        }
    }

    private static int pick(int value, int candidate, boolean smaller) {
        return smaller ? Math.min(value, candidate) : Math.max(value, candidate);
    }

    /**
     * A altura com as depressões tampadas: a inundação parte da borda e da água, sempre pela coluna mais baixa, e
     * cada coluna livre alcançada fica no mínimo na altura de quem a alcançou.
     */
    static int[][] filled(int[][] natural, Kind[][] kinds) {
        int width = natural.length;
        int depth = width == 0 ? 0 : natural[0].length;
        int[][] level = copy(natural);
        boolean[][] seen = new boolean[width][depth];
        PriorityQueue<long[]> frontier = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean border = x == 0 || z == 0 || x == width - 1 || z == depth - 1;

                if (border || kinds[x][z] == Kind.WATER) {
                    seen[x][z] = true;
                    frontier.add(new long[] {natural[x][z], x, z});
                }
            }
        }

        while (!frontier.isEmpty()) {
            long[] at = frontier.poll();
            int height = (int) at[0];

            for (int[] side : SIDES) {
                int nx = (int) at[1] + side[0];
                int nz = (int) at[2] + side[1];

                if (nx < 0 || nz < 0 || nx >= width || nz >= depth || seen[nx][nz]) {
                    continue;
                }

                seen[nx][nz] = true;

                if (kinds[nx][nz] == Kind.FREE) {
                    level[nx][nz] = Math.max(natural[nx][nz], height);
                }

                frontier.add(new long[] {level[nx][nz], nx, nz});
            }
        }

        return level;
    }

    private static int[][] copy(int[][] grid) {
        int[][] copy = new int[grid.length][];

        for (int x = 0; x < grid.length; x++) {
            copy[x] = grid[x].clone();
        }

        return copy;
    }
}
