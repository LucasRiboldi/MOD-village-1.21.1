package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * O corredor de caminho — regra do autor de 2026-10-22: <i>"todo caminho é uma linha reta até encontrar
 * outro caminho", "com quebras de 90 graus, sem considerar blocos na diagonal", "caminho deve ser apenas
 * um bloco conectado a outro", "sempre saem da porta das estruturas em direção linear a outra linha de
 * caminhos"</i>.
 *
 * <p>O corredor parte do bloco em frente à porta, seguindo na direção dela, anda em retas de um bloco de
 * largura, vira só em ângulo reto (no máximo {@link #MAX_TURNS} vezes, e nunca antes de
 * {@link #MIN_STRAIGHT} blocos retos), e acaba no primeiro bloco que encosta em caminho — de frente ou de
 * lado. Por isso nunca corre colado ao lado de outra rua: encostou, parou. Cada bloco segue o terreno com
 * degrau de no máximo um ({@link RoadPaving#groundNear}), passa pelas recusas da rua
 * ({@link RoadPaving#refusalAt}) e tem espaço para o corpo.
 *
 * <p>Na encosta, o corredor que pode cortar ({@code maxCut} &gt; 0) também desce ou sobe um bloco por vez
 * cortando o chão do bioma acima dele, até {@code maxCut} de fundo ({@link LotTerraform#cutAbove}) — o
 * degrau cortado na duna, escolha do autor de 2026-10-22. Cada bloco cortado custa um passo a mais: o
 * terreno natural vence o corte. Nunca aterra.
 */
public final class Corridor {

    /** Quantas vezes o corredor vira quando corta a encosta. */
    public static final int MAX_TURNS = 2;

    /**
     * Quantas vezes o corredor que só segue o terreno pode virar: a serpentina que sobe a encosta com
     * degrau de no máximo um, como trilha de morro — opção 3 do autor, 2026-10-28. É a primeira tentativa;
     * só sem ela o corredor corta.
     */
    public static final int SERPENTINE_TURNS = 6;

    /** Quantos blocos retos antes de virar: sai da porta, não rente à parede. */
    static final int MIN_STRAIGHT = 2;

    /** O que uma curva custa a mais que um bloco: a reta é o caminho. */
    static final int TURN_COST = 6;

    /**
     * O teto de tempo de uma busca: passou dele, a busca para e o motivo vai para o log. Na 0.3.90 a busca
     * com corte chegou a 3,2 s num tique (planejador, `VC_COST`) e travou o servidor a cada volta pela faixa.
     */
    static final long MAX_PLAN_NANOS = 50_000_000L;

    /** O motivo que a busca anota quando passa de {@link #MAX_PLAN_NANOS}. */
    public static final String OUT_OF_TIME = "a search longer than 50 ms";

    /**
     * Até quantos blocos de fundo o corredor corta a encosta, quando a serpentina não tem volta: corte raso,
     * com talude dos lados ({@code CorridorCut}) — opção 3 do autor, 2026-10-28. Era 12, e o corte de 12 com
     * paredes retas abria um cânion no morro.
     */
    public static final int MAX_CUT = 3;

    private Corridor() {
    }

    /** Onde o corredor de uma obra começa: o bloco em frente à porta, e para que lado ela abre. */
    public record Door(BlockPos front, Direction out) {
    }

    /** A porta desta obra, se a planta tem porta. */
    public static Optional<Door> doorOf(ConstructionProject project) {
        Optional<com.villagecolony.core.type.Side> side = project.blueprint().doorSide();

        if (side.isEmpty()) {
            return Optional.empty();
        }

        Direction out = Direction.fromVector(side.get().offsetX(), 0, side.get().offsetZ());

        if (out == null) {
            return Optional.empty();
        }

        ColonyPos origin = project.origin();

        return project.blueprint().blocks().stream()
                .filter(block -> block.block().path().endsWith("_door"))
                .findFirst()
                .map(block -> new Door(new BlockPos(
                        origin.x() + block.offset().x(), origin.y(), origin.z() + block.offset().z()).offset(out), out));
    }

    /**
     * O corredor do chão {@code start} (em frente à porta) até encostar em caminho.
     *
     * @param avoid colunas que o corredor não atravessa (o lote)
     * @param stopped recebe o motivo de cada ramo recusado, para o log
     * @return os blocos a calçar, da porta até o que encosta no caminho; vazio sem corredor
     */
    public static Optional<List<BlockPos>> plan(
            ServerWorld world, UUID colonyId, BlockPos start, Direction out, int maxLength,
            Predicate<BlockPos> avoid, Consumer<String> stopped) {

        return plan(world, colonyId, start, out, maxLength, 0, avoid, stopped);
    }

    /**
     * O corredor que pode cortar a encosta até {@code maxCut} blocos de fundo; 0 segue só o terreno.
     *
     * @return os blocos a calçar, cada um na altura em que fica depois do corte
     */
    public static Optional<List<BlockPos>> plan(
            ServerWorld world, UUID colonyId, BlockPos start, Direction out, int maxLength, int maxCut,
            Predicate<BlockPos> avoid, Consumer<String> stopped) {

        return plan(world, colonyId, start, out, maxLength, maxCut, MAX_TURNS, MAX_PLAN_NANOS, avoid, stopped);
    }

    /** O corredor em serpentina que só segue o terreno: até {@link #SERPENTINE_TURNS} curvas, sem cortar. */
    public static Optional<List<BlockPos>> planWinding(
            ServerWorld world, UUID colonyId, BlockPos start, Direction out, int maxLength,
            Predicate<BlockPos> avoid, Consumer<String> stopped) {

        return plan(world, colonyId, start, out, maxLength, 0, SERPENTINE_TURNS, MAX_PLAN_NANOS, avoid, stopped);
    }

    /** O mesmo, com o teto de tempo dado — o teste o zera para medir a parada sem depender da máquina. */
    static Optional<List<BlockPos>> plan(
            ServerWorld world, UUID colonyId, BlockPos start, Direction out, int maxLength, int maxCut,
            long maxNanos, Predicate<BlockPos> avoid, Consumer<String> stopped) {

        return plan(world, colonyId, start, out, maxLength, maxCut, MAX_TURNS, maxNanos, avoid, stopped);
    }

    private static Optional<List<BlockPos>> plan(
            ServerWorld world, UUID colonyId, BlockPos start, Direction out, int maxLength, int maxCut,
            int maxTurns, long maxNanos, Predicate<BlockPos> avoid, Consumer<String> stopped) {

        if (isPath(world, colonyId, start) || touchesPath(world, colonyId, start, out.getOpposite())) {
            return Optional.of(isPath(world, colonyId, start) ? List.of() : List.of(start));
        }

        // A fila ordena pela prioridade; o estado não é Comparable, porque a igualdade dele a ignora e o
        // HashMap usa a ordem do Comparable para achar a chave nos baldes que viram árvore.
        PriorityQueue<State> open = new PriorityQueue<>(java.util.Comparator.comparingInt(State::priority));
        Map<State, Integer> cost = new HashMap<>();
        Map<State, State> cameFrom = new HashMap<>();
        java.util.Set<State> touching = new java.util.HashSet<>();
        State first = new State(start, out, 0, 1);

        open.add(first);
        cost.put(first, 0);

        // A mesma coluna, saindo do mesmo lugar, se lê uma vez só: os estados que diferem só nas curvas e
        // nos retos passam por ela de novo, e o corte relê a coluna inteira e os vizinhos de fluido.
        Map<Move, Expansion> expansions = new HashMap<>();
        long deadline = System.nanoTime() + maxNanos;
        int polled = 0;

        while (!open.isEmpty()) {
            if ((++polled & 63) == 0 && System.nanoTime() > deadline) {
                stopped.accept(OUT_OF_TIME);
                return Optional.empty();
            }

            State at = open.poll();
            int here = cost.get(at);

            if (at.priority() > here) {
                continue;
            }

            // O encontro fecha a busca quando sai da fila, com o menor custo: a reta vence a curva.
            if (touching.contains(at)) {
                return Optional.of(trace(cameFrom, at));
            }

            if (at.length() >= maxLength) {
                stopped.accept("more than " + maxLength + " blocks");
                continue;
            }

            for (Direction way : Direction.Type.HORIZONTAL) {
                if (way == at.way().getOpposite()) {
                    continue;
                }

                boolean turn = way != at.way();

                if (turn && (at.turns() >= maxTurns || at.straight() < MIN_STRAIGHT)) {
                    continue;
                }

                Expansion expansion = expansions.computeIfAbsent(new Move(at.pos(), way),
                        move -> expand(world, colonyId, move, maxCut, avoid, stopped));

                if (expansion.reachesPath()) {
                    return Optional.of(trace(cameFrom, at));
                }

                for (Map.Entry<BlockPos, Integer> step : expansion.steps().entrySet()) {
                    State next = new State(step.getKey(), way, at.turns() + (turn ? 1 : 0),
                            turn ? 1 : at.straight() + 1, at.length() + 1);
                    int reached = here + 1 + (turn ? TURN_COST : 0) + step.getValue();

                    if (reached < cost.getOrDefault(next, Integer.MAX_VALUE)) {
                        cost.put(next, reached);
                        cameFrom.put(next, at);

                        if (expansion.touching().contains(next.pos())) {
                            touching.add(next);
                        }

                        open.add(next.withCost(reached));
                    }
                }
            }
        }

        return Optional.empty();
    }

    /** Um passo da busca: de onde, para que lado. */
    private record Move(BlockPos from, Direction way) {
    }

    /**
     * O que um passo dá, lido do mundo uma vez: se o bloco à frente já é caminho, os degraus possíveis
     * (natural e cortados, com o que cada um corta) e quais deles encostam em caminho de lado.
     */
    private record Expansion(boolean reachesPath, Map<BlockPos, Integer> steps, java.util.Set<BlockPos> touching) {
    }

    private static Expansion expand(
            ServerWorld world, UUID colonyId, Move move, int maxCut, Predicate<BlockPos> avoid, Consumer<String> stopped) {

        Optional<BlockPos> ground = RoadPaving.groundNear(world, move.from().offset(move.way()), move.from().getY());

        if (ground.isPresent() && isPath(world, colonyId, ground.get())) {
            return new Expansion(true, Map.of(), java.util.Set.of());
        }

        Map<BlockPos, Integer> steps = new java.util.LinkedHashMap<>();

        if (ground.isEmpty()) {
            stopped.accept("a step of more than 1");
        } else {
            Optional<String> refusal = refusal(world, move.from(), ground.get(), avoid);

            if (refusal.isPresent()) {
                stopped.accept(refusal.get());
            } else {
                steps.put(ground.get(), 0);
            }
        }

        if (maxCut > 0) {
            cutSteps(world, move.from(), move.way(), maxCut, avoid, stopped).forEach(steps::putIfAbsent);
        }

        java.util.Set<BlockPos> touching = new java.util.HashSet<>();

        for (BlockPos step : steps.keySet()) {
            if (touchesPath(world, colonyId, step, move.way().getOpposite())) {
                touching.add(step);
            }
        }

        return new Expansion(false, steps, touching);
    }

    /** O mesmo, sem anotar os motivos. */
    public static Optional<List<BlockPos>> plan(
            ServerWorld world, UUID colonyId, BlockPos start, Direction out, int maxLength, Predicate<BlockPos> avoid) {

        return plan(world, colonyId, start, out, maxLength, avoid, why -> { });
    }

    /**
     * Os degraus cortados para o lado {@code way}: o chão um acima, no nível ou um abaixo de {@code from},
     * com chão do bioma por cima que se pode tirar ({@link LotTerraform#cutAbove}), e quantos blocos sai —
     * 0 para o degrau natural que sai de uma coluna cortada.
     */
    private static Map<BlockPos, Integer> cutSteps(
            ServerWorld world, BlockPos from, Direction way, int maxCut, Predicate<BlockPos> avoid,
            Consumer<String> stopped) {

        Map<BlockPos, Integer> steps = new java.util.LinkedHashMap<>();
        BlockPos column = from.offset(way);

        for (int dy = 1; dy >= -1; dy--) {
            BlockPos at = column.up(dy);
            BlockState state = world.getBlockState(at);

            if (!world.isInBuildLimit(at) || LotGround.isNothing(state) || !state.getFluidState().isEmpty()) {
                continue;
            }

            java.util.OptionalInt depth = LotTerraform.cutAbove(world, at, maxCut);

            if (depth.isEmpty()) {
                stopped.accept("a cut deeper than " + maxCut + " or not of the biome ground");
                continue;
            }

            // O degrau que sobe precisa de cabeça livre em cima de onde se está — livre já, ou livre
            // depois do corte da própria coluna.
            boolean head = dy <= 0 || world.getBlockState(from.up(2)).getCollisionShape(world, from.up(2)).isEmpty()
                    || LotTerraform.cutAbove(world, from, maxCut).orElse(0) > 0;

            if (head && !avoid.test(at) && RoadPaving.refusalOfTheGround(world, at, state).isEmpty()) {
                steps.put(at, depth.getAsInt());
            }
        }

        return steps;
    }

    /** O que impede este bloco de ser corredor, se algo impede. */
    private static Optional<String> refusal(ServerWorld world, BlockPos from, BlockPos at, Predicate<BlockPos> avoid) {
        if (avoid.test(at)) {
            return Optional.of("the lot itself");
        }

        BlockState state = world.getBlockState(at);
        Optional<String> refusal = RoadPaving.refusalAt(world, at, state);

        if (refusal.isPresent()) {
            return refusal;
        }

        boolean room = world.getBlockState(at.up(2)).getCollisionShape(world, at.up(2)).isEmpty()
                && (at.getY() <= from.getY()
                        || world.getBlockState(from.up(2)).getCollisionShape(world, from.up(2)).isEmpty());

        return room ? Optional.empty() : Optional.of("no room to walk");
    }

    /** Encosta em caminho por algum lado que não o de onde veio. */
    private static boolean touchesPath(ServerWorld world, UUID colonyId, BlockPos at, Direction back) {
        for (Direction way : Direction.Type.HORIZONTAL) {
            if (way == back) {
                continue;
            }

            Optional<BlockPos> beside = RoadPaving.groundNear(world, at.offset(way), at.getY());

            if (beside.isPresent() && isPath(world, colonyId, beside.get())) {
                return true;
            }
        }

        return false;
    }

    static boolean isPath(ServerWorld world, UUID colonyId, BlockPos ground) {
        return VillageRoad.isPaving(world, ground) || RoadIndex.isRoadArea(world, colonyId, ground);
    }

    private static List<BlockPos> trace(Map<State, State> cameFrom, State last) {
        List<BlockPos> cells = new ArrayList<>();

        for (State at = last; at != null; at = cameFrom.get(at)) {
            cells.add(at.pos());
        }

        Collections.reverse(cells);

        return cells;
    }

    /** Os blocos do corredor como posições do Core. */
    public static List<ColonyPos> asColonyPositions(List<BlockPos> cells) {
        return cells.stream().map(MinecraftTypeAdapter::toColonyPos).toList();
    }

    /** Um estado da busca: onde, para que lado, quantas curvas, quantos retos desde a última, e o tamanho. */
    private record State(BlockPos pos, Direction way, int turns, int straight, int length, int priority) {

        State(BlockPos pos, Direction way, int turns, int straight) {
            this(pos, way, turns, straight, 1, 0);
        }

        State(BlockPos pos, Direction way, int turns, int straight, int length) {
            this(pos, way, turns, straight, length, 0);
        }

        State withCost(int cost) {
            return new State(pos, way, turns, straight, length, cost);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof State state && pos.equals(state.pos) && way == state.way
                    && turns == state.turns && Math.min(straight, MIN_STRAIGHT) == Math.min(state.straight, MIN_STRAIGHT);
        }

        @Override
        public int hashCode() {
            return pos.hashCode() * 31 + way.hashCode() * 7 + turns * 3 + Math.min(straight, MIN_STRAIGHT);
        }
    }
}
