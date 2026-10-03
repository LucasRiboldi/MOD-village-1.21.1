package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.Capability;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.WorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Distribuição entre ciclos — F10, 2026-09-30.
 *
 * <p>A tarefa só era entregue no ciclo de 30 s: quem terminava no meio ficava
 * parado até lá. A passagem entre ciclos entrega tarefa ao ocioso, mas não
 * pode avançar o relógio dos descansos, que é a passagem do ciclo.
 */
class WorkAssignmentBetweenCyclesTest {

    private final UUID colony = UUID.randomUUID();
    private WorkerService workers;
    private TaskService tasks;

    @BeforeEach
    void setUp() {
        workers = new WorkerService();
        tasks = new TaskService();
    }

    private Worker lumberjack() {
        Worker worker = workers.register(UUID.randomUUID(), colony);
        worker.assign(ProfessionType.LUMBERJACK);
        return worker;
    }

    private Task woodTask() {
        return tasks.create(colony, TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 8);
    }

    @Test
    void anIdleWorkerGetsATaskBetweenCycles() {
        Worker worker = lumberjack();
        Task task = woodTask();

        int assigned = WorkAssignment.assignWithoutAClock(
                colony, workers, tasks, id -> true, (w, t) -> true);

        assertEquals(1, assigned);
        assertEquals(TaskState.RESERVED, task.state());
        assertEquals(worker.villagerId(), task.executor().orElseThrow());
    }

    @Test
    void passingBetweenCyclesDoesNotShortenARest() {
        Worker worker = lumberjack();
        Task task = woodTask();
        worker.rest(Capability.COLLECT_WOOD);

        for (int pass = 0; pass < Worker.REST_CYCLES * 3; pass++) {
            WorkAssignment.assignWithoutAClock(colony, workers, tasks, id -> true, (w, t) -> true);
        }

        assertTrue(worker.isResting(Capability.COLLECT_WOOD),
                "a distribuição entre ciclos gastou o descanso, que é contado em ciclos");
        assertEquals(TaskState.AVAILABLE, task.state());
    }
}
