package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.Supplies;
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
 * Regra 308: a fundição para na quantidade pedida, e a argila que surge (Regra 301) é só para a obra — no jogo da 0.4.90
 * uma tarefa fez 1.000 terracotas com argila surgida, e os baús ficaram com 3.364 terracotas e 1.944 tijolos.
 */
public class ClayLoopGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(3, 2, 3);

    /** Com 32 argilas no baú e pedido de 2 terracotas, saem 2, e a tarefa se conclui. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "clay_loop_amount", tickLimit = 200)
    public void aSmeltTaskEndsAtItsAmount(TestContext context) {
        Setup setup = setUp(context, 32);
        Task task = VillageColonyMod.TASKS.create(
                setup.colony().id(), TaskType.SMELT_MATERIAL, TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.TERRACOTTA, 2);
        task.reserveFor(setup.smelter());
        SmelterWork.run(context.getWorld(), setup.colony());

        context.runAtTick(160, () -> {
            var inChest = ChestInventoryReader.read(context.getWorld(), context.getAbsolutePos(CHEST));

            try {
                context.assertTrue(inChest.amountOf(ResourceType.TERRACOTTA) == 2,
                        "Regra 308: a fundição passou da quantidade pedida: " + inChest.amountOf(ResourceType.TERRACOTTA));
                context.assertTrue(task.state() == TaskState.COMPLETED, "Regra 308: a tarefa não se concluiu: " + task.state());
            } finally {
                cleanUp(context, setup);
            }

            context.complete();
        });
    }

    /** Pedido de reserva, sem argila no baú: a argila não surge. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "clay_loop_reserve", tickLimit = 200)
    public void clayDoesNotAppearForAReserveTask(TestContext context) {
        Setup setup = setUp(context, 0);
        long before = Supplies.sessionCount(setup.colony().id(), SupplyException.CLAY_ON_REQUEST);
        Task task = VillageColonyMod.TASKS.create(
                setup.colony().id(), TaskType.SMELT_MATERIAL, TaskPriority.PRODUCTION, ResourceType.TERRACOTTA, 4);
        task.reserveFor(setup.smelter());
        SmelterWork.run(context.getWorld(), setup.colony());

        context.runAtTick(100, () -> {
            try {
                context.assertTrue(Supplies.sessionCount(setup.colony().id(), SupplyException.CLAY_ON_REQUEST) == before,
                        "Regra 308: a argila surgiu para um pedido de reserva");
            } finally {
                cleanUp(context, setup);
            }

            context.complete();
        });
    }

    private record Setup(Colony colony, ColonyFixture owned, UUID smelter) {
    }

    /** Nada do teste sobra para o seguinte: tarefas, a oficina registrada e o fundidor (o teste do pastor caía). */
    private static void cleanUp(TestContext context, Setup setup) {
        VillageColonyMod.TASKS.ofColony(setup.colony().id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
        VillageColonyMod.BUILDINGS.removeOfColony(setup.colony().id());
        setup.owned().cleanUp();

        if (context.getWorld().getEntity(setup.smelter()) instanceof VillagerEntity villager) {
            villager.discard();
        }
    }

    private static Setup setUp(TestContext context, int clay) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        if (clay > 0) {
            ChestDepositor.deposit(world, chest, Items.CLAY, clay);
        }

        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);
        // Funde onde está (Regra 263): parado, ele não anda até o arena vizinho (o do pastor é de 11 x 11).
        villager.setAiDisabled(true);
        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SMELTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        owned.owning(villager.getUuid());
        VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                ResourceId.vanilla("village/plains/houses/smelter_workshop"),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 1, 1))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(4, 4, 4)))));

        return new Setup(colony, owned, villager.getUuid());
    }
}
