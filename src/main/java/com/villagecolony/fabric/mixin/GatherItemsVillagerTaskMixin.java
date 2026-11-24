package com.villagecolony.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.villagecolony.fabric.brain.WorkHours;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.GatherItemsVillagerTask;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Reavalia o expediente sempre que o Vanilla verifica se a conversa pode continuar (ADR-004). */
@Mixin(GatherItemsVillagerTask.class)
public abstract class GatherItemsVillagerTaskMixin {

    @SuppressWarnings("UnusedMethod")
    @WrapOperation(method = "shouldRun(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/VillagerEntity;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/brain/task/LookTargetUtil;canSee("
                    + "Lnet/minecraft/entity/ai/brain/Brain;Lnet/minecraft/entity/ai/brain/MemoryModuleType;"
                    + "Lnet/minecraft/entity/EntityType;)Z"))
    private boolean villagecolony$checkSocialHours(Brain<?> brain, MemoryModuleType<?> memory, EntityType<?> type,
            Operation<Boolean> original, @Local(argsOnly = true) VillagerEntity villager) {
        boolean available = WorkHours.canTalkToCurrentTarget(villager);
        return original.call(brain, memory, type) && available;
    }
}
