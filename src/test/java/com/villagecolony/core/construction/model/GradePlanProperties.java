package com.villagecolony.core.construction.model;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * O plano de alturas sob terrenos aleatórios — ADR-063, fase 1. O jqwik gera grades com chão, âncoras, caminho e o que fica
 * fora, e confere as garantias do plano; quando uma cai, ele encolhe a grade até a menor que ainda falha.
 */
class GradePlanProperties {

    /** Uma grade: chão, papel e altura de âncora de cada coluna. */
    static final class Grid {
        private final int[][] natural;
        private final int[][] role;
        private final int[][] anchor;

        Grid(int[][] natural, int[][] role, int[][] anchor) {
            this.natural = natural;
            this.role = role;
            this.anchor = anchor;
        }

        int[][] role() {
            return role;
        }

        int[][] anchor() {
            return anchor;
        }

        GradePlan toPlan() {
            GradePlan plan = new GradePlan(natural.length, natural[0].length);

            for (int x = 0; x < natural.length; x++) {
                for (int z = 0; z < natural[0].length; z++) {
                    plan.ground(x, z, natural[x][z]);
                    switch (role[x][z]) {
                        case 1 -> plan.path(x, z);
                        case 2 -> plan.anchor(x, z, anchor[x][z]);
                        case 3 -> plan.out(x, z);
                        default -> { }
                    }
                }
            }

            return plan;
        }

        @Override
        public String toString() {
            return "natural=" + java.util.Arrays.deepToString(natural) + " role=" + java.util.Arrays.deepToString(role)
                    + " anchor=" + java.util.Arrays.deepToString(anchor);
        }
    }

    /** Sem conflito, todo vizinho dentro do plano fica a no máximo 1 (menos entre duas âncoras), e toda âncora na altura dela. */
    @Property(tries = 400)
    void withoutConflictEveryStepIsAtMostOne(@ForAll("grids") Grid grid) {
        GradePlan.Result result = grid.toPlan().plan();

        net.jqwik.api.statistics.Statistics.collect(result.conflicts().isEmpty() ? "sem conflito" : "com conflito");

        if (!result.conflicts().isEmpty()) {
            return;
        }

        int[][] target = result.target();

        for (int x = 0; x < target.length; x++) {
            for (int z = 0; z < target[0].length; z++) {
                if (grid.role()[x][z] == 3) {
                    continue;
                }
                if (grid.role()[x][z] == 2 && target[x][z] != grid.anchor()[x][z]) {
                    throw new AssertionError("âncora fora da altura em " + x + "," + z);
                }
                boolean anchored = grid.role()[x][z] == 2;

                // Duas âncoras vizinhas mandam nas duas colunas: o degrau entre elas é delas, não do plano.
                if (x + 1 < target.length && grid.role()[x + 1][z] != 3 && !(anchored && grid.role()[x + 1][z] == 2)
                        && Math.abs(target[x + 1][z] - target[x][z]) > 1) {
                    throw new AssertionError("degrau maior que 1 entre " + x + "," + z + " e o vizinho em x");
                }
                if (z + 1 < target[0].length && grid.role()[x][z + 1] != 3 && !(anchored && grid.role()[x][z + 1] == 2)
                        && Math.abs(target[x][z + 1] - target[x][z]) > 1) {
                    throw new AssertionError("degrau maior que 1 entre " + x + "," + z + " e o vizinho em z");
                }
            }
        }
    }

    /** Âncoras tiradas de um relevo de degrau 1 nunca dão conflito, com qualquer chão e qualquer parede. */
    @Property(tries = 300)
    void anchorsFromAGentleReliefNeverConflict(@ForAll("gentleAnchors") Grid grid) {
        GradePlan.Result result = grid.toPlan().plan();

        if (!result.conflicts().isEmpty()) {
            throw new AssertionError("conflito com âncoras de degrau 1: " + result.conflicts());
        }
    }

    /** O chão de degrau 1, com as âncoras nele e sem caminho, não se mexe. */
    @Property(tries = 300)
    void gentleGroundWithItsOwnAnchorsDoesNotMove(@ForAll("gentleGround") Grid grid) {
        GradePlan.Result result = grid.toPlan().plan();

        if (result.pending() != 0) {
            throw new AssertionError(result.pending() + " colunas mexidas em chão que já anda em degraus de 1");
        }
    }

    @Provide
    Arbitrary<Grid> grids() {
        return sizes().flatMap(size -> Combinators.combine(
                heights(size[0], size[1], 0, 10), roles(size[0], size[1]), heights(size[0], size[1], 0, 10))
                .as(Grid::new));
    }

    @Provide
    Arbitrary<Grid> gentleAnchors() {
        return sizes().flatMap(size -> Combinators.combine(
                heights(size[0], size[1], 0, 10), roles(size[0], size[1]), gentle(size[0], size[1]))
                .as(Grid::new));
    }

    @Provide
    Arbitrary<Grid> gentleGround() {
        return sizes().flatMap(size -> Combinators.combine(gentle(size[0], size[1]), rolesWithoutPath(size[0], size[1]))
                .as((relief, role) -> new Grid(relief, role, relief)));
    }

    private static Arbitrary<int[]> sizes() {
        return Combinators.combine(Arbitraries.integers().between(1, 12), Arbitraries.integers().between(1, 12))
                .as((width, depth) -> new int[] {width, depth});
    }

    private static Arbitrary<int[][]> heights(int width, int depth, int low, int high) {
        return Arbitraries.integers().between(low, high).array(int[].class).ofSize(depth)
                .array(int[][].class).ofSize(width);
    }

    /** Papéis: 0 livre (mais comum), 1 caminho, 2 âncora, 3 fora. */
    private static Arbitrary<int[][]> roles(int width, int depth) {
        return Arbitraries.frequency(
                        net.jqwik.api.Tuple.of(14, 0), net.jqwik.api.Tuple.of(3, 1), net.jqwik.api.Tuple.of(2, 2),
                        net.jqwik.api.Tuple.of(2, 3))
                .array(Integer[].class).ofSize(depth).map(GradePlanProperties::unbox)
                .array(int[][].class).ofSize(width);
    }

    private static Arbitrary<int[][]> rolesWithoutPath(int width, int depth) {
        return roles(width, depth).map(role -> {
            for (int[] row : role) {
                for (int z = 0; z < row.length; z++) {
                    row[z] = row[z] == 1 ? 0 : row[z];
                }
            }
            return role;
        });
    }

    /** Um relevo de degrau no máximo 1 em cada eixo: {@code (a·x + b·z) / c}, com |a| e |b| até c. */
    private static Arbitrary<int[][]> gentle(int width, int depth) {
        return Combinators.combine(Arbitraries.integers().between(1, 4), Arbitraries.integers().between(-4, 4),
                        Arbitraries.integers().between(-4, 4), Arbitraries.integers().between(0, 6))
                .as((c, a, b, base) -> {
                    int stepA = Math.max(-c, Math.min(c, a));
                    int stepB = Math.max(-c, Math.min(c, b));
                    int[][] relief = new int[width][depth];

                    for (int x = 0; x < width; x++) {
                        for (int z = 0; z < depth; z++) {
                            relief[x][z] = base + Math.floorDiv(stepA * x + stepB * z, c);
                        }
                    }

                    return relief;
                });
    }

    private static int[] unbox(Integer[] values) {
        int[] out = new int[values.length];

        for (int i = 0; i < values.length; i++) {
            out[i] = values[i];
        }

        return out;
    }
}
