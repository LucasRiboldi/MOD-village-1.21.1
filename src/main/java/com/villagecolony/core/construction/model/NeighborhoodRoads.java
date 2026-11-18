package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

/**
 * O modo 3 do grafo, plano primeiro — ADR-057 (autor, 2026-11-18). Os nós são o centro e as entradas; dois nós ligam se
 * nenhum terceiro está mais perto dos dois do que eles estão um do outro (grafo de vizinhança relativa, com voltas como numa
 * vila de verdade). Cada ligação é traçada por A* sobre o chão: plano {@value #STEP}, degrau de 1 bloco +{@value #CLIMB},
 * sobre caminho que já existe ×{@value #ON_PATH}, curva +{@value #TURN}; água, construção e degrau de mais de 1 bloco não
 * passam. A ligação traçada fica guardada por par de nós e só é refeita quando um quadrado sujo ({@link RoadRegions}) a
 * cruza. Puro: o chão vem por {@link PathTerrain}.
 */
public final class NeighborhoodRoads {

    static final double STEP = 1.0;

    static final double CLIMB = 2.0;

    static final double ON_PATH = 0.3;

    static final double TURN = 0.5;

    /** A ligação mais longa que se tenta. */
    static final int MAX_EDGE = 96;

    /** A folga da busca em volta dos dois nós. */
    static final int SEARCH_MARGIN = 16;

    /** O teto de colunas abertas por ligação. */
    static final int MAX_EXPANSIONS = 8_000;

    /** Quantas ligações novas ou sujas se traçam por montagem; as outras esperam a próxima. */
    public static final int ROUTES_PER_BUILD = 16;

    /** As ligações já traçadas, por par de nós (colunas empacotadas, a menor primeiro). */
    public static final class Routes {

        private final Map<List<Long>, List<ColonyPos>> byPair = new HashMap<>();

        /** Quantas ligações estão guardadas. */
        public int size() {
            return byPair.size();
        }
    }

    private NeighborhoodRoads() {
    }

    /** O grafo do modo 3. */
    public static RoadLines.Result build(Set<ColonyPos> cells, ColonyPos center, Set<ColonyPos> fronts, PathTerrain terrain,
                                         Routes routes, Set<Long> dirty) {
        Map<Long, Integer> physical = new HashMap<>();

        cells.forEach(at -> physical.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));

        Map<Long, ColonyPos> nodes = new TreeMap<>();
        int hubY = terrain.groundY(center.x(), center.z(), center.y()).orElse(center.y());

        nodes.put(ColonyRoads.column(center.x(), center.z()), new ColonyPos(center.x(), hubY, center.z()));
        fronts.forEach(at -> nodes.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at));

        List<List<Long>> pairs = neighborhood(nodes);
        Map<Long, Integer> columns = new LinkedHashMap<>();
        Set<Long> linked = new HashSet<>();
        int routed = 0;

        routes.byPair.keySet().retainAll(new HashSet<>(pairs));

        for (List<Long> pair : pairs) {
            List<ColonyPos> route = routes.byPair.get(pair);
            boolean stale = route == null || route.stream().anyMatch(at -> dirty.contains(RoadRegions.regionOf(at.x(), at.z())));

            ColonyPos first = nodes.get(pair.get(0));
            ColonyPos second = nodes.get(pair.get(1));

            if (stale && routed < ROUTES_PER_BUILD && first != null && second != null) {
                routed++;
                route = route(first, second, physical, terrain).orElse(null);

                if (route == null) {
                    routes.byPair.remove(pair);
                } else {
                    routes.byPair.put(pair, route);
                }
            }

            if (route != null) {
                route.forEach(at -> columns.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));
                linked.addAll(pair);
            }
        }

        Map<Long, Integer> toLay = new HashMap<>();
        Set<Long> doors = new HashSet<>();

        fronts.forEach(at -> {
            long column = ColonyRoads.column(at.x(), at.z());

            toLay.putIfAbsent(column, at.y());
            doors.add(column);
        });

        List<String> loose = new ArrayList<>();

        for (ColonyPos front : fronts) {
            if (!linked.contains(ColonyRoads.column(front.x(), front.z()))) {
                loose.add("door@" + front.x() + "," + front.y() + "," + front.z() + ":no-route");
            }
        }

        if (columns.isEmpty()) {
            return new RoadLines.Result(Optional.empty(), List.of(), 0, List.of(), List.copyOf(loose));
        }

        return new RoadLines.Result(Optional.of(RoadLinesGraph.of(columns, toLay, center, doors)),
                RoadLines.toPave(columns, toLay, physical), 0, List.of(), List.copyOf(loose));
    }

    /** Os pares do grafo de vizinhança relativa entre os nós, até {@value #MAX_EDGE} blocos, cada par em ordem. */
    static List<List<Long>> neighborhood(Map<Long, ColonyPos> nodes) {
        List<ColonyPos> all = List.copyOf(nodes.values());
        List<List<Long>> pairs = new ArrayList<>();

        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                ColonyPos p = all.get(i);
                ColonyPos q = all.get(j);
                long pq = squared(p, q);

                if (pq > (long) MAX_EDGE * MAX_EDGE || pq == 0) {
                    continue;
                }

                boolean blocked = false;

                for (int k = 0; k < all.size() && !blocked; k++) {
                    ColonyPos r = all.get(k);

                    blocked = k != i && k != j && Math.max(squared(p, r), squared(q, r)) < pq;
                }

                if (!blocked) {
                    long a = ColonyRoads.column(p.x(), p.z());
                    long b = ColonyRoads.column(q.x(), q.z());

                    pairs.add(List.of(Math.min(a, b), Math.max(a, b)));
                }
            }
        }

        return pairs;
    }

    /** A ligação por A*, de {@code from} a {@code to}, ou vazio sem passagem dentro do teto. */
    static Optional<List<ColonyPos>> route(ColonyPos from, ColonyPos to, Map<Long, Integer> physical, PathTerrain terrain) {
        int minX = Math.min(from.x(), to.x()) - SEARCH_MARGIN;
        int maxX = Math.max(from.x(), to.x()) + SEARCH_MARGIN;
        int minZ = Math.min(from.z(), to.z()) - SEARCH_MARGIN;
        int maxZ = Math.max(from.z(), to.z()) + SEARCH_MARGIN;
        long goal = ColonyRoads.column(to.x(), to.z());
        Map<Long, Double> cost = new HashMap<>();
        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, Integer> height = new HashMap<>();
        Map<Long, Integer> heading = new HashMap<>();
        PriorityQueue<Open> open = new PriorityQueue<>(Comparator.comparingDouble(Open::estimate));
        long start = ColonyRoads.column(from.x(), from.z());

        cost.put(start, 0.0);
        height.put(start, from.y());
        open.add(new Open(estimate(from.x(), from.z(), to), start));

        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int expanded = 0;

        while (!open.isEmpty() && expanded < MAX_EXPANSIONS) {
            long here = open.poll().column();

            if (here == goal) {
                return Optional.of(path(cameFrom, height, here));
            }

            expanded++;

            int x = ColonyRoads.xOf(here);
            int z = ColonyRoads.zOf(here);
            int y = height.getOrDefault(here, from.y());

            for (int s = 0; s < steps.length; s++) {
                int nx = x + steps[s][0];
                int nz = z + steps[s][1];

                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) {
                    continue;
                }

                long next = ColonyRoads.column(nx, nz);
                OptionalInt ground = next == goal ? OptionalInt.of(to.y()) : groundOf(physical, terrain, nx, nz, y);

                if (ground.isEmpty() || Math.abs(ground.getAsInt() - y) > 1) {
                    continue;
                }

                double step = STEP + (ground.getAsInt() != y ? CLIMB : 0);

                if (physical.containsKey(next)) {
                    step *= ON_PATH;
                }

                if (heading.containsKey(here) && heading.get(here) != s) {
                    step += TURN;
                }

                double total = cost.getOrDefault(here, 0.0) + step;

                if (total < cost.getOrDefault(next, Double.MAX_VALUE)) {
                    cost.put(next, total);
                    cameFrom.put(next, here);
                    height.put(next, ground.getAsInt());
                    heading.put(next, s);
                    open.add(new Open(total + estimate(nx, nz, to), next));
                }
            }
        }

        return Optional.empty();
    }

    /** Uma coluna na fila do A*: o custo estimado até o fim e a coluna empacotada (o long não cabe num double). */
    private record Open(double estimate, long column) {
    }

    private static OptionalInt groundOf(Map<Long, Integer> physical, PathTerrain terrain, int x, int z, int nearY) {
        Integer known = physical.get(ColonyRoads.column(x, z));

        if (known != null) {
            return OptionalInt.of(known);
        }

        return terrain.blocked(x, z) ? OptionalInt.empty() : terrain.groundY(x, z, nearY);
    }

    private static double estimate(int x, int z, ColonyPos to) {
        return (Math.abs(to.x() - x) + Math.abs(to.z() - z)) * STEP * ON_PATH;
    }

    private static List<ColonyPos> path(Map<Long, Long> cameFrom, Map<Long, Integer> height, long end) {
        List<ColonyPos> path = new ArrayList<>();

        for (Long at = end; at != null; at = cameFrom.get(at)) {
            path.add(new ColonyPos(ColonyRoads.xOf(at), height.getOrDefault(at, 0), ColonyRoads.zOf(at)));
        }

        return path.reversed();
    }

    private static long squared(ColonyPos a, ColonyPos b) {
        long dx = (long) a.x() - b.x();
        long dz = (long) a.z() - b.z();

        return dx * dx + dz * dz;
    }
}
