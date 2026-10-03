package com.villagecolony.fabric.event;

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
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Quem fica livre recebe tarefa sem esperar o ciclo — F10, 2026-09-30.
 *
 * <p>A tarefa só era distribuída no ciclo de 30 s. Na sessão das 02:45, 518
 * tarefas somadas continuaram abertas ao fim das rodadas.
 */
public final class IdleHandsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_hands")
    public void anIdleWorkerIsHandedAnOpenTaskBetweenCycles(TestContext context) {
        BlockPos chestAt = new BlockPos(2, 2, 2);
        context.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chestAt));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        UUID villager = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(villager, colony.id()).assign(ProfessionType.LUMBERJACK);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager, chest));
        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 8);

        try {
            int handed = IdleHands.assignNow(context.getWorld(), colony);

            context.assertTrue(handed == 1 && task.state() == TaskState.RESERVED,
                    "o lenhador livre não recebeu a tarefa aberta entre ciclos: " + handed
                            + ", " + task.state());
        } finally {
            task.cancel();
            VillageColonyMod.STORAGES.remove(villager);
            VillageColonyMod.WORKERS.remove(villager);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }

    /**
     * A tarefa que ele soltou não volta a ele no segundo seguinte — E49,
     * 2026-09-30. No playtest, seis fundidores sem areia pegaram e soltaram a
     * tarefa de vidro 6.324 vezes em 18 minutos.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_hands")
    public void aReleasedTaskDoesNotGoBackToTheSameWorkerBetweenCycles(TestContext context) {
        BlockPos chestAt = new BlockPos(2, 2, 2);
        context.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chestAt));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        UUID villager = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(villager, colony.id()).assign(ProfessionType.LUMBERJACK);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager, chest));
        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 8);
        UUID second = null;

        try {
            int first = IdleHands.assignNow(context.getWorld(), colony);
            task.release();

            int again = IdleHands.assignNow(context.getWorld(), colony);

            context.assertTrue(first == 1 && again == 0 && task.state() == TaskState.AVAILABLE,
                    "a tarefa solta voltou ao mesmo lenhador entre ciclos: " + first + ", "
                            + again + ", " + task.state());

            UUID other = UUID.randomUUID();
            second = other;
            VillageColonyMod.WORKERS.register(other, colony.id()).assign(ProfessionType.LUMBERJACK);
            VillageColonyMod.STORAGES.register(WorkerStorage.of(other, chest));

            int toTheOther = IdleHands.assignNow(context.getWorld(), colony);

            context.assertTrue(toTheOther == 1
                            && task.executor().filter(other::equals).isPresent(),
                    "a tarefa solta não foi na hora a outro lenhador livre: " + toTheOther
                            + ", " + task.executor());
        } finally {
            task.cancel();
            IdleHands.clearAll();
            if (second != null) {
                VillageColonyMod.STORAGES.remove(second);
                VillageColonyMod.WORKERS.remove(second);
            }
            VillageColonyMod.STORAGES.remove(villager);
            VillageColonyMod.WORKERS.remove(villager);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
