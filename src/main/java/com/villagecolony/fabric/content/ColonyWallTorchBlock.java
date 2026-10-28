package com.villagecolony.fabric.content;

import net.minecraft.block.BlockState;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A tocha da colônia presa na parede — Regra 127: o mesmo par da tocha comum (TORCH e WALL_TORCH com um item
 * só), com a luz, o anel de luz e o bloqueio de monstros de {@link ColonyTorchBlock}.
 */
public final class ColonyWallTorchBlock extends WallTorchBlock {

    public ColonyWallTorchBlock(Settings settings) {
        super(ParticleTypes.FLAME, settings);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        ColonyTorchBlock.ringOnAdded(state, world, pos, oldState);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        ColonyTorchBlock.ringOnReplaced(state, world, pos, newState);
    }
}
