package com.villagecolony.core.task.service;

import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.Capability;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskServiceTest {

    private static final UUID COLONY = UUID.randomUUID();
    private static final UUID OTHER_COLONY = UUID.randomUUID();

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService();
    }

    private Task wood(TaskPriority priority) {
        return service.create(
                COLONY, TaskType.COLLECT_WOOD, priority, ResourceType.OAK_LOG, 16);
    }

    @Test
    void createsAndFinds() {
        Task task = wood(TaskPriority.PRODUCTION);

        assertEquals(task, service.find(task.id()).orElseThrow());
        assertEquals(1, service.count());
    }

    @Test
    void unknownTaskIsNotFound() {
        assertTrue(service.find(UUID.randomUUID()).isEmpty());
        assertTrue(service.find(null).isEmpty());
    }

    /** A prioridade é regra da colônia, não de quem chama. */
    @Test
    void availableComesSortedByPriority() {
        Task construction = service.create(
                COLONY, TaskType.BUILD, TaskPriority.CONSTRUCTION,
                ResourceType.OAK_PLANKS, 8);

        Task production = wood(TaskPriority.PRODUCTION);

        Task survival = service.create(
                COLONY, TaskType.COLLECT_WOOD, TaskPriority.SURVIVAL,
                ResourceType.OAK_LOG, 4);

        assertEquals(
                List.of(survival, production, construction),
                service.availableFor(COLONY));
    }

    /** Empate mantém a ordem de criação: quem pediu primeiro é atendido. */
    @Test
    void tiesKeepCreationOrder() {
        Task first = wood(TaskPriority.PRODUCTION);
        Task second = wood(TaskPriority.PRODUCTION);

        assertEquals(List.of(first, second), service.availableFor(COLONY));
    }

    @Test
    void reservedTasksAreNoLongerAvailable() {
        Task task = wood(TaskPriority.PRODUCTION);
        task.reserveFor(UUID.randomUUID());

        assertTrue(service.availableFor(COLONY).isEmpty());
    }

    @Test
    void theOtherColonyQueueIsSeparate() {
        wood(TaskPriority.PRODUCTION);

        assertTrue(service.availableFor(OTHER_COLONY).isEmpty());
        assertTrue(service.ofColony(OTHER_COLONY).isEmpty());
    }

    /** "Há algo que este trabalhador saiba fazer?" */
    @Test
    void findsTheNextTaskForACapability() {
        service.create(
                COLONY, TaskType.BUILD, TaskPriority.CONSTRUCTION,
                ResourceType.OAK_PLANKS, 8);

        Task wood = wood(TaskPriority.PRODUCTION);

        assertEquals(
                wood,
                service.nextFor(COLONY, Capability.COLLECT_WOOD).orElseThrow());
    }

    @Test
    void findsNothingForACapabilityNobodyAskedFor() {
        wood(TaskPriority.PRODUCTION);

        assertTrue(service.nextFor(COLONY, Capability.MAINTAIN_FOOD).isEmpty());
        assertTrue(service.nextFor(COLONY, null).isEmpty());
    }

    /** Entre duas que ele sabe fazer, a mais urgente vem primeiro. */
    @Test
    void theNextTaskRespectsPriority() {
        wood(TaskPriority.PRODUCTION);
        Task urgent = wood(TaskPriority.SURVIVAL);

        assertEquals(
                urgent,
                service.nextFor(COLONY, Capability.COLLECT_WOOD).orElseThrow());
    }

    @Test
    void tracksWhatAWorkerIsDoing() {
        UUID worker = UUID.randomUUID();

        Task mine = wood(TaskPriority.PRODUCTION);
        mine.reserveFor(worker);

        wood(TaskPriority.PRODUCTION);

        assertEquals(List.of(mine), service.assignedTo(worker));
    }

    @Test
    void aClosedTaskIsNoLongerAssigned() {
        UUID worker = UUID.randomUUID();

        Task task = wood(TaskPriority.PRODUCTION);
        task.reserveFor(worker);
        task.start();
        task.complete();

        assertTrue(service.assignedTo(worker).isEmpty());
    }

    /** O trabalhador morreu: a tarefa volta para a fila, não é cancelada. */
    @Test
    void releasingAWorkerReturnsTheTasks() {
        UUID worker = UUID.randomUUID();

        Task reserved = wood(TaskPriority.PRODUCTION);
        reserved.reserveFor(worker);

        Task executing = wood(TaskPriority.PRODUCTION);
        executing.reserveFor(worker);
        executing.start();

        assertEquals(2, service.releaseAllOf(worker));

        assertEquals(TaskState.AVAILABLE, reserved.state());
        assertEquals(TaskState.AVAILABLE, executing.state());
        assertEquals(2, service.availableFor(COLONY).size());
    }

    @Test
    void releasingAWorkerWithNothingChangesNothing() {
        assertEquals(0, service.releaseAllOf(UUID.randomUUID()));
    }

    /** Sem limpeza, uma colônia produtiva acumula tarefas para sempre. */
    @Test
    void purgeDropsOnlyClosedTasks() {
        Task done = wood(TaskPriority.PRODUCTION);
        done.reserveFor(UUID.randomUUID());
        done.start();
        done.complete();

        Task cancelled = wood(TaskPriority.PRODUCTION);
        cancelled.cancel();

        Task open = wood(TaskPriority.PRODUCTION);

        assertEquals(2, service.purgeClosed());
        assertEquals(1, service.count());
        assertEquals(open, service.all().iterator().next());
    }

    @Test
    void removeAndClear() {
        Task task = wood(TaskPriority.PRODUCTION);

        assertTrue(service.remove(task.id()));
        assertFalse(service.remove(task.id()));
        assertFalse(service.remove(null));

        wood(TaskPriority.PRODUCTION);
        service.clear();

        assertEquals(0, service.count());
    }

    @Test
    void allIsReadOnly() {
        wood(TaskPriority.PRODUCTION);

        var all = service.all();

        assertThrows(UnsupportedOperationException.class, () -> all.clear());
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> service.ofColony(null));
        assertThrows(NullPointerException.class, () -> service.assignedTo(null));
    }
}
