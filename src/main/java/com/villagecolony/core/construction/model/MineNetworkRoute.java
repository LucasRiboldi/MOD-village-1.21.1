package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A rota pelos nós prontos de um nível em rede — ADR-042 D5: anda-se só pelo que está
 * pronto (piso garantido, aberturas vedadas), de nó em nó, pelos corredores que ligam
 * pai e filho. Busca em largura no grafo; o nível tem dezenas de nós.
 */
final class MineNetworkRoute {

    private MineNetworkRoute() {
    }

    /** Os centros dos nós, do pronto mais perto de {@code from} até o pai de {@code to}. */
    static List<ColonyPos> route(MineNetwork network, ColonyPos from, MineNetwork.Node to) {
        long start = nearestDone(network, from);
        long goal = MineNetwork.key(to.parentGx(), to.parentGz());

        if (start == Long.MIN_VALUE || !isDone(network, goal)) {
            return List.of();
        }

        Map<Long, Long> cameFrom = new HashMap<>();
        Deque<Long> queue = new ArrayDeque<>();
        cameFrom.put(start, start);
        queue.add(start);

        while (!queue.isEmpty()) {
            long at = queue.poll();

            if (at == goal) {
                break;
            }

            int gx = MineNetwork.gxOf(at);
            int gz = MineNetwork.gzOf(at);

            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                long next = MineNetwork.key(gx + d[0], gz + d[1]);

                if (isDone(network, next) && !cameFrom.containsKey(next) && linked(network, at, next)) {
                    cameFrom.put(next, at);
                    queue.add(next);
                }
            }
        }

        if (!cameFrom.containsKey(goal)) {
            return List.of();
        }

        List<ColonyPos> route = new ArrayList<>();
        long at = goal;

        while (true) {
            route.add(network.centerOf(MineNetwork.gxOf(at), MineNetwork.gzOf(at)));

            if (at == start) {
                break;
            }

            at = Objects.requireNonNull(cameFrom.get(at), "route");
        }

        Collections.reverse(route);

        return route;
    }

    /** Dois nós vizinhos estão ligados se um é pai do outro: o corredor entre eles existe. */
    private static boolean linked(MineNetwork network, long a, long b) {
        MineNetwork.Entry one = network.nodes.get(a);
        MineNetwork.Entry other = network.nodes.get(b);

        return one != null && other != null
                && ((one.node.parentGx() == other.node.gx() && one.node.parentGz() == other.node.gz())
                        || (other.node.parentGx() == one.node.gx() && other.node.parentGz() == one.node.gz()));
    }

    private static long nearestDone(MineNetwork network, ColonyPos from) {
        long best = Long.MIN_VALUE;
        long bestDistance = Long.MAX_VALUE;

        for (Map.Entry<Long, MineNetwork.Entry> entry : network.nodes.entrySet()) {
            if (entry.getValue().state != MineNetwork.State.DONE) {
                continue;
            }

            ColonyPos center = network.centerOf(entry.getValue().node.gx(), entry.getValue().node.gz());
            long dx = (long) center.x() - from.x();
            long dz = (long) center.z() - from.z();
            long distance = dx * dx + dz * dz;

            if (distance < bestDistance) {
                best = entry.getKey();
                bestDistance = distance;
            }
        }

        return best;
    }

    private static boolean isDone(MineNetwork network, long key) {
        MineNetwork.Entry entry = network.nodes.get(key);

        return entry != null && entry.state == MineNetwork.State.DONE;
    }
}
