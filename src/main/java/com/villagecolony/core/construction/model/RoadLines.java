package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * O grafo de caminhos da vila, como um mapa de trilhos — Regra 177 (autor, 2026-11-05).
 *
 * <p>Da faixa física ({@link RoadMask}) tira uma linha só por corredor ({@link RoadThinning}), com nós onde as linhas se
 * encontram ({@link RoadChains}); alinha os nós do mesmo corredor e traça cada trecho reto, com degrau de no máximo um
 * bloco ({@link RoadRouting}). Componente separada se liga à principal pelo caminho mais curto; a que não se liga fica
 * fora do grafo. Puro: o mundo vem por {@link PathTerrain}. O que falta calçar sai em {@link Result#toPave()}.
 */
public final class RoadLines {

    /** Quantos blocos, no máximo, um trecho de ligação entre componentes atravessa. */
    static final int MAX_LINK = 64;

    /** O maior ramal reto de uma porta até a linha. */
    static final int MAX_STUB = 16;

    /** Até onde da linha o caminho conta como a sobra da mesma rua. */
    private static final int SURPLUS_REACH = 3;

    /** Quantas vezes o afinamento roda sobre o próprio resultado. */
    private static final int PASSES = 3;

    /**
     * O resultado.
     *
     * @param graph o grafo, vazio quando não há caminho
     * @param toPave as colunas do grafo e da faixa fechada que ainda não são caminho, em ordem de andar
     * @param isolated componentes que ficaram fora do grafo
     * @param surplus os blocos de caminho colados à linha que não são a linha: a rua é de um bloco (Regra 186)
     */
    public record Result(Optional<RoadGraph> graph, List<PathStep> toPave, int isolated, List<ColonyPos> surplus) {
    }

    private RoadLines() {
    }

    /**
     * O grafo da vila.
     *
     * @param cells os blocos de caminho confirmados no mundo
     * @param planned as colunas que uma releitura anterior mandou calçar e ainda não são caminho
     * @param center o centro da vila: o nó central é a coluna do grafo mais perto dele
     * @param entrances o chão em frente às portas
     * @param farmRings o chão colado nas plantações: o caminho nele sai se a rede continua inteira (Regra 208)
     * @param maxLinks quantas componentes separadas tentar ligar nesta passada; cada ligação é uma busca cara
     * @param turn a vez desta passada: a parte que não se ligou agora não segura as outras na próxima
     */
    public static Result build(Set<ColonyPos> cells, Set<ColonyPos> planned, ColonyPos center, Set<ColonyPos> entrances,
                               Set<ColonyPos> farmRings, PathTerrain terrain, int maxLinks, int turn) {
        return build(cells, planned, center, entrances, farmRings, terrain, terrain, 0, maxLinks, turn);
    }

    /**
     * O mesmo, com a travessia: sem saída a seco, a ligação pode cruzar até {@code maxSpan} colunas sem chão.
     *
     * @param bridging o terreno em que a água conta como piso
     */
    public static Result build(Set<ColonyPos> cells, Set<ColonyPos> planned, ColonyPos center, Set<ColonyPos> entrances,
                               Set<ColonyPos> farmRings, PathTerrain terrain, PathTerrain bridging, int maxSpan, int maxLinks, int turn) {
        Map<Long, Integer> physical = new HashMap<>();

        cells.forEach(at -> physical.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));

        if (physical.isEmpty()) {
            return new Result(Optional.empty(), List.of(), 0, List.of());
        }

        Map<Long, Integer> start = new HashMap<>(physical);

        planned.forEach(at -> start.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));

        Set<Long> doors = new HashSet<>();

        for (ColonyPos door : entrances) {
            long column = ColonyRoads.column(door.x(), door.z());

            doors.add(column);
            start.putIfAbsent(column, door.y());
        }

        Map<Long, Integer> mask = RoadMask.closed(start, terrain);
        Map<Long, Integer> columns = lines(mask, mask, doors, terrain);

        // De novo sobre as próprias linhas: duas retas a até dois blocos viram uma, e os encontros colados, um nó.
        for (int pass = 1; pass < PASSES; pass++) {
            Map<Long, Integer> band = RoadMask.closed(columns, terrain);
            Map<Long, Integer> heights = new HashMap<>(mask);

            heights.putAll(band);

            Map<Long, Integer> again = lines(band, heights, doors, terrain);

            if (again.equals(columns)) {
                break;
            }

            columns = again;
        }

        List<Set<Long>> parts = components(columns);

        parts.sort(Comparator.comparingInt((Set<Long> part) -> -part.size()));

        if (parts.isEmpty()) {
            return new Result(Optional.empty(), List.of(), 0, List.of());
        }

        Set<Long> main = new HashSet<>(parts.getFirst());
        int isolated = 0;
        int links = 0;

        List<Set<Long>> others = new ArrayList<>(parts.subList(1, parts.size()));

        java.util.Collections.rotate(others, -turn);

        for (Set<Long> part : others) {
            boolean aloneDoor = part.size() == 1 && doors.contains(part.iterator().next());

            if (aloneDoor && stub(part, main, columns, mask, terrain)) {
                main.addAll(part);
                continue;
            }

            if (links < maxLinks) {
                links++;

                if (link(part, main, columns, terrain, bridging, maxSpan)) {
                    main.addAll(part);
                    continue;
                }
            }

            isolated += aloneDoor ? 0 : 1;
        }

        columns.keySet().retainAll(main);

        Set<Long> oldRing = new HashSet<>();

        farmRings.forEach(at -> oldRing.add(ColonyRoads.column(at.x(), at.z())));
        oldRing.retainAll(columns.keySet());

        FarmRingPaths.retire(columns, oldRing, center, doors);
        RoadTips.prune(columns, center, doors);

        // Só as linhas e a frente de toda porta: a rua é de um bloco (Regra 186).
        Map<Long, Integer> toLay = new HashMap<>();

        for (ColonyPos door : entrances) {
            toLay.putIfAbsent(ColonyRoads.column(door.x(), door.z()), door.y());
        }

        return new Result(Optional.of(RoadLinesGraph.of(columns, toLay, center, doors)), toPave(columns, toLay, physical), isolated,
                surplus(physical, columns, toLay, oldRing));
    }

    /**
     * O caminho físico que não é linha nem frente de porta, a até {@value #SURPLUS_REACH} blocos da linha (Regra 186) ou
     * no anel antigo que saiu do grafo, a qualquer distância (Regra 208).
     */
    private static List<ColonyPos> surplus(Map<Long, Integer> physical, Map<Long, Integer> columns, Map<Long, Integer> doors,
                                           Set<Long> oldRing) {
        List<ColonyPos> surplus = new ArrayList<>();

        physical.forEach((column, y) -> {
            if (!columns.containsKey(column) && !doors.containsKey(column)
                    && (oldRing.contains(column) || nearTheLine(columns, column))) {
                surplus.add(new ColonyPos(ColonyRoads.xOf(column), y, ColonyRoads.zOf(column)));
            }
        });

        surplus.sort(Comparator.comparingInt(ColonyPos::x).thenComparingInt(ColonyPos::z));

        return surplus;
    }

    private static boolean nearTheLine(Map<Long, Integer> columns, long column) {
        int x = ColonyRoads.xOf(column);
        int z = ColonyRoads.zOf(column);

        for (int dx = -SURPLUS_REACH; dx <= SURPLUS_REACH; dx++) {
            for (int dz = -SURPLUS_REACH; dz <= SURPLUS_REACH; dz++) {
                if (columns.containsKey(ColonyRoads.column(x + dx, z + dz))) {
                    return true;
                }
            }
        }

        return false;
    }

    /** Uma passada: a linha do meio de {@code band}, em nós e retas, com as portas. */
    private static Map<Long, Integer> lines(Map<Long, Integer> band, Map<Long, Integer> heights, Set<Long> doors,
                                            PathTerrain terrain) {
        RoadChains.Net net = RoadChains.of(RoadThinning.skeleton(new HashSet<>(band.keySet()), doors), doors);
        Map<Long, Integer> columns = RoadRouting.route(net, heights, terrain);

        for (long door : doors) {
            Integer y = heights.get(door);

            if (y != null) {
                columns.putIfAbsent(door, y);
            }
        }

        withoutCorners(columns, doors);

        return columns;
    }

    /** Tira a quina que dobra a linha num quadrado de quatro colunas: dois encontros colados viram um trecho só. */
    private static void withoutCorners(Map<Long, Integer> columns, Set<Long> doors) {
        boolean changed = true;

        while (changed) {
            changed = false;

            for (long at : List.copyOf(columns.keySet())) {
                if (!doors.contains(at) && isCornerOfASquare(columns, at)) {
                    columns.remove(at);
                    changed = true;
                }
            }
        }
    }

    private static boolean isCornerOfASquare(Map<Long, Integer> columns, long at) {
        int x = ColonyRoads.xOf(at);
        int z = ColonyRoads.zOf(at);
        int[][] diagonals = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

        if (cardinalDegree(columns, x, z) != 2) {
            return false;
        }

        for (int[] way : diagonals) {
            if (columns.containsKey(ColonyRoads.column(x + way[0], z))
                    && columns.containsKey(ColonyRoads.column(x, z + way[1]))
                    && columns.containsKey(ColonyRoads.column(x + way[0], z + way[1]))) {
                return true;
            }
        }

        return false;
    }

    static int cardinalDegree(Map<Long, Integer> columns, int x, int z) {
        int degree = 0;

        for (Side side : Side.values()) {
            degree += columns.containsKey(ColonyRoads.column(x + side.offsetX(), z + side.offsetZ())) ? 1 : 0;
        }

        return degree;
    }

    /** As partes ligadas: passo de lado, com degrau de no máximo um bloco. */
    static List<Set<Long>> components(Map<Long, Integer> columns) {
        List<Set<Long>> parts = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (long start : columns.keySet()) {
            if (seen.contains(start)) {
                continue;
            }

            Set<Long> part = new HashSet<>();
            Deque<Long> open = new ArrayDeque<>();

            part.add(start);
            open.add(start);

            while (!open.isEmpty()) {
                long at = open.poll();

                for (Side side : Side.values()) {
                    long next = ColonyRoads.column(ColonyRoads.xOf(at) + side.offsetX(), ColonyRoads.zOf(at) + side.offsetZ());
                    Integer y = columns.get(next);

                    if (y != null && !part.contains(next) && Math.abs(y - columns.getOrDefault(at, y)) <= 1) {
                        part.add(next);
                        open.add(next);
                    }
                }
            }

            seen.addAll(part);
            parts.add(part);
        }

        return parts;
    }

    /** O ramal da porta sozinha: a reta mais curta até a linha principal, sem busca — Regra 178. */
    private static boolean stub(Set<Long> part, Set<Long> main, Map<Long, Integer> columns, Map<Long, Integer> mask,
                                PathTerrain terrain) {
        long door = part.iterator().next();
        Map<Long, Integer> best = null;

        for (Side side : Side.values()) {
            Map<Long, Integer> ray = new HashMap<>();
            int x = ColonyRoads.xOf(door);
            int z = ColonyRoads.zOf(door);
            int y = columns.getOrDefault(door, 0);

            for (int step = 1; step <= MAX_STUB && (best == null || step < best.size() + 1); step++) {
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

                OptionalInt ground = groundOf(mask, terrain, x, z, y);

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
    private static boolean link(Set<Long> part, Set<Long> main, Map<Long, Integer> columns, PathTerrain terrain,
                                PathTerrain bridging, int maxSpan) {
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

        if (best > MAX_LINK) {
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

        if ((steps.isEmpty() || steps.get().isEmpty()) && maxSpan > 0) {
            steps = VillagePathPlanner.plan(bridging, ColonyRoads.xOf(from), columns.getOrDefault(from, 0), ColonyRoads.zOf(from),
                    headingTo(from, to), goal).filter(found -> found.stream()
                    .filter(step -> terrain.groundY(step.x(), step.z(), step.floorY()).isEmpty()).count() <= maxSpan);
        }

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

    /** O que falta calçar: a faixa fechada e as colunas do grafo, numa ordem de andar coluna a coluna. */
    private static List<PathStep> toPave(Map<Long, Integer> columns, Map<Long, Integer> mask,
                                         Map<Long, Integer> physical) {
        Map<Long, Integer> missing = new HashMap<>(mask);

        missing.putAll(columns);
        missing.keySet().removeAll(physical.keySet());

        List<PathStep> ordered = new ArrayList<>();
        List<Long> left = new ArrayList<>(missing.keySet());

        left.sort(null);

        long at = left.isEmpty() ? 0 : left.getFirst();

        while (!left.isEmpty()) {
            long here = at;
            long next = left.stream().min(Comparator.comparingInt((Long other) -> manhattan(here, other))).orElseThrow();

            left.remove(next);
            ordered.add(new PathStep(ColonyRoads.xOf(next), missing.getOrDefault(next, 0), ColonyRoads.zOf(next), Side.NORTH, 0, 0));
            at = next;
        }

        return ordered;
    }

    /** O chão desta coluna: o do caminho que já existe, ou o do terreno. */
    static OptionalInt groundOf(Map<Long, Integer> mask, PathTerrain terrain, int x, int z, int nearY) {
        Integer known = mask.get(ColonyRoads.column(x, z));

        if (known != null) {
            return OptionalInt.of(known);
        }

        return terrain.blocked(x, z) ? OptionalInt.empty() : terrain.groundY(x, z, nearY);
    }
}
