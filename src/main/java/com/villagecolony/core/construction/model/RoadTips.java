package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * As pontas do grafo são os cantos da vila — Regra 206 (autor, 2026-11-05): fica só a ponta mais distante do centro em cada
 * quadrante, no máximo quatro, por onde a vila pode crescer. As outras perdem o trecho morto até o encontro, a entrada ou a
 * quina fixa mais perto. Entrada e centro não são pontas e nunca saem.
 */
final class RoadTips {

    private static final int MAX_STEP = 1;

    private RoadTips() {
    }

    static void prune(Map<Long, Integer> columns, ColonyPos center, Set<Long> fixed) {
        if (columns.isEmpty()) {
            return;
        }

        long hub = nearest(columns, center);
        List<Long> ends = new ArrayList<>();

        for (long column : columns.keySet()) {
            if (column != hub && !fixed.contains(column) && neighbours(columns, column).size() == 1) {
                ends.add(column);
            }
        }

        ends.sort(Comparator.comparingInt((Long column) -> -distance(column, center))
                .thenComparingInt(ColonyRoads::xOf).thenComparingInt(ColonyRoads::zOf));

        Set<Integer> quadrants = new HashSet<>();
        Set<Long> kept = new HashSet<>();

        for (long end : ends) {
            if (quadrants.add(quadrant(end, center))) {
                kept.add(end);
            }
        }

        for (long end : ends) {
            if (!kept.contains(end)) {
                cutBack(columns, end, hub, fixed, kept);
            }
        }
    }

    private static void cutBack(Map<Long, Integer> columns, long end, long hub, Set<Long> fixed, Set<Long> kept) {
        long at = end;

        while (true) {
            List<Long> next = neighbours(columns, at);

            if (next.size() != 1) {
                return;
            }

            columns.remove(at);
            at = next.getFirst();

            if (at == hub || fixed.contains(at) || kept.contains(at)) {
                return;
            }
        }
    }

    private static List<Long> neighbours(Map<Long, Integer> columns, long column) {
        List<Long> result = new ArrayList<>();
        int y = java.util.Objects.requireNonNull(columns.get(column));

        for (Side side : Side.values()) {
            long next = ColonyRoads.column(ColonyRoads.xOf(column) + side.offsetX(), ColonyRoads.zOf(column) + side.offsetZ());
            Integer height = columns.get(next);

            if (height != null && Math.abs(height - y) <= MAX_STEP) {
                result.add(next);
            }
        }

        return result;
    }

    private static long nearest(Map<Long, Integer> columns, ColonyPos center) {
        return columns.keySet().stream()
                .min(Comparator.comparingInt((Long column) -> distance(column, center))
                        .thenComparingInt(ColonyRoads::xOf).thenComparingInt(ColonyRoads::zOf))
                .orElseThrow();
    }

    private static int distance(long column, ColonyPos center) {
        return Math.abs(ColonyRoads.xOf(column) - center.x()) + Math.abs(ColonyRoads.zOf(column) - center.z());
    }

    private static int quadrant(long column, ColonyPos center) {
        return (ColonyRoads.xOf(column) >= center.x() ? 1 : 0) + (ColonyRoads.zOf(column) >= center.z() ? 2 : 0);
    }
}
