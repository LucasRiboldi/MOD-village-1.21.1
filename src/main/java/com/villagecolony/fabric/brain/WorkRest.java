package com.villagecolony.fabric.brain;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;

/** Libera o movimento de trabalho sem perder a tarefa que será retomada de manhã. */
public final class WorkRest {

    private WorkRest() {
    }

    public static void release(VillagerEntity villager) {
        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
        villager.getBrain().forget(MemoryModuleType.LOOK_TARGET);
        villager.getBrain().forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        villager.getNavigation().stop();
    }
}
