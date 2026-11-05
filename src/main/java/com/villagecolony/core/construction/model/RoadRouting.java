package com.villagecolony.core.construction.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Os trechos retos entre os nós — Regra 177: alinha os nós do mesmo corredor, e cada trecho vira uma reta, ou uma
 * dobra só, sem se afastar mais de {@value #MAX_DRIFT} blocos da faixa e com degrau de no máximo um bloco. Trecho que
 * não cabe assim se parte no ponto mais longe. Puro.
 */
final class RoadRouting {

    /** Quanto a reta pode se afastar da linha do meio. */
    static final int MAX_DRIFT = 3;

    /** Quanto o alinhamento move um nó. */
    static final int MAX_SHIFT = 3;

    /** O lado de um corredor que ainda conta como reto. */
    static final int STRAIGHT_SPAN = 3;

    /** Quantos blocos fora da faixa valem uma dobra a menos: a reta ganha da dobra que só segue a beira. */
    private static final int BEND_COST = 8;

    /** Quanto custa cortar uma coluna: o trajeto limpo ganha quando há. */
    private static final int CUT_COST = 3;

    /** Quantas vezes um trecho se parte, no máximo. */
    private static final int MAX_DEPTH = 10;

    private record Route(List<Long> columns, Map<Long, Integer> heights, int score) {
    }

    private RoadRouting() {
    }

    /** As colunas das retas de todos os trechos, com a altura de cada uma. */
    static Map<Long, Integer> route(RoadChains.Net net, Map<Long, Integer> mask, PathTerrain terrain) {
        align(net, mask, terrain, true);
        align(net, mask, terrain, false);
        mergeNear(net);

        Map<Long, Integer> columns = new HashMap<>();

        for (RoadChains.Chain chain : net.chains()) {
            long from = ColonyRoads.column(chain.from.x, chain.from.z);
            long to = ColonyRoads.column(chain.to.x, chain.to.z);
            OptionalInt startY = RoadLines.groundOf(mask, terrain, chain.from.x, chain.from.z, heightNear(mask, chain));

            if (startY.isEmpty()) {
                continue;
            }

            fit(from, to, chain.pixels, mask, terrain, startY.getAsInt(), 0)
                    .ifPresent(route -> route.heights().forEach(columns::putIfAbsent));
        }

        // Nó sem trecho é ponta solta: entra se a coluna serve, e a ligação entre partes resolve.
        for (RoadChains.Node node : net.nodes()) {
            long column = ColonyRoads.column(node.x, node.z);

            if (!columns.containsKey(column) && mask.containsKey(column)) {
                columns.put(column, mask.get(column));
            }
        }

        return columns;
    }

    private static int heightNear(Map<Long, Integer> mask, RoadChains.Chain chain) {
        return mask.getOrDefault(chain.pixels.getFirst(), 64);
    }

    /** Alinha os nós ligados por trechos quase retos, no eixo {@code z} (corredor ao longo de x) ou no outro. */
    private static void align(RoadChains.Net net, Map<Long, Integer> mask, PathTerrain terrain, boolean alongX) {
        Map<RoadChains.Node, RoadChains.Node> parent = new HashMap<>();
        Map<RoadChains.Node, List<Integer>> samples = new HashMap<>();
        List<RoadChains.Chain> straight = new ArrayList<>();

        for (RoadChains.Chain chain : net.chains()) {
            if (runsAlong(chain, alongX)) {
                straight.add(chain);
                parent.put(root(parent, chain.from), root(parent, chain.to));
            }
        }

        for (RoadChains.Chain chain : straight) {
            RoadChains.Node group = root(parent, chain.from);

            for (long pixel : chain.pixels) {
                samples.computeIfAbsent(group, ignored -> new ArrayList<>())
                        .add(alongX ? ColonyRoads.zOf(pixel) : ColonyRoads.xOf(pixel));
            }
        }

        Map<RoadChains.Node, List<RoadChains.Node>> groups = new HashMap<>();

        for (RoadChains.Node node : new HashSet<>(parent.keySet())) {
            groups.computeIfAbsent(root(parent, node), ignored -> new ArrayList<>()).add(node);
        }

        groups.forEach((group, members) -> shift(members, samples.getOrDefault(group, List.of()), mask, terrain, alongX));
    }

    private static void shift(List<RoadChains.Node> members, List<Integer> samples, Map<Long, Integer> mask,
                              PathTerrain terrain, boolean alongX) {
        if (samples.isEmpty()) {
            return;
        }

        List<RoadChains.Node> free = members.stream().filter(node -> !node.fixed).toList();
        List<RoadChains.Node> doors = members.stream().filter(node -> node.fixed).toList();

        if (free.isEmpty()) {
            return;
        }

        int target;

        // Trecho de uma porta até a rua: o encontro vem para a frente da porta, e a linha sai reta dela.
        if (free.size() == 1 && doors.size() == 1) {
            target = alongX ? doors.getFirst().z : doors.getFirst().x;
        } else {
            List<Integer> sorted = new ArrayList<>(samples);

            sorted.sort(null);
            target = sorted.get(sorted.size() / 2);
        }

        for (RoadChains.Node node : free) {
            int current = alongX ? node.z : node.x;
            int x = alongX ? node.x : target;
            int z = alongX ? target : node.z;

            if (current != target && Math.abs(current - target) <= MAX_SHIFT
                    && fits(mask, terrain, x, z, nearestY(mask, node))) {
                if (alongX) {
                    node.z = target;
                } else {
                    node.x = target;
                }
            }
        }
    }

    private static int nearestY(Map<Long, Integer> mask, RoadChains.Node node) {
        return mask.getOrDefault(ColonyRoads.column(node.x, node.z), 64);
    }

    private static boolean fits(Map<Long, Integer> mask, PathTerrain terrain, int x, int z, int nearY) {
        return RoadLines.groundOf(mask, terrain, x, z, nearY).isPresent();
    }

    private static boolean runsAlong(RoadChains.Chain chain, boolean alongX) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (long pixel : chain.pixels) {
            minX = Math.min(minX, ColonyRoads.xOf(pixel));
            maxX = Math.max(maxX, ColonyRoads.xOf(pixel));
            minZ = Math.min(minZ, ColonyRoads.zOf(pixel));
            maxZ = Math.max(maxZ, ColonyRoads.zOf(pixel));
        }

        int along = alongX ? maxX - minX : maxZ - minZ;
        int across = alongX ? maxZ - minZ : maxX - minX;

        return along > across && across <= STRAIGHT_SPAN;
    }

    private static RoadChains.Node root(Map<RoadChains.Node, RoadChains.Node> parent, RoadChains.Node node) {
        parent.putIfAbsent(node, node);

        RoadChains.Node found = node;
        RoadChains.Node up = parent.getOrDefault(found, found);

        while (up != found) {
            found = up;
            up = parent.getOrDefault(found, found);
        }

        parent.put(node, found);

        return found;
    }

    /** Depois de alinhar, dois encontros perto viram um: os trechos passam ao que fica. */
    private static void mergeNear(RoadChains.Net net) {
        List<RoadChains.Node> nodes = new ArrayList<>(net.nodes());

        nodes.removeIf(node -> node.fixed);

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = nodes.size() - 1; j > i; j--) {
                RoadChains.Node keep = nodes.get(i);
                RoadChains.Node gone = nodes.get(j);

                if (Math.abs(keep.x - gone.x) <= RoadChains.MERGE && Math.abs(keep.z - gone.z) <= RoadChains.MERGE) {
                    for (RoadChains.Chain chain : net.chains()) {
                        chain.from = chain.from == gone ? keep : chain.from;
                        chain.to = chain.to == gone ? keep : chain.to;
                    }

                    net.nodes().remove(gone);
                    nodes.remove(j);
                }
            }
        }

        net.chains().removeIf(chain -> chain.from == chain.to && chain.pixels.size() <= 2 * RoadChains.MERGE + 2);

        List<RoadChains.Chain> kept = RoadChains.withoutTwins(net.chains());

        net.chains().clear();
        net.chains().addAll(kept);
    }

    /** A melhor reta ou dobra de {@code from} a {@code to}; sem nenhuma, parte o trecho no ponto mais longe. */
    private static Optional<Route> fit(long from, long to, List<Long> chain, Map<Long, Integer> mask,
                                       PathTerrain terrain, int startY, int depth) {
        Optional<Route> best = Optional.empty();

        for (List<Long> candidate : candidates(from, to)) {
            Optional<Route> route = checked(candidate, chain, mask, terrain, startY);

            if (route.isPresent() && (best.isEmpty() || route.get().score() < best.get().score())) {
                best = route;
            }
        }

        if (best.isPresent() || depth >= MAX_DEPTH || chain.size() < 3) {
            return best;
        }

        int split = farthest(from, to, chain);
        long middle = chain.get(split);

        return fit(from, middle, chain.subList(0, split + 1), mask, terrain, startY, depth + 1)
                .flatMap(first -> fit(middle, to, chain.subList(split, chain.size()), mask, terrain,
                        first.heights().getOrDefault(middle, startY), depth + 1)
                        .map(second -> joined(first, second)));
    }

    private static Route joined(Route first, Route second) {
        List<Long> columns = new ArrayList<>(first.columns());

        columns.addAll(second.columns().subList(1, second.columns().size()));

        Map<Long, Integer> heights = new HashMap<>(first.heights());

        heights.putAll(second.heights());

        return new Route(columns, heights, first.score() + second.score());
    }

    private static List<List<Long>> candidates(long from, long to) {
        int x1 = ColonyRoads.xOf(from);
        int z1 = ColonyRoads.zOf(from);
        int x2 = ColonyRoads.xOf(to);
        int z2 = ColonyRoads.zOf(to);

        if (x1 == x2 || z1 == z2) {
            return List.of(line(x1, z1, x2, z2));
        }

        List<Long> xFirst = new ArrayList<>(line(x1, z1, x2, z1));
        List<Long> second = line(x2, z1, x2, z2);

        xFirst.addAll(second.subList(1, second.size()));

        List<Long> zFirst = new ArrayList<>(line(x1, z1, x1, z2));
        List<Long> other = line(x1, z2, x2, z2);

        zFirst.addAll(other.subList(1, other.size()));

        return List.of(xFirst, zFirst);
    }

    private static List<Long> line(int x1, int z1, int x2, int z2) {
        List<Long> columns = new ArrayList<>();
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));

        for (int step = 0; step <= steps; step++) {
            columns.add(ColonyRoads.column(x1 + Integer.signum(x2 - x1) * step, z1 + Integer.signum(z2 - z1) * step));
        }

        return columns;
    }

    /** A rota, se o degrau é de no máximo um bloco, o chão serve e ela não se afasta da linha do meio. */
    private static Optional<Route> checked(List<Long> columns, List<Long> chain, Map<Long, Integer> mask,
                                           PathTerrain terrain, int startY) {
        Map<Long, Integer> heights = new HashMap<>();
        int previous = startY;
        int score = bends(columns) * BEND_COST;

        for (long column : columns) {
            OptionalInt ground = RoadLines.groundOf(mask, terrain, ColonyRoads.xOf(column), ColonyRoads.zOf(column), previous);

            if (ground.isPresent() && Math.abs(ground.getAsInt() - previous) <= 1) {
                previous = ground.getAsInt();
                score += mask.containsKey(column) ? 0 : 1;
            } else if (terrain.canCut(ColonyRoads.xOf(column), ColonyRoads.zOf(column), previous)) {
                // O que está acima do chão no trajeto sai: o piso segue na altura de antes (Regra 178).
                score += CUT_COST;
            } else {
                return Optional.empty();
            }

            heights.put(column, previous);
        }

        for (long pixel : chain) {
            if (distanceTo(pixel, columns) > MAX_DRIFT) {
                return Optional.empty();
            }
        }

        return Optional.of(new Route(columns, heights, score));
    }

    private static int bends(List<Long> columns) {
        int bends = 0;

        for (int i = 2; i < columns.size(); i++) {
            boolean firstAlongX = ColonyRoads.zOf(columns.get(i - 2)) == ColonyRoads.zOf(columns.get(i - 1));
            boolean secondAlongX = ColonyRoads.zOf(columns.get(i - 1)) == ColonyRoads.zOf(columns.get(i));

            bends += firstAlongX != secondAlongX ? 1 : 0;
        }

        return bends;
    }

    private static int distanceTo(long pixel, List<Long> columns) {
        int best = Integer.MAX_VALUE;

        for (long column : columns) {
            best = Math.min(best, Math.max(Math.abs(ColonyRoads.xOf(pixel) - ColonyRoads.xOf(column)),
                    Math.abs(ColonyRoads.zOf(pixel) - ColonyRoads.zOf(column))));
        }

        return best;
    }

    private static int farthest(long from, long to, List<Long> chain) {
        double x1 = ColonyRoads.xOf(from);
        double z1 = ColonyRoads.zOf(from);
        double dx = ColonyRoads.xOf(to) - x1;
        double dz = ColonyRoads.zOf(to) - z1;
        double length = Math.max(1, Math.sqrt(dx * dx + dz * dz));
        int best = chain.size() / 2;
        double bestDistance = -1;

        for (int i = 1; i < chain.size() - 1; i++) {
            double px = ColonyRoads.xOf(chain.get(i)) - x1;
            double pz = ColonyRoads.zOf(chain.get(i)) - z1;
            double distance = Math.abs(px * dz - pz * dx) / length;

            if (distance > bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }

        return best;
    }
}
