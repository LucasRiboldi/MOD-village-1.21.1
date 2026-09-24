package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.telemetry.model.ActivityKind;
import com.villagecolony.core.telemetry.model.ActivityProfession;
import com.villagecolony.core.telemetry.model.ActivityState;
import com.villagecolony.core.telemetry.model.ActivityTraceEvent;
import com.villagecolony.core.telemetry.model.ControlledReason;
import com.villagecolony.core.telemetry.model.TargetKind;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Desistir de uma tarefa escreve um evento no traço persistido —
 * decisão 7B, 2026-09-24.
 *
 * <p>É o único ponto de {@code ActivityTrace} integrado nesta entrega:
 * ver o javadoc de {@link ActivityTraceRegistry} para o porquê de
 * {@code IdleLog} (que fala por colônia, sem trabalhador identificável)
 * ficar de fora.
 */
class WorkerStrikesTest {

    @AfterEach
    void clearRegistries() {
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.ACTIVITY_TRACES.clear();
    }

    @Test
    void givingUpWritesAnAbandonedEventWithTheRealWorker() {
        UUID colonyId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        Worker worker = VillageColonyMod.WORKERS.register(workerId, colonyId);
        worker.assign(ProfessionType.MINER);

        Task task = Task.create(
                colonyId, TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE, 1);

        WorkerStrikes.gaveUp(workerId, task);

        List<ActivityTraceEvent> newest =
                VillageColonyMod.ACTIVITY_TRACES.of(colonyId).orElseThrow().newestFirst(1);

        assertEquals(1, newest.size());

        ActivityTraceEvent event = newest.get(0);

        assertEquals(workerId, event.workerId());
        assertEquals(ActivityProfession.MINER, event.profession());
        assertEquals(ActivityKind.MINING, event.activity());
        assertEquals(ActivityState.ABANDONED, event.state());
        assertEquals(ControlledReason.WORK_STALLED, event.reason());
        assertEquals(TargetKind.STONE, event.target());
    }

    /** Um aldeão que já não é trabalhador da colônia não gera evento nenhum. */
    @Test
    void aWorkerThatNoLongerExistsWritesNoEvent() {
        UUID colonyId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        Task task = Task.create(
                colonyId, TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE, 1);

        WorkerStrikes.gaveUp(workerId, task);

        assertTrue(VillageColonyMod.ACTIVITY_TRACES.of(colonyId).isEmpty());
    }
}
