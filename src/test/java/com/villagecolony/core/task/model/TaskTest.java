package com.villagecolony.core.task.model;

import com.villagecolony.core.type.Capability;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTest {

    private static final UUID COLONY = UUID.randomUUID();

    private static Task collectWood() {
        return Task.create(
                COLONY,
                TaskType.COLLECT_WOOD,
                TaskPriority.PRODUCTION,
                ResourceType.OAK_LOG,
                16);
    }

    /** Criar e atribuir são passos diferentes do loop, §5 e §6. */
    @Test
    void isBornAvailableAndOwnerless() {
        Task task = collectWood();

        assertEquals(TaskState.AVAILABLE, task.state());
        assertTrue(task.executor().isEmpty());
        assertTrue(task.isOpen());
    }

    @Test
    void keepsWhatItWasGiven() {
        Task task = collectWood();

        assertEquals(COLONY, task.colonyId());
        assertEquals(TaskType.COLLECT_WOOD, task.type());
        assertEquals(TaskPriority.PRODUCTION, task.priority());
        assertEquals(ResourceType.OAK_LOG, task.targetResource());
        assertEquals(16, task.amount());
    }

    /** A tarefa pede capacidade, não profissão. */
    @Test
    void declaresTheCapabilityItNeeds() {
        assertEquals(Capability.COLLECT_WOOD, collectWood().requiredCapability());

        assertEquals(Capability.BUILD_STRUCTURE, Task.create(
                COLONY, TaskType.BUILD, TaskPriority.CONSTRUCTION,
                ResourceType.OAK_PLANKS, 8).requiredCapability());
    }

    @Test
    void walksTheHappyPath() {
        Task task = collectWood();
        UUID worker = UUID.randomUUID();

        task.reserveFor(worker);
        assertEquals(TaskState.RESERVED, task.state());
        assertEquals(worker, task.executor().orElseThrow());

        task.start();
        assertEquals(TaskState.EXECUTING, task.state());

        task.complete();
        assertEquals(TaskState.COMPLETED, task.state());
        assertFalse(task.isOpen());
    }

    /**
     * Simulation-Loop.md: uma tarefa tem um executor só. Substituir em
     * silêncio poria dois aldeões a cortar a mesma árvore.
     */
    @Test
    void cannotBeReservedTwice() {
        Task task = collectWood();
        task.reserveFor(UUID.randomUUID());

        assertThrows(IllegalStateException.class,
                () -> task.reserveFor(UUID.randomUUID()));
    }

    @Test
    void cannotSkipSteps() {
        Task task = collectWood();

        assertThrows(IllegalStateException.class, task::start);
        assertThrows(IllegalStateException.class, task::complete);

        task.reserveFor(UUID.randomUUID());
        assertThrows(IllegalStateException.class, task::complete);
    }

    /**
     * O trabalhador morreu no meio: a tarefa continua fazendo sentido,
     * só perdeu quem a faria. Cancelar seria errado.
     */
    @Test
    void releasingReturnsItToTheQueue() {
        Task task = collectWood();
        task.reserveFor(UUID.randomUUID());
        task.start();

        task.release();

        assertEquals(TaskState.AVAILABLE, task.state());
        assertTrue(task.executor().isEmpty());
        assertTrue(task.isOpen());
    }

    @Test
    void onlyAnOwnedTaskCanBeReleased() {
        assertThrows(IllegalStateException.class, () -> collectWood().release());
    }

    /** Deixou de fazer sentido: construção removida, recurso dispensado. */
    @Test
    void cancellingClosesItAndDropsTheExecutor() {
        Task task = collectWood();
        task.reserveFor(UUID.randomUUID());

        task.cancel();

        assertEquals(TaskState.CANCELLED, task.state());
        assertTrue(task.executor().isEmpty());
        assertFalse(task.isOpen());
    }

    @Test
    void anAvailableTaskCanBeCancelled() {
        Task task = collectWood();
        task.cancel();

        assertEquals(TaskState.CANCELLED, task.state());
    }

    /** O trabalho foi feito; desfazer faria a colônia recontar o baú. */
    @Test
    void aCompletedTaskCannotBeCancelled() {
        Task task = collectWood();
        task.reserveFor(UUID.randomUUID());
        task.start();
        task.complete();

        assertThrows(IllegalStateException.class, task::cancel);
    }

    /** Uma tarefa de zero unidades ocuparia um trabalhador para nada. */
    @Test
    void rejectsNonPositiveAmounts() {
        assertThrows(IllegalArgumentException.class, () -> Task.create(
                COLONY, TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION,
                ResourceType.OAK_LOG, 0));

        assertThrows(IllegalArgumentException.class, () -> Task.create(
                COLONY, TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION,
                ResourceType.OAK_LOG, -5));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> Task.create(
                null, TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION,
                ResourceType.OAK_LOG, 1));

        assertThrows(NullPointerException.class, () -> Task.create(
                COLONY, null, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 1));

        assertThrows(NullPointerException.class, () -> Task.create(
                COLONY, TaskType.COLLECT_WOOD, null, ResourceType.OAK_LOG, 1));

        assertThrows(NullPointerException.class, () -> Task.create(
                COLONY, TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, null, 1));

        assertThrows(NullPointerException.class,
                () -> collectWood().reserveFor(null));
    }

    @Test
    void twoTasksAreNeverTheSameByContent() {
        assertNotEquals(collectWood(), collectWood());
    }

    @Test
    void belongsToItsColony() {
        assertTrue(collectWood().belongsTo(COLONY));
        assertFalse(collectWood().belongsTo(UUID.randomUUID()));
    }

    /** Uma colônia com fome não ergue casa. */
    @Test
    void survivalOutranksTheRest() {
        assertTrue(TaskPriority.SURVIVAL.isHigherThan(TaskPriority.PRODUCTION));
        assertTrue(TaskPriority.PRODUCTION.isHigherThan(TaskPriority.CONSTRUCTION));
        assertFalse(TaskPriority.CONSTRUCTION.isHigherThan(TaskPriority.SURVIVAL));
    }
}
