package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * O que o caminho do grafo atravessa e o Zelador tira — Regra 331. O fardo de feno solto no chão não é chão nem
 * construção: o grafo passa por ele como se fosse ar, e o Zelador o leva para os baús ao calçar a coluna. O fardo
 * posto pela colônia numa obra fica.
 */
public final class PathObstacles {

    private PathObstacles() {
    }

    /** Se o caminho do grafo atravessa este bloco (Regra 331). */
    public static boolean isCrossable(BlockPos pos, BlockState state) {
        return state.isOf(Blocks.HAY_BLOCK) && !BlockProtection.isColonyBuilt(pos);
    }

    /**
     * Tira o que o caminho atravessa na coluna, de {@code floorY + 1} até {@code floorY + height}, e guarda nos baús;
     * o que não couber fica no chão.
     *
     * @return quantos blocos saíram
     */
    public static int clearAbove(ServerWorld world, int x, int z, int floorY, int height, List<ColonyPos> chests) {
        int cleared = 0;

        for (int up = height; up >= 1; up--) {
            BlockPos at = new BlockPos(x, floorY + up, z);
            BlockState state = world.getBlockState(at);

            if (!isCrossable(at, state)) {
                continue;
            }

            for (ItemStack drop : Block.getDroppedStacks(state, world, at, world.getBlockEntity(at))) {
                int left = drop.getCount();

                for (ColonyPos chest : chests) {
                    left = ChestDepositor.deposit(world, chest, drop.getItem(), left);

                    if (left == 0) {
                        break;
                    }
                }

                if (left > 0) {
                    world.spawnEntity(new ItemEntity(world, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5,
                            drop.copyWithCount(left)));
                }
            }

            world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            cleared++;
        }

        return cleared;
    }
}
