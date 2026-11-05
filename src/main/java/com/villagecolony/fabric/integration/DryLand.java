package com.villagecolony.fabric.integration;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Até onde se anda a seco a partir do centro da vila — casa principal não nasce separada da vila por água (autor,
 * 2026-11-04). Uma busca por colunas numa janela: anda de coluna em coluna, com degrau de até {@value #MAX_STEP} e
 * sem água na superfície. Só lê chunk carregado.
 */
final class DryLand {

    /** O degrau que o caminho do aldeão vence ({@code ColonyPathing.STEP}). */
    static final int MAX_STEP = 2;

    /** Até onde, em volta do centro, se procura a primeira coluna seca. */
    static final int START_RADIUS = 16;

    private static final int NOT_DRY = Integer.MIN_VALUE;

    private DryLand() {
    }

    /** A grade da janela, {@code true} onde se chega a seco; sem coluna seca perto do centro, tudo falso. */
    static boolean[] from(ServerWorld world, BlockPos center, int minX, int minZ, int width, int depth) {
        boolean[] reached = new boolean[width * depth];
        int[] tops = new int[width * depth];

        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) {
                tops[dz * width + dx] = dryTop(world, minX + dx, minZ + dz);
            }
        }

        int start = startOf(tops, center, minX, minZ, width, depth);

        if (start < 0) {
            return reached;
        }

        Deque<Integer> open = new ArrayDeque<>();

        reached[start] = true;
        open.add(start);

        while (!open.isEmpty()) {
            int at = open.poll();
            int x = at % width;
            int z = at / width;

            for (int[] way : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = x + way[0];
                int nz = z + way[1];

                if (nx < 0 || nz < 0 || nx >= width || nz >= depth) {
                    continue;
                }

                int next = nz * width + nx;

                if (!reached[next] && tops[next] != NOT_DRY && Math.abs(tops[next] - tops[at]) <= MAX_STEP) {
                    reached[next] = true;
                    open.add(next);
                }
            }
        }

        return reached;
    }

    /** Se alguma coluna do lote, ou logo em volta dele, é alcançada a seco. */
    static boolean touches(boolean[] reached, int width, int offsetX, int offsetZ, int sizeX, int sizeZ) {
        int depth = reached.length / width;

        for (int dz = Math.max(0, offsetZ - 1); dz <= offsetZ + sizeZ && dz < depth; dz++) {
            for (int dx = Math.max(0, offsetX - 1); dx <= offsetX + sizeX && dx < width; dx++) {
                if (reached[dz * width + dx]) {
                    return true;
                }
            }
        }

        return false;
    }

    /** A altura da superfície seca; {@link #NOT_DRY} para água ou chunk descarregado. */
    private static int dryTop(ServerWorld world, int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return NOT_DRY;
        }

        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

        return world.getFluidState(new BlockPos(x, top, z)).isEmpty() ? top : NOT_DRY;
    }

    private static int startOf(int[] tops, BlockPos center, int minX, int minZ, int width, int depth) {
        for (int radius = 0; radius <= START_RADIUS; radius++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int x = center.getX() + dx - minX;
                    int z = center.getZ() + dz - minZ;

                    if ((Math.abs(dx) == radius || Math.abs(dz) == radius) && x >= 0 && z >= 0 && x < width && z < depth
                            && tops[z * width + x] != NOT_DRY) {
                        return z * width + x;
                    }
                }
            }
        }

        return -1;
    }
}
