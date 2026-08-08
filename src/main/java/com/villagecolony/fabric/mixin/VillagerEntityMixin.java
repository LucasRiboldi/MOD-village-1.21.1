package com.villagecolony.fabric.mixin;

import com.villagecolony.fabric.brain.ColonyBrainInitializer;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * O único mixin do mod — ADR-004 §3, mixin 1.
 *
 * <p>{@code @Inject} em {@code TAIL}, sem cancelar nada e sem remover
 * task alguma. Toda a lógica está em
 * {@link ColonyBrainInitializer}: aqui só existe a chamada, como pede a
 * ADR-004 §4, regra 3.
 *
 * <p>Ponto de conflito conhecido com outros mods de aldeão, registrado
 * na ADR-004 §7. Acrescentar ao fim é a forma que convive com eles.
 */
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {

    @Inject(method = "initBrain", at = @At("TAIL"))
    private void villagecolony$installColonyTask(
            Brain<VillagerEntity> brain, CallbackInfo info) {

        ColonyBrainInitializer.install(brain);
    }
}
