package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.BedPlan;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A caminhada real, em passos, pelo mapa a pé da vila ({@link ReachMap}) — R-148 do estudo v2, usada pela
 * cama mais perto do trabalho (Regra 74). Numa vila em duas alturas a linha reta curta pode ser uma volta
 * longa; aqui conta o caminho.
 *
 * <p>Onde o mapa não alcança o trabalho (a mina, debaixo da terra) vale a linha reta, para todas as camas
 * daquele trabalho, e a medida continua comparável entre elas. A cama a que não se chega a pé fica
 * infinitamente longe.
 */
final class Walks {

    /** Até onde do trabalho se procura a célula do mapa de onde a caminhada parte. */
    static final int WORK_AROUND = 3;

    /** Até onde da cama vale chegar: o mesmo alcance com que o mapa diz que a cama se alcança. */
    static final int BED_AROUND = 2;

    private final Set<BlockPos> cells;

    private final Map<BlockPos, Map<BlockPos, Integer>> fromStart = new HashMap<>();

    Walks(Set<BlockPos> cells) {
        this.cells = cells;
    }

    /** Os passos do trabalho até a cama; a linha reta sem mapa; infinito sem caminho até a cama. */
    double between(ColonyPos work, ColonyPos bed) {
        Optional<BlockPos> start = cells.isEmpty() ? Optional.empty()
                : nearestCell(MinecraftTypeAdapter.toBlockPos(work), WORK_AROUND);

        if (start.isEmpty()) {
            return BedPlan.distance(work, bed);
        }

        Map<BlockPos, Integer> steps = fromStart.computeIfAbsent(start.get(), this::walkFrom);
        BlockPos target = MinecraftTypeAdapter.toBlockPos(bed);
        int best = Integer.MAX_VALUE;

        for (int dx = -BED_AROUND; dx <= BED_AROUND; dx++) {
            for (int dz = -BED_AROUND; dz <= BED_AROUND; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    Integer at = steps.get(target.add(dx, dy, dz));

                    if (at != null && at < best) {
                        best = at;
                    }
                }
            }
        }

        return best == Integer.MAX_VALUE ? Double.POSITIVE_INFINITY : best;
    }

    /** A célula do mapa mais perto de {@code place}, a até {@code around} blocos. */
    Optional<BlockPos> nearestCell(BlockPos place, int around) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -around; dx <= around; dx++) {
            for (int dz = -around; dz <= around; dz++) {
                for (int dy = -around; dy <= around; dy++) {
                    BlockPos at = place.add(dx, dy, dz);
                    double distance = at.getSquaredDistance(place);

                    if (cells.contains(at) && distance < bestDistance) {
                        best = at;
                        bestDistance = distance;
                    }
                }
            }
        }

        return Optional.ofNullable(best);
    }

    /** Busca em largura pelo mapa, com o mesmo passo dele: um bloco de lado, subindo ou descendo até um. */
    private Map<BlockPos, Integer> walkFrom(BlockPos start) {
        Map<BlockPos, Integer> steps = new HashMap<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();

        steps.put(start, 0);
        frontier.add(start);

        while (!frontier.isEmpty()) {
            BlockPos at = frontier.poll();
            int next = steps.get(at) + 1;

            for (Direction way : Direction.Type.HORIZONTAL) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos neighbour = at.offset(way).up(dy);

                    if (cells.contains(neighbour) && !steps.containsKey(neighbour)) {
                        steps.put(neighbour, next);
                        frontier.add(neighbour);
                    }
                }
            }
        }

        return steps;
    }
}
