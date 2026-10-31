package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** O fundidor ocioso adianta o cru que sobra no baú — regra do autor, 2026-10-12. */
public class SmelterAdvanceGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_advance_catalog", tickLimit = 20)
    public void anIdleSmelterStocksTerracottaAndReplenishesIt(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos chestAt = context.getAbsolutePos(new BlockPos(1, 2, 1));
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(chestAt);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID smelter = UUID.randomUUID();
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(smelter, colony.id()).assign(ProfessionType.SMELTER);
        owned.owning(smelter);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(smelter, chest));
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ((Inventory) world.getBlockEntity(chestAt)).setStack(0, new ItemStack(Items.CLAY, 48));
        try {
            for (int i = 0; i < 2; i++) {
                SmelterAdvance.smeltForIdleSmelters(world, colony);
            }
            context.assertTrue(ColonyChests.countIn(world, List.of(chest), Items.TERRACOTTA) == 16,
                    "o fundidor não estocou 16 terracotas");
            ColonyChests.withdraw(world, List.of(chest), Items.TERRACOTTA, 5);
            SmelterAdvance.smeltForIdleSmelters(world, colony);
            context.assertTrue(ColonyChests.countIn(world, List.of(chest), Items.TERRACOTTA) == 16,
                    "o fundidor não repôs o consumo");
        } finally {
            ((Inventory) world.getBlockEntity(chestAt)).clear();
            world.setBlockState(chestAt, Blocks.AIR.getDefaultState());
            owned.cleanUp();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_advance_catalog", tickLimit = 20)
    public void anIdleSmelterStocksBlocksOutsideTheOldRawList(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos chestAt = context.getAbsolutePos(new BlockPos(1, 2, 1));
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(chestAt);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID smelter = UUID.randomUUID();
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(smelter, colony.id()).assign(ProfessionType.SMELTER);
        owned.owning(smelter);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(smelter, chest));
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ((Inventory) world.getBlockEntity(chestAt)).setStack(0, new ItemStack(Items.WHITE_TERRACOTTA, 40));
        try {
            context.assertTrue(SmelterAdvance.smeltForIdleSmelters(world, colony) == 8,
                    "o fundidor ignorou a receita de cerâmica vidrada");
            context.assertTrue(ColonyChests.countIn(world, List.of(chest), Items.WHITE_GLAZED_TERRACOTTA) == 8,
                    "a cerâmica vidrada não entrou no baú");
        } finally {
            ((Inventory) world.getBlockEntity(chestAt)).clear();
            world.setBlockState(chestAt, Blocks.AIR.getDefaultState());
            owned.cleanUp();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_advance", tickLimit = 20)
    public void anIdleSmelterTurnsSpareSandIntoGlass(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos chestAt = context.getAbsolutePos(new BlockPos(1, 2, 1));
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(chestAt);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID smelter = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(smelter, colony.id()).assign(ProfessionType.SMELTER);
        owned.owning(smelter);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(smelter, chest));
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ((Inventory) world.getBlockEntity(chestAt)).setStack(0, new ItemStack(Items.SAND, 64));

        try {
            int smelted = SmelterAdvance.smeltForIdleSmelters(world, colony);
            List<ColonyPos> chests = List.of(chest);

            context.assertTrue(smelted == SmelterAdvance.PER_SMELTER, "fundiu " + smelted + " em vez de uma fornada");
            context.assertTrue(ColonyChests.countIn(world, chests, Items.GLASS) == SmelterAdvance.PER_SMELTER,
                    "o vidro não entrou no baú");
            context.assertTrue(ColonyChests.countIn(world, chests, Items.SAND) == 64 - SmelterAdvance.PER_SMELTER,
                    "a areia não saiu do baú");
        } finally {
            ((Inventory) world.getBlockEntity(chestAt)).clear();
            world.setBlockState(chestAt, Blocks.AIR.getDefaultState());
            owned.cleanUp();
        }

        context.complete();
    }
}
