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

/** Regra 128: a espera parada e o deslocamento sem resultado soltam a tarefa após cinco minutos. */
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
                IdleWatch.sample(context.getWorld(), worker, WorkTime.State.WAIT);
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
    public void stationaryWatchDoesNotTreatWalkingAsWaiting(TestContext context) {
        UUID worker = UUID.randomUUID();
        Task task = VillageColonyMod.TASKS.create(UUID.randomUUID(), TaskType.SMELT_MATERIAL,
                TaskPriority.PRODUCTION, ResourceType.SMOOTH_STONE, 4);
        task.reserveFor(worker);

        try {
            for (int second = 1; second < IdleWatch.WAIT_LIMIT; second++) {
                IdleWatch.sample(context.getWorld(), worker, WorkTime.State.WALK);
            }
            context.assertFalse(VillageColonyMod.TASKS.assignedTo(worker).isEmpty(),
                    "watch estacionário não deve soltar quem ainda caminha");
        } finally {
            VillageColonyMod.TASKS.remove(task.id());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_watch", tickLimit = 20)
    public void usefulProgressResetsWalkingDeadline(TestContext context) {
        UUID worker = UUID.randomUUID();
        Task task = VillageColonyMod.TASKS.create(UUID.randomUUID(), TaskType.SMELT_MATERIAL,
                TaskPriority.PRODUCTION, ResourceType.SMOOTH_STONE, 4);
        task.reserveFor(worker);

        try {
            for (int second = 1; second < TaskProgressWatch.LIMIT_SECONDS; second++) {
                TaskProgressWatch.sample(context.getWorld(), worker, WorkTime.State.WALK);
            }
            TaskProgressWatch.progressed(task);
            for (int second = 1; second < TaskProgressWatch.LIMIT_SECONDS; second++) {
                TaskProgressWatch.sample(context.getWorld(), worker, WorkTime.State.WALK);
            }
            context.assertFalse(VillageColonyMod.TASKS.assignedTo(worker).isEmpty(),
                    "progresso útil não reiniciou o prazo da tarefa");
            TaskProgressWatch.sample(context.getWorld(), worker, WorkTime.State.WALK);
            context.assertTrue(VillageColonyMod.TASKS.assignedTo(worker).isEmpty(),
                    "cinco minutos sem outro progresso mantiveram a tarefa presa");
        } finally {
            TaskProgressWatch.clearAll();
            VillageColonyMod.TASKS.remove(task.id());
        }

        context.complete();
    }

}
