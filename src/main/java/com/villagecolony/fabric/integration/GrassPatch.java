package com.villagecolony.fabric.integration;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Optional;

/** Colunas de grama natural exposta no setor de coleta da colônia. */
public final class GrassPatch {

    private static final int WINDOW_UP = 4;
    private static final int WINDOW_DOWN = 12;

    private GrassPatch() {
    }

    public static Optional<BlockPos> in(
            ServerWorld world, BlockPos column, int aroundY, BlockPos center, Direction sector) {
        if (!FarthestVillageSector.isInSector(center, column, sector)) {
            return Optional.empty();
        }

        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(column.getX() >> 4, column.getZ() >> 4);

        if (chunk == null) {
            return Optional.empty();
        }

        for (int y = aroundY + WINDOW_UP; y >= aroundY - WINDOW_DOWN; y--) {
            BlockPos at = new BlockPos(column.getX(), y, column.getZ());
            BlockState state = chunk.getBlockState(at);

            if (!state.isOf(Blocks.GRASS_BLOCK)) {
                continue;
            }

            if (!chunk.getBlockState(at.up()).isAir()
                    || !BlockProtection.mayBreak(world, at, state)) {
                return Optional.empty();
            }

            return Optional.of(at);
        }

        return Optional.empty();
    }
}
