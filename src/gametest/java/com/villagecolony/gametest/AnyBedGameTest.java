package com.villagecolony.gametest;

import com.villagecolony.fabric.work.MaterialChoice;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;

/**
 * Cama de qualquer cor serve — 2026-09-19, decisão do autor.
 *
 * <p><b>O que isto destrava, medido na sessão de 13:04.</b> A obra parou
 * a <b>12 blocos do fim</b> esperando {@code green_bed} — dezessete vezes
 * —, numa vila de deserto cuja colônia produz lã <b>branca</b>. A planta
 * grava a cor que estiver no arquivo, e esperar por ela é esperar tinta
 * que ninguém fabrica.
 */
public class AnyBedGameTest {

    /** A cama verde da planta aceita a branca da colônia. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "any_bed",
            tickLimit = 100)
    public void theGreenBedAcceptsTheWhiteOne(TestContext context) {
        List<Item> choices = MaterialChoice.forBlock(Blocks.GREEN_BED);

        if (choices.get(0) != Items.GREEN_BED) {
            throw new AssertionError(
                    "a cor da planta deixou de vir primeiro: " + choices.get(0));
        }

        if (!choices.contains(Items.WHITE_BED)) {
            throw new AssertionError(
                    "a cama branca nao serve para a verde — a obra fica esperando tinta"
                            + " que ninguem fabrica. Ofertas: " + choices);
        }

        context.complete();
    }

    /**
     * E a cama não vira porta.
     *
     * <p>A metade que impede a regra de virar "qualquer coisa serve":
     * uma família por peça, como já valia para a madeira.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "any_bed",
            tickLimit = 100)
    public void theBedIsNotADoor(TestContext context) {
        List<Item> choices = MaterialChoice.forBlock(Blocks.GREEN_BED);

        if (choices.contains(Items.OAK_DOOR)) {
            throw new AssertionError("a cama aceitou uma porta: " + choices);
        }

        context.complete();
    }
}
