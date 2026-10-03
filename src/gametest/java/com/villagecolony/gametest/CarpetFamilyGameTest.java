package com.villagecolony.gametest;

import com.villagecolony.fabric.work.MaterialChoice;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * O tapete de outra cor serve — decisão do autor, 2026-09-26: "quando a peça é
 * decorativa e só a própria cor falta, aceitar a primeira da família que já
 * estiver no baú".
 *
 * <p>Aqui e não em teste de unidade: a família vem da tag {@code wool_carpets},
 * e tag só existe com um mundo carregado.
 */
public class CarpetFamilyGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "carpet_family", tickLimit = 20)
    public void aGreenCarpetAcceptsAnyCarpetOfTheFamily(TestContext context) {
        var choices = MaterialChoice.forBlock(Blocks.GREEN_CARPET);

        context.assertTrue(choices.get(0) == Items.GREEN_CARPET,
                "a cor da planta deixou de ser a primeira escolha: " + choices);
        context.assertTrue(choices.contains(Items.WHITE_CARPET),
                "o tapete branco saiu da família do verde: " + choices);
        context.complete();
    }

    /**
     * No livro de receitas real, o tapete verde sai de lã verde — e não de
     * "corante + tapete de outra cor", que foi o que a vila de 26-09 tentou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "carpet_family", tickLimit = 20)
    public void theGreenCarpetIsMadeFromWoolNotByDyeingAnotherCarpet(TestContext context) {
        var bill = com.villagecolony.fabric.integration.CraftingLookup.billFor(
                context.getWorld(), Items.GREEN_CARPET, item -> true);

        context.assertTrue(bill.isPresent(), "o livro não achou receita para o tapete verde");
        context.assertTrue(bill.get().ingredients().containsKey(Items.GREEN_WOOL),
                "a receita escolhida não é a de lã: " + bill.get().ingredients());
        context.assertTrue(bill.get().ingredients().keySet().stream()
                        .noneMatch(item -> item.toString().endsWith("_carpet")),
                "a receita escolhida tinge outro tapete: " + bill.get().ingredients());
        context.complete();
    }
}
