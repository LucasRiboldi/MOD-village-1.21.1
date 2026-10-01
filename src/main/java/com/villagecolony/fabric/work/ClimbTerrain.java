package com.villagecolony.fabric.work;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * O que o {@link ClimbOut} pergunta ao terreno: o que ele pode quebrar, por
 * onde ele passa, e o que serve de bloco para pôr debaixo dos pés.
 */
final class ClimbTerrain {

    private static final ItemStack LOOT_TOOL = new ItemStack(Items.IRON_PICKAXE);

    private ClimbTerrain() {
    }

    // Para o lado, do rumo mais direto para a vila ao menos direto: os dois
    // vãos abertos ou que ele pode abrir, e chão debaixo.
    static Optional<Direction> tunnelWay(ServerWorld world, BlockPos feet, BlockPos home) {
        List<Direction> ways = new ArrayList<>(Direction.Type.HORIZONTAL.stream().toList());
        int dx = home.getX() - feet.getX();
        int dz = home.getZ() - feet.getZ();

        ways.sort(Comparator.comparingInt((Direction way) -> -(way.getOffsetX() * dx + way.getOffsetZ() * dz)));

        for (Direction way : ways) {
            BlockPos ahead = feet.offset(way);
            BlockPos floor = ahead.down();
            boolean cells = (isOpen(world, ahead) || mayBreak(world, ahead))
                    && (isOpen(world, ahead.up()) || mayBreak(world, ahead.up()));

            if (cells && !world.getBlockState(floor).getCollisionShape(world, floor).isEmpty()) {
                return Optional.of(way);
            }
        }

        return Optional.empty();
    }

    // Um bloco da parede, na altura dos pés ou da cabeça, que dá bloco para
    // pôr debaixo dos pés.
    static Optional<BlockPos> wallWithBlock(ServerWorld world, BlockPos feet) {
        for (BlockPos level : List.of(feet.up(), feet)) {
            for (Direction way : Direction.Type.HORIZONTAL) {
                BlockPos wall = level.offset(way);

                if (!isOpen(world, wall) && mayBreak(world, wall) && givesABlock(world, wall)) {
                    return Optional.of(wall.toImmutable());
                }
            }
        }

        return Optional.empty();
    }

    static boolean givesABlock(ServerWorld world, BlockPos at) {
        return Block.getDroppedStacks(world.getBlockState(at), world, at, null, null, LOOT_TOOL).stream()
                .anyMatch(drop -> isBuildingBlock(drop.getItem()));
    }

    // Terreno natural que a proteção deixa quebrar, sem lava encostada e sem
    // areia ou cascalho em cima, que cairia na cabeça dele.
    static boolean mayBreak(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        if (!StrandedEscape.mayDig(world, at, state)
                || world.getBlockState(at.up()).getBlock() instanceof FallingBlock) {
            return false;
        }

        for (Direction side : Direction.values()) {
            if (world.getFluidState(at.offset(side)).isIn(FluidTags.LAVA)) {
                return false;
            }
        }

        return true;
    }

    // Sem colisão e sem lava: ar, água, planta.
    static boolean isOpen(ServerWorld world, BlockPos at) {
        FluidState fluid = world.getFluidState(at);

        return world.getBlockState(at).getCollisionShape(world, at).isEmpty() && !fluid.isIn(FluidTags.LAVA);
    }

    static boolean isBuildingBlock(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return false;
        }

        BlockState state = blockItem.getBlock().getDefaultState();

        return !(blockItem.getBlock() instanceof FallingBlock) && !state.hasBlockEntity()
                && state.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
    }
}
