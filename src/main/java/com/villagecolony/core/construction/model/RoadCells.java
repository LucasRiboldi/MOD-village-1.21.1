package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Que blocos de caminho entram no grafo — Regra 224 (autor, 2026-11-10): todo caminho da caixa da vila, mais o de fora dela
 * que encosta nele. Acima do teto, ficam os mais perto do centro, e o resto é contado em vez de calar o grafo. Puro.
 */
public final class RoadCells {

    private RoadCells() {
    }

    /** O caminho de fora da caixa ligado, passo a passo, a algum caminho de dentro. */
    public static Set<ColonyPos> touching(Set<ColonyPos> inside, Set<ColonyPos> outside) {
        Map<Long, ColonyPos> loose = new HashMap<>();

        outside.forEach(at -> loose.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at));

        Deque<Long> open = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();

        inside.forEach(at -> {
            long column = ColonyRoads.column(at.x(), at.z());

            if (seen.add(column)) {
                open.add(column);
            }
        });

        Set<ColonyPos> reached = new LinkedHashSet<>();

        while (!open.isEmpty()) {
            long at = open.poll();
            int x = ColonyRoads.xOf(at);
            int z = ColonyRoads.zOf(at);

            for (long next : List.of(ColonyRoads.column(x + 1, z), ColonyRoads.column(x - 1, z),
                    ColonyRoads.column(x, z + 1), ColonyRoads.column(x, z - 1))) {
                ColonyPos cell = loose.get(next);

                if (cell != null && seen.add(next)) {
                    reached.add(cell);
                    open.add(next);
                }
            }
        }

        return reached;
    }

    /** No máximo {@code max} blocos, os mais perto do centro; ordem estável para o grafo não oscilar. */
    public static Set<ColonyPos> nearest(Set<ColonyPos> cells, ColonyPos center, int max) {
        if (cells.size() <= max) {
            return cells;
        }

        Set<ColonyPos> kept = new LinkedHashSet<>();

        cells.stream()
                .sorted(Comparator.comparingInt((ColonyPos at) -> Math.abs(at.x() - center.x()) + Math.abs(at.z() - center.z()))
                        .thenComparingInt(ColonyPos::x).thenComparingInt(ColonyPos::z))
                .limit(max)
                .forEach(kept::add);

        return kept;
    }
}
