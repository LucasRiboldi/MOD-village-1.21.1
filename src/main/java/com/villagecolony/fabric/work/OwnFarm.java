package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.CropPatch;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * O fazendeiro trabalha a própria roça primeiro — Regra 52 (autor,
 * 2026-10-08). Colhe o maduro; sem maduro e com semente no baú, semeia o
 * canteiro vazio. Nada a fazer nela, ajuda nas outras pela varredura da vila.
 */
final class OwnFarm {

    /** Uma roça sem trabalho só é relida depois disto: ela cresce devagar. */
    static final long QUIET_TICKS = 200;

    /** Folga de altura na leitura da roça, para cima e para baixo. */
    private static final int SLOPE = 2;

    record Found(BlockPos at, FarmerWork.Chore chore) {
    }

    private OwnFarm() {
    }

    /** O trabalho na roça deste fazendeiro, se ela tem algum. */
    static Optional<Found> work(ServerWorld world, BlockBox farm, boolean hasSeed) {
        if (!world.isChunkLoaded(farm.getMinX() >> 4, farm.getMinZ() >> 4)
                || !world.isChunkLoaded(farm.getMaxX() >> 4, farm.getMaxZ() >> 4)) {
            return Optional.empty();
        }

        BlockPos empty = null;

        // A roça da vila segue o relevo coluna a coluna, e a caixa da peça
        // pode não cobrir a altura dela num terreno inclinado.
        for (BlockPos at : BlockPos.iterate(farm.getMinX(), farm.getMinY() - SLOPE, farm.getMinZ(),
                farm.getMaxX(), farm.getMaxY() + SLOPE, farm.getMaxZ())) {

            if (CropPatch.isRipe(world.getBlockState(at))) {
                return Optional.of(new Found(at.toImmutable(), FarmerWork.Chore.HARVEST));
            }

            if (hasSeed && empty == null && CropPatch.isEmptyPlot(world, at)) {
                empty = at.toImmutable();
            }
        }

        return Optional.ofNullable(empty).map(plot -> new Found(plot, FarmerWork.Chore.SOW));
    }
}
