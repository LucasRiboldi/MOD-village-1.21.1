package com.villagecolony.fabric.mixin;

import com.villagecolony.fabric.integration.PenFloors;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathContext;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O chão do curral é caro no caminho do aldeão de ofício que não é pastor — Regra 122.
 *
 * <p>Injeção no retorno, permitida pela ADR-004 §4 Regra 1 com justificativa: o tipo de cada nó do
 * caminho só existe dentro do construtor de caminho do jogo, e é ali que o custo se decide. Nada é
 * cancelado (Regra 2): o cálculo do Vanilla roda inteiro e só o tipo devolvido muda. A decisão mora em
 * {@link PenFloors} (Regra 3).
 */
@Mixin(LandPathNodeMaker.class)
public abstract class LandPathNodeMakerMixin {

    // Chamado pelo Mixin por injeção de bytecode; o contexto vem na assinatura do alvo.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "getNodeType(Lnet/minecraft/entity/ai/pathing/PathContext;IIILnet/minecraft/entity/mob/MobEntity;)"
            + "Lnet/minecraft/entity/ai/pathing/PathNodeType;", at = @At("RETURN"), cancellable = true)
    private void villagecolony$penFloorIsCostly(PathContext context, int x, int y, int z, MobEntity mob,
                                                CallbackInfoReturnable<PathNodeType> returned) {
        PathNodeType type = returned.getReturnValue();
        PathNodeType costed = PenFloors.forWalker(mob, x, y, z, type);

        if (costed != type) {
            returned.setReturnValue(costed);
        }
    }
}
