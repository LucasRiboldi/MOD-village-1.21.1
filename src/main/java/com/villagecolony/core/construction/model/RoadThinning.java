package com.villagecolony.core.construction.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A linha do meio da faixa de caminho — Regra 177: afina a faixa até um bloco de largura (Zhang-Suen) e tira as pontas
 * curtas que a beira irregular deixa. Coluna fixa (a frente de uma porta) nunca sai. Puro.
 */
final class RoadThinning {

    /** A ponta até este tamanho é rebarba da beira da faixa, não caminho. */
    static final int MAX_SPUR = 4;

    /** A maior janela que se afina de uma vez: além disso, a faixa volta como veio. */
    private static final long MAX_WINDOW = 4_000_000;

    /** Os oito vizinhos em volta, em círculo, a partir do norte. */
    static final int[][] RING = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};

    private RoadThinning() {
    }

    /** A linha do meio de {@code mask}, sem rebarbas. */
    static Set<Long> skeleton(Set<Long> mask, Set<Long> fixed) {
        Set<Long> thin = thin(mask, fixed);

        prune(thin, fixed);

        return thin;
    }

    private static Set<Long> thin(Set<Long> mask, Set<Long> fixed) {
        if (mask.isEmpty()) {
            return new HashSet<>();
        }

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (long at : mask) {
            minX = Math.min(minX, ColonyRoads.xOf(at));
            minZ = Math.min(minZ, ColonyRoads.zOf(at));
            maxX = Math.max(maxX, ColonyRoads.xOf(at));
            maxZ = Math.max(maxZ, ColonyRoads.zOf(at));
        }

        int width = maxX - minX + 3;
        int depth = maxZ - minZ + 3;

        if ((long) width * depth > MAX_WINDOW) {
            return new HashSet<>(mask);
        }

        boolean[] grid = new boolean[width * depth];
        boolean[] kept = new boolean[width * depth];

        for (long at : mask) {
            grid[(ColonyRoads.zOf(at) - minZ + 1) * width + ColonyRoads.xOf(at) - minX + 1] = true;
        }

        for (long at : fixed) {
            int x = ColonyRoads.xOf(at) - minX + 1;
            int z = ColonyRoads.zOf(at) - minZ + 1;

            if (x > 0 && z > 0 && x < width - 1 && z < depth - 1) {
                kept[z * width + x] = true;
            }
        }

        boolean changed = true;

        while (changed) {
            changed = pass(grid, kept, width, depth, true);
            changed |= pass(grid, kept, width, depth, false);
        }

        Set<Long> thin = new HashSet<>();

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                if (grid[z * width + x]) {
                    thin.add(ColonyRoads.column(x + minX - 1, z + minZ - 1));
                }
            }
        }

        return thin;
    }

    private static boolean pass(boolean[] grid, boolean[] kept, int width, int depth, boolean first) {
        List<Integer> doomed = new ArrayList<>();

        for (int z = 1; z < depth - 1; z++) {
            for (int x = 1; x < width - 1; x++) {
                int at = z * width + x;

                if (grid[at] && !kept[at] && removable(grid, width, at, first)) {
                    doomed.add(at);
                }
            }
        }

        doomed.forEach(at -> grid[at] = false);

        return !doomed.isEmpty();
    }

    private static boolean removable(boolean[] grid, int width, int at, boolean first) {
        boolean north = grid[at - width];
        boolean east = grid[at + 1];
        boolean south = grid[at + width];
        boolean west = grid[at - 1];
        boolean[] ring = {north, grid[at - width + 1], east, grid[at + width + 1], south, grid[at + width - 1], west,
            grid[at - width - 1]};
        int neighbours = 0;
        int transitions = 0;

        for (int i = 0; i < ring.length; i++) {
            neighbours += ring[i] ? 1 : 0;
            transitions += !ring[i] && ring[(i + 1) % ring.length] ? 1 : 0;
        }

        if (neighbours < 2 || neighbours > 6 || transitions != 1) {
            return false;
        }

        return first
                ? !(north && east && south) && !(east && south && west)
                : !(north && east && west) && !(north && south && west);
    }

    /** Quantos ramos saem desta coluna: 1 é ponta, 2 é meio de linha, 3 ou mais é encontro. */
    static int branches(Set<Long> line, long at) {
        int x = ColonyRoads.xOf(at);
        int z = ColonyRoads.zOf(at);
        int transitions = 0;
        boolean previous = line.contains(ColonyRoads.column(x + RING[RING.length - 1][0], z + RING[RING.length - 1][1]));

        for (int[] way : RING) {
            boolean here = line.contains(ColonyRoads.column(x + way[0], z + way[1]));

            transitions += !previous && here ? 1 : 0;
            previous = here;
        }

        return transitions;
    }

    /** Os vizinhos de uma coluna na linha, os de lado (norte, leste, sul, oeste) primeiro. */
    static List<Long> neighbours(Set<Long> line, long at) {
        int x = ColonyRoads.xOf(at);
        int z = ColonyRoads.zOf(at);
        List<Long> found = new ArrayList<>();

        for (int i = 0; i < RING.length; i += 2) {
            long next = ColonyRoads.column(x + RING[i][0], z + RING[i][1]);

            if (line.contains(next)) {
                found.add(next);
            }
        }

        for (int i = 1; i < RING.length; i += 2) {
            long next = ColonyRoads.column(x + RING[i][0], z + RING[i][1]);

            if (line.contains(next)) {
                found.add(next);
            }
        }

        return found;
    }

    private static void prune(Set<Long> line, Set<Long> fixed) {
        for (int round = 0; round < 3; round++) {
            boolean removed = false;

            for (long at : List.copyOf(line)) {
                if (line.contains(at) && !fixed.contains(at) && branches(line, at) == 1) {
                    removed |= pruneSpur(line, fixed, at);
                }
            }

            if (!removed) {
                return;
            }
        }
    }

    private static boolean pruneSpur(Set<Long> line, Set<Long> fixed, long tip) {
        List<Long> spur = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        long at = tip;

        while (spur.size() <= MAX_SPUR) {
            spur.add(at);
            seen.add(at);

            Long next = null;

            for (long candidate : neighbours(line, at)) {
                if (!seen.contains(candidate)) {
                    next = candidate;
                    break;
                }
            }

            if (next == null || fixed.contains(next)) {
                return false;
            }

            if (branches(line, next) >= 3) {
                spur.forEach(line::remove);
                return true;
            }

            at = next;
        }

        return false;
    }
}
