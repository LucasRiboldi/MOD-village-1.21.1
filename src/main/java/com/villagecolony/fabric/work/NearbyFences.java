package com.villagecolony.fabric.work;

import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Optional;

/** Buscas locais de cercas para detectar cercados e amarrar animais. */
final class NearbyFences {

    private NearbyFences() {
    }

    /**
     * A que distância de uma cerca, portão ou muro vale a pena medir.
     *
     * <p>Seis, e não dois: com dois, quem fica parado no meio de um curral de
     * 7 × 7 — três blocos de cada cerca — nunca era medido. Seis cobre o
     * meio de um curral de 13 × 13; num maior, quem quer sair anda até a
     * cerca e é visto lá.
     */
    private static final int NEAR_FENCE = 6;

    /** Até onde da cama procurar a cerca. */
    private static final int FENCE_RADIUS = 16;

    static boolean isNearAFence(ServerWorld world, BlockPos feet) {
        for (BlockPos at : BlockPos.iterate(
                feet.add(-NEAR_FENCE, -1, -NEAR_FENCE), feet.add(NEAR_FENCE, 1, NEAR_FENCE))) {

            if (FencedIn.isBarrier(world.getBlockState(at))) {
                return true;
            }
        }

        return false;
    }

    /** A cerca mais perto deste ponto, até {@link #FENCE_RADIUS}. */
    static Optional<BlockPos> nearestFence(ServerWorld world, BlockPos from) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos at : BlockPos.iterate(from.add(-FENCE_RADIUS, -3, -FENCE_RADIUS),
                from.add(FENCE_RADIUS, 3, FENCE_RADIUS))) {
            WorldChunk chunk = world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);

            if (chunk != null && chunk.getBlockState(at).isIn(BlockTags.FENCES)) {
                double distance = at.getSquaredDistance(from);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = at.toImmutable();
                }
            }
        }

        return Optional.ofNullable(best);
    }
}
