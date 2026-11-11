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
 * As pontas do grafo são os lados da vila — Regras 206, 229 e 231 (autor, 2026-11-10): fica só a ponta mais distante do centro em
 * cada lado, norte, sul, leste e oeste ({@link RoadSides}), no máximo quatro, por onde a vila cresce para fora. A ponta só vale
 * na extremidade do grafo inteiro naquele lado, a até {@value #BAND} blocos da coluna mais externa, e a que aponta para fora
 * vem antes. Ponta no meio da vila, entre linhas, perde o trecho morto até o encontro ou a entrada mais perto. Entrada e centro
 * não são pontas e nunca saem.
 */
final class RoadTips {

    private static final int MAX_STEP = 1;

    /** A ponta precisa estar a até esta distância da extremidade do grafo, no lado dela (Regra 231). */
    static final int BAND = 8;

    private RoadTips() {
    }

    /** Poda as pontas; a que termina numa coluna de {@code deadEnds}, uma entrada que saiu, nunca é a do lado (Regra 229). */
    static void prune(Map<Long, Integer> columns, ColonyPos center, Set<Long> fixed, Set<Long> deadEnds) {
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

        Map<Side, Integer> extreme = new java.util.EnumMap<>(Side.class);

        for (long column : columns.keySet()) {
            for (Side side : Side.values()) {
                extreme.merge(side, outward(side, ColonyRoads.xOf(column), ColonyRoads.zOf(column)), Math::max);
            }
        }

        ends.sort(Comparator.comparing((Long column) -> !pointsOutward(columns, column, center))
                .thenComparingInt((Long column) -> -distance(column, center))
                .thenComparingInt(ColonyRoads::xOf).thenComparingInt(ColonyRoads::zOf));

        Set<Side> sides = new HashSet<>();
        Set<Long> kept = new HashSet<>();

        for (long end : ends) {
            Side side = RoadSides.of(ColonyRoads.xOf(end), ColonyRoads.zOf(end), center);

            if (!deadEnds.contains(end) && atTheEdge(side, end, extreme) && sides.add(side)) {
                kept.add(end);
            }
        }

        for (long end : ends) {
            if (!kept.contains(end)) {
                cutBack(columns, end, hub, fixed, kept);
            }
        }
    }

    /** Quanto a coluna avança para fora neste lado: o x no leste, o -x no oeste, o z no sul, o -z no norte. */
    private static int outward(Side side, int x, int z) {
        return x * side.offsetX() + z * side.offsetZ();
    }

    private static boolean atTheEdge(Side side, long end, Map<Side, Integer> extreme) {
        return extreme.getOrDefault(side, Integer.MIN_VALUE) - outward(side, ColonyRoads.xOf(end), ColonyRoads.zOf(end)) <= BAND;
    }

    /** Se a linha chega à ponta vindo de dentro e segue para fora do lado dela (Regra 231). */
    private static boolean pointsOutward(Map<Long, Integer> columns, long end, ColonyPos center) {
        Side side = RoadSides.of(ColonyRoads.xOf(end), ColonyRoads.zOf(end), center);
        List<Long> next = neighbours(columns, end);

        return next.size() == 1
                && outward(side, ColonyRoads.xOf(next.getFirst()), ColonyRoads.zOf(next.getFirst()))
                < outward(side, ColonyRoads.xOf(end), ColonyRoads.zOf(end));
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

    static List<Long> neighbours(Map<Long, Integer> columns, long column) {
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

    static long nearest(Map<Long, Integer> columns, ColonyPos center) {
        return columns.keySet().stream()
                .min(Comparator.comparingInt((Long column) -> distance(column, center))
                        .thenComparingInt(ColonyRoads::xOf).thenComparingInt(ColonyRoads::zOf))
                .orElseThrow();
    }

    private static int distance(long column, ColonyPos center) {
        return Math.abs(ColonyRoads.xOf(column) - center.x()) + Math.abs(ColonyRoads.zOf(column) - center.z());
    }
}
