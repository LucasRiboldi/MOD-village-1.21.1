package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.integration.PublicChests;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

/** Ajuda ociosa do pastor, com a mesma regra de criação do cuidado regular (Regra 167). */
final class FlockCare {

    private FlockCare() {
    }

    /** Alimenta um par de ovelhas usando trigo dos baús públicos. */
    static int feed(ServerWorld world, Colony colony, VillagerEntity shepherd) {
        var chests = PublicChests.of(world, colony.id(), shepherd.getBlockPos());
        return ShepherdFlock.feedSheep(world, colony, shepherd, chests) ? ShepherdFlock.PAIR_FOOD : 0;
    }
}
