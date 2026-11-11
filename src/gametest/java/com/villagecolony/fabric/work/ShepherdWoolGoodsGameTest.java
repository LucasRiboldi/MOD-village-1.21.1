package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A lã guardada vira tapete e lã tingida, de cor sorteada, e nunca desce de 16 — Regra 240. */
public class ShepherdWoolGoodsGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    private static int wool(TestContext context, List<ColonyPos> chests) {
        int total = 0;

        for (DyeColor color : DyeColor.values()) {
            total += ColonyChests.countIn(context.getWorld(), chests, ShepherdWoolGoods.item(color, "wool"));
        }

        return total;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "wool_goods", tickLimit = 20)
    public void storedWoolBecomesColouredGoodsAndSixteenStay(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        List<ColonyPos> chests = List.of(chest);

        ChestDepositor.deposit(context.getWorld(), chest, Items.WHITE_WOOL, 16);

        context.assertTrue(ShepherdWoolGoods.step(context.getWorld(), chests, Random.create(1)).isEmpty(),
                "Regra 240: com 16 lãs o pastor gastou a reserva");

        ChestDepositor.deposit(context.getWorld(), chest, Items.WHITE_WOOL, 16);
        ChestDepositor.deposit(context.getWorld(), chest, Items.RED_DYE, 4);
        ChestDepositor.deposit(context.getWorld(), chest, Items.BLUE_DYE, 4);

        Random random = Random.create(7);
        Set<net.minecraft.item.Item> products = new HashSet<>();

        for (int lot = 0; lot < 64; lot++) {
            ShepherdWoolGoods.step(context.getWorld(), chests, random).ifPresent(made -> products.add(made.product()));
        }

        context.assertTrue(wool(context, chests) >= ShepherdWoolGoods.KEEP, "Regra 240: a lã desceu abaixo de 16: " + wool(context, chests));
        context.assertTrue(products.contains(Items.WHITE_CARPET), "Regra 240: a lã branca não virou tapete: " + products);
        context.assertTrue(products.contains(Items.RED_WOOL) && products.contains(Items.BLUE_WOOL),
                "Regra 240: o corante da vila não deu lã de cor: " + products);

        context.setBlockState(CHEST, Blocks.AIR.getDefaultState());
        context.complete();
    }
}
