package com.villagecolony.fabric.work;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Passagem de animais largos pela porteira nos currais construídos pelo mod. */
final class PenGateClearance {
    private PenGateClearance() {}

    static boolean guideWideAnimal(ServerWorld world, ShepherdPens.Pen pen, AnimalEntity animal, BlockPos target) {
        boolean wide = (pen.large() || pen.theme().isPresent()) && animal.getWidth() > 1.0F;
        if (wide && animal.getBlockPos().isWithinDistance(pen.gate(), 3.5)) {
            openWideForDelivery(world, pen.gate());
            animal.getNavigation().stop();
            animal.getMoveControl().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
            return true;
        }
        return false;
    }

    /** Regra 299: libera os braços da cerca voltados à porteira aberta para o animal largo passar. */
    private static void openWideForDelivery(ServerWorld world, BlockPos gate) {
        FencedIn.holdForDelivery(world, gate);
        BlockState state = world.getBlockState(gate);
        if (!(state.getBlock() instanceof FenceGateBlock) || !state.get(FenceGateBlock.OPEN)) return;

        Direction side = state.get(FenceGateBlock.FACING).rotateYClockwise();
        disconnectArm(world, gate, side);
        disconnectArm(world, gate, side.getOpposite());
    }

    private static void disconnectArm(ServerWorld world, BlockPos gate, Direction side) {
        BlockPos at = gate.offset(side);
        BlockState state = world.getBlockState(at);
        if (!(state.getBlock() instanceof FenceBlock)) return;

        var towardGate = switch (side) {
            case NORTH -> Properties.SOUTH;
            case SOUTH -> Properties.NORTH;
            case EAST -> Properties.WEST;
            case WEST -> Properties.EAST;
            default -> throw new IllegalArgumentException("Horizontal gate side required");
        };
        if (state.get(towardGate)) {
            // A atualização normal da porteira ao fechar reconecta as cercas.
            world.setBlockState(at, state.with(towardGate, false), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
    }
}
