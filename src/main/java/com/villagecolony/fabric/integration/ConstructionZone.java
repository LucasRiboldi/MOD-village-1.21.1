package com.villagecolony.fabric.integration;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * A faixa de construção da vila — regra do autor, 2026-10-11: o lote novo nasce
 * ao lado da rua, e muda, flor alta ou arbusto crescendo ali tiram o lote da
 * escolha ({@link LotGround#isNothing} não os aceita). Nessa faixa ninguém planta,
 * e o construtor sem tarefa limpa o que nasceu.
 */
public final class ConstructionZone {

    /** Até quantos blocos da rua vai a faixa — a mesma folga que o bosque guarda da rua. */
    public static final int BAND = 6;

    private ConstructionZone() {
    }

    /** Se este chão fica a menos de {@link #BAND} de um caminho de terra. */
    public static boolean nearRoad(ServerWorld world, BlockPos ground) {
        for (int dx = -BAND + 1; dx < BAND; dx++) {
            for (int dz = -BAND + 1; dz < BAND; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos at = ground.add(dx, dy, dz);

                    if (world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4) != null
                            && world.getBlockState(at).isOf(Blocks.DIRT_PATH)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Planta que derruba o lote: muda, flor alta, arbusto de baga. Grama e flor pequena o lote aceita. */
    public static boolean isWeed(BlockState state) {
        return state.isIn(BlockTags.SAPLINGS)
                || state.isIn(BlockTags.TALL_FLOWERS)
                || state.isOf(Blocks.SWEET_BERRY_BUSH);
    }
}
