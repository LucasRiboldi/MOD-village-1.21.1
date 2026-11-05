package com.villagecolony.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villagecolony.fabric.integration.ColonyPathing;
import com.villagecolony.fabric.integration.PenFloors;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathContext;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O custo dos nós do caminho do aldeão: o chão do curral sai do caminho de quem não é pastor (Regra 122), e para o aldeão da
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
 *
 * <p>E o teto do pulo — Regra 181: o jogo só confere a cabeça de quem pula para nó {@code OPEN} ou {@code WALKABLE}, e o
 * nó fora da rua ({@link ColonyPathing#OFF_STREET}) escapava da conferência: o caminho subia 2 blocos embaixo de um teto.
 */
@Mixin(LandPathNodeMaker.class)
public abstract class LandPathNodeMakerMixin extends PathNodeMaker {

    @Shadow
    @SuppressWarnings("UnusedVariable")
    private boolean checkBoxCollision(Box box) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract double getFeetY(BlockPos pos);

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

    // Chamado pelo Mixin por injeção de bytecode; a mesma conta que o jogo faz para OPEN e WALKABLE.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "getJumpOnTopNode", at = @At("RETURN"), cancellable = true)
    private void villagecolony$headRoomOffStreet(int x, int y, int z, int maxYStep, double prevFeetY, Direction direction,
                                                 PathNodeType nodeType, BlockPos.Mutable mutablePos,
                                                 CallbackInfoReturnable<PathNode> returned) {
        PathNode node = returned.getReturnValue();

        if (node == null || node.type != ColonyPathing.OFF_STREET || this.entity.getWidth() >= 1.0F) {
            return;
        }

        double fromX = x - direction.getOffsetX() + 0.5;
        double fromZ = z - direction.getOffsetZ() + 0.5;
        double half = this.entity.getWidth() / 2.0;
        Box body = new Box(fromX - half, getFeetY(mutablePos.set(fromX, y + 1, fromZ)) + 0.001, fromZ - half,
                fromX + half, this.entity.getHeight() + getFeetY(mutablePos.set((double) node.x, (double) node.y, (double) node.z)) - 0.002,
                fromZ + half);

        if (checkBoxCollision(body)) {
            returned.setReturnValue(null);
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
