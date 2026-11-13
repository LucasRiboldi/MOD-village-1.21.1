package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.Side;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Como uma parte solta do caminho se liga à linha principal — Regras 178 e 224 (autor, 2026-11-10): primeiro em reta, até o
 * ponto mais perto da linha, com uma dobra só e degrau de no máximo um bloco; depois o ramal reto da porta, e por fim a
 * busca cara, que o chamador limita. Puro.
 */
final class RoadLinks {

    /** Quantos pares de pontos mais perto se tentam antes de desistir da reta. */
    static final int CANDIDATES = 8;

    /** Acima disto de pares a comparar, a reta não é tentada: a parte é grande e cabe à busca. */
    private static final long MAX_PAIRS = 2_000_000L;

    private RoadLinks() {
    }

    /** Liga a parte à linha principal em reta, até o ponto mais perto; verdadeiro se ligou. */
    static boolean straight(Set<Long> part, Set<Long> main, Map<Long, Integer> columns, Map<Long, Integer> mask,
                            PathTerrain terrain) {
        if (main.isEmpty() || (long) part.size() * main.size() > MAX_PAIRS) {
            return false;
        }

        List<long[]> pairs = new ArrayList<>();

        for (long a : part) {
            for (long b : main) {
                int distance = manhattan(a, b);

                if (distance <= RoadLines.MAX_LINK) {
                    pairs.add(new long[] {distance, a, b});
                }
            }
        }

        pairs.sort(Comparator.<long[]>comparingLong(pair -> pair[0]).thenComparingLong(pair -> pair[1])
                .thenComparingLong(pair -> pair[2]));

        for (int i = 0; i < Math.min(CANDIDATES, pairs.size()); i++) {
            long from = pairs.get(i)[1];
            long to = pairs.get(i)[2];

            for (boolean alongX : new boolean[] {true, false}) {
                Optional<Map<Long, Integer>> route = bend(from, to, alongX, main, columns, mask, terrain);

                if (route.isPresent()) {
                    columns.putAll(route.get());
                    part.addAll(route.get().keySet());

                    return true;
                }
            }
        }

        return false;
    }

    /** O trajeto em L de {@code from} até a linha: primeiro no eixo pedido, depois no outro; vazio se não cabe. */
    private static Optional<Map<Long, Integer>> bend(long from, long to, boolean alongX, Set<Long> main,
                                                     Map<Long, Integer> columns, Map<Long, Integer> mask, PathTerrain terrain) {
        Map<Long, Integer> route = new HashMap<>();
        int x = ColonyRoads.xOf(from);
        int z = ColonyRoads.zOf(from);
        int y = columns.getOrDefault(from, 0);
        int toX = ColonyRoads.xOf(to);
        int toZ = ColonyRoads.zOf(to);

        while (x != toX || z != toZ) {
            boolean stepX = alongX ? x != toX : z == toZ;

            if (stepX) {
                x += Integer.signum(toX - x);
            } else {
                z += Integer.signum(toZ - z);
            }

            long column = ColonyRoads.column(x, z);
            Integer line = main.contains(column) ? columns.get(column) : null;

            if (line != null) {
                return Math.abs(line - y) <= 1 ? Optional.of(route) : Optional.empty();
            }

            OptionalInt ground = RoadLines.groundOf(mask, terrain, x, z, y);

            if (ground.isPresent() && Math.abs(ground.getAsInt() - y) <= 1) {
                y = ground.getAsInt();
            } else if (!terrain.canCut(x, z, y)) {
                return Optional.empty();
            }

            route.put(column, y);
        }

        return Optional.empty();
    }

    /** O ramal da porta sozinha: a reta mais curta até a linha principal, sem busca — Regra 178. */
    static boolean stub(Set<Long> part, Set<Long> main, Map<Long, Integer> columns, Map<Long, Integer> mask,
                                PathTerrain terrain) {
        long door = part.iterator().next();
        Map<Long, Integer> best = null;

        for (Side side : Side.values()) {
            Map<Long, Integer> ray = new HashMap<>();
            int x = ColonyRoads.xOf(door);
            int z = ColonyRoads.zOf(door);
            int y = columns.getOrDefault(door, 0);

            for (int step = 1; step <= RoadLines.MAX_STUB && (best == null || step < best.size() + 1); step++) {
                x += side.offsetX();
                z += side.offsetZ();
                long column = ColonyRoads.column(x, z);
                Integer line = main.contains(column) ? columns.get(column) : null;

                if (line != null) {
                    if (Math.abs(line - y) <= 1) {
                        best = ray;
                    }
                    break;
                }

                OptionalInt ground = RoadLines.groundOf(mask, terrain, x, z, y);

                if (ground.isPresent() && Math.abs(ground.getAsInt() - y) <= 1) {
                    y = ground.getAsInt();
                } else if (!terrain.canCut(x, z, y)) {
                    break;
                }

                ray.put(column, y);
            }
        }

        if (best == null) {
            return false;
        }

        columns.putAll(best);
        part.addAll(best.keySet());

        return true;
    }

    /** Liga esta parte à principal pelo trecho mais curto de chão andável. */
    static boolean link(Set<Long> part, Set<Long> main, Map<Long, Integer> columns, PathTerrain terrain) {
        long from = part.iterator().next();
        long to = main.iterator().next();
        int best = Integer.MAX_VALUE;

        for (long a : part) {
            for (long b : main) {
                int distance = manhattan(a, b);

                if (distance < best || (distance == best && a < from)) {
                    best = distance;
                    from = a;
                    to = b;
                }
            }
        }

        if (best > RoadLines.MAX_LINK) {
            return false;
        }

        long target = to;
        VillagePathPlanner.Goal goal = new VillagePathPlanner.Goal() {
            @Override
            public boolean reached(int x, int floorY, int z) {
                long column = ColonyRoads.column(x, z);
                Integer y = columns.get(column);

                return y != null && main.contains(column) && Math.abs(y - floorY) <= 1;
            }

            @Override
            public int estimate(int x, int z) {
                return Math.abs(x - ColonyRoads.xOf(target)) + Math.abs(z - ColonyRoads.zOf(target));
            }

            @Override
            public boolean isPath(int x, int floorY, int z) {
                return columns.containsKey(ColonyRoads.column(x, z));
            }
        };

        Optional<List<PathStep>> steps = VillagePathPlanner.plan(terrain, ColonyRoads.xOf(from),
                columns.getOrDefault(from, 0), ColonyRoads.zOf(from), headingTo(from, to), goal);

        if (steps.isEmpty() || steps.get().isEmpty()) {
            return false;
        }

        for (PathStep step : steps.get()) {
            long column = ColonyRoads.column(step.x(), step.z());

            columns.putIfAbsent(column, step.floorY());
            part.add(column);
        }

        return true;
    }

    private static Side headingTo(long from, long to) {
        int dx = ColonyRoads.xOf(to) - ColonyRoads.xOf(from);
        int dz = ColonyRoads.zOf(to) - ColonyRoads.zOf(from);

        for (Side side : Side.values()) {
            if (Math.abs(dx) >= Math.abs(dz) ? side.offsetX() == Integer.signum(dx) && side.offsetZ() == 0
                    : side.offsetZ() == Integer.signum(dz) && side.offsetX() == 0) {
                return side;
            }
        }

        return Side.NORTH;
    }

    private static int manhattan(long a, long b) {
        return Math.abs(ColonyRoads.xOf(a) - ColonyRoads.xOf(b)) + Math.abs(ColonyRoads.zOf(a) - ColonyRoads.zOf(b));
    }
}
