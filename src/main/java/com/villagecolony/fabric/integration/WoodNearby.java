package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Se há árvore em pé ao alcance da colônia — a prova de madeira da rota de suprimento
 * ({@link WorldEvidence}), 2026-10-20.
 *
 * <p>O tronco à mostra no alto da coluna, fora do que é da vila gerada ou da colônia: é o que o
 * lenhador derruba. Só chunk carregado, e a resposta vale {@link SandNearWater#RECHECK_TICKS}.
 */
public final class WoodNearby {

    static {
        ServerMemory.register(WoodNearby.class, WoodNearby::clearAll);
    }

    private record Answer(boolean found, long at) {
    }

    private static final Map<UUID, Answer> ANSWERS = new HashMap<>();

    private WoodNearby() {
    }

    public static boolean around(ServerWorld world, UUID colonyId, BlockPos center, int radius) {
        Answer answer = ANSWERS.get(colonyId);

        if (answer != null && world.getTime() - answer.at() < SandNearWater.RECHECK_TICKS) {
            return answer.found();
        }

        boolean found = within(world, center, radius);
        ANSWERS.put(colonyId, new Answer(found, world.getTime()));

        return found;
    }

    /** A mesma pergunta, sem guardar: para a bateria. */
    public static boolean within(ServerWorld world, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;

                if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
                    continue;
                }

                BlockPos top = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                BlockState state = world.getBlockState(top);

                if (state.isIn(BlockTags.LOGS) && LotGrowth.isClearable(world, top, state)
                        && !BlockProtection.isColonyBuilt(top)) {
                    return true;
                }
            }
        }

        return false;
    }

    static void clearAll() {
        ANSWERS.clear();
    }
}
