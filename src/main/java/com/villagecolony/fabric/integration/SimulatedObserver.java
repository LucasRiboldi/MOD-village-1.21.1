package com.villagecolony.fabric.integration;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

/**
 * O jogador de mentira da bancada de simulação por bioma — chave só de teste, decisão do autor de
 * 2026-10-20.
 *
 * <p>A colônia só trabalha com jogador dentro da vila (ADR-003 Emenda 6), e o servidor sem jogador
 * não roda o ciclo. Para medir uma vila de deserto ou de neve num servidor sem tela, o observador
 * fica no centro dos chunks forçados ({@code /forceload}) e conta como um jogador ali: descobre a
 * vila, atende a colônia e mantém o expediente, pelos mesmos caminhos do jogo.
 *
 * <p>Ligado só com {@code -Dvillagecolony.simulate=true} <b>e</b> em ambiente de desenvolvimento:
 * o JAR do jogador nunca o liga, mesmo com a propriedade.
 */
public final class SimulatedObserver {

    public static final String PROPERTY = "villagecolony.simulate";

    private static final boolean ENABLED = Boolean.getBoolean(PROPERTY)
            && FabricLoader.getInstance().isDevelopmentEnvironment();

    private SimulatedObserver() {
    }

    /** Se a bancada de simulação está ligada. */
    public static boolean enabled() {
        return ENABLED;
    }

    /** Onde o observador está: o centro dos chunks forçados, na superfície; vazio sem chave ou sem chunk forçado. */
    public static Optional<BlockPos> at(ServerWorld world) {
        if (!ENABLED) {
            return Optional.empty();
        }

        LongSet forced = world.getForcedChunks();

        if (forced.isEmpty()) {
            return Optional.empty();
        }

        long[] column = centerOf(forced.toLongArray());

        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) column[0], (int) column[1]);

        return Optional.of(new BlockPos((int) column[0], y, (int) column[1]));
    }

    /** Se há quem observe o mundo: um jogador, ou o observador simulado. */
    public static boolean anyone(ServerWorld world) {
        return !world.getPlayers().isEmpty() || at(world).isPresent();
    }

    /** Se o observador simulado está a até {@code radius} blocos (no plano) desta coluna. */
    public static boolean isNear(ServerWorld world, int x, int z, long radius) {
        return at(world).filter(at -> {
            long dx = (long) at.getX() - x;
            long dz = (long) at.getZ() - z;

            return dx * dx + dz * dz <= radius * radius;
        }).isPresent();
    }

    /**
     * A coluna central de um conjunto de chunks: a média dos centros deles, em blocos.
     *
     * @return {@code {x, z}}
     */
    static long[] centerOf(long[] chunks) {
        long sumX = 0;
        long sumZ = 0;

        for (long chunk : chunks) {
            sumX += ChunkPos.getPackedX(chunk) * 16L + 8;
            sumZ += ChunkPos.getPackedZ(chunk) * 16L + 8;
        }

        return new long[] {Math.floorDiv(sumX, chunks.length), Math.floorDiv(sumZ, chunks.length)};
    }
}
