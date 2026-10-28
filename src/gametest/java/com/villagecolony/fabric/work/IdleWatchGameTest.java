package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.UUID;

/** Regra 128: cinco minutos esperando com tarefa e ele a larga; a segunda vez em 10 minutos faz surgir. */
public class IdleWatchGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_watch", tickLimit = 20)
    public void fiveMinutesWaitingLetsTheTaskGo(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        UUID worker = UUID.randomUUID();
        Task task = VillageColonyMod.TASKS.create(
                colonyId, TaskType.SMELT_MATERIAL, TaskPriority.PRODUCTION, ResourceType.SMOOTH_STONE, 4);
        task.reserveFor(worker);

        try {
            for (int second = 1; second < IdleWatch.WAIT_LIMIT; second++) {
                IdleWatch.sample(context.getWorld(), worker, second % 7 == 0 ? WorkTime.State.WALK : WorkTime.State.WAIT);
            }
            context.assertFalse(VillageColonyMod.TASKS.assignedTo(worker).isEmpty(),
                    "largou antes dos 5 minutos de espera");

            IdleWatch.sample(context.getWorld(), worker, WorkTime.State.WORK);
            for (int second = 1; second < IdleWatch.WAIT_LIMIT; second++) {
                IdleWatch.sample(context.getWorld(), worker, WorkTime.State.BLOCKED);
            }
            context.assertFalse(VillageColonyMod.TASKS.assignedTo(worker).isEmpty(),
                    "o golpe de trabalho devia zerar a conta");

            IdleWatch.sample(context.getWorld(), worker, WorkTime.State.WAIT);
            context.assertTrue(VillageColonyMod.TASKS.assignedTo(worker).isEmpty(),
                    "5 minutos esperando e a tarefa continua presa nele");
        } finally {
            VillageColonyMod.TASKS.remove(task.id());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_watch", tickLimit = 20)
    public void theSecondLetGoTenMinutesLaterMakesItAppear(TestContext context) {
        Task task = VillageColonyMod.TASKS.create(
                UUID.randomUUID(), TaskType.SMELT_MATERIAL, TaskPriority.PRODUCTION, ResourceType.GLASS, 4);

        try {
            context.assertFalse(IdleWatch.isSecondTime(task, 1_000), "a primeira vez não faz surgir");
            context.assertFalse(IdleWatch.isSecondTime(task, 1_000 + IdleWatch.APPEAR_AFTER - 1),
                    "antes dos 10 minutos não surge");
            context.assertTrue(IdleWatch.isSecondTime(task, 1_000 + IdleWatch.APPEAR_AFTER),
                    "a segunda vez, 10 minutos depois, devia fazer surgir");
            context.assertFalse(IdleWatch.isSecondTime(task, 1_000 + IdleWatch.APPEAR_AFTER + 1),
                    "depois de surgir a conta recomeça");
        } finally {
            VillageColonyMod.TASKS.remove(task.id());
        }

        context.complete();
    }
}
