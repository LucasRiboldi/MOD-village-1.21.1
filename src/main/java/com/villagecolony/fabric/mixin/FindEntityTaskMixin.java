package com.villagecolony.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villagecolony.fabric.brain.WorkHours;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.FindEntityTask;
import net.minecraft.entity.ai.brain.task.Task;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

/** Acrescenta o expediente aos critérios de busca de um parceiro social (ADR-004). */
@Mixin(FindEntityTask.class)
public abstract class FindEntityTaskMixin {

    @SuppressWarnings("UnusedMethod")
    @WrapOperation(method = "create(Lnet/minecraft/entity/EntityType;ILnet/minecraft/entity/ai/brain/MemoryModuleType;FI)"
            + "Lnet/minecraft/entity/ai/brain/task/Task;", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/brain/task/FindEntityTask;create(Lnet/minecraft/entity/EntityType;"
                    + "ILjava/util/function/Predicate;Ljava/util/function/Predicate;"
                    + "Lnet/minecraft/entity/ai/brain/MemoryModuleType;FI)Lnet/minecraft/entity/ai/brain/task/Task;"))
    private static Task<LivingEntity> villagecolony$checkSocialHours(EntityType<?> type, int range,
            Predicate<LivingEntity> actor, Predicate<LivingEntity> partner, MemoryModuleType<?> memory,
            float speed, int distance, Operation<Task<LivingEntity>> original) {
        if (type == EntityType.VILLAGER && memory == MemoryModuleType.INTERACTION_TARGET) {
            return original.call(type, range, actor.and(WorkHours::canSocialize), partner.and(WorkHours::canSocialize),
                    memory, speed, distance);
        }
        return original.call(type, range, actor, partner, memory, speed, distance);
    }
}
