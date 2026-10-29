package com.villagecolony.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villagecolony.fabric.integration.ColonyPathing;
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
 * O custo dos nós do caminho do aldeão: o chão do curral é caro para quem não é pastor (Regra 122), e para o aldeão da
 * colônia a água é cara e o gramado de dentro da vila custa mais que a rua ({@link ColonyPathing}, autor 2026-10-28).
 *
 * <p>Injeção no retorno, permitida pela ADR-004 §4 Regra 1 com justificativa: o tipo de cada nó do
 * caminho só existe dentro do construtor de caminho do jogo, e é ali que o custo se decide. Nada é
 * cancelado (Regra 2): o cálculo do Vanilla roda inteiro e só o tipo devolvido muda. A decisão mora em
 * {@link PenFloors} (Regra 3).
 *
 * <p>E a altura de passo que o caminho usa: o aldeão da colônia sobe 2 blocos e passa a cerca (autor, 2026-10-29); o
 * jogo lê {@code getStepHeight} do bicho em {@code getSuccessors} e {@code getStepHeight}, e só esse valor muda — o
 * corpo dele continua com o passo do jogo e pula ({@link ColonyPathing#stepFor}).
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
        PathNodeType costed = ColonyPathing.forWalker(context.getWorld(), mob, x, y, z,
                PenFloors.forWalker(mob, x, y, z, type));

        if (costed != type) {
            returned.setReturnValue(costed);
        }
    }

    // Chamado pelo Mixin por injeção de bytecode.
    @SuppressWarnings("UnusedMethod")
    @WrapOperation(method = {"getSuccessors", "getStepHeight"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/mob/MobEntity;getStepHeight()F"))
    private float villagecolony$colonyVillagersJumpTwo(MobEntity mob, Operation<Float> original) {
        return ColonyPathing.stepFor(mob, original.call(mob));
    }
}
