package com.villagecolony.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.villagecolony.client.ClientOverlayState;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * O nome do trabalhador vai dentro do painel da profissão, e a plaquinha
 * Vanilla sobre a cabeça some — pedido do autor, 2026-10-08.
 *
 * <p>Só no cliente com o mod. O servidor não escreve mais a profissão no
 * nome do aldeão desde a P0.8 ({@code WorkerNameplate} só limpa nomes antigos),
 * então a plaquinha que resta é a de nome dado pelo jogador. A decisão mora em
 * {@link ClientOverlayState#drawsNameOf} (ADR-004 §4, Regra 3).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class WorkerNameplateMixin {

    // Chamado pelo Mixin por injeção de bytecode.
    @SuppressWarnings("UnusedMethod")
    @ModifyReturnValue(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("RETURN"))
    private boolean villagecolony$nameInsideTheOverlay(boolean original, LivingEntity entity) {
        return original && !ClientOverlayState.drawsNameOf(entity.getUuid());
    }
}
