package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Grafo derivado de ruas fisicamente confirmadas, sem escrita no mundo — ADR-043.
 * Cadeias entre nós preservam todas as colunas; diagonais e falésias não criam ligações.
 */
public record RoadGraph(List<Node> nodes, List<Edge> edges) {
    private static final Comparator<ColonyPos> ORDER = Comparator.comparingInt(ColonyPos::x)
            .thenComparingInt(ColonyPos::y).thenComparingInt(ColonyPos::z);

    public enum Role { HUB, ENTRANCE, JUNCTION, ENDPOINT, CYCLE_ANCHOR }

    public enum RoadClass { PRIMARY, SECONDARY }

    public record Node(ColonyPos position, Set<Role> roles) {
        public Node {
            Objects.requireNonNull(position, "position");
            roles = Set.copyOf(roles);
        }
    }

    public record Edge(ColonyPos from, ColonyPos to, List<ColonyPos> columns, RoadClass roadClass) {
        public Edge {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            Objects.requireNonNull(roadClass, "roadClass");
            columns = List.copyOf(columns);
        }
    }

    private record Segment(ColonyPos low, ColonyPos high) {
        static Segment between(ColonyPos a, ColonyPos b) {
            return ORDER.compare(a, b) <= 0 ? new Segment(a, b) : new Segment(b, a);
        }
    }

    public RoadGraph {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    /**
     * Contrai ruas confirmadas sem inventar colunas para centro ou entradas ausentes — ADR-043.
     * O chamador confirma apoio, passagem e validade no mundo antes de fornecer as posições.
     */
    public static RoadGraph from(Set<ColonyPos> confirmed, ColonyPos hub, Set<ColonyPos> entrances) {
        Objects.requireNonNull(hub, "hub");
        Objects.requireNonNull(entrances, "entrances");
        List<ColonyPos> ordered = confirmed.stream().sorted(ORDER).toList();
        Map<ColonyPos, List<ColonyPos>> adjacent = adjacency(confirmed, ordered);
        Set<ColonyPos> anchors = new HashSet<>();

        for (ColonyPos position : ordered) {
            if (Objects.requireNonNull(adjacent.get(position)).size() != 2 || position.equals(hub) || entrances.contains(position)) {
                anchors.add(position);
            }
        }

        Set<ColonyPos> primary = primaryComponent(ordered, adjacent, anchors, hub);
        return contract(ordered, adjacent, anchors, primary, hub, entrances);
    }

    private static Set<ColonyPos> primaryComponent(List<ColonyPos> ordered,
            Map<ColonyPos, List<ColonyPos>> adjacent, Set<ColonyPos> anchors, ColonyPos hub) {
        Set<ColonyPos> primary = new HashSet<>();
        Set<ColonyPos> seen = new HashSet<>();
        for (ColonyPos position : ordered) {
            if (seen.contains(position)) {
                continue;
            }
            Set<ColonyPos> component = component(position, adjacent);
            seen.addAll(component);
            if (component.contains(hub)) {
                primary.addAll(component);
            }
            if (component.stream().noneMatch(anchors::contains)) {
                anchors.add(position);
            }
        }
        return primary;
    }

    private static RoadGraph contract(List<ColonyPos> ordered, Map<ColonyPos, List<ColonyPos>> adjacent,
            Set<ColonyPos> anchors, Set<ColonyPos> primary, ColonyPos hub, Set<ColonyPos> entrances) {
        List<Node> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        Set<Segment> visited = new HashSet<>();
        for (ColonyPos position : ordered) {
            if (!anchors.contains(position)) {
                continue;
            }
            List<ColonyPos> neighbors = Objects.requireNonNull(adjacent.get(position));
            nodes.add(new Node(position, roles(position, hub, entrances, neighbors.size())));
            for (ColonyPos next : neighbors) {
                if (visited.contains(Segment.between(position, next))) {
                    continue;
                }
                List<ColonyPos> columns = trace(position, next, adjacent, anchors, visited);
                edges.add(new Edge(position, columns.getLast(), columns,
                        primary.contains(position) ? RoadClass.PRIMARY : RoadClass.SECONDARY));
            }
        }
        return new RoadGraph(nodes, edges);
    }

    private static Map<ColonyPos, List<ColonyPos>> adjacency(Set<ColonyPos> confirmed,
                                                           List<ColonyPos> ordered) {
        Map<ColonyPos, List<ColonyPos>> result = new HashMap<>();
        int[][] directions = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
        for (ColonyPos at : ordered) {
            List<ColonyPos> neighbors = new ArrayList<>();
            for (int[] direction : directions) {
                for (int dy = -1; dy <= 1; dy++) {
                    ColonyPos next = new ColonyPos(at.x() + direction[0], at.y() + dy, at.z() + direction[1]);
                    if (confirmed.contains(next)) {
                        neighbors.add(next);
                    }
                }
            }
            neighbors.sort(ORDER);
            result.put(at, neighbors);
        }
        return result;
    }

    private static Set<ColonyPos> component(ColonyPos start, Map<ColonyPos, List<ColonyPos>> adjacent) {
        Set<ColonyPos> found = new LinkedHashSet<>();
        ArrayDeque<ColonyPos> pending = new ArrayDeque<>();
        found.add(start);
        pending.add(start);
        while (!pending.isEmpty()) {
            for (ColonyPos next : Objects.requireNonNull(adjacent.get(pending.removeFirst()))) {
                if (found.add(next)) {
                    pending.addLast(next);
                }
            }
        }
        return found;
    }

    private static List<ColonyPos> trace(ColonyPos start, ColonyPos next,
                                         Map<ColonyPos, List<ColonyPos>> adjacent,
                                         Set<ColonyPos> anchors, Set<Segment> visited) {
        List<ColonyPos> columns = new ArrayList<>();
        columns.add(start);
        ColonyPos previous = start;
        ColonyPos current = next;
        while (true) {
            visited.add(Segment.between(previous, current));
            columns.add(current);
            if (anchors.contains(current)) {
                return columns;
            }
            List<ColonyPos> neighbors = Objects.requireNonNull(adjacent.get(current));
            ColonyPos following = neighbors.getFirst().equals(previous) ? neighbors.getLast() : neighbors.getFirst();
            previous = current;
            current = following;
        }
    }

    private static Set<Role> roles(ColonyPos position, ColonyPos hub, Set<ColonyPos> entrances, int degree) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        if (position.equals(hub)) {
            roles.add(Role.HUB);
        }
        if (entrances.contains(position)) {
            roles.add(Role.ENTRANCE);
        }
        if (degree > 2) {
            roles.add(Role.JUNCTION);
        } else if (degree < 2) {
            roles.add(Role.ENDPOINT);
        }
        if (roles.isEmpty()) {
            roles.add(Role.CYCLE_ANCHOR);
        }
        return roles;
    }
}
