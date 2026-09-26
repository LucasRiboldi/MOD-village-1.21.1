package com.villagecolony.core.movement;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * O desvio que cava e põe bloco — ADR-025, fase 2.
 *
 * <p><b>Quando roda.</b> Só depois de a navegação Vanilla falhar: ela não quebra
 * nem põe bloco, e na maior parte do tempo basta. Este A* é a reserva, com
 * teto de raio e de nós, para que um mineiro preso nunca custe mais que uma
 * fração de tique.
 *
 * <p><b>O que ele devolve.</b> A sequência de pés — um passo por vizinho —, e
 * não as ações: quem executa pergunta ao {@link DetourMoves} de novo, contra o
 * mundo de agora, antes de cada passo.
 *
 * <p>Custos inteiros em meios-passos ({@link DetourMoves#WALK} = 2). A
 * heurística é a distância até o alvo em passos, e o objetivo é uma região
 * (qualquer lugar de onde a picareta alcança a pedra, por exemplo): ela pode
 * superestimar um pouco quando a região é larga, o que troca o caminho ótimo
 * por um bom caminho achado mais cedo — a troca que o orçamento pede.
 */
public final class DetourPlanner {

    /** Até onde o desvio vai, em cada eixo, a partir de onde o aldeão está. */
    public static final int RADIUS = 16;

    /** Quantas posições o A* abre antes de desistir. */
    public static final int MAX_NODES = 2500;

    private static final int[][] HORIZONTAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private DetourPlanner() {
    }

    /**
     * O desvio achado.
     *
     * @param steps os pés, passo a passo, sem a posição de partida
     * @param reachesGoal falso quando é só o trecho que mais se aproximou
     */
    public record Detour(List<ColonyPos> steps, boolean reachesGoal, int cost, int nodes) {

        public Detour {
            steps = List.copyOf(steps);
        }
    }

    /**
     * Procura o caminho de {@code start} a qualquer posição que satisfaça
     * {@code goal}.
     *
     * @param toward para onde fica o objetivo — guia a busca
     * @param keep células que não se cavam nem se ocupam
     * @param partial se, sem chegar, devolve o trecho que mais se aproximou
     * @return vazio quando não há caminho (nem trecho, com {@code partial})
     */
    public static java.util.Optional<Detour> plan(
            Terrain terrain,
            ColonyPos start,
            ColonyPos toward,
            Predicate<ColonyPos> goal,
            Set<ColonyPos> keep,
            boolean partial) {

        Map<ColonyPos, Integer> cost = new HashMap<>();
        Map<ColonyPos, ColonyPos> cameFrom = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();

        cost.put(start, 0);
        open.add(new Node(start, 0, estimate(start, toward)));

        ColonyPos best = start;
        int bestEstimate = estimate(start, toward);
        int nodes = 0;

        while (!open.isEmpty() && nodes < MAX_NODES) {
            Node node = open.poll();

            if (node.cost > cost.get(node.pos)) {
                continue;
            }

            nodes++;

            if (goal.test(node.pos)) {
                return java.util.Optional.of(
                        new Detour(pathTo(node.pos, cameFrom), true, node.cost, nodes));
            }

            int left = estimate(node.pos, toward);

            if (left < bestEstimate) {
                bestEstimate = left;
                best = node.pos;
            }

            for (ColonyPos next : neighbours(node.pos)) {
                if (!withinRadius(start, next)) {
                    continue;
                }

                java.util.Optional<DetourMoves.Actions> step =
                        DetourMoves.actions(terrain, node.pos, next, keep);

                if (step.isEmpty()) {
                    continue;
                }

                int reached = node.cost + step.get().cost();

                if (reached < cost.getOrDefault(next, Integer.MAX_VALUE)) {
                    cost.put(next, reached);
                    cameFrom.put(next, node.pos);
                    open.add(new Node(next, reached, reached + estimate(next, toward)));
                }
            }
        }

        if (partial && !best.equals(start)) {
            return java.util.Optional.of(
                    new Detour(pathTo(best, cameFrom), false, cost.get(best), nodes));
        }

        return java.util.Optional.empty();
    }

    /** Passos que faltam, no custo mais barato: um bloco andado por eixo dominante. */
    static int estimate(ColonyPos from, ColonyPos to) {
        int flat = Math.abs(to.x() - from.x()) + Math.abs(to.z() - from.z());
        int rise = Math.abs(to.y() - from.y());

        return Math.max(flat, rise) * DetourMoves.WALK;
    }

    static boolean withinRadius(ColonyPos start, ColonyPos at) {
        return Math.abs(at.x() - start.x()) <= RADIUS
                && Math.abs(at.y() - start.y()) <= RADIUS
                && Math.abs(at.z() - start.z()) <= RADIUS;
    }

    private static List<ColonyPos> neighbours(ColonyPos pos) {
        List<ColonyPos> out = new ArrayList<>(12);

        for (int[] way : HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy++) {
                out.add(new ColonyPos(pos.x() + way[0], pos.y() + dy, pos.z() + way[1]));
            }
        }

        return out;
    }

    private static List<ColonyPos> pathTo(ColonyPos end, Map<ColonyPos, ColonyPos> cameFrom) {
        List<ColonyPos> path = new ArrayList<>();

        for (ColonyPos at = end; cameFrom.containsKey(at); at = cameFrom.get(at)) {
            path.add(at);
        }

        Collections.reverse(path);

        return path;
    }

    private record Node(ColonyPos pos, int cost, int priority) implements Comparable<Node> {

        @Override
        public int compareTo(Node other) {
            return priority != other.priority
                    ? Integer.compare(priority, other.priority)
                    : Integer.compare(other.cost, cost);
        }
    }
}
