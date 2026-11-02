package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockProtection;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

/** Mantém a fundição dentro da área construída da vila. */
final class SmelterLocation {
    private SmelterLocation() {
    }

    static boolean isInsideBuiltArea(ServerWorld world, UUID workerId) {
        if (!(world.getEntity(workerId) instanceof VillagerEntity villager)) {
            return false;
        }
        if (BlockProtection.isColonyBuilt(villager.getBlockPos())) {
            return true;
        }
        VillageColonyMod.STORAGES.of(workerId)
                .map(WorkerStorage::chestPosition)
                .map(MinecraftTypeAdapter::toBlockPos)
                .ifPresent(chest -> WorkTargets.set(workerId, chest.up(), 2));
        return false;
    }
}
