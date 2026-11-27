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
 * passam. A ligação traçada fica guardada por par de nós, com estado ({@link RoadLink}, Regra 388, ADR-065), e só é refeita
 * quando um quadrado sujo ({@link RoadRegions}) a cruza. Puro: o chão vem por {@link PathTerrain}.
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

    private NeighborhoodRoads() {
    }

    /** Quantos pares mais próximos cada parte solta tenta para chegar à parte do centro. */
    static final int LINK_TRIES = 3;

    /** As quatro direções para fora da vila: leste, oeste, sul e norte. */
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * O grafo do modo 3. Depois das ligações de vizinhança, toda parte que não chega ao centro liga pelo par mais próximo,
     * sem o limite de {@value #MAX_EDGE} blocos. Em cada lado, o caminho mais distante do centro vira nó, e um bloco além
     * dele vira a ponta: ela anda no máximo um bloco por dia e não passa de {@code limit} ({minX, minZ, maxX, maxZ}).
     */
    public static RoadLines.Result build(Set<ColonyPos> cells, ColonyPos center, Set<ColonyPos> fronts, PathTerrain terrain,
                                         NeighborhoodRoutes routes, Set<Long> dirty, int[] limit, long today) {
        Map<Long, Integer> physical = new HashMap<>();

        routes.queued = 0;

        cells.forEach(at -> physical.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));

        Map<Long, ColonyPos> nodes = new TreeMap<>();
        int hubY = terrain.groundY(center.x(), center.z(), center.y()).orElse(center.y());
        long hub = ColonyRoads.column(center.x(), center.z());

        nodes.put(hub, new ColonyPos(center.x(), hubY, center.z()));
        fronts.forEach(at -> nodes.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at));

        ColonyPos[] frontier = frontiers(cells, center);

        for (ColonyPos edge : frontier) {
            if (edge != null) {
                nodes.putIfAbsent(ColonyRoads.column(edge.x(), edge.z()), edge);
            }
        }

        Map<Long, Integer> columns = new LinkedHashMap<>();
        Map<Long, Long> parent = new HashMap<>();
        Set<List<Long>> used = new HashSet<>();
        Set<List<Long>> considered = new HashSet<>();
        int[] routed = {0};

        nodes.keySet().forEach(node -> parent.put(node, node));

        for (List<Long> pair : neighborhood(nodes)) {
            if (link(pair, nodes, physical, terrain, routes, dirty, routed, true, columns, considered)) {
                used.add(pair);
                union(parent, pair.get(0), pair.get(1));
            }
        }

        connect(nodes, hub, parent, physical, terrain, routes, dirty, routed, columns, used, considered);
        // A falha fica guardada enquanto o par for tentado: só um bloco mudado perto a traça de novo (Regra 388).
        routes.byPair.keySet().retainAll(considered);
        routes.byPair.replaceAll((pair, link) -> link.paved(
                link.columns().stream().allMatch(at -> physical.containsKey(ColonyRoads.column(at.x(), at.z())))));

        for (int side = 0; side < SIDES.length; side++) {
            tip(side, frontier[side], physical, terrain, routes, limit, today, parent, hub).ifPresent(stub ->
                    columns.putIfAbsent(ColonyRoads.column(stub.x(), stub.z()), stub.y()));
        }

        Map<Long, Integer> toLay = new HashMap<>();
        Set<Long> doors = new HashSet<>();

        fronts.forEach(at -> {
            long column = ColonyRoads.column(at.x(), at.z());

            toLay.putIfAbsent(column, at.y());
            doors.add(column);
        });

        List<String> loose = new ArrayList<>();
        long main = find(parent, hub);

        for (ColonyPos front : fronts) {
            long column = ColonyRoads.column(front.x(), front.z());

            if (find(parent, column) != main) {
                loose.add("door@" + front.x() + "," + front.y() + "," + front.z() + ":" + whyLoose(routes, column));
            }
        }

        if (columns.isEmpty()) {
            return new RoadLines.Result(Optional.empty(), List.of(), 0, List.of(), List.copyOf(loose));
        }

        return new RoadLines.Result(Optional.of(RoadLinesGraph.of(columns, toLay, center, doors)),
                RoadLines.toPave(columns, toLay, physical), 0, List.of(), List.copyOf(loose));
    }

    /**
     * Uma ligação: a guardada, se nenhum quadrado sujo a cruza, ou uma nova pelo A*; diz se ligou. A guardada que se repete
     * conta mais uma montagem igual e trava na terceira; a que falha fica guardada com o motivo e só volta a ser traçada
     * com um quadrado sujo dentro da busca; a que não tem vez nem traçado guardado entra na fila (Regra 388).
     */
    private static boolean link(List<Long> pair, Map<Long, ColonyPos> nodes, Map<Long, Integer> physical,
                                PathTerrain terrain, NeighborhoodRoutes routes, Set<Long> dirty, int[] routed, boolean budgeted,
                                Map<Long, Integer> columns, Set<List<Long>> considered) {
        considered.add(pair);

        RoadLink stored = routes.byPair.get(pair);
        ColonyPos first = nodes.get(pair.get(0));
        ColonyPos second = nodes.get(pair.get(1));

        if (first == null || second == null) {
            return false;
        }

        boolean stale = stored == null || (stored.state() == RoadEdgeState.FAILED ? nearAny(first, second, dirty)
                : stored.columns().stream().anyMatch(at -> dirty.contains(RoadRegions.regionOf(at.x(), at.z()))));
        RoadLink link;

        if (stale && (!budgeted || routed[0] < ROUTES_PER_BUILD)) {
            routed[0] += budgeted ? 1 : 0;

            String[] why = {""};
            Optional<List<ColonyPos>> traced = route(first, second, physical, terrain, why);

            link = traced.map(path -> stored != null && path.equals(stored.columns()) ? stored.reused() : RoadLink.planned(path))
                    .orElseGet(() -> RoadLink.failed(why[0]));
        } else if (stored == null) {
            routes.queued++;

            return false;
        } else {
            // Sem vez, a guardada fica como está até a próxima montagem; sem sujo, conta mais uma igual.
            link = stale ? stored : stored.reused();
        }

        routes.byPair.put(pair, link);

        if (link.state() == RoadEdgeState.FAILED) {
            return false;
        }

        link.columns().forEach(at -> columns.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));

        return true;
    }

    /** Se algum quadrado sujo cai na caixa da busca entre os dois nós. */
    private static boolean nearAny(ColonyPos first, ColonyPos second, Set<Long> dirty) {
        if (dirty.isEmpty()) {
            return false;
        }

        int maxX = Math.max(first.x(), second.x()) + SEARCH_MARGIN + RoadRegions.SIZE - 1;
        int maxZ = Math.max(first.z(), second.z()) + SEARCH_MARGIN + RoadRegions.SIZE - 1;

        for (int x = Math.min(first.x(), second.x()) - SEARCH_MARGIN; x <= maxX; x += RoadRegions.SIZE) {
            for (int z = Math.min(first.z(), second.z()) - SEARCH_MARGIN; z <= maxZ; z += RoadRegions.SIZE) {
                if (dirty.contains(RoadRegions.regionOf(x, z))) {
                    return true;
                }
            }
        }

        return false;
    }

    /** O motivo da porta solta: o da ligação dela que falhou, ou {@code no-route} sem nenhuma tentada. */
    private static String whyLoose(NeighborhoodRoutes routes, long column) {
        return routes.byPair.entrySet().stream()
                .filter(entry -> entry.getKey().contains(column) && entry.getValue().state() == RoadEdgeState.FAILED)
                .map(entry -> entry.getValue().reason()).findFirst().orElse("no-route");
    }

    /** Toda parte que não chega ao centro tenta os {@value #LINK_TRIES} pares mais próximos com a parte do centro. */
    private static void connect(Map<Long, ColonyPos> nodes, long hub, Map<Long, Long> parent, Map<Long, Integer> physical,
                                PathTerrain terrain, NeighborhoodRoutes routes, Set<Long> dirty, int[] routed,
                                Map<Long, Integer> columns, Set<List<Long>> used, Set<List<Long>> considered) {
        for (long node : nodes.keySet()) {
            if (find(parent, node) == find(parent, hub)) {
                continue;
            }

            long part = find(parent, node);
            List<List<Long>> candidates = new ArrayList<>();

            for (long inPart : nodes.keySet()) {
                if (find(parent, inPart) != part) {
                    continue;
                }

                for (long inMain : nodes.keySet()) {
                    if (find(parent, inMain) == find(parent, hub)) {
                        candidates.add(List.of(Math.min(inPart, inMain), Math.max(inPart, inMain)));
                    }
                }
            }

            candidates.sort(Comparator.comparingLong(pair -> squaredColumns(pair.get(0), pair.get(1))));

            for (List<Long> pair : candidates.subList(0, Math.min(LINK_TRIES, candidates.size()))) {
                if (link(pair, nodes, physical, terrain, routes, dirty, routed, false, columns, considered)) {
                    used.add(pair);
                    union(parent, pair.get(0), pair.get(1));
                    break;
                }
            }
        }
    }

    /**
     * O caminho mais distante do centro em cada lado (leste, oeste, sul, norte), dentro do cone daquele lado; nulo no lado
     * sem caminho.
     */
    static ColonyPos[] frontiers(Set<ColonyPos> cells, ColonyPos center) {
        ColonyPos[] best = new ColonyPos[SIDES.length];
        int[] reach = new int[SIDES.length];

        for (ColonyPos at : cells) {
            int dx = at.x() - center.x();
            int dz = at.z() - center.z();

            for (int side = 0; side < SIDES.length; side++) {
                int along = dx * SIDES[side][0] + dz * SIDES[side][1];
                int across = Math.abs(SIDES[side][0] == 0 ? dx : dz);

                if (along > 0 && across <= along && (best[side] == null || along > reach[side])) {
                    best[side] = at;
                    reach[side] = along;
                }
            }
        }

        return best;
    }

    /**
     * A ponta deste lado: um bloco além do caminho mais distante, no chão, a até 1 bloco de degrau, dentro do limite. Anda
     * no máximo um bloco por dia: com o caminho mais distante novo no mesmo dia em que a ponta andou, espera o dia seguinte.
     */
    private static Optional<ColonyPos> tip(int side, ColonyPos edge, Map<Long, Integer> physical, PathTerrain terrain,
                                           NeighborhoodRoutes routes, int[] limit, long today, Map<Long, Long> parent,
                                           long hub) {
        if (edge == null || find(parent, ColonyRoads.column(edge.x(), edge.z())) != find(parent, hub)) {
            return Optional.empty();
        }

        ColonyPos last = routes.lastEdge.get(side);

        if (!edge.equals(last)) {
            if (last != null && today <= routes.lastDay.getOrDefault(side, Long.MIN_VALUE)) {
                return Optional.empty();
            }

            routes.lastEdge.put(side, edge);
            routes.lastDay.put(side, today);
        }

        int x = edge.x() + SIDES[side][0];
        int z = edge.z() + SIDES[side][1];

        if (x < limit[0] || z < limit[1] || x > limit[2] || z > limit[3] || physical.containsKey(ColonyRoads.column(x, z))
                || terrain.blocked(x, z)) {
            return Optional.empty();
        }

        OptionalInt ground = terrain.groundY(x, z, edge.y());

        return ground.isPresent() && Math.abs(ground.getAsInt() - edge.y()) <= 1
                ? Optional.of(new ColonyPos(x, ground.getAsInt(), z))
                : Optional.empty();
    }

    private static long find(Map<Long, Long> parent, long node) {
        long root = node;

        while (parent.getOrDefault(root, root) != root) {
            root = parent.getOrDefault(root, root);
        }

        return root;
    }

    private static void union(Map<Long, Long> parent, long a, long b) {
        parent.put(find(parent, a), find(parent, b));
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
        return route(from, to, physical, terrain, new String[1]);
    }

    /** O mesmo, com o motivo da falha em {@code why[0]}: {@code exhausted} no teto de colunas, {@code closed} sem passagem. */
    static Optional<List<ColonyPos>> route(ColonyPos from, ColonyPos to, Map<Long, Integer> physical, PathTerrain terrain,
                                           String[] why) {
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

        why[0] = expanded >= MAX_EXPANSIONS ? "exhausted" : "closed";

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

    private static long squaredColumns(long a, long b) {
        long dx = (long) ColonyRoads.xOf(a) - ColonyRoads.xOf(b);
        long dz = (long) ColonyRoads.zOf(a) - ColonyRoads.zOf(b);

        return dx * dx + dz * dz;
    }

    private static long squared(ColonyPos a, ColonyPos b) {
        long dx = (long) a.x() - b.x();
        long dz = (long) a.z() - b.z();

        return dx * dx + dz * dz;
    }
}
