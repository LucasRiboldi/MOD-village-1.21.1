package com.villagecolony.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.villagecolony.fabric.integration.ColonyPathing;
import net.minecraft.entity.ai.brain.task.MoveToTargetTask;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * O trajeto longo do aldeão da colônia vai pela rede de ruas — {@link ColonyPathing#legToward} (autor, 2026-10-28).
 *
 * <p>Embrulha só a busca de caminho da tarefa que anda até o {@code WALK_TARGET} ({@code hasFinishedPath}), permitida
 * pela ADR-004 §4 Regra 1 com justificativa: é ali, e só ali, que o cérebro escolhe para onde o caminho vai. As outras
 * chamadas de {@code findPathTo} (as que só perguntam se há caminho) ficam intactas. Nada é cancelado (Regra 2): a
 * busca do jogo roda inteira, só com o destino do trecho; a decisão mora no {@code ColonyPathing} (Regra 3).
 */
@Mixin(MoveToTargetTask.class)
public abstract class MoveToTargetTaskMixin {

    // Chamado pelo Mixin por injeção de bytecode.
    @SuppressWarnings("UnusedMethod")
    @WrapOperation(method = "hasFinishedPath", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/pathing/EntityNavigation;findPathTo(Lnet/minecraft/util/math/BlockPos;I)"
                    + "Lnet/minecraft/entity/ai/pathing/Path;"))
    private Path villagecolony$alongTheStreets(EntityNavigation navigation, BlockPos target, int distance,
                                               Operation<Path> original, @Local(argsOnly = true) MobEntity entity) {
        return original.call(navigation, ColonyPathing.legToward(entity, target), distance);
    }
}
