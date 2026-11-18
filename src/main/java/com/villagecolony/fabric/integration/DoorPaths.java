package com.villagecolony.fabric.integration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * O caminho na porta da casa do mod — Regra 314 (autor, 2026-11-17, jogo da 0.4.91: a porta da BigHouseMOD tinha grama
 * dos dois lados). Depois de a casa ser posta inteira (BigHouseMOD e casa do mineiro), a coluna de cada lado da porta cujo
 * topo, no piso da porta, é terra ou grama com espaço livre acima ganha o bloco de caminho, rente ao chão. O piso de
 * tábua da casa e o chão mais baixo que o piso ficam como estão: nada sobe acima do chão em volta.
 */
public final class DoorPaths {

    private DoorPaths() {
    }

    /**
     * Calça a frente de cada porta dentro de {@code box}.
     *
     * @return quantos blocos de caminho foram postos
     */
    public static int pave(ServerWorld world, BlockBox box) {
        int paved = 0;

        for (BlockPos at : BlockPos.iterate(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ())) {
            BlockState state = world.getBlockState(at);

            if (!(state.getBlock() instanceof DoorBlock) || state.get(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                continue;
            }

            Direction facing = state.get(DoorBlock.FACING);

            for (Direction side : new Direction[] {facing, facing.getOpposite()}) {
                BlockPos ground = at.offset(side).down();
                BlockState above = world.getBlockState(ground.up());

                if (world.getBlockState(ground).isIn(BlockTags.DIRT) && above.isReplaceable() && above.getFluidState().isEmpty()) {
                    if (!above.isAir()) {
                        world.setBlockState(ground.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    }

                    world.setBlockState(ground, Blocks.DIRT_PATH.getDefaultState(), Block.NOTIFY_ALL);
                    paved++;
                }
            }
        }

        return paved;
    }
}
