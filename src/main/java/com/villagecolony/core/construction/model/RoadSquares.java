package com.villagecolony.core.construction.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A rua é uma linha: quatro colunas em quadrado fariam um nó em cada uma — Regra 223 (autor, 2026-11-07). Tira do quadrado a
 * coluna que não é porta e cuja falta não separa os vizinhos, até não sobrar quadrado.
 */
final class RoadSquares {

    /** Até onde em volta da coluna se procura a volta entre os vizinhos dela. */
    private static final int AROUND = 8;

    private RoadSquares() {
    }

    static void thin(Map<Long, Integer> columns, Set<Long> doors) {
        boolean changed = true;

        while (changed) {
            changed = false;

            for (long at : new TreeSet<>(columns.keySet())) {
                if (!doors.contains(at) && inASquare(columns, at) && staysConnectedWithout(columns, at)) {
                    columns.remove(at);
                    changed = true;
                }
            }
        }
    }

    private static boolean inASquare(Map<Long, Integer> columns, long at) {
        int x = ColonyRoads.xOf(at);
        int z = ColonyRoads.zOf(at);

        for (int dx : new int[] {-1, 1}) {
            for (int dz : new int[] {-1, 1}) {
                if (columns.containsKey(ColonyRoads.column(x + dx, z)) && columns.containsKey(ColonyRoads.column(x, z + dz))
                        && columns.containsKey(ColonyRoads.column(x + dx, z + dz))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static List<Long> neighbors(Map<Long, Integer> columns, long at) {
        int x = ColonyRoads.xOf(at);
        int z = ColonyRoads.zOf(at);
        List<Long> found = new ArrayList<>();

        for (long next : List.of(ColonyRoads.column(x + 1, z), ColonyRoads.column(x - 1, z),
                ColonyRoads.column(x, z + 1), ColonyRoads.column(x, z - 1))) {
            if (columns.containsKey(next)) {
                found.add(next);
            }
        }

        return found;
    }

    /** Se os vizinhos desta coluna ainda se alcançam sem ela, por perto. */
    private static boolean staysConnectedWithout(Map<Long, Integer> columns, long at) {
        List<Long> around = neighbors(columns, at);
        int x = ColonyRoads.xOf(at);
        int z = ColonyRoads.zOf(at);
        Set<Long> seen = new HashSet<>(List.of(at, around.getFirst()));
        Deque<Long> open = new ArrayDeque<>(List.of(around.getFirst()));

        while (!open.isEmpty()) {
            for (long next : neighbors(columns, open.poll())) {
                if (Math.abs(ColonyRoads.xOf(next) - x) <= AROUND && Math.abs(ColonyRoads.zOf(next) - z) <= AROUND && seen.add(next)) {
                    open.add(next);
                }
            }
        }

        return seen.containsAll(around);
    }
}
