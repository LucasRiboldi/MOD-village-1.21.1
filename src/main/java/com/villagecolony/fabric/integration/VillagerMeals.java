package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;

/**
 * Quem come ou recebe comida fica com a vida cheia — regra do autor, 2026-10-29: <i>"aldeões quando recebem comida ou
 * comem recuperam suas vidas ao máximo"</i>. Vale para todo aldeão: ao pegar do chão a comida que o jogo dá a ele (pão,
 * batata, cenoura, beterraba — {@code VillagerEntity.ITEM_FOOD_VALUES}, o mesmo que o colega joga para ele), ao comer a
 * comida do inventário ({@code consumeAvailableFood}) e ao comer para cruzar ({@code eatForBreeding}).
 */
public final class VillagerMeals {

    private VillagerMeals() {
    }

    /** O aldeão comeu: a vida vai ao máximo. */
    public static void ate(VillagerEntity villager) {
        if (villager.getHealth() < villager.getMaxHealth()) {
            VillageColonyMod.LOGGER.debug("Villager {} ate and is back to full health ({} -> {})",
                    villager.getUuid().toString().substring(0, 8), villager.getHealth(), villager.getMaxHealth());
            villager.setHealth(villager.getMaxHealth());
        }
    }

    /** O aldeão pega do chão: se for comida, a vida vai ao máximo. */
    public static void picksUp(VillagerEntity villager, ItemEntity item) {
        if (VillagerEntity.ITEM_FOOD_VALUES.containsKey(item.getStack().getItem())) {
            ate(villager);
        }
    }
}
