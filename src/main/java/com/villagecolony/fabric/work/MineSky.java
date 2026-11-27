package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.VillageBounds;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * O céu e o acesso da boca da mina — Regra 359 (autor, 2026-11-25, jogo da 0.4.139: o arco nasceu 22 blocos abaixo do
 * topo do morro, no oco de uma caverna, e o mineiro não chegou nele). A boca vale no chão aberto para o céu, com no
 * máximo {@value #OVERHANG} blocos naturais logo acima, que o Zelador tira ({@link MineStairClearing}), e alcançado a pé
 * da vila com degraus de 1.
 */
final class MineSky {

    /** Quanto acima do centro a boca da borda pode nascer: o topo do morro que a vila ocupa. */
    static final int LOOK_UP = 32;

    /** Quantos blocos naturais por cima da boca ainda se tiram para abrir o céu. */
    static final int OVERHANG = 4;

    /** Até onde acima da boca esses blocos podem estar; mais alto que isso é morro por cima. */
    static final int OVERHANG_TOP = 6;

    /** Até onde se procura teto acima da boca. */
    static final int ROOF_LOOK = 32;

    /** Quantas posições a busca a pé visita, no máximo. */
    static final int MAX_STEPS = 65_536;

    private MineSky() {
    }

    /**
     * Quantos blocos naturais há por cima deste chão, de 3 a {@value #ROOF_LOOK} acima (os 2 primeiros já são o vão da
     * boca). Um bloco natural acima de {@value #OVERHANG_TOP} é morro por cima: devolve {@link Integer#MAX_VALUE}.
     */
    static int overhang(ServerWorld world, BlockPos ground) {
        int count = 0;

        for (int dy = 3; dy <= ROOF_LOOK; dy++) {
            BlockPos at = ground.up(dy);

            if (at.getY() >= world.getTopY()) {
                break;
            }

            if (WorldTerrain.isNaturalGround(world.getBlockState(at))) {
                if (dy > OVERHANG_TOP) {
                    return Integer.MAX_VALUE;
                }

                count++;
            }
        }

        return count;
    }

    /**
     * As posições de pé alcançadas a partir do centro, com degrau de no máximo 1, dentro da caixa mais {@code margin}.
     * Vazio quando o centro não tem onde ficar de pé.
     */
    static Set<Long> reach(ServerWorld world, BlockPos center, VillageBounds box, int margin) {
        Set<Long> seen = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();

        for (int dy : new int[] {0, -1, 1, -2, 2, -3, 3}) {
            BlockPos feet = center.up(dy);

            if (standable(world, feet)) {
                seen.add(feet.asLong());
                open.add(feet);
                break;
            }
        }

        while (!open.isEmpty() && seen.size() < MAX_STEPS) {
            BlockPos feet = open.poll();

            for (Direction side : Direction.Type.HORIZONTAL) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos next = feet.offset(side).up(dy);

                    if (!box.containsColumn(next.getX(), next.getZ(), margin) || seen.contains(next.asLong())
                            || (dy == 1 && !free(world, feet.up(2))) || !standable(world, next)) {
                        continue;
                    }

                    seen.add(next.asLong());
                    open.add(next);
                }
            }
        }

        return seen;
    }

    /** Se dá para ficar de pé aqui: chão firme embaixo, pés e cabeça livres, sem água. */
    static boolean standable(ServerWorld world, BlockPos feet) {
        if (!world.isChunkLoaded(feet.getX() >> 4, feet.getZ() >> 4)) {
            return false;
        }

        BlockPos below = feet.down();

        return !world.getBlockState(below).getCollisionShape(world, below).isEmpty()
                && world.getFluidState(below).isEmpty() && world.getFluidState(feet).isEmpty()
                && free(world, feet) && free(world, feet.up());
    }

    private static boolean free(ServerWorld world, BlockPos at) {
        return world.getBlockState(at).getCollisionShape(world, at).isEmpty();
    }
}
