package com.villagecolony.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.villagecolony.fabric.brain.WorkHours;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.task.MeetVillagerTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Predicate;

/** Aplica o mesmo expediente à escolha de um parceiro perto do sino (ADR-004). */
@Mixin(MeetVillagerTask.class)
public abstract class MeetVillagerTaskMixin {

    @SuppressWarnings("UnusedMethod")
    @WrapOperation(method = "method_47116", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/brain/LivingTargetCache;findFirst(Ljava/util/function/Predicate;)Ljava/util/Optional;"))
    private static Optional<LivingEntity> villagecolony$selectAvailableNeighbor(LivingTargetCache visible,
            Predicate<LivingEntity> partner, Operation<Optional<LivingEntity>> original,
            @Local(argsOnly = true) LivingEntity actor) {
        return original.call(visible, partner.and(other -> WorkHours.canSocialize(actor) && WorkHours.canSocialize(other)));
    }
}
