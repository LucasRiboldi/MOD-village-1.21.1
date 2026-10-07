package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.work.CraftingWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Sem obra pedindo, a tarefa de adiantamento faz a própria peça e para no alvo
 * (ADR-039 D1). Antes ela caía na conversão de tora em tábua e a peça nunca saía.
 */
public class CraftingAdvanceGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(3, 2, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_stock", tickLimit = 300)
    public void theCarpenterAdvancesStairsAndStopsAtTheGoal(TestContext context) {
        advance(context, ProfessionType.CARPENTER, TaskType.CRAFT_WOOD_MATERIAL,
                Items.OAK_PLANKS, ResourceType.OAK_STAIRS, Items.OAK_STAIRS, 5, 8);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_stock", tickLimit = 300)
    public void theMasonAdvancesStoneBricksAndStopsAtTheGoal(TestContext context) {
        advance(context, ProfessionType.MASON, TaskType.CRAFT_STONE_MATERIAL,
                Items.STONE, ResourceType.STONE_BRICKS, Items.STONE_BRICKS, 5, 8);
    }

    /** A peça vai para o baú da profissão, não para o baú de outro ofício no centro da vila (ADR-038 P3c). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_stock", tickLimit = 300)
    public void thePieceGoesToTheProfessionChest(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos freeAt = new BlockPos(2, 2, 5);
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.setBlockState(freeAt, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        ColonyPos own = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ColonyPos free = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(freeAt));
        ChestDepositor.deposit(world, free, Items.OAK_PLANKS, 64);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), free);
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.CARPENTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), own));
        UUID farmer = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(farmer, colony.id()).assign(ProfessionType.FARMER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(farmer, free));
        ColonyFixture fixture = ColonyFixture.create().owning(colony).owning(villager.getUuid()).owning(farmer);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.CRAFT_WOOD_MATERIAL, TaskPriority.PRODUCTION, ResourceType.OAK_STAIRS, 4);
        task.reserveFor(villager.getUuid());
        CraftingWork.run(world, colony);

        context.runAtTick(280, () -> {
            try {
                int inOwn = ColonyChests.countIn(world, List.of(own), Items.OAK_STAIRS);
                int inFree = ColonyChests.countIn(world, List.of(free), Items.OAK_STAIRS);

                context.assertTrue(inOwn >= 4 && inFree == 0,
                        "a escada devia ir ao baú da profissão: profissão " + inOwn + ", baú do fazendeiro " + inFree);
            } finally {
                fixture.cleanUp();
            }

            context.complete();
        });
    }

    private static void advance(TestContext context, ProfessionType profession, TaskType type,
            Item material, ResourceType piece, Item pieceItem, int amount, int most) {
        ServerWorld world = context.getWorld();
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ChestDepositor.deposit(world, chest, material, 64);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(profession);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        ColonyFixture fixture = ColonyFixture.create().owning(colony).owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(colony.id(), type, TaskPriority.PRODUCTION, piece, amount);
        task.reserveFor(villager.getUuid());
        CraftingWork.run(world, colony);

        context.runAtTick(280, () -> {
            try {
                int made = ColonyChests.countIn(world, List.of(chest), pieceItem);

                context.assertTrue(made >= amount, "a tarefa pedia " + amount + " " + piece + " e saíram " + made);
                context.assertTrue(made <= most, "passou do alvo: " + made + " " + piece);
                context.assertTrue(task.state() == TaskState.COMPLETED,
                        "a tarefa não terminou no alvo: " + task.state());
            } finally {
                fixture.cleanUp();
            }

            context.complete();
        });
    }
}
