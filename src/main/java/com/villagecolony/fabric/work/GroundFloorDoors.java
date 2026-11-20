package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.SiteSurroundings;
import com.villagecolony.fabric.integration.VillageRoad;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A porta do térreo — Regra 335: o grafo nunca busca porta do andar de cima. Uma porta é do térreo quando, a até
 * {@value #REACH} blocos à frente dela, descendo por assoalho, degrau e sacada, o primeiro chão de fora (chão natural,
 * caminho ou tábua de rua) fica no máximo {@value #DROP} abaixo do piso da porta. A porta da sacada, do mezanino e do
 * andar de cima, com o chão longe embaixo, não vira entrada.
 */
final class GroundFloorDoors {

    /** Quantas colunas à frente da porta se olham. */
    static final int REACH = 3;

    /** Até quantos blocos abaixo do piso da porta o chão de fora ainda é o do térreo: um degrau e uma folga. */
    static final int DROP = 2;

    /** Até onde se desce à procura do chão de fora. */
    private static final int DEPTH = 8;

    private GroundFloorDoors() {
    }

    /** Se a porta (a metade de baixo em {@code door}) é do térreo, olhando para {@code out}. */
    static boolean isGroundFloor(ServerWorld world, BlockPos door, Direction out) {
        int floor = door.getY() - 1;

        for (int step = 1; step <= REACH; step++) {
            BlockPos column = door.offset(out, step);

            if (!world.isChunkLoaded(column)) {
                continue;
            }

            for (int y = floor + 1; y >= floor - DEPTH; y--) {
                BlockPos at = column.withY(y);
                BlockState state = world.getBlockState(at);

                if (isOutsideFloor(world, at, state)) {
                    if (floor - y <= DROP) {
                        return true;
                    }

                    break;
                }
            }
        }

        return false;
    }

    /** Chão de fora: o natural, o caminho e a tábua de rua; assoalho, degrau e sacada não são. */
    private static boolean isOutsideFloor(ServerWorld world, BlockPos at, BlockState state) {
        return SiteSurroundings.isNaturalGround(state) || VillageRoad.isPathBlock(state)
                || VillageRoad.isStreetPlank(world, at, state);
    }
}
