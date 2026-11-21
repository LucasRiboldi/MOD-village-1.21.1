package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.GatheringReach;
import com.villagecolony.core.coordination.ResourceReach;
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

    /** Margem da coleta de argila (Regra 154). */
    public static final int SEARCH_MARGIN = 2 * ResourceReach.WIDE_MARGIN;

    static {
        ServerMemory.register(ClayNearby.class, ClayNearby::clearAll);
    }

    private record Answer(boolean found, long at) {
    }

    private static final Map<UUID, Answer> ANSWERS = new HashMap<>();

    private ClayNearby() {
    }

    public static boolean around(ServerWorld world, Colony colony) {
        UUID colonyId = colony.id();
        Answer answer = ANSWERS.get(colonyId);

        if (answer != null && world.getTime() - answer.at() < SandNearWater.RECHECK_TICKS) {
            return answer.found();
        }

        int radius = GatheringReach.radius(colony.observedBeds(), SandNearWater.RADIUS);
        VillageBounds bounds = colony.bounds()
                .map(box -> ResourceReach.limit(box, ResourceReach.search(box, colony.center(), 0, SEARCH_MARGIN).radius()))
                .orElseGet(() -> ResourceReach.limit(VillageBounds.block(colony.center()), radius));
        boolean found = within(world, bounds, colony.center().y());
        ANSWERS.put(colonyId, new Answer(found, world.getTime()));

        return found;
    }

    private static boolean within(ServerWorld world, VillageBounds bounds, int y) {
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                if (ClayPatch.in(world, new BlockPos(x, y, z), y).isPresent()) {
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
