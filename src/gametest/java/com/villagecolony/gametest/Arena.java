package com.villagecolony.gametest;

import net.minecraft.block.BlockState;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Montagem de terreno que vários GameTests copiavam — ADR-035 §7.
 *
 * <p>Pública para servir também aos testes que moram no pacote da classe
 * testada ({@code fabric.work}, {@code fabric.event}...). Só entra aqui o que
 * já existia repetido e idêntico; helper de um teste só fica no teste.
 */
public final class Arena {

    private Arena() {
    }

    /**
     * Força (ou solta) os chunks que cobrem o retângulo entre os dois cantos
     * relativos. O cenário que atravessa a borda do chunk não pode depender
     * de o vizinho estar carregado.
     */
    public static void forceChunks(TestContext context, BlockPos lowCorner, BlockPos highCorner, boolean force) {
        BlockPos low = context.getAbsolutePos(lowCorner);
        BlockPos high = context.getAbsolutePos(highCorner);

        for (int cx = Math.min(low.getX(), high.getX()) >> 4; cx <= Math.max(low.getX(), high.getX()) >> 4; cx++) {
            for (int cz = Math.min(low.getZ(), high.getZ()) >> 4; cz <= Math.max(low.getZ(), high.getZ()) >> 4;
                    cz++) {
                context.getWorld().setChunkForced(cx, cz, force);
            }
        }
    }

    /** Um piso quadrado de {@code size} × {@code size} a partir de (0, y, 0). */
    public static void floor(TestContext context, int size, int y, BlockState block) {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                context.setBlockState(new BlockPos(x, y, z), block);
            }
        }
    }
}
