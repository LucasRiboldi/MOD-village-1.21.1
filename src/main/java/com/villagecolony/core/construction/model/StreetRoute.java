package com.villagecolony.core.construction.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * O trajeto longo vai pela rede de ruas — autor, 2026-10-28, depois do jogo da 0.3.104 (escolha "as duas juntas": o
 * gramado custa mais dentro da vila, {@code ColonyPathing}, e o trajeto longo segue a rede). Acima de {@link #LONG}
 * blocos, o aldeão vai até a coluna de rua mais perto dele, anda pela rede até a coluna mais perto do destino, em
 * trechos de até {@link #LEG} passos, e de lá sai para o destino.
 *
 * <p>Não vai pela rua quando ela não liga os dois pontos, quando nenhuma rua fica a até {@link #REACH} blocos dele ou
 * do destino, ou quando a volta pela rua passa de {@link #DETOUR} vezes o direto (o mesmo teto da D5,
 * {@link NetworkDetour#SHORTCUT}). Distâncias em passos de 8 vizinhos, como o aldeão anda.
 *
 * <p>Puro: as colunas da rede vêm empacotadas por {@link ColonyRoads#column}.
 */
public final class StreetRoute {

    /** A partir de quantos blocos o trajeto é longo e vai pela rua. */
    public static final int LONG = 24;

    /** Quantos passos de rua cada trecho anda, no máximo — cabe com folga na busca de caminho do jogo (48). */
    public static final int LEG = 32;

    /** Até onde se procura a rua em volta do aldeão e do destino. */
    public static final int REACH = 8;

    /** A esta distância da coluna de entrada ele já está na rua. */
    static final int ON_STREET = 1;

    /** Quantas vezes o direto a volta pela rua pode ter. */
    public static final double DETOUR = NetworkDetour.SHORTCUT;

    /** Quantas colunas a busca pela rede percorre, no máximo. */
    static final int MAX_VISITED = 20_000;

    private StreetRoute() {
    }

    /**
     * O próximo ponto do trajeto de {@code (fromX, fromZ)} a {@code (toX, toZ)}: a coluna de rua para onde ir agora,
     * ou vazio quando ele deve ir direto.
     */
    public static Optional<Long> nextWaypoint(Set<Long> roads, int fromX, int fromZ, int toX, int toZ) {
        int direct = steps(fromX, fromZ, toX, toZ);

        if (direct < LONG || roads.isEmpty()) {
            return Optional.empty();
        }

        Optional<Long> exit = nearest(roads, toX, toZ);
        Optional<Long> entry = nearest(roads, fromX, fromZ);

        if (exit.isEmpty() || entry.isEmpty()) {
            return Optional.empty();
        }

        List<Long> route = route(roads, entry.get(), exit.get());

        if (route.isEmpty()) {
            return Optional.empty();
        }

        int toEntry = steps(fromX, fromZ, ColonyRoads.xOf(entry.get()), ColonyRoads.zOf(entry.get()));
        int fromExit = steps(ColonyRoads.xOf(exit.get()), ColonyRoads.zOf(exit.get()), toX, toZ);

        if (toEntry + (route.size() - 1) + fromExit > DETOUR * direct) {
            return Optional.empty();
        }

        if (toEntry > ON_STREET) {
            return entry;
        }

        return Optional.of(route.get(Math.min(LEG, route.size() - 1)));
    }

    /**
     * A coluna de rua mais perto de {@code (x, z)}, a até {@link #REACH}: o anel de passos mais perto que tem rua, e
     * nele a coluna mais perto em linha reta.
     */
    static Optional<Long> nearest(Set<Long> roads, int x, int z) {
        for (int ring = 0; ring <= REACH; ring++) {
            Long best = null;
            int bestSquared = Integer.MAX_VALUE;

            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    long column = ColonyRoads.column(x + dx, z + dz);

                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring && roads.contains(column)
                            && dx * dx + dz * dz < bestSquared) {
                        best = column;
                        bestSquared = dx * dx + dz * dz;
                    }
                }
            }

            if (best != null) {
                return Optional.of(best);
            }
        }

        return Optional.empty();
    }

    /** As colunas da rede de {@code from} a {@code to}, as duas incluídas; vazio quando a rede não as liga. */
    static List<Long> route(Set<Long> roads, long from, long to) {
        Map<Long, Long> parent = new HashMap<>();
        Deque<Long> frontier = new ArrayDeque<>();

        parent.put(from, from);
        frontier.add(from);

        while (!frontier.isEmpty() && parent.size() <= MAX_VISITED) {
            long at = frontier.poll();

            if (at == to) {
                List<Long> route = new ArrayList<>();
                Long step = to;

                while (step != null && step != from) {
                    route.add(step);
                    step = parent.get(step);
                }

                route.add(from);
                Collections.reverse(route);

                return route;
            }

            int x = ColonyRoads.xOf(at);
            int z = ColonyRoads.zOf(at);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long next = ColonyRoads.column(x + dx, z + dz);

                    if ((dx != 0 || dz != 0) && roads.contains(next) && !parent.containsKey(next)) {
                        parent.put(next, at);
                        frontier.add(next);
                    }
                }
            }
        }

        return List.of();
    }

    private static int steps(int x1, int z1, int x2, int z2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(z1 - z2));
    }
}
