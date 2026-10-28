package com.villagecolony.fabric.content;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * A tocha da colônia — Regra 127, item 8 das escolhas do autor (2026-10-27): igual à tocha comum, com a
 * chama laranja e luz 15, e um anel de luz invisível em volta ({@link #lightRing}): a área clara fica cerca
 * de três vezes maior. Monstro não nasce sozinho perto dela ({@code ColonyTorches}). Quem a põe é o
 * carpinteiro sem tarefa, ao lado do caminho escuro. Quebrada, o anel de luz sai junto.
 */
public final class ColonyTorchBlock extends TorchBlock {

    /** A distância do anel de luz à tocha. */
    public static final int RING_RADIUS = 10;

    /** Os pontos do anel: os quatro rumos e as quatro diagonais. */
    public static final List<int[]> RING = List.of(
            new int[] {RING_RADIUS, 0}, new int[] {-RING_RADIUS, 0}, new int[] {0, RING_RADIUS},
            new int[] {0, -RING_RADIUS}, new int[] {7, 7}, new int[] {7, -7}, new int[] {-7, 7},
            new int[] {-7, -7});

    /** Quanto acima e abaixo da tocha o anel procura ar. */
    static final int RING_HEIGHT = 4;

    public ColonyTorchBlock(Settings settings) {
        super(ParticleTypes.FLAME, settings);
    }

    /** Põe o anel de luz invisível em volta da tocha em {@code torch}: um bloco de luz 15 no ar, por ponto. */
    public static void lightRing(ServerWorld world, BlockPos torch) {
        BlockState light = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15);

        for (int[] point : RING) {
            for (int dy = RING_HEIGHT; dy >= -RING_HEIGHT; dy--) {
                BlockPos spot = torch.add(point[0], dy, point[1]);

                if (world.isChunkLoaded(spot) && world.getBlockState(spot).isAir()
                        && !world.getBlockState(spot.down()).isAir()) {
                    world.setBlockState(spot, light);
                    break;
                }
            }
        }
    }

    /** Tira o anel de luz da tocha em {@code torch}. */
    public static void clearRing(ServerWorld world, BlockPos torch) {
        for (int[] point : RING) {
            for (int dy = RING_HEIGHT; dy >= -RING_HEIGHT; dy--) {
                BlockPos spot = torch.add(point[0], dy, point[1]);

                if (world.isChunkLoaded(spot) && world.getBlockState(spot).isOf(Blocks.LIGHT)) {
                    world.setBlockState(spot, Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        ringOnAdded(state, world, pos, oldState);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        ringOnReplaced(state, world, pos, newState);
    }

    /** O anel acende quando a tocha (de chão ou de parede) chega, seja quem a pôs: o carpinteiro ou o jogador. */
    static void ringOnAdded(BlockState state, World world, BlockPos pos, BlockState oldState) {
        if (!oldState.isOf(state.getBlock()) && world instanceof ServerWorld server) {
            lightRing(server, pos);
        }
    }

    /** E sai quando ela sai. */
    static void ringOnReplaced(BlockState state, World world, BlockPos pos, BlockState newState) {
        if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld server) {
            clearRing(server, pos);
        }
    }
}
