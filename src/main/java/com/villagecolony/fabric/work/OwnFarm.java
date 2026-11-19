package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.CropPatch;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/**
 * O fazendeiro trabalha a própria roça primeiro — Regra 52 (autor,
 * 2026-10-08). Colhe o maduro; sem maduro e com semente no baú, semeia o
 * canteiro vazio, arando antes a terra comum entre canteiros (Regra 289). Nada a fazer nela, ajuda nas outras pela varredura da vila.
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

    /** O trabalho na roça deste fazendeiro, se ela tem algum, varrida do canto de menor x e z. */
    static Optional<Found> work(ServerWorld world, BlockBox farm, boolean hasSeed) {
        return work(world, farm, hasSeed, false);
    }

    /**
     * O trabalho na roça: o maduro primeiro, depois o canteiro vazio ou a terra comum entre canteiros (Regra 289).
     * Com {@code fromFar}, vale o último achado da varredura, e não o primeiro: os dois donos da plantação grande
     * começam por cantos opostos e não disputam o mesmo canteiro.
     */
    static Optional<Found> work(ServerWorld world, BlockBox farm, boolean hasSeed, boolean fromFar) {
        if (!world.isChunkLoaded(farm.getMinX() >> 4, farm.getMinZ() >> 4)
                || !world.isChunkLoaded(farm.getMaxX() >> 4, farm.getMaxZ() >> 4)) {
            return Optional.empty();
        }

        BlockPos ripe = null;
        BlockPos empty = null;

        // A roça da vila segue o relevo coluna a coluna, e a caixa da peça
        // pode não cobrir a altura dela num terreno inclinado.
        for (BlockPos at : BlockPos.iterate(farm.getMinX(), farm.getMinY() - SLOPE, farm.getMinZ(),
                farm.getMaxX(), farm.getMaxY() + SLOPE, farm.getMaxZ())) {

            if (CropPatch.isRipe(world.getBlockState(at))) {
                if (!fromFar) {
                    return Optional.of(new Found(at.toImmutable(), FarmerWork.Chore.HARVEST));
                }

                ripe = at.toImmutable();
            }

            if (hasSeed && (fromFar || empty == null)
                    && (CropPatch.isEmptyPlot(world, at) || isTillable(world, at))) {
                empty = at.toImmutable();
            }
        }

        if (ripe != null) {
            return Optional.of(new Found(ripe, FarmerWork.Chore.HARVEST));
        }

        return Optional.ofNullable(empty).map(plot -> new Found(plot, FarmerWork.Chore.SOW));
    }

    /**
     * Regra 289: terra ou grama com ar em cima e canteiro arado ao lado — o canteiro que voltou a ser terra comum,
     * dentro da roça. A terra solta, longe de canteiro, não é roça. Na plantação grande, todo canteiro do molde é roça,
     * mesmo longe de terra arada e com mato em cima (Regra 325).
     */
    static boolean isTillable(ServerWorld world, BlockPos at) {
        if (LargeFarmBeds.isFallowBed(world, at)) {
            return true;
        }

        if (!(world.getBlockState(at).isOf(Blocks.DIRT) || world.getBlockState(at).isOf(Blocks.GRASS_BLOCK))
                || !world.getBlockState(at.up()).isAir()) {
            return false;
        }

        for (Direction side : Direction.Type.HORIZONTAL) {
            if (CropPatch.isFarmland(world.getBlockState(at.offset(side)))) {
                return true;
            }
        }

        return false;
    }

    /** Ara a terra comum da roça antes de semear, tirando o mato de cima; diz se arou. */
    static boolean tillIfDirt(ServerWorld world, BlockPos at) {
        if (!isTillable(world, at)) {
            return false;
        }

        if (!world.getBlockState(at.up()).isAir()) {
            world.breakBlock(at.up(), false);
        }

        world.setBlockState(at, Blocks.FARMLAND.getDefaultState());

        return true;
    }
}
