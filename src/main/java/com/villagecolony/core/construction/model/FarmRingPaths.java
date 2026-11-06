package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Plantação não tem anel — Regra 208 (autor, 2026-11-06): o caminho colado numa plantação sai do grafo quando a rede
 * continua inteira sem ele. Encontro, entrada e centro ficam, e o trecho que é a única ligação da rede também.
 */
final class FarmRingPaths {

    private FarmRingPaths() {
    }

    static void retire(Map<Long, Integer> columns, Set<Long> ring, ColonyPos center, Set<Long> doors) {
        if (ring.isEmpty()) {
            return;
        }

        for (List<Long> stretch : stretches(columns, ring, RoadTips.nearest(columns, center), doors)) {
            int parts = RoadLines.components(columns).size();
            Map<Long, Integer> removed = new HashMap<>();

            for (long column : stretch) {
                removed.put(column, Objects.requireNonNull(columns.remove(column)));
            }

            if (RoadLines.components(columns).size() > parts) {
                columns.putAll(removed);
            }
        }
    }

    /** Os trechos de passagem colados nas plantações, do mais longo para o mais curto: o laço cai antes do lado que liga. */
    private static List<List<Long>> stretches(Map<Long, Integer> columns, Set<Long> ring, long hub, Set<Long> doors) {
        Set<Long> free = new HashSet<>();

        for (long column : ring) {
            if (column != hub && !doors.contains(column) && RoadTips.neighbours(columns, column).size() <= 2) {
                free.add(column);
            }
        }

        List<List<Long>> stretches = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (long start : free.stream().sorted().toList()) {
            if (seen.add(start)) {
                stretches.add(flood(columns, free, seen, start));
            }
        }

        stretches.sort(Comparator.comparingInt((List<Long> stretch) -> -stretch.size()).thenComparingLong(List::getFirst));

        return stretches;
    }

    private static List<Long> flood(Map<Long, Integer> columns, Set<Long> free, Set<Long> seen, long start) {
        List<Long> stretch = new ArrayList<>();
        Deque<Long> open = new ArrayDeque<>();

        open.add(start);

        while (!open.isEmpty()) {
            long at = open.poll();

            stretch.add(at);

            for (long next : RoadTips.neighbours(columns, at)) {
                if (free.contains(next) && seen.add(next)) {
                    open.add(next);
                }
            }
        }

        return stretch;
    }
}
