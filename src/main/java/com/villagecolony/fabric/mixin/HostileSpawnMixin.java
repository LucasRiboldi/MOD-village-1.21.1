package com.villagecolony.fabric.mixin;

import com.villagecolony.fabric.integration.ColonyTorches;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Monstro não nasce sozinho perto da tocha da colônia — Regra 127, item 8 das escolhas do autor (27-10).
 *
 * <p>Injeção no começo, permitida pela ADR-004 §4 Regra 1 com justificativa: o "pode nascer no escuro" é
 * a pergunta que todo monstro comum faz ao nascer, e só existe aqui. Cancelar responde "não" (Regra 2: a
 * resposta, e não o Vanilla, é o que muda, e só quando há tocha da colônia perto). A decisão mora em
 * {@link ColonyTorches} (Regra 3).
 */
@Mixin(HostileEntity.class)
public abstract class HostileSpawnMixin {

    // Chamado pelo Mixin por injeção de bytecode; tipo e sorte vêm na assinatura do alvo.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "canSpawnInDark", at = @At("HEAD"), cancellable = true)
    private static void villagecolony$notNearAColonyTorch(EntityType<? extends HostileEntity> type,
                                                          ServerWorldAccess world, SpawnReason reason, BlockPos pos,
                                                          Random random, CallbackInfoReturnable<Boolean> returned) {
        if (ColonyTorches.suppresses(world, reason, pos)) {
            returned.setReturnValue(false);
        }
    }
}
