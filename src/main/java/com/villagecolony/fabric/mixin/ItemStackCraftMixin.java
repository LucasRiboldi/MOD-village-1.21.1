package com.villagecolony.fabric.mixin;

import com.villagecolony.fabric.integration.CraftedEnchant;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A peça que o jogador fabrica dentro da vila sai encantada — passivas da Regra 132 (autor, 2026-10-27).
 *
 * <p>Injeção no começo de {@code onCraftByPlayer}, a única porta por onde passa toda peça tirada da grade
 * de fabricar (ADR-004 §4 Regra 1). Nada é cancelado: o Vanilla segue igual, a peça só ganha o
 * encantamento. A decisão mora em {@link CraftedEnchant} (Regra 3).
 */
@Mixin(ItemStack.class)
public abstract class ItemStackCraftMixin {

    // Chamado pelo Mixin por injeção de bytecode.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "onCraftByPlayer", at = @At("HEAD"))
    private void villagecolony$enchantInTheVillage(World world, PlayerEntity player, int amount, CallbackInfo info) {
        if (world instanceof ServerWorld server) {
            CraftedEnchant.onCrafted(server, player.getBlockPos(), (ItemStack) (Object) this);
        }
    }
}
