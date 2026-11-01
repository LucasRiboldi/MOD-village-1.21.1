package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Rotas da hierarquia medidas pelos trechos físicos confirmados — ADR-043. */
public final class RoadGraphRoutes {
    private record Visit(ColonyPos position, int distance) {
    }

    private RoadGraphRoutes() {
    }

    /** Distâncias ao centro; exclui componentes desconectados e preserva as cotas — ADR-043. */
    public static Map<ColonyPos, Integer> distancesToHub(RoadGraph graph) {
        Map<ColonyPos, List<RoadGraph.Edge>> adjacent = new HashMap<>();
        for (RoadGraph.Edge edge : graph.edges()) {
            adjacent.computeIfAbsent(edge.from(), ignored -> new ArrayList<>()).add(edge);
            adjacent.computeIfAbsent(edge.to(), ignored -> new ArrayList<>()).add(edge);
        }

        Map<ColonyPos, Integer> nodes = new HashMap<>();
        PriorityQueue<Visit> queue = new PriorityQueue<>(Comparator.comparingInt(Visit::distance)
                .thenComparingInt(visit -> visit.position().x())
                .thenComparingInt(visit -> visit.position().y()).thenComparingInt(visit -> visit.position().z()));
        for (RoadGraph.Node node : graph.nodes()) {
            if (node.roles().contains(RoadGraph.Role.HUB)) {
                nodes.put(node.position(), 0);
                queue.add(new Visit(node.position(), 0));
            }
        }

        while (!queue.isEmpty()) {
            Visit visit = queue.remove();
            if (visit.distance() != nodes.getOrDefault(visit.position(), Integer.MAX_VALUE)) {
                continue;
            }
            for (RoadGraph.Edge edge : adjacent.getOrDefault(visit.position(), List.of())) {
                ColonyPos next = edge.from().equals(visit.position()) ? edge.to() : edge.from();
                int distance = visit.distance() + edge.columns().size() - 1;
                if (distance < nodes.getOrDefault(next, Integer.MAX_VALUE)) {
                    nodes.put(next, distance);
                    queue.add(new Visit(next, distance));
                }
            }
        }

        return columns(graph, nodes);
    }

    private static Map<ColonyPos, Integer> columns(RoadGraph graph, Map<ColonyPos, Integer> nodes) {
        Map<ColonyPos, Integer> distances = new HashMap<>(nodes);
        for (RoadGraph.Edge edge : graph.edges()) {
            Integer from = nodes.get(edge.from());
            Integer to = nodes.get(edge.to());
            if (from == null || to == null) {
                continue;
            }
            int length = edge.columns().size() - 1;
            for (int index = 0; index <= length; index++) {
                int distance = Math.min(from + index, to + length - index);
                distances.merge(edge.columns().get(index), distance, Math::min);
            }
        }
        return Map.copyOf(distances);
    }
}
