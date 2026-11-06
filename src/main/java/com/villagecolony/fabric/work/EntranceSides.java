package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Onde parece ser a porta de quem não tem porta nem portão — Regra 210: o lado com a maior parte da borda aberta na altura de
 * quem anda; no empate, e quando nenhum lado está aberto, o mais perto do centro da vila. A entrada é o meio da abertura,
 * pelo lado de fora. Nenhum bloco é posto: só o grafo sabe.
 */
final class EntranceSides {

    /** Quantos blocos livres acima do chão fazem uma passagem. */
    private static final int HEADROOM = 2;

    /** Uma coluna da borda e a coluna de fora dela. */
    private record Border(int x, int z, int outX, int outZ) {
    }

    private EntranceSides() {
    }

    static Optional<ColonyPos> of(ServerWorld world, BlockPos min, BlockPos max, BlockPos hub, WorldPathTerrain terrain) {
        Optional<ColonyPos> best = Optional.empty();
        double bestShare = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (List<Border> side : sides(min, max)) {
            List<ColonyPos> reachable = new ArrayList<>();
            List<ColonyPos> open = new ArrayList<>();

            for (Border border : side) {
                OptionalInt ground = terrain.groundY(border.outX(), border.outZ(), min.getY());

                if (ground.isPresent() && !terrain.blocked(border.outX(), border.outZ())) {
                    ColonyPos out = new ColonyPos(border.outX(), ground.getAsInt(), border.outZ());

                    reachable.add(out);

                    if (isOpen(world, border, ground.getAsInt())) {
                        open.add(out);
                    }
                }
            }

            if (!reachable.isEmpty()) {
                double share = (double) open.size() / side.size();
                List<ColonyPos> from = open.isEmpty() ? reachable : open;
                ColonyPos middle = from.get(from.size() / 2);
                int distance = Math.abs(middle.x() - hub.getX()) + Math.abs(middle.z() - hub.getZ());

                if (share > bestShare || (share == bestShare && distance < bestDistance)) {
                    bestShare = share;
                    bestDistance = distance;
                    best = Optional.of(middle);
                }
            }
        }

        return best;
    }

    private static boolean isOpen(ServerWorld world, Border border, int groundY) {
        for (int up = 1; up <= HEADROOM; up++) {
            BlockPos at = new BlockPos(border.x(), groundY + up, border.z());

            if (!world.isChunkLoaded(at) || !world.getBlockState(at).getCollisionShape(world, at).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static List<List<Border>> sides(BlockPos min, BlockPos max) {
        List<Border> north = new ArrayList<>();
        List<Border> south = new ArrayList<>();
        List<Border> west = new ArrayList<>();
        List<Border> east = new ArrayList<>();

        for (int x = min.getX(); x <= max.getX(); x++) {
            north.add(new Border(x, min.getZ(), x, min.getZ() - 1));
            south.add(new Border(x, max.getZ(), x, max.getZ() + 1));
        }

        for (int z = min.getZ(); z <= max.getZ(); z++) {
            west.add(new Border(min.getX(), z, min.getX() - 1, z));
            east.add(new Border(max.getX(), z, max.getX() + 1, z));
        }

        return List.of(north, south, west, east);
    }
}
