package com.villagecolony.core.construction.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * A faixa de caminho da vila vista de cima, antes de virar linha — Regra 177 (autor, 2026-11-05).
 *
 * <p>Fecha o que interrompe a faixa: vão de até {@value #MAX_GAP} blocos entre dois blocos de caminho, e buraco cercado
 * de caminho até {@value #MAX_HOLE} blocos. Só fecha onde o chão serve e o degrau fica em um bloco. Puro: as colunas
 * são {@link ColonyRoads#column}, com a altura do piso.
 */
final class RoadMask {

    /** O maior vão que ainda liga dois blocos de caminho. */
    static final int MAX_GAP = 2;

    /** O maior buraco cercado de caminho que vira caminho. */
    static final int MAX_HOLE = 25;

    private static final int[][] CARDINAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private RoadMask() {
    }

    /** A faixa com os vãos e buracos fechados; as colunas novas são as que faltam em {@code cells}. */
    static Map<Long, Integer> closed(Map<Long, Integer> cells, PathTerrain terrain) {
        Map<Long, Integer> mask = new HashMap<>(cells);

        for (Map.Entry<Long, Integer> cell : cells.entrySet()) {
            closeGap(cells, mask, terrain, cell.getKey(), cell.getValue(), 1, 0);
            closeGap(cells, mask, terrain, cell.getKey(), cell.getValue(), 0, 1);
        }

        fillHoles(mask, terrain);

        return mask;
    }

    private static void closeGap(Map<Long, Integer> cells, Map<Long, Integer> mask, PathTerrain terrain,
                                 long from, int fromY, int dx, int dz) {
        int x = ColonyRoads.xOf(from);
        int z = ColonyRoads.zOf(from);

        if (cells.containsKey(ColonyRoads.column(x + dx, z + dz))) {
            return;
        }

        for (int gap = 1; gap <= MAX_GAP; gap++) {
            Integer end = cells.get(ColonyRoads.column(x + dx * (gap + 1), z + dz * (gap + 1)));

            if (end != null) {
                fill(mask, terrain, x, z, fromY, end, dx, dz, gap);
                return;
            }
        }
    }

    private static void fill(Map<Long, Integer> mask, PathTerrain terrain, int x, int z, int fromY, int endY,
                             int dx, int dz, int gap) {
        List<int[]> found = new ArrayList<>();
        int previous = fromY;

        for (int step = 1; step <= gap; step++) {
            int cx = x + dx * step;
            int cz = z + dz * step;
            OptionalInt ground = terrain.groundY(cx, cz, previous);

            if (ground.isEmpty() || terrain.blocked(cx, cz) || Math.abs(ground.getAsInt() - previous) > 1) {
                return;
            }

            previous = ground.getAsInt();
            found.add(new int[] {cx, previous, cz});
        }

        if (Math.abs(endY - previous) > 1) {
            return;
        }

        for (int[] cell : found) {
            mask.putIfAbsent(ColonyRoads.column(cell[0], cell[2]), cell[1]);
        }
    }

    private static void fillHoles(Map<Long, Integer> mask, PathTerrain terrain) {
        Set<Long> outside = new HashSet<>();

        for (Map.Entry<Long, Integer> cell : new ArrayList<>(mask.entrySet())) {
            int x = ColonyRoads.xOf(cell.getKey());
            int z = ColonyRoads.zOf(cell.getKey());

            for (int[] way : CARDINAL) {
                long start = ColonyRoads.column(x + way[0], z + way[1]);

                if (!mask.containsKey(start) && !outside.contains(start)) {
                    fillHole(mask, terrain, start, cell.getValue(), outside);
                }
            }
        }
    }

    private static void fillHole(Map<Long, Integer> mask, PathTerrain terrain, long start, int nearY, Set<Long> outside) {
        Set<Long> hole = new HashSet<>();
        Deque<Long> open = new ArrayDeque<>();

        hole.add(start);
        open.add(start);

        while (!open.isEmpty()) {
            long at = open.poll();

            for (int[] way : CARDINAL) {
                long next = ColonyRoads.column(ColonyRoads.xOf(at) + way[0], ColonyRoads.zOf(at) + way[1]);

                if (mask.containsKey(next) || hole.contains(next)) {
                    continue;
                }

                if (outside.contains(next) || hole.size() >= MAX_HOLE) {
                    outside.addAll(hole);
                    return;
                }

                hole.add(next);
                open.add(next);
            }
        }

        Map<Long, Integer> ground = new HashMap<>();

        for (long at : hole) {
            int x = ColonyRoads.xOf(at);
            int z = ColonyRoads.zOf(at);
            OptionalInt y = terrain.groundY(x, z, nearY);

            // Buraco com construção ou água é de alguém: a faixa o contorna.
            if (y.isEmpty() || terrain.blocked(x, z) || Math.abs(y.getAsInt() - nearY) > MAX_GAP) {
                outside.addAll(hole);
                return;
            }

            ground.put(at, y.getAsInt());
        }

        mask.putAll(ground);
    }
}
