package com.villagecolony.gametest;

import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.Optional;

/**
 * A receita vem do jogo, não do mod — Fase 9.
 *
 * <p>Estes testes não afirmam "um tronco dá quatro tábuas": afirmam que
 * o mod pergunta e recebe resposta. O número é do Vanilla, e o dia em
 * que ele mudar o mod acompanha sozinho.
 */
public class CraftingLookupGameTest implements FabricGameTest {

    /** Tronco vira tábua, e o jogo é quem diz quantas. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "recipe_log")
    public void aLogBecomesPlanks(TestContext context) {
        Optional<ItemStack> result = CraftingLookup.resultOfOne(
                context.getWorld(), new ItemStack(Items.OAK_LOG));

        context.assertTrue(result.isPresent(), "o jogo não conhece receita para um tronco só");

        ItemStack planks = result.get();

        context.assertTrue(
                planks.isOf(Items.OAK_PLANKS),
                "esperava tábua de carvalho, veio " + planks);

        context.assertTrue(planks.getCount() > 0, "a receita rendeu nada");

        context.complete();
    }

    /**
     * A espécie atravessa a receita.
     *
     * <p>É o que impede o fabricante de transformar bétula em carvalho —
     * e o que faz a colônia precisar contar as oito tábuas, e não só a
     * de carvalho.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "recipe_species")
    public void theSpeciesSurvivesTheRecipe(TestContext context) {
        ItemStack planks = CraftingLookup
                .resultOfOne(context.getWorld(), new ItemStack(Items.BIRCH_LOG))
                .orElseThrow();

        context.assertTrue(
                planks.isOf(Items.BIRCH_PLANKS),
                "esperava tábua de bétula, veio " + planks);

        context.complete();
    }

    /**
     * E o resultado é coisa que a colônia sabe contar.
     *
     * <p>Fabricar o que o estoque não enxerga faria a contagem mentir
     * sem avisar — o trabalho aconteceria e a colônia continuaria se
     * achando pobre.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "recipe_counted")
    public void whatComesOutIsCountedByTheColony(TestContext context) {
        for (ItemStack log : new ItemStack[] {
                new ItemStack(Items.OAK_LOG),
                new ItemStack(Items.SPRUCE_LOG),
                new ItemStack(Items.MANGROVE_LOG)}) {

            ItemStack planks = CraftingLookup.resultOfOne(context.getWorld(), log).orElseThrow();

            Optional<ResourceType> counted =
                    MinecraftTypeAdapter.toResourceType(planks.getItem());

            context.assertTrue(
                    counted.isPresent(),
                    "a colônia não conta " + planks + ", feita de " + log);

            context.assertTrue(
                    counted.get().group() == ResourceGroup.PLANKS,
                    counted.get() + " devia estar no grupo das tábuas");
        }

        context.complete();
    }

    /** O que não tem receita de um item só não vira nada. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "recipe_none")
    public void somethingWithoutARecipeStaysAsItIs(TestContext context) {
        context.assertTrue(
                CraftingLookup.resultOfOne(
                        context.getWorld(), new ItemStack(Items.DIAMOND)).isEmpty(),
                "inventou receita para um diamante sozinho");

        context.complete();
    }
}
