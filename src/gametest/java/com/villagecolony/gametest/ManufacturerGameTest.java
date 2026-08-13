package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.work.ManufacturerWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O fabricante trabalhando — Fase 9.
 *
 * <p>A primeira profissão que consome. Estes testes olham tanto o que ela
 * produz quanto o que ela **não** faz com o que estava no baú.
 */
public class ManufacturerGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);
    private static final BlockPos STAND = new BlockPos(3, 2, 2);

    /** Colônia, fabricante, baú e tarefa reservada. */
    private static Fixture setUp(TestContext context, int logs) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        if (logs > 0) {
            ChestDepositor.deposit(world, chest, Items.OAK_LOG, logs);
        }

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST)));

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MANUFACTURER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.CRAFT_MATERIAL,
                TaskPriority.PRODUCTION,
                ResourceType.OAK_PLANKS,
                16);

        task.reserveFor(villager.getUuid());

        ManufacturerWork.run(world, colony);

        return new Fixture(
                colony,
                task,
                chest,
                ColonyFixture.create().owning(colony).owning(villager.getUuid()));
    }

    private record Fixture(Colony colony, Task task, ColonyPos chest, ColonyFixture owned) {
    }

    private static int planksIn(TestContext context, ColonyPos chest) {
        return ChestInventoryReader
                .read(context.getWorld(), MinecraftTypeAdapter.toBlockPos(chest))
                .amountOfGroup(ResourceGroup.PLANKS);
    }

    private static int logsIn(TestContext context, ColonyPos chest) {
        return ChestInventoryReader
                .read(context.getWorld(), MinecraftTypeAdapter.toBlockPos(chest))
                .amountOfGroup(ResourceGroup.WOOD);
    }

    /** Tronco vira tábua, e a tábua volta para o mesmo baú. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_planks",
            tickLimit = 300)
    public void logsBecomePlanksInTheChest(TestContext context) {
        Fixture fixture = setUp(context, 4);

        context.runAtTick(90, () -> {
            int planks = planksIn(context, fixture.chest);
            int logs = logsIn(context, fixture.chest);

            context.assertTrue(planks > 0, "o baú não recebeu tábua nenhuma");

            context.assertTrue(
                    logs < 4,
                    "nenhum tronco foi consumido: ainda são " + logs);

            fixture.owned.cleanUp();

            context.complete();
        });
    }

    /**
     * Nada sai do baú antes de a peça ficar pronta.
     *
     * <p>É a regra que o lenhador não tem e esta profissão precisa: o
     * tronco é retirado, transformado e devolvido no mesmo tick. Durante
     * a espera existe um contador, e não um tronco na mão de um aldeão
     * que pode morrer — ou de um servidor que vai ser desligado. Ver o E3
     * do §17 para o que acontece quando algo sai do mundo antes de ter
     * para onde ir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_atomic",
            tickLimit = 300)
    public void nothingLeavesTheChestBeforeThePieceIsDone(TestContext context) {
        Fixture fixture = setUp(context, 4);

        context.runAtTick(8, () -> {
            context.assertTrue(
                    logsIn(context, fixture.chest) == 4,
                    "o tronco saiu do baú antes de virar tábua");

            context.assertTrue(
                    planksIn(context, fixture.chest) == 0,
                    "apareceu tábua antes do tempo da peça");
        });

        context.runAtTick(90, () -> {
            context.assertTrue(
                    planksIn(context, fixture.chest) > 0,
                    "e no fim nenhuma tábua apareceu — o teste não provou nada");

            fixture.owned.cleanUp();

            context.complete();
        });
    }

    /** Sem tronco no baú, a tarefa se encerra em vez de girar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_no_logs",
            tickLimit = 300)
    public void withoutLogsTheTaskEnds(TestContext context) {
        Fixture fixture = setUp(context, 0);

        context.runAtTick(60, () -> {
            context.assertTrue(
                    fixture.task.state() == TaskState.COMPLETED,
                    "a tarefa devia ter se encerrado, está em " + fixture.task.state());

            fixture.owned.cleanUp();

            context.complete();
        });
    }

}
