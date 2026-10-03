package com.villagecolony.fabric.mixin;

import com.villagecolony.fabric.integration.VanillaProfessionGuard;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Trabalhador da colônia não recebe ofício do Vanilla — ADR-029,
 * 2026-09-30.
 *
 * <p>{@code @ModifyVariable}, permitido pela ADR-004 §4 Regra 1 com
 * justificativa: o Vanilla dá o ofício por vários caminhos (tarefa de ir ao
 * trabalho, cura de zumbi, NBT), e todos passam por {@code setVillagerData}.
 * Nada é cancelado (Regra 2): a chamada segue, só o dado muda. A decisão
 * mora em {@link VanillaProfessionGuard} (Regra 3).
 */
@Mixin(VillagerEntity.class)
public abstract class VillagerDataMixin {

    // Chamado pelo Mixin por injeção de bytecode.
    @SuppressWarnings("UnusedMethod")
    @ModifyVariable(method = "setVillagerData", at = @At("HEAD"), argsOnly = true)
    private VillagerData villagecolony$noVanillaTradeForColonyWorkers(VillagerData data) {
        return VanillaProfessionGuard.filter((VillagerEntity) (Object) this, data);
    }
}
