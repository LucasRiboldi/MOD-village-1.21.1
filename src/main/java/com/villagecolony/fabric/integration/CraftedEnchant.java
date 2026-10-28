package com.villagecolony.fabric.integration;

import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * O que se fabrica na vila sai com um encantamento — passivas da Regra 132 (autor, 2026-10-27): ferramenta
 * e arma com a bancada do carpinteiro na vila, armadura com a do fundidor. Um encantamento só, sorteado
 * entre os da mesa de encantamentos que servem à peça. Vale para o que o jogador fabrica dentro da caixa
 * da vila ({@code ItemStackCraftMixin}).
 */
public final class CraftedEnchant {

    /** O nível de mesa de encantamentos usado no sorteio. */
    static final int LEVEL = 20;

    private CraftedEnchant() {
    }

    /** Encanta esta peça recém-fabricada em {@code where}, se a vila tem a bancada do ofício dela. */
    public static boolean onCrafted(ServerWorld world, BlockPos where, ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable() || stack.hasEnchantments()) {
            return false;
        }

        ProfessionType trade = stack.getItem() instanceof ArmorItem ? ProfessionType.SMELTER : ProfessionType.CARPENTER;

        if (BenchPresence.colonyAt(where).filter(colony -> BenchPresence.has(world, colony, trade)).isEmpty()) {
            return false;
        }

        var choices = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT)
                .getEntryList(EnchantmentTags.IN_ENCHANTING_TABLE);

        if (choices.isEmpty()) {
            return false;
        }

        List<EnchantmentLevelEntry> drawn =
                EnchantmentHelper.generateEnchantments(world.getRandom(), stack, LEVEL, choices.get().stream());

        if (drawn.isEmpty()) {
            return false;
        }

        EnchantmentLevelEntry one = drawn.get(world.getRandom().nextInt(drawn.size()));
        stack.addEnchantment(one.enchantment, one.level);

        return true;
    }
}
