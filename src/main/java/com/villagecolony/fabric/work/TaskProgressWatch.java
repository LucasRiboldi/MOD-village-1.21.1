package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.DemandKey;
import com.villagecolony.core.coordination.DemandRetries;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.Supplies;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Prazo de uma tarefa sem progresso útil. */
public final class TaskProgressWatch {

    static final long LIMIT_SECONDS = 300;

    private record Watch(UUID taskId, long seconds) {
    }

    private static final HashMap<UUID, Watch> STALLED = new HashMap<>();

    private static final DemandRetries RETRIES = new DemandRetries();

    static {
        ServerMemory.register(TaskProgressWatch.class, TaskProgressWatch::clearAll);
    }

    private TaskProgressWatch() {
    }

    /** Se o prazo de progresso útil passou. */
    public static boolean expired(long limit, long lastProgress, long now) {
        return now - lastProgress >= limit;
    }

    /** Amostra de um trabalhador que ainda tem tarefa, uma vez por segundo. */
    static void sample(ServerWorld world, UUID workerId, WorkTime.State state) {
        List<Task> tasks = VillageColonyMod.TASKS.assignedTo(workerId);
        if (tasks.isEmpty()) {
            STALLED.remove(workerId);
            return;
        }

        Task task = tasks.getFirst();
        Watch previous = STALLED.get(workerId);
        long seconds = previous != null && previous.taskId().equals(task.id()) ? previous.seconds() + 1 : 1;
        STALLED.put(workerId, new Watch(task.id(), seconds));
        if (seconds < LIMIT_SECONDS) {
            return;
        }

        int attempts = RETRIES.recordFailure(new DemandKey(task.colonyId(), task.type(), task.targetResource()),
                world.getTimeOfDay() / 24_000L);
        int supplied = attempts < 2 ? 0 : supplyMissing(world, workerId, task);
        int released = WorkerJobs.dropAll(workerId);
        STALLED.remove(workerId);
        VillageColonyMod.LOGGER.warn("VC_TASK_PROGRESS worker={} released={} attempts={} reason=no_useful_progress",
                workerId.toString().substring(0, 8), released, attempts);
        if (supplied > 0) {
            VillageColonyMod.LOGGER.info("VC_TASK_PROGRESS worker={} supplied={} reason=second_failure",
                    workerId.toString().substring(0, 8), supplied);
        }
    }

    /** Progresso útil reinicia somente a tarefa que produziu efeito no mundo. */
    static void progressed(Task task) {
        task.executor().ifPresent(STALLED::remove);
    }

    static void clearAll() {
        STALLED.clear();
        RETRIES.restore(java.util.List.of());
    }

    public static List<DemandRetries.Retry> retries(long day) {
        return RETRIES.snapshot(day);
    }

    public static void restoreRetries(java.util.Collection<DemandRetries.Retry> retries) {
        RETRIES.restore(retries);
    }

    private static int supplyMissing(ServerWorld world, UUID workerId, Task task) {
        if (task.type() == TaskType.BUILD || task.priority() == TaskPriority.CONSTRUCTION_MATERIAL) {
            return 0;
        }
        Optional<Item> item = MinecraftTypeAdapter.toItem(task.targetResource());
        Optional<com.villagecolony.core.type.ColonyPos> chest = VillageColonyMod.STORAGES.of(workerId)
                .map(WorkerStorage::chestPosition);
        if (item.isEmpty() || chest.isEmpty()) {
            return 0;
        }
        int requested = Math.max(1, task.amount());
        int supplied = requested - ChestDepositor.deposit(world, chest.get(), item.get(), requested);
        if (supplied > 0) {
            Supplies.record(SupplyException.WAITED_TOO_LONG, task.colonyId(),
                    Registries.ITEM.getId(item.get()).getPath(), supplied);
        }
        return supplied;
    }
}
