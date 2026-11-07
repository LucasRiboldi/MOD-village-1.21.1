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
import java.util.Set;

/**
 * O ramal que perdeu a entrada — Regra 223 (autor, 2026-11-07): sem entrada não há ramal, nem no grafo nem no chão. Parte da
 * coluna que deixou de ser entrada e segue o caminho físico até a linha, a qualquer distância. Trecho maior que
 * {@value #MAX_CELLS} colunas é rua, e fica: a rua da vila gerada e o caminho do jogador não saem por aqui.
 */
public final class RetiredSpurs {

    static final int MAX_CELLS = 32;

    private RetiredSpurs() {
    }

    /** As colunas de caminho que sobraram dos ramais de {@code retired}, em ordem estável. */
    public static List<ColonyPos> of(Set<ColonyPos> physical, RoadGraph graph, Set<ColonyPos> retired) {
        Map<Long, ColonyPos> paths = new HashMap<>();

        physical.forEach(at -> paths.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at));

        Set<Long> kept = new HashSet<>();

        graph.nodes().forEach(node -> kept.add(ColonyRoads.column(node.position().x(), node.position().z())));
        graph.edges().forEach(edge -> edge.columns().forEach(at -> kept.add(ColonyRoads.column(at.x(), at.z()))));

        Set<ColonyPos> spurs = new HashSet<>();

        for (ColonyPos start : retired) {
            spurs.addAll(spurFrom(ColonyRoads.column(start.x(), start.z()), paths, kept));
        }

        List<ColonyPos> ordered = new ArrayList<>(spurs);

        ordered.sort(Comparator.comparingInt(ColonyPos::x).thenComparingInt(ColonyPos::z));

        return ordered;
    }

    private static Set<ColonyPos> spurFrom(long start, Map<Long, ColonyPos> paths, Set<Long> kept) {
        Set<ColonyPos> found = new HashSet<>();

        if (kept.contains(start) || !paths.containsKey(start)) {
            return found;
        }

        Set<Long> seen = new HashSet<>(Set.of(start));
        Deque<Long> open = new ArrayDeque<>(List.of(start));

        while (!open.isEmpty()) {
            long at = open.poll();

            found.add(paths.get(at));

            if (found.size() > MAX_CELLS) {
                return Set.of();
            }

            int x = ColonyRoads.xOf(at);
            int z = ColonyRoads.zOf(at);

            for (long next : List.of(ColonyRoads.column(x + 1, z), ColonyRoads.column(x - 1, z),
                    ColonyRoads.column(x, z + 1), ColonyRoads.column(x, z - 1))) {
                if (paths.containsKey(next) && !kept.contains(next) && seen.add(next)) {
                    open.add(next);
                }
            }
        }

        return found;
    }
}
