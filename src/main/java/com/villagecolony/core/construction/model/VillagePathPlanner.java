package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.PriorityQueue;

/**
 * O traçado do caminho da vila — Regra 123, rente ao chão (autor, 2026-10-28; estudo em
 * {@code docs/research/2026-10-27-rede-de-caminhos.md}).
 *
 * <p>Um A* em colunas: cada passo anda um bloco num dos quatro rumos e o piso sobe, desce ou fica —
 * <b>nunca mais de um bloco</b>. O caminho troca só o bloco de cima do chão: o piso do eixo fica no
 * chão natural, corta no máximo {@link #MAX_CUT} bloco e aterra no máximo {@link #MAX_FILL}, e o aterro
 * só vale para o <b>buraco</b> — o passo de antes e o de depois ficam no mesmo nível ou acima dele. Nunca
 * nasce bloco acima do terreno: no 0.3.98 o aterro de até 4 por coluna levantou paredes de terra nas
 * encostas. As colunas de lado seguem o próprio chão, até um bloco acima ou abaixo do eixo; onde não dá,
 * o caminho estreita ali. Desnível maior que um bloco se contorna, ou a ligação não sai.
 *
 * <p>Coluna de caminho já existente custa pouco, e os traçados se juntam na rede; correr colado ao lado de
 * um caminho custa caro, para nunca haver dois lado a lado. Construção é coluna proibida
 * ({@link PathTerrain#blocked}). Puro: o terreno vem de {@link PathTerrain}.
 */
public final class VillagePathPlanner {

    /**
     * A largura do caminho da porta até a rua: só o eixo (autor, 2026-10-28). O traçado ainda prefere passar onde
     * cabem as colunas de lado ({@code NARROW}), e foge de correr colado a outro caminho ({@code BESIDE}).
     */
    public static final int WIDTH = 1;

    /** O degrau máximo entre um passo e o seguinte, e entre o eixo e a coluna de lado. */
    public static final int MAX_STEP = 1;

    /** Quanto o piso do eixo fica, no máximo, abaixo do chão natural. */
    public static final int MAX_CUT = 1;

    /** Quanto o piso do eixo fica, no máximo, acima do chão natural — só no buraco. */
    public static final int MAX_FILL = 1;

    /**
     * Quantos estados o traçado olha antes de desistir — o bastante para contornar um barranco de 20
     * blocos. O custo de cada estado é o da leitura do terreno, que o lado Fabric guarda por traçado: sem
     * isso, nove portas sem saída num tique pararam o servidor 12 s (Spark S3FcIdzPmh).
     */
    public static final int MAX_NODES = 20_000;

    static final double STEP = 1.0;
    static final double TURN = 0.5;
    static final double CLIMB = 1.0;
    static final double FILL = 3.0;
    static final double CUT = 4.0;
    static final double REUSE = 0.25;
    static final double BESIDE = 3.0;
    static final double NARROW = 1.0;

    /** Até onde o traçado vai, e onde ele para. */
    public interface Goal {

        /** O passo com piso em {@code floorY} nesta coluna chegou. */
        boolean reached(int x, int floorY, int z);

        /** Quantos passos, no mínimo, até chegar — nunca mais que a verdade. */
        int estimate(int x, int z);

        /** A coluna já é caminho com piso perto de {@code floorY}: custa {@link #REUSE}. */
        default boolean isPath(int x, int floorY, int z) {
            return false;
        }
    }

    /** O estado da busca; {@code raised} quando este passo aterrou um buraco. */
    private record State(int x, int y, int z, Side heading, boolean raised) {
    }

    private record Open(State state, double cost, double priority) {
    }

    private record Section(double cost, int fill, int cut) {
    }

    private VillagePathPlanner() {
    }

    /**
     * O traçado mais barato a partir do piso {@code (x, floorY, z)}, saindo no rumo {@code heading}.
     *
     * @return os passos, do primeiro depois da partida até o que chega; vazio quando não há traçado
     *     dentro de {@link #MAX_NODES}
     */
    public static Optional<List<PathStep>> plan(PathTerrain terrain, int x, int floorY, int z, Side heading,
                                                Goal goal) {
        State start = new State(x, floorY, z, heading, false);
        Map<State, Double> best = new HashMap<>();
        Map<State, State> cameFrom = new HashMap<>();
        Map<State, Section> sections = new HashMap<>();
        PriorityQueue<Open> open = new PriorityQueue<>((a, b) -> Double.compare(a.priority(), b.priority()));

        best.put(start, 0.0);
        open.add(new Open(start, 0.0, goal.estimate(x, z) * REUSE));

        int expanded = 0;

        while (!open.isEmpty() && expanded++ < MAX_NODES) {
            Open current = open.poll();
            State at = current.state();

            if (current.cost() > best.getOrDefault(at, Double.MAX_VALUE)) {
                continue;
            }

            if (!at.equals(start) && goal.reached(at.x(), at.y(), at.z())) {
                return Optional.of(walkBack(at, cameFrom, sections));
            }

            for (Side towards : Side.values()) {
                if (towards == at.heading().opposite()) {
                    continue;
                }

                int nx = at.x() + towards.offsetX();
                int nz = at.z() + towards.offsetZ();

                for (int dy = -MAX_STEP; dy <= MAX_STEP; dy++) {
                    int ny = at.y() + dy;
                    Optional<Section> section = section(terrain, goal, nx, ny, nz, towards);

                    if (section.isEmpty() || !fitsTheHoleRule(at.raised(), dy, section.get().fill())) {
                        continue;
                    }

                    double step = goal.isPath(nx, ny, nz) ? REUSE : STEP + section.get().cost();
                    double cost = current.cost() + step
                            + (dy != 0 ? CLIMB : 0)
                            + (towards != at.heading() ? TURN : 0);
                    State next = new State(nx, ny, nz, towards, section.get().fill() > 0);

                    if (cost < best.getOrDefault(next, Double.MAX_VALUE)) {
                        best.put(next, cost);
                        cameFrom.put(next, at);
                        sections.put(next, section.get());
                        open.add(new Open(next, cost, cost + goal.estimate(nx, nz) * REUSE));
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * O aterro só fecha buraco: o passo aterrado não vem de baixo ({@code dy <= 0}) e não emenda em
     * outro aterro; o passo depois dele não desce ({@code dy >= 0}). Um trecho elevado sobre o chão, por
     * curto que seja, nunca sai.
     */
    static boolean fitsTheHoleRule(boolean previousRaised, int dy, int fill) {
        if (previousRaised && (dy < 0 || fill > 0)) {
            return false;
        }

        return fill <= 0 || dy <= 0;
    }

    /**
     * A seção com o eixo em {@code floorY}, de frente para {@code heading}: o eixo livre, com chão a no
     * máximo um bloco do piso; cada coluna de lado entra se estiver livre e com o próprio chão a no máximo
     * um bloco do piso do eixo, e fica de fora (o caminho estreita) se não.
     */
    private static Optional<Section> section(PathTerrain terrain, Goal goal, int x, int floorY, int z,
                                             Side heading) {
        if (terrain.blocked(x, z)) {
            return Optional.empty();
        }

        OptionalInt axisGround = terrain.groundY(x, z, floorY);

        if (axisGround.isEmpty()) {
            return Optional.empty();
        }

        int fill = floorY - axisGround.getAsInt();

        if (fill > MAX_FILL || -fill > MAX_CUT) {
            return Optional.empty();
        }

        double cost = fill > 0 ? fill * FILL : -fill * CUT;
        Side right = heading.clockwise();

        for (int offset : new int[] {-1, 1}) {
            int cx = x + right.offsetX() * offset;
            int cz = z + right.offsetZ() * offset;

            if (!laneFits(terrain, cx, cz, floorY)) {
                cost += NARROW;
                continue;
            }

            // Colado de lado num caminho que já existe é sobrepor: ou se junta (eixo no caminho), ou se
            // afasta — nunca dois caminhos lado a lado (autor, 27-10).
            if (goal.isPath(cx, floorY, cz) && !goal.isPath(x, floorY, z)) {
                cost += BESIDE;
            }
        }

        return Optional.of(new Section(cost, Math.max(0, fill), Math.max(0, -fill)));
    }

    /** A coluna de lado recebe caminho no próprio chão: livre e a no máximo um bloco do piso do eixo. */
    public static boolean laneFits(PathTerrain terrain, int x, int z, int axisFloorY) {
        if (terrain.blocked(x, z)) {
            return false;
        }

        OptionalInt ground = terrain.groundY(x, z, axisFloorY);

        return ground.isPresent() && Math.abs(ground.getAsInt() - axisFloorY) <= MAX_STEP;
    }

    private static List<PathStep> walkBack(State end, Map<State, State> cameFrom, Map<State, Section> sections) {
        List<PathStep> steps = new ArrayList<>();

        for (State at = end; cameFrom.containsKey(at); at = cameFrom.get(at)) {
            Section section = java.util.Objects.requireNonNull(sections.get(at), "section");
            steps.add(new PathStep(at.x(), at.y(), at.z(), at.heading(), section.fill(), section.cut()));
        }

        Collections.reverse(steps);

        return List.copyOf(steps);
    }
}
