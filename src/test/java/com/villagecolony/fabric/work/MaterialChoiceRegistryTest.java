package com.villagecolony.fabric.work;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * As famílias de equivalentes lidas do registro de verdade — 2026-09-24.
 *
 * <p>Primeiro teste unitário do projeto com o jogo carregado, pelo
 * fabric-loader-junit. Antes dele, perguntar "que itens servem no lugar desta
 * escada?" só se respondia num teste de jogo: a resposta vem das tags e do
 * registro, e o JUnit puro não os tem.
 *
 * <p><b>O limite, medido no primeiro dia:</b> o registro está carregado, as
 * <b>tags não</b>. Elas vêm dos datapacks quando o servidor sobe, e aqui não
 * há servidor. Família de tag (botão, lã, tapete) continua no teste de jogo;
 * família por nome (pedra, vidro, madeira descascada) cabe aqui.
 */
class MaterialChoiceRegistryTest {

    @BeforeAll
    static void bootMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void aGraniteStairTakesCobblestoneStairsRightAfterItself() {
        List<Item> choices = MaterialChoice.forBlock(Blocks.GRANITE_STAIRS);

        assertEquals(Items.GRANITE_STAIRS, choices.get(0), "o preferido saiu da frente");
        assertEquals(Items.COBBLESTONE_STAIRS, choices.get(1));
    }

    @Test
    void aStrippedWoodTakesAnyStrippedWood() {
        List<Item> choices = MaterialChoice.forBlock(Blocks.STRIPPED_OAK_WOOD);

        assertEquals(Items.STRIPPED_OAK_WOOD, choices.get(0));
        assertTrue(choices.contains(Items.STRIPPED_SPRUCE_WOOD),
                "a madeira descascada de abeto não entrou na família: " + choices);
    }

    @Test
    void aWorkstationStillHasOnlyItself() {
        assertEquals(List.of(Items.LECTERN), MaterialChoice.forBlock(Blocks.LECTERN));
    }
}
