package com.villagecolony.fabric.work;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Optional;

/**
 * Caminhada longa por pernas na superfície — A7, pedido do autor de 2026-10-08. A
 * navegação Vanilla não traça caminho de 50–130 blocos e para no meio: o lenhador
 * ficou 31% bloqueado indo a árvores longe. Destino além de {@link #LEG} blocos vira
 * um ponto de chão {@link #LEG} adiante, na direção dele, e a perna seguinte sai
 * quando ele chega perto dela.
 *
 * <p>O ponto é arredondado à grade de {@link #GRID} blocos: quem pergunta a cada
 * tique recebe o mesmo destino até andar uma casa da grade, e a navegação não refaz o
 * caminho a cada passo.
 */
public final class WalkLegs {

    /** A partir de quantos blocos na horizontal a caminhada vai por pernas, e o tamanho de cada uma. */
    public static final int LEG = 24;

    /** A grade das pernas. */
    static final int GRID = 8;

    private WalkLegs() {
    }

    /**
     * O ponto de chão da próxima perna — vazio quando o destino está perto, ou quando
     * não há chão de pé carregado ali (então vai direto, como antes).
     */
    public static Optional<BlockPos> towards(ServerWorld world, BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance <= LEG + GRID) {
            return Optional.empty();
        }

        int x = Math.round((float) ((from.getX() + dx / distance * LEG) / GRID)) * GRID;
        int z = Math.round((float) ((from.getZ() + dz / distance * LEG) / GRID)) * GRID;

        for (int radius = 0; radius <= 2; radius++) {
            for (int ox = -radius; ox <= radius; ox++) {
                for (int oz = -radius; oz <= radius; oz++) {
                    if (Math.max(Math.abs(ox), Math.abs(oz)) != radius
                            || world.getChunkManager().getWorldChunk((x + ox) >> 4, (z + oz) >> 4) == null) {
                        continue;
                    }

                    BlockPos ground = new BlockPos(x + ox,
                            world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x + ox, z + oz), z + oz);

                    if (BuilderApproach.standable(world, ground)) {
                        return Optional.of(ground);
                    }
                }
            }
        }

        return Optional.empty();
    }
}
