package com.villagecolony.core.task.model;

import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Cada passo da tarefa chega ao observador do diário de ações, com quem a tinha. */
class TaskTransitionsTest {

    private final List<String> seen = new ArrayList<>();

    @AfterEach
    void tearDown() {
        TaskTransitions.stopObserving();
    }

    private Task task() {
        return Task.create(UUID.randomUUID(), TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE, 4);
    }

    @Test
    void everyStepOfATaskIsObservedWithItsWorker() {
        UUID worker = UUID.randomUUID();
        TaskTransitions.observe((task, from, to, who) -> seen.add(from + ">" + to + ":" + who.equals(worker)));

        Task done = task();
        done.reserveFor(worker);
        done.start();
        done.complete();

        Task released = task();
        released.reserveFor(worker);
        released.release();

        Task cancelled = task();
        cancelled.reserveFor(worker);
        cancelled.cancel();

        assertEquals(List.of(
                "AVAILABLE>RESERVED:true", "RESERVED>EXECUTING:true", "EXECUTING>COMPLETED:true",
                "AVAILABLE>RESERVED:true", "RESERVED>AVAILABLE:true",
                "AVAILABLE>RESERVED:true", "RESERVED>CANCELLED:true"), seen);
    }

    @Test
    void aTaskNobodyHeldIsNotAnAction() {
        TaskTransitions.observe((task, from, to, who) -> seen.add(to.name()));

        task().cancel();

        assertEquals(List.of(), seen);
    }

    @Test
    void aFailingObserverNeverBreaksTheTask() {
        TaskTransitions.observe((task, from, to, who) -> {
            throw new IllegalStateException("disco cheio");
        });

        Task task = task();
        assertDoesNotThrow(() -> task.reserveFor(UUID.randomUUID()));
        assertEquals(TaskState.RESERVED, task.state());
    }
}
