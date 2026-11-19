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
     * @param loose cada parte que não se ligou à linha, com a porta sozinha também: "door" ou "part:N" e a coluna
     *              mais perto do centro (jogo da 0.4.84, diagnóstico das entradas fora do grafo)
     */
    public record Result(Optional<RoadGraph> graph, List<PathStep> toPave, int isolated, List<ColonyPos> surplus,
                         List<String> loose) {
        public Result(Optional<RoadGraph> graph, List<PathStep> toPave, int isolated, List<ColonyPos> surplus) {
            this(graph, toPave, isolated, surplus, List.of());
        }
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
        return build(cells, planned, center, entrances, farmRings, terrain, maxLinks, turn, Set.of());
    }

    /**
     * O mesmo, com os trechos fixos: a linha passa por eles como estão, e o resto se ajeita em volta — Regra 225.
     *
     * @param fixed as colunas de trecho que não mudou há {@link RoadSettling#DAYS} dias
     */
    public static Result build(Set<ColonyPos> cells, Set<ColonyPos> planned, ColonyPos center, Set<ColonyPos> entrances,
                               Set<ColonyPos> farmRings, PathTerrain terrain, int maxLinks, int turn, Set<ColonyPos> fixed) {
        return build(cells, planned, center, entrances, farmRings, terrain, maxLinks, turn, fixed, Set.of());
    }

    /**
     * O mesmo, com as colunas que deixaram de ser entrada: a ponta que termina nelas não é ponta de crescer, e sai (Regras 223
     * e 229).
     */
    public static Result build(Set<ColonyPos> cells, Set<ColonyPos> planned, ColonyPos center, Set<ColonyPos> entrances,
                               Set<ColonyPos> farmRings, PathTerrain terrain, int maxLinks, int turn, Set<ColonyPos> fixed,
                               Set<ColonyPos> retired) {
        Map<Long, Integer> physical = new HashMap<>();

        cells.forEach(at -> physical.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));

        if (physical.isEmpty()) {
            return new Result(Optional.empty(), List.of(), 0, List.of());
        }

        Map<Long, Integer> start = new HashMap<>(physical);

        // A linha nunca passa sobre a água (Regra 264): o trecho planejado que perdeu o chão seco sai.
        planned.stream().filter(at -> terrain.groundY(at.x(), at.z(), at.y()).isPresent())
                .forEach(at -> start.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));

        Set<Long> doors = new HashSet<>();

        for (ColonyPos door : entrances) {
            long column = ColonyRoads.column(door.x(), door.z());

            doors.add(column);
            start.putIfAbsent(column, door.y());
        }

        Map<Long, Integer> settled = new HashMap<>();

        fixed.forEach(at -> settled.put(ColonyRoads.column(at.x(), at.z()), at.y()));
        settled.forEach(start::putIfAbsent);

        // Porta e trecho fixo não saem do afinamento, nem do corte de quadrado e de ponta.
        Set<Long> kept = new HashSet<>(doors);

        kept.addAll(settled.keySet());

        Map<Long, Integer> mask = RoadMask.closed(start, terrain);
        Map<Long, Integer> columns = lines(mask, mask, doors, kept, terrain);

        // De novo sobre as próprias linhas: duas retas a até dois blocos viram uma, e os encontros colados, um nó.
        for (int pass = 1; pass < PASSES; pass++) {
            Map<Long, Integer> band = RoadMask.closed(columns, terrain);
            Map<Long, Integer> heights = new HashMap<>(mask);

            heights.putAll(band);

            Map<Long, Integer> again = lines(band, heights, doors, kept, terrain);

            if (again.equals(columns)) {
                break;
            }

            columns = again;
        }

        settled.forEach(columns::putIfAbsent);
        RoadSquares.thin(columns, kept);

        List<Set<Long>> parts = components(columns);

        // Regra 300: a porta sozinha se junta antes ao caminho encostado nela, de qualquer parte, e liga com ela.
        if (joinLoneDoors(parts, columns, doors, mask, terrain)) {
            parts = components(columns);
        }

        parts.sort(Comparator.comparingInt((Set<Long> part) -> -part.size()));

        if (parts.isEmpty()) {
            return new Result(Optional.empty(), List.of(), 0, List.of());
        }

        Set<Long> main = new HashSet<>(parts.getFirst());
        int isolated = 0;
        int links = 0;
        List<String> loose = new ArrayList<>();

        List<Set<Long>> others = new ArrayList<>(parts.subList(1, parts.size()));

        // Regras 295 e 300: a maior parte solta tenta primeiro, sem gastar o limite; as outras se revezam na busca cara.
        if (others.size() > 1) {
            List<Set<Long>> rest = new ArrayList<>(others.subList(1, others.size()));
            java.util.Collections.rotate(rest, -turn);
            others = new ArrayList<>(List.of(others.getFirst()));
            others.addAll(rest);
        }

        boolean biggest = others.size() > 1 && maxLinks > 0;

        for (Set<Long> part : others) {
            boolean aloneDoor = part.size() == 1 && doors.contains(part.iterator().next());
            // A busca da maior parte solta não conta no limite: as outras seguem no rodízio (Regra 300).
            boolean free = biggest;

            biggest = false;

            // Toda parte solta tenta a reta até o ponto mais perto da linha, sem gastar a busca cara (Regra 224).
            if (RoadLinks.straight(part, main, columns, mask, terrain)) {
                main.addAll(part);
                continue;
            }

            if (aloneDoor && RoadLinks.stub(part, main, columns, mask, terrain)) {
                main.addAll(part);
                continue;
            }

            if (free || links < maxLinks) {
                links += free ? 0 : 1;

                if (RoadLinks.link(part, main, columns, terrain)) {
                    main.addAll(part);
                    continue;
                }
            }

            isolated += aloneDoor ? 0 : 1;
            loose.add(describe(part, columns, center, aloneDoor));
        }

        columns.keySet().retainAll(main);

        Set<Long> oldRing = new HashSet<>();

        farmRings.forEach(at -> oldRing.add(ColonyRoads.column(at.x(), at.z())));
        oldRing.retainAll(columns.keySet());

        FarmRingPaths.retire(columns, oldRing, center, kept);
        Set<Long> deadEnds = new HashSet<>();

        retired.forEach(at -> deadEnds.add(ColonyRoads.column(at.x(), at.z())));
        RoadTips.prune(columns, center, kept, deadEnds);

        // Só as linhas e a frente de toda porta: a rua é de um bloco (Regra 186).
        Map<Long, Integer> toLay = new HashMap<>();

        for (ColonyPos door : entrances) {
            toLay.putIfAbsent(ColonyRoads.column(door.x(), door.z()), door.y());
        }

        return new Result(Optional.of(RoadLinesGraph.of(columns, toLay, center, doors)), toPave(columns, toLay, physical), isolated,
                surplus(physical, columns, toLay, oldRing), List.copyOf(loose));
    }

    /** Liga cada porta sozinha ao caminho mais perto em reta, de qualquer parte; diz se alguma ligou. */
    private static boolean joinLoneDoors(List<Set<Long>> parts, Map<Long, Integer> columns, Set<Long> doors,
                                         Map<Long, Integer> mask, PathTerrain terrain) {
        boolean joined = false;

        for (Set<Long> part : parts) {
            if (part.size() != 1 || !doors.contains(part.iterator().next())) {
                continue;
            }

            Set<Long> elsewhere = new HashSet<>(columns.keySet());

            elsewhere.removeAll(part);
            joined |= RoadLinks.stub(new HashSet<>(part), elsewhere, columns, mask, terrain);
        }

        return joined;
    }

    /** A parte solta para o log: o tipo e a coluna dela mais perto do centro, com a altura. */
    private static String describe(Set<Long> part, Map<Long, Integer> columns, ColonyPos center, boolean aloneDoor) {
        long nearest = part.stream().min(Comparator.comparingLong((Long at) -> {
            long dx = ColonyRoads.xOf(at) - (long) center.x();
            long dz = ColonyRoads.zOf(at) - (long) center.z();
            return dx * dx + dz * dz;
        })).orElseThrow();
        return (aloneDoor ? "door" : "part:" + part.size()) + "@" + ColonyRoads.xOf(nearest) + ","
                + columns.getOrDefault(nearest, center.y()) + "," + ColonyRoads.zOf(nearest);
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

    /** Uma passada: a linha do meio de {@code band}, em nós e retas, com as portas; {@code kept} não sai do afinamento. */
    private static Map<Long, Integer> lines(Map<Long, Integer> band, Map<Long, Integer> heights, Set<Long> doors,
                                            Set<Long> kept, PathTerrain terrain) {
        RoadChains.Net net = RoadChains.of(RoadThinning.skeleton(new HashSet<>(band.keySet()), kept), doors);
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

    private static int manhattan(long a, long b) {
        return Math.abs(ColonyRoads.xOf(a) - ColonyRoads.xOf(b)) + Math.abs(ColonyRoads.zOf(a) - ColonyRoads.zOf(b));
    }

    /** O que falta calçar: a faixa fechada e as colunas do grafo, numa ordem de andar coluna a coluna. */
    static List<PathStep> toPave(Map<Long, Integer> columns, Map<Long, Integer> mask,
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
