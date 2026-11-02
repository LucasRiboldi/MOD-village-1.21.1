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
    public void anIdleSmelterKeepsAvailableIngotsAt64(TestContext context) {
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
        Inventory chestInventory = (Inventory) world.getBlockEntity(chestAt);
        chestInventory.setStack(0, new ItemStack(Items.RAW_IRON, 64));
        chestInventory.setStack(1, new ItemStack(Items.RAW_IRON, 32));

        try {
            List<ColonyPos> chests = List.of(chest);
            for (int attempt = 0; attempt < 10; attempt++) {
                SmelterAdvance.smeltForIdleSmelters(world, colony);
            }

            int ingots = ColonyChests.countIn(world, chests, Items.IRON_INGOT);
            context.assertTrue(ingots == SmelterAdvance.INGOT_RESERVE,
                    "reserva de lingotes deveria parar em 64, veio " + ingots);
            context.assertTrue(SmelterAdvance.smeltForIdleSmelters(world, colony) == 0,
                    "não deveria fundir além da reserva de lingotes");
        } finally {
            ((Inventory) world.getBlockEntity(chestAt)).clear();
            world.setBlockState(chestAt, Blocks.AIR.getDefaultState());
            owned.cleanUp();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_advance", tickLimit = 20)
    public void anIdleSmelterDoesNotMakeLooseBricksAsReserve(TestContext context) {
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
        ((Inventory) world.getBlockEntity(chestAt)).setStack(0, new ItemStack(Items.CLAY_BALL, 64));

        try {
            context.assertTrue(SmelterAdvance.smeltForIdleSmelters(world, colony) == 0,
                    "reserva ociosa não deve fabricar tijolos soltos");
            context.assertTrue(ColonyChests.countIn(world, List.of(chest), Items.BRICK) == 0,
                    "tijolo solto não deve entrar na reserva");
        } finally {
            ((Inventory) world.getBlockEntity(chestAt)).clear();
            world.setBlockState(chestAt, Blocks.AIR.getDefaultState());
            owned.cleanUp();
        }

        context.complete();
    }
}
