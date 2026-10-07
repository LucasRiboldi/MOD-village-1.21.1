package com.villagecolony.fabric.integration;

import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Set;

/**
 * Os ingredientes que aparecem sozinhos no baú — decisão do autor,
 * 2026-09-30: <i>"corantes, linha, pó de osso, todos itens que caem de
 * inimigos e animais, sempre aparecerão automático"</i>.
 *
 * <p>Nenhuma profissão da colônia caça, cria animal de abate ou colhe flor
 * para tingir. Sem esta regra o tear esperava linha, a vidraça colorida
 * esperava corante (o F8) e a cama colorida nunca saía. A regra "a colônia
 * não cria recurso" foi retirada no mesmo dia (ADR-028).
 *
 * <p>Não espera as quatro tentativas da peça sem rota: quando a receita do
 * artesão pede um destes e o baú não tem, a quantidade que falta aparece
 * no baú na hora e o artesão fabrica.
 */
public final class DropIngredients {

    /** O que cai de inimigo ou de animal, e o que só se faz a partir disso. */
    private static final Set<Item> DROPS = Set.of(
            Items.STRING, Items.BONE, Items.BONE_MEAL, Items.LEATHER, Items.FEATHER,
            Items.GUNPOWDER, Items.SPIDER_EYE, Items.SLIME_BALL, Items.ENDER_PEARL,
            Items.ROTTEN_FLESH, Items.INK_SAC, Items.GLOW_INK_SAC, Items.EGG,
            Items.RABBIT_HIDE, Items.RABBIT_FOOT, Items.BLAZE_ROD, Items.GHAST_TEAR,
            Items.PHANTOM_MEMBRANE, Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS,
            Items.SHULKER_SHELL, Items.TURTLE_SCUTE, Items.ARMADILLO_SCUTE,
            Items.HONEYCOMB, Items.MAGMA_CREAM, Items.BREEZE_ROD,
            Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT,
            Items.COD, Items.SALMON);

    private DropIngredients() {
    }

    /** Se este ingrediente aparece sozinho no baú quando falta. */
    public static boolean isAutomatic(Item item) {
        return item instanceof DyeItem || DROPS.contains(item);
    }
}
