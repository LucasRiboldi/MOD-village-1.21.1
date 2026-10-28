package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.work.SmelterWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O cru que também sai da fornalha vem antes: o pedido de pedra lisa, com só pedregulho no baú, faz
 * primeiro a pedra (playtest 0.3.97: o fundidor parou 806 vezes com 626 pedregulhos guardados).
 */
public class SmelterChainGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(3, 2, 3);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_chain", tickLimit = 200)
    public void smoothStoneWithOnlyCobblestoneSmeltsTheStoneFirst(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.COBBLESTONE, 32);

        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SMELTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.SMELT_MATERIAL, TaskPriority.PRODUCTION, ResourceType.SMOOTH_STONE, 4);
        task.reserveFor(villager.getUuid());

        SmelterWork.run(world, colony);

        context.runAtTick(150, () -> {
            var inChest = ChestInventoryReader.read(world, context.getAbsolutePos(CHEST));

            try {
                context.assertTrue(inChest.amountOf(ResourceType.STONE) + inChest.amountOf(ResourceType.SMOOTH_STONE) > 0,
                        "o pedregulho não virou pedra: o pedido de pedra lisa ficou esperando");
                context.assertTrue(inChest.amountOf(ResourceType.COBBLESTONE) < 32,
                        "a pedra apareceu sem gastar pedregulho");
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }
}
