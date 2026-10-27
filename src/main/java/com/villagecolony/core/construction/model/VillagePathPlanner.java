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
 * O traçado do caminho da vila — Regra 123 (autor, 2026-10-27; estudo em
 * {@code docs/research/2026-10-27-rede-de-caminhos.md}).
 *
 * <p>Um A* em colunas: cada passo anda um bloco num dos quatro rumos e o piso sobe, desce ou fica
 * — <b>nunca mais de um bloco</b>, para todo aldeão subir e descer. A seção tem {@link #WIDTH} colunas
 * no mesmo y (o eixo e uma de cada lado). O buraco é aterrado (custo por bloco); o morro é cortado no
 * máximo {@link #MAX_CUT} blocos, e o corte custa caro: no morro íngreme sai mais barato subir em
 * serpentina pela encosta, e só onde ela não cabe o traçado corta ou contorna (decisão D3). Coluna de
 * caminho já existente custa pouco, e os traçados se juntam na rede; correr colado ao lado de um caminho
 * custa caro, para nunca haver dois lado a lado. Construção é coluna proibida ({@link PathTerrain#blocked}):
 * o caminho não destrói nada (autor, 27-10).
 *
 * <p>Puro: o terreno vem de {@link PathTerrain}.
 */
public final class VillagePathPlanner {

    /** A largura do caminho: o eixo e uma coluna de cada lado. */
    public static final int WIDTH = 3;

    /** O degrau máximo entre um passo e o seguinte. */
    public static final int MAX_STEP = 1;

    /** Quantos blocos de morro o caminho corta, no máximo, abaixo do chão natural. */
    public static final int MAX_CUT = 2;

    /** Quantos blocos o caminho aterra, no máximo, acima do chão natural; mais fundo que isso ele desvia. */
    public static final int MAX_FILL = 4;

    /** Quantos estados o traçado olha antes de desistir: o teto de custo de uma busca. */
    public static final int MAX_NODES = 40_000;

    static final double STEP = 1.0;
    static final double TURN = 0.5;
    static final double CLIMB = 1.0;
    static final double FILL = 1.5;
    static final double CUT = 4.0;
    static final double REUSE = 0.25;
    static final double BESIDE = 3.0;

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

    private record State(int x, int y, int z, Side heading) {
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
        State start = new State(x, floorY, z, heading);
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

                    if (section.isEmpty()) {
                        continue;
                    }

                    double step = goal.isPath(nx, ny, nz) ? REUSE : STEP + section.get().cost();
                    double cost = current.cost() + step
                            + (dy != 0 ? CLIMB : 0)
                            + (towards != at.heading() ? TURN : 0);
                    State next = new State(nx, ny, nz, towards);

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
     * A seção de {@link #WIDTH} colunas com piso em {@code floorY}, de frente para {@code heading}: cada
     * coluna livre, com chão, e o piso dentro do aterro e do corte permitidos.
     */
    private static Optional<Section> section(PathTerrain terrain, Goal goal, int x, int floorY, int z,
                                             Side heading) {
        Side right = heading.clockwise();
        double cost = 0;
        int axisFill = 0;
        int axisCut = 0;

        for (int offset = -(WIDTH / 2); offset <= WIDTH / 2; offset++) {
            int cx = x + right.offsetX() * offset;
            int cz = z + right.offsetZ() * offset;

            if (terrain.blocked(cx, cz)) {
                return Optional.empty();
            }

            OptionalInt ground = terrain.groundY(cx, cz, floorY);

            if (ground.isEmpty()) {
                return Optional.empty();
            }

            int fill = floorY - ground.getAsInt();

            if (fill > MAX_FILL || -fill > MAX_CUT) {
                return Optional.empty();
            }

            cost += fill > 0 ? fill * FILL : -fill * CUT;

            // Colado de lado num caminho que já existe é sobrepor: ou se junta (eixo no caminho), ou se
            // afasta — nunca dois caminhos lado a lado (autor, 27-10).
            if (offset != 0 && goal.isPath(cx, floorY, cz) && !goal.isPath(x, floorY, z)) {
                cost += BESIDE * WIDTH;
            }

            if (offset == 0) {
                axisFill = Math.max(0, fill);
                axisCut = Math.max(0, -fill);
            }
        }

        return Optional.of(new Section(cost / WIDTH, axisFill, axisCut));
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
