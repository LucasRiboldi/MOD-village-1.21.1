package com.villagecolony.core.construction.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A linha do meio em nós e trechos, como um mapa de trilhos — Regra 177: nó é onde linhas se encontram, a ponta e a
 * frente de porta. Encontros a até {@value #MERGE} blocos viram um nó só; a frente de porta nunca se funde. Puro.
 */
final class RoadChains {

    /** Dois nós a esta distância, ou menos, são o mesmo nó: não há nó ao lado de nó. */
    static final int MERGE = 3;

    /** Trechos gêmeos até este tamanho entre os mesmos dois nós: fica só o mais curto. */
    private static final int TWIN = 8;

    /** Um nó do mapa, que o alinhamento pode mover quando não é fixo. */
    static final class Node {
        int x;
        int z;
        final boolean fixed;

        Node(int x, int z, boolean fixed) {
            this.x = x;
            this.z = z;
            this.fixed = fixed;
        }
    }

    /** Um trecho entre dois nós, com as colunas da linha do meio na ordem. */
    static final class Chain {
        Node from;
        Node to;
        final List<Long> pixels;

        Chain(Node from, Node to, List<Long> pixels) {
            this.from = from;
            this.to = to;
            this.pixels = pixels;
        }
    }

    record Net(List<Node> nodes, List<Chain> chains) {
    }

    private RoadChains() {
    }

    /** O mapa da linha do meio {@code line}; {@code fixed} são as frentes de porta. */
    static Net of(Set<Long> line, Set<Long> fixed) {
        Set<Long> anchors = new HashSet<>();

        for (long at : line) {
            if (fixed.contains(at) || RoadThinning.branches(line, at) != 2) {
                anchors.add(at);
            }
        }

        List<List<Long>> raw = new ArrayList<>();
        Set<Long> walked = new HashSet<>();

        for (long at : sorted(anchors)) {
            traceFrom(line, anchors, walked, at, raw);
        }

        // Volta fechada, sem encontro nenhum: qualquer coluna dela serve de nó.
        for (long at : sorted(line)) {
            if (!anchors.contains(at) && !walked.contains(at)) {
                anchors.add(at);
                traceFrom(line, anchors, walked, at, raw);
            }
        }

        Map<Long, Node> nodeOf = clusters(anchors, fixed);

        return net(nodeOf, raw);
    }

    private static List<Long> sorted(Set<Long> columns) {
        List<Long> ordered = new ArrayList<>(columns);

        ordered.sort(null);

        return ordered;
    }

    private static void traceFrom(Set<Long> line, Set<Long> anchors, Set<Long> walked, long start, List<List<Long>> raw) {
        for (long first : RoadThinning.neighbours(line, start)) {
            if (anchors.contains(first)) {
                if (start < first) {
                    raw.add(List.of(start, first));
                }
                continue;
            }

            if (walked.contains(first)) {
                continue;
            }

            raw.add(walk(line, anchors, walked, start, first));
        }
    }

    private static List<Long> walk(Set<Long> line, Set<Long> anchors, Set<Long> walked, long start, long first) {
        List<Long> pixels = new ArrayList<>(List.of(start, first));
        long at = first;

        while (true) {
            walked.add(at);

            Long next = null;

            for (long candidate : RoadThinning.neighbours(line, at)) {
                // O ponto de partida só vale de chegada depois de se afastar dele.
                if (anchors.contains(candidate) && (candidate != start || pixels.size() > 3)) {
                    pixels.add(candidate);
                    return pixels;
                }

                if (next == null && !anchors.contains(candidate) && !walked.contains(candidate)) {
                    next = candidate;
                }
            }

            if (next == null) {
                // Beco que o afinamento deixou: a última coluna vira ponta.
                anchors.add(at);
                return pixels;
            }

            pixels.add(next);
            at = next;
        }
    }

    /** Agrupa os nós próximos; o nó do grupo é a coluna mais perto do meio dele. */
    private static Map<Long, Node> clusters(Set<Long> anchors, Set<Long> fixed) {
        List<Long> free = sorted(anchors);
        free.removeIf(fixed::contains);
        Map<Long, Long> parent = new HashMap<>();
        free.forEach(at -> parent.put(at, at));

        for (int i = 0; i < free.size(); i++) {
            for (int j = i + 1; j < free.size(); j++) {
                if (near(free.get(i), free.get(j))) {
                    parent.put(root(parent, free.get(i)), root(parent, free.get(j)));
                }
            }
        }

        Map<Long, List<Long>> groups = new HashMap<>();
        free.forEach(at -> groups.computeIfAbsent(root(parent, at), ignored -> new ArrayList<>()).add(at));

        Map<Long, Node> nodeOf = new HashMap<>();

        for (List<Long> group : groups.values()) {
            Node node = centerOf(group);

            group.forEach(at -> nodeOf.put(at, node));
        }

        for (long at : anchors) {
            if (fixed.contains(at)) {
                nodeOf.put(at, new Node(ColonyRoads.xOf(at), ColonyRoads.zOf(at), true));
            }
        }

        return nodeOf;
    }

    private static boolean near(long a, long b) {
        return Math.abs(ColonyRoads.xOf(a) - ColonyRoads.xOf(b)) <= MERGE
                && Math.abs(ColonyRoads.zOf(a) - ColonyRoads.zOf(b)) <= MERGE;
    }

    private static long root(Map<Long, Long> parent, long at) {
        long found = at;
        long up = parent.getOrDefault(found, found);

        while (up != found) {
            found = up;
            up = parent.getOrDefault(found, found);
        }

        parent.put(at, found);

        return found;
    }

    private static Node centerOf(List<Long> group) {
        double x = group.stream().mapToInt(ColonyRoads::xOf).average().orElse(0);
        double z = group.stream().mapToInt(ColonyRoads::zOf).average().orElse(0);
        long best = group.getFirst();
        double bestDistance = Double.MAX_VALUE;

        for (long at : group) {
            double distance = Math.abs(ColonyRoads.xOf(at) - x) + Math.abs(ColonyRoads.zOf(at) - z);

            if (distance < bestDistance) {
                best = at;
                bestDistance = distance;
            }
        }

        return new Node(ColonyRoads.xOf(best), ColonyRoads.zOf(best), false);
    }

    private static Net net(Map<Long, Node> nodeOf, List<List<Long>> raw) {
        List<Chain> chains = new ArrayList<>();

        for (List<Long> pixels : raw) {
            Node from = nodeOf.get(pixels.getFirst());
            Node to = nodeOf.get(pixels.getLast());

            if (from != null && to != null && (from != to || pixels.size() > 2 * MERGE + 2)) {
                chains.add(new Chain(from, to, pixels));
            }
        }

        return new Net(new ArrayList<>(new HashSet<>(nodeOf.values())), withoutTwins(chains));
    }

    /** Laço curto entre os mesmos dois nós é beira de faixa, não duas ruas. */
    static List<Chain> withoutTwins(List<Chain> chains) {
        Map<Set<Node>, Chain> shortest = new HashMap<>();
        List<Chain> kept = new ArrayList<>();

        for (Chain chain : chains) {
            if (chain.from == chain.to || chain.pixels.size() > TWIN) {
                kept.add(chain);
                continue;
            }

            shortest.merge(Set.of(chain.from, chain.to), chain,
                    (a, b) -> a.pixels.size() <= b.pixels.size() ? a : b);
        }

        kept.addAll(shortest.values());

        return kept;
    }
}
