package com.villagecolony.fabric.integration;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Optional;

/**
 * A argila que a colônia pode cavar — 2026-09-19.
 *
 * <p>Primeira pedra da cadeia do vaso, que é decisão do autor:
 * <b>argila → tijolo (fornalha) → vaso (bancada)</b>, três tijolos por
 * vaso. Receita do jogo, e o mod não inventa receita.
 *
 * <p><b>Ao contrário da areia, ela pode estar sob água</b> — e é onde
 * quase sempre está, no fundo do lago. O que se exige é que o aldeão
 * alcance: argila com <b>ar</b> em cima é a que ele pega sem mergulhar,
 * e é a mesma linha que o {@code SandPatch} traça ("aldeão não
 * mergulha").
 *
 * <p><b>O buraco fica</b>, como o da areia e o da pedra: nada no mod
 * repõe argila. É limite conhecido, e está dito aqui para não ser
 * descoberto de novo.
 */
public final class ClayPatch {

    private static final int WINDOW_UP = 4;

    private static final int WINDOW_DOWN = 12;

    private ClayPatch() {
    }

    /** A argila alcançável nesta coluna, se houver. */
    public static Optional<BlockPos> in(ServerWorld world, BlockPos column, int aroundY) {
        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(column.getX() >> 4, column.getZ() >> 4);

        if (chunk == null) {
            return Optional.empty();
        }

        for (int y = aroundY + WINDOW_UP; y >= aroundY - WINDOW_DOWN; y--) {
            BlockPos at = new BlockPos(column.getX(), y, column.getZ());

            if (!chunk.getBlockState(at).isOf(Blocks.CLAY)) {
                continue;
            }

            if (!chunk.getBlockState(at.up()).isAir()) {
                // Enterrada, ou sob água: aldeão não mergulha, e a de
                // cima já foi olhada nesta mesma descida.
                return Optional.empty();
            }

            if (BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)) {
                return Optional.empty();
            }

            return Optional.of(at);
        }

        return Optional.empty();
    }
}
