package com.villagecolony.core.construction.model;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Quanto se anda pela rede de caminhos até o chafariz — decisão D5 do autor (2026-10-27): quando o
 * percurso pela rede passa de {@link #SHORTCUT} vezes o direto, a porta ganha um atalho direto.
 *
 * <p>Puro: as colunas da rede vêm empacotadas por {@link ColonyRoads#column}.
 */
public final class NetworkDetour {

    /** A partir de quantas vezes o direto a rede é longa demais. */
    public static final double SHORTCUT = 2.0;

    /** A que distância do chafariz a rede já chegou nele. */
    public static final int AT_THE_HUB = 3;

    /** Quantas colunas a busca percorre, no máximo. */
    static final int MAX_VISITED = 20_000;

    private NetworkDetour() {
    }

    /** Passos pela rede da coluna {@code (x, z)} até o chafariz; vazio quando a rede não chega nele. */
    public static OptionalInt stepsToHub(Set<Long> network, int x, int z, int hubX, int hubZ) {
        long start = ColonyRoads.column(x, z);

        if (!network.contains(start)) {
            return OptionalInt.empty();
        }

        ArrayDeque<long[]> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        queue.add(new long[] {start, 0});
        seen.add(start);

        while (!queue.isEmpty() && seen.size() <= MAX_VISITED) {
            long[] at = queue.poll();
            int cx = ColonyRoads.xOf(at[0]);
            int cz = ColonyRoads.zOf(at[0]);

            if (Math.abs(cx - hubX) <= AT_THE_HUB && Math.abs(cz - hubZ) <= AT_THE_HUB) {
                return OptionalInt.of((int) at[1]);
            }

            for (int[] way : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                long next = ColonyRoads.column(cx + way[0], cz + way[1]);

                if (network.contains(next) && seen.add(next)) {
                    queue.add(new long[] {next, at[1] + 1});
                }
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Se a ligação pela rede é longa demais: os passos até a rede mais os da rede até o chafariz passam
     * de {@link #SHORTCUT} vezes a distância direta, ou a rede não chega ao chafariz.
     */
    public static boolean needsShortcut(int stepsToNetwork, OptionalInt networkToHub, int directDistance) {
        return networkToHub.isEmpty()
                || stepsToNetwork + networkToHub.getAsInt() > SHORTCUT * Math.max(1, directDistance);
    }
}
