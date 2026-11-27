package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.PriorityQueue;

/**
 * O A* das ruas do grafo planejado — Regras 320 e 389, ADR-065. Passo plano {@value #STEP}, degrau de 1 bloco
 * +{@value #CLIMB}, curva +{@value #TURN}; caminho que já existe ×{@value #ON_PATH}; coluna que já é da rede nesta montagem
 * ×{@value #ON_NETWORK}, para as ligações se juntarem; coluna encostada de lado numa linha da rede, sem estar nela,
 * +{@value #BESIDE}, para nunca haver duas linhas lado a lado. Água, construção e degrau de mais de 1 bloco não passam. A
 * busca fica dentro de uma caixa: o corredor do braço ou do ramal reto, ou a folga em volta dos dois pontos. Puro.
 */
final class StreetRouter {

    static final double STEP = 1.0;

    static final double CLIMB = 2.0;

    static final double ON_PATH = 0.3;

    static final double ON_NETWORK = 0.1;

    static final double BESIDE = 4.0;

    static final double TURN = 2.0;

    /** O teto de colunas abertas por ligação. */
    static final int MAX_EXPANSIONS = 8_000;

    private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private StreetRouter() {
    }

    /** A caixa da busca, nas colunas. */
    record Box(int minX, int minZ, int maxX, int maxZ) {

        /** Os dois pontos, com {@code margin} de folga em volta. */
        static Box around(ColonyPos a, ColonyPos b, int margin) {
            return new Box(Math.min(a.x(), b.x()) - margin, Math.min(a.z(), b.z()) - margin,
                    Math.max(a.x(), b.x()) + margin, Math.max(a.z(), b.z()) + margin);
        }

        boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    /**
     * A rua de {@code from} a {@code to} dentro de {@code box}, ou vazio, com o motivo em {@code why[0]}: {@code exhausted}
     * no teto de colunas, {@code closed} sem passagem.
     */
    static Optional<List<ColonyPos>> route(ColonyPos from, ColonyPos to, Map<Long, Integer> physical, PathTerrain terrain,
                                           Map<Long, Integer> network, Box box, String[] why) {
        long goal = ColonyRoads.column(to.x(), to.z());
        long start = ColonyRoads.column(from.x(), from.z());
        Map<Long, Double> cost = new HashMap<>();
        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, Integer> height = new HashMap<>();
        Map<Long, Integer> heading = new HashMap<>();
        PriorityQueue<Open> open = new PriorityQueue<>(Comparator.comparingDouble(Open::estimate));

        cost.put(start, 0.0);
        height.put(start, from.y());
        open.add(new Open(estimate(from.x(), from.z(), to), start));

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

            for (int s = 0; s < STEPS.length; s++) {
                int nx = x + STEPS[s][0];
                int nz = z + STEPS[s][1];

                if (!box.contains(nx, nz)) {
                    continue;
                }

                long next = ColonyRoads.column(nx, nz);
                OptionalInt ground = next == goal ? OptionalInt.of(to.y()) : groundOf(physical, network, terrain, nx, nz, y);

                if (ground.isEmpty() || Math.abs(ground.getAsInt() - y) > 1) {
                    continue;
                }

                double total = cost.getOrDefault(here, 0.0)
                        + step(next, nx, nz, ground.getAsInt() != y, physical, network, goal, start)
                        + (heading.containsKey(here) && heading.get(here) != s ? TURN : 0);

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

    /** O custo de pisar nesta coluna. */
    static double step(long next, int x, int z, boolean climbs, Map<Long, Integer> physical,
                               Map<Long, Integer> network, long goal, long start) {
        double step = STEP + (climbs ? CLIMB : 0);

        if (network.containsKey(next)) {
            return step * ON_NETWORK;
        }

        if (physical.containsKey(next)) {
            step *= ON_PATH;
        }

        // A coluna colada à chegada ou à saída encosta na rede por força: só a que corre ao lado de uma linha paga.
        if (besideTheNetwork(x, z, network) && !touches(x, z, goal) && !touches(x, z, start)) {
            step += BESIDE;
        }

        return step;
    }

    static boolean besideTheNetwork(int x, int z, Map<Long, Integer> network) {
        for (int[] s : STEPS) {
            if (network.containsKey(ColonyRoads.column(x + s[0], z + s[1]))) {
                return true;
            }
        }

        return false;
    }

    static boolean touches(int x, int z, long column) {
        return Math.abs(ColonyRoads.xOf(column) - x) + Math.abs(ColonyRoads.zOf(column) - z) <= 1;
    }

    /** Uma coluna na fila do A*: o custo estimado até o fim e a coluna empacotada (o long não cabe num double). */
    private record Open(double estimate, long column) {
    }

    static OptionalInt groundOf(Map<Long, Integer> physical, Map<Long, Integer> network, PathTerrain terrain, int x,
                                        int z, int nearY) {
        long column = ColonyRoads.column(x, z);
        Integer known = network.containsKey(column) ? network.get(column) : physical.get(column);

        if (known != null) {
            return OptionalInt.of(known);
        }

        return terrain.blocked(x, z) ? OptionalInt.empty() : terrain.groundY(x, z, nearY);
    }

    static double estimate(int x, int z, ColonyPos to) {
        return (Math.abs(to.x() - x) + Math.abs(to.z() - z)) * STEP * ON_NETWORK;
    }

    private static List<ColonyPos> path(Map<Long, Long> cameFrom, Map<Long, Integer> height, long end) {
        List<ColonyPos> path = new ArrayList<>();

        for (Long at = end; at != null; at = cameFrom.get(at)) {
            path.add(new ColonyPos(ColonyRoads.xOf(at), height.getOrDefault(at, 0), ColonyRoads.zOf(at)));
        }

        return path.reversed();
    }
}
