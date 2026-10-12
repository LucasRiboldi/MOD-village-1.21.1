package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.StockCeiling;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * O que o fundidor sem cru pode fundir para a obra — F2 (30-09) e B-4
 * (02-10). Até 02-10 nenhum teste olhava a lista: o carvão vegetal e o
 * degrau de fornalha entraram sem nada que reprovasse a volta do defeito.
 *
 * <p>Precisa do mundo: as receitas de fornalha moram no gerenciador de
 * receitas do servidor, e é delas que a lista sai.
 */
public final class SmelterFallbackGameTest implements FabricGameTest {

    /** No teto do estoque o fundidor não faz o produto "para as obras" — playtest 0.3.31. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_fallback")
    public void charcoalStopsWhenTheVillageHasCoalUpToTheCeiling(TestContext context) {
        BlockPos local = new BlockPos(1, 2, 1);
        context.setBlockState(local, Blocks.CHEST.getDefaultState());
        List<ColonyPos> chests = List.of(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(local)));
        Inventory chest = (Inventory) context.getWorld().getBlockEntity(context.getAbsolutePos(local));

        try {
            chest.setStack(0, new ItemStack(Items.COAL, 10));
            context.assertFalse(SmelterFallback.stocked(context.getWorld(), chests, Items.CHARCOAL),
                    "com 10 carvões a vila ainda está abaixo do teto");

            chest.setStack(0, new ItemStack(Items.COAL, StockCeiling.CRAFTED));
            context.assertTrue(SmelterFallback.stocked(context.getWorld(), chests, Items.CHARCOAL),
                    "com o carvão no teto, o carvão vegetal ainda seria feito");
        } finally {
            chest.clear();
            context.setBlockState(local, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_fallback")
    public void smoothStoneBringsTheStoneThatGoesIntoIt(TestContext context) {
        List<Item> wanted = wantedFor(context, "smooth_stone");

        context.assertTrue(wanted.contains(Items.SMOOTH_STONE), "a pedra lisa que a obra pede: " + wanted);
        context.assertTrue(wanted.contains(Items.STONE),
                "a pedra, cru da pedra lisa, devia vir logo atrás — sai do pedregulho: " + wanted);
        context.assertTrue(wanted.indexOf(Items.SMOOTH_STONE) < wanted.indexOf(Items.STONE),
                "o que a obra pede vem antes do degrau: " + wanted);
        context.assertFalse(wanted.contains(Items.COBBLESTONE),
                "pedregulho não sai da fornalha, não é produto: " + wanted);
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_fallback")
    public void theTorchAcceptsCharcoalFromTheLog(TestContext context) {
        List<Item> wanted = wantedFor(context, "torch");

        context.assertTrue(wanted.contains(Items.CHARCOAL),
                "a tocha aceita carvão vegetal, que sai da tora sem mineiro: " + wanted);
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_fallback")
    public void stoneIsOneStepAndBringsNothingElse(TestContext context) {
        List<Item> wanted = wantedFor(context, "stone");

        // O degrau só entra quando o cru também sai da fornalha: o
        // pedregulho da pedra é cru comum, minerado, e a lista fica só com ela.
        context.assertTrue(List.of(Items.STONE).equals(wanted), "a pedra sai do pedregulho, e só: " + wanted);
        context.complete();
    }

    private static List<Item> wantedFor(TestContext context, String block) {
        Set<Item> into = new LinkedHashSet<>();

        SmelterFallback.addSmeltedFrom(context.getWorld(), List.of(ResourceId.vanilla(block)), into);

        return new ArrayList<>(into);
    }
}
