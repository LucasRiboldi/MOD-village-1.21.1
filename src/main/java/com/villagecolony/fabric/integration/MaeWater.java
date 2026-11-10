package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A água sob a obra pedida pelo jogador — Regra 227 (autor, 2026-11-10): a água interna da vila, um lago ou uma poça cercados
 * dentro da caixa, é aterrada pela obra; mar e rio aberto, que saem da caixa ou passam de {@value #MAX_POND} colunas, recusam o
 * lote. A obra automática segue recusando toda água (ADR-049).
 */
final class MaeWater {

    /** Mais colunas de água que isto, ligadas pela superfície, já é rio ou mar. */
    static final int MAX_POND = 512;

    /** Até quantos blocos abaixo do piso se procura a água do lote. */
    static final int MAX_DEPTH = 8;

    private MaeWater() {
    }

    /**
     * Os blocos de água do lote a aterrar, de baixo para cima; vazio sem água.
     *
     * @throws IllegalStateException {@code open_water} quando a água é de mar ou de rio aberto
     */
    static List<BlockPos> toFill(ServerWorld world, VillageBounds box, ColonyPos min, ColonyPos max, int floorY) {
        List<BlockPos> water = new ArrayList<>();
        Set<BlockPos> surfaces = new HashSet<>();

        for (int x = min.x(); x <= max.x(); x++) {
            for (int z = min.z(); z <= max.z(); z++) {
                BlockPos top = null;

                for (int y = floorY + 1; y >= floorY - MAX_DEPTH; y--) {
                    BlockPos at = new BlockPos(x, y, z);

                    if (isWater(world, at)) {
                        water.add(at);

                        if (top == null) {
                            top = at;
                        }
                    } else if (top != null) {
                        break;
                    }
                }

                if (top != null) {
                    surfaces.add(top);
                }
            }
        }

        for (BlockPos surface : surfaces) {
            if (!isEnclosed(world, box, surface)) {
                throw new IllegalStateException("open_water");
            }
        }

        water.sort((a, b) -> Integer.compare(a.getY(), b.getY()));

        return water;
    }

    /** Aterra com o chão do bioma: a água some do lote, e a base fica de pé. */
    static void fill(ServerWorld world, List<BlockPos> water) {
        for (BlockPos at : water) {
            world.setBlockState(at, VillageBiomes.foundationGroundAt(world, at).getDefaultState());
        }
    }

    /** Se a superfície desta água fica toda dentro da caixa da vila e não passa do tamanho de lago. */
    static boolean isEnclosed(ServerWorld world, VillageBounds box, BlockPos surface) {
        Set<BlockPos> seen = new HashSet<>(Set.of(surface));
        Deque<BlockPos> open = new ArrayDeque<>(List.of(surface));

        while (!open.isEmpty()) {
            BlockPos at = open.poll();

            if (at.getX() < box.minX() || at.getX() > box.maxX() || at.getZ() < box.minZ() || at.getZ() > box.maxZ()
                    || !world.isChunkLoaded(at) || seen.size() > MAX_POND) {
                return false;
            }

            for (Direction side : Direction.Type.HORIZONTAL) {
                BlockPos next = at.offset(side);

                if (!seen.contains(next) && isWater(world, next)) {
                    seen.add(next);
                    open.add(next);
                }
            }
        }

        return true;
    }

    private static boolean isWater(ServerWorld world, BlockPos at) {
        return world.isChunkLoaded(at) && world.getFluidState(at).isIn(FluidTags.WATER);
    }
}
