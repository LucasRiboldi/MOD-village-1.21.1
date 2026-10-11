package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** O compostor do fazendeiro — P8b, 2026-10-11. */
public class FarmerCompostGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    /** 64 sementes: 32 vão ao compostor (a reserva fica), e os níveis dão 1 farinha de osso. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_sowing", tickLimit = 20)
    public void spareSeedsBecomeBoneMealAndTheReserveStays(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ChestDepositor.deposit(context.getWorld(), chest, Items.WHEAT_SEEDS, 64);
        List<ColonyPos> chests = List.of(chest);
        UUID colony = UUID.randomUUID();

        try {
            int composted = 0;

            while (FarmerCompost.compostASeed(context.getWorld(), colony, chests, chest)) {
                composted++;
            }

            int seeds = ColonyChests.countIn(context.getWorld(), chests, Items.WHEAT_SEEDS);
            int boneMeal = ColonyChests.countIn(context.getWorld(), chests, Items.BONE_MEAL);

            context.assertTrue(composted == 32 && seeds == FarmerCompost.SEED_RESERVE,
                    "devia compostar só acima da reserva: " + composted + " compostadas, " + seeds + " restantes");
            context.assertTrue(boneMeal == 1,
                    "32 sementes a 30% dão 9,6 níveis: uma farinha de osso, e não " + boneMeal);
        } finally {
            FarmerCompost.clearAll();
            context.setBlockState(CHEST, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_sowing", tickLimit = 20)
    public void boneMealGrowsTheGreenWheat(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ChestDepositor.deposit(context.getWorld(), chest, Items.BONE_MEAL, 1);

        BlockPos wheat = new BlockPos(4, 2, 4);
        context.setBlockState(wheat.down(), Blocks.FARMLAND.getDefaultState());
        context.setBlockState(wheat, Blocks.WHEAT.getDefaultState());

        try {
            boolean used = FarmerCompost.fertilizeACrop(
                    context.getWorld(), context.getAbsolutePos(new BlockPos(3, 2, 3)), List.of(chest));
            int age = ((CropBlock) Blocks.WHEAT).getAge(context.getBlockState(wheat));

            context.assertTrue(used && age > 0, "a farinha de osso não fez o trigo crescer (idade " + age + ")");
            context.assertTrue(ColonyChests.countIn(context.getWorld(), List.of(chest), Items.BONE_MEAL) == 0,
                    "a farinha de osso usada não saiu do baú");
        } finally {
            context.setBlockState(CHEST, Blocks.AIR.getDefaultState());
            context.setBlockState(wheat, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }
}
