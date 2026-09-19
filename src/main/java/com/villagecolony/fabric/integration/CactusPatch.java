package com.villagecolony.fabric.integration;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Optional;

/**
 * O cacto que a colônia pode cortar — 2026-09-19.
 *
 * <p><b>Pergunta do autor:</b> <i>"colhe cacto e replanta?"</i>. Não
 * colhia, e a obra pagava: ela pede {@code cactus} e
 * {@code potted_cactus}, e a colônia não tinha nenhum.
 *
 * <p><b>Só o topo, e é isso que replanta.</b> Cacto cresce de baixo para
 * cima, até três de altura. Cortar o <b>bloco mais alto</b> e deixar a
 * base é colheita que se repõe sozinha — o cacto volta a crescer dali, e
 * a colônia colhe de novo daqui a pouco. Arrancar a base mataria a planta
 * inteira e o deserto ficaria sem cacto, que é a Regra 7 do lenhador
 * ("replantar o que se derruba") aplicada a outra planta.
 *
 * <p>Por isso não há um método de replantio: <b>não cortar a base já é o
 * replantio</b>. É a mesma economia da lavoura do fazendeiro, que replanta
 * da própria colheita.
 *
 * <p><b>E o de baixo tem de ser cacto.</b> Um cacto de altura um é a
 * planta inteira, e cortá-lo é arrancá-la; a colônia deixa passar e volta
 * quando ele tiver crescido.
 */
public final class CactusPatch {

    /** A janela de altura em volta do centro, a mesma do {@code SandPatch}. */
    private static final int WINDOW_UP = 4;

    private static final int WINDOW_DOWN = 12;

    private CactusPatch() {
    }

    /**
     * O topo de cacto cortável nesta coluna, se houver.
     *
     * <p>Devolve a posição do bloco mais alto de uma planta com pelo
     * menos dois — nunca a base, nunca uma planta de altura um.
     */
    public static Optional<BlockPos> in(ServerWorld world, BlockPos column, int aroundY) {
        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(column.getX() >> 4, column.getZ() >> 4);

        if (chunk == null) {
            // Chunk descarregado: pedir por ele aqui forçaria carregamento
            // dentro do tique, que já travou este servidor duas vezes.
            return Optional.empty();
        }

        for (int y = aroundY + WINDOW_UP; y >= aroundY - WINDOW_DOWN; y--) {
            BlockPos at = new BlockPos(column.getX(), y, column.getZ());

            if (!chunk.getBlockState(at).isOf(Blocks.CACTUS)) {
                continue;
            }

            if (!chunk.getBlockState(at.up()).isAir()) {
                // Não é o topo — o de cima é, e a descida ainda não
                // chegou nele. Seguir cortaria o meio da planta.
                return Optional.empty();
            }

            if (!chunk.getBlockState(at.down()).isOf(Blocks.CACTUS)) {
                // Planta de altura um: cortá-la é arrancá-la. A colônia
                // volta quando tiver crescido.
                return Optional.empty();
            }

            if (BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)) {
                // Cacto que a vila gerou ou que a colônia plantou — a
                // Regra 3 nas duas pontas, como na areia.
                return Optional.empty();
            }

            return Optional.of(at);
        }

        return Optional.empty();
    }
}
