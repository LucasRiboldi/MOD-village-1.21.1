package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Se há argila alcançável perto da colônia — a prova de argila da rota de suprimento
 * ({@link WorldEvidence}), 2026-10-20.
 *
 * <p>A mesma sonda da coleta ({@link ClayPatch}: exposta, fora da vila gerada e da obra da colônia,
 * na janela de altura da vila), para a rota não existir só no papel. Só chunk carregado, e a
 * resposta vale {@link SandNearWater#RECHECK_TICKS}.
 */
public final class ClayNearby {

    static {
        ServerMemory.register(ClayNearby.class, ClayNearby::clearAll);
    }

    private record Answer(boolean found, long at) {
    }

    private static final Map<UUID, Answer> ANSWERS = new HashMap<>();

    private ClayNearby() {
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
                if (ClayPatch.in(world, new BlockPos(center.getX() + dx, center.getY(), center.getZ() + dz),
                        center.getY()).isPresent()) {
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
