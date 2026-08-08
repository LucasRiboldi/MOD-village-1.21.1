package com.villagecolony.core.coordination;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.service.WorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TASK-020 — o ciclo em que a colônia pensa.
 *
 * <p>O loop da ADR-002 nunca tinha sido escrito, e por isso
 * {@code ColonyResources} sabia responder "o que tenho" e
 * {@code ResourceDemand} sabia responder "o que falta" sem que houvesse
 * onde perguntar. É este ciclo que pergunta.
 */
class ColonyCycleTest {

    private static final UUID COLONY = UUID.randomUUID();

    private static final Map<ResourceType, Integer> GOAL =
            Map.of(ResourceType.OAK_LOG, 64);

    private WorkerService workers;

    private TaskService tasks;

    @BeforeEach
    void setUp() {
        workers = new WorkerService();
        tasks = new TaskService();
    }

    private void lumberjack() {
        workers.register(UUID.randomUUID(), COLONY).assign(ProfessionType.LUMBERJACK);
    }

    private static ResourceTally owning(int logs) {
        return ResourceTally.of(Map.of(ResourceType.OAK_LOG, logs));
    }

    @Test
    void aDeficitBecomesATask() {
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        List<Task> created = tasks.ofColony(COLONY);

        assertEquals(1, created.size());
        assertEquals(TaskType.COLLECT_WOOD, created.get(0).type());
        assertEquals(54, created.get(0).amount(), "pede o que falta, não a meta inteira");
    }

    @Test
    void nothingMissingCreatesNoTask() {
        ColonyCycle.run(COLONY, owning(64), GOAL, tasks, workers);

        assertEquals(0, tasks.count());
    }

    @Test
    void theTaskGoesToAWorkerInTheSameCycle() {
        lumberjack();

        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        assertEquals(TaskState.RESERVED, tasks.ofColony(COLONY).get(0).state());
    }

    /**
     * O ciclo roda a cada trinta segundos e não pode empilhar pedidos.
     *
     * <p>Sem isto, uma colônia com falta permanente acumularia uma tarefa
     * por ciclo até a fila crescer sem limite — e o §9 registra que
     * tarefa aberta é o que segura trabalhador.
     */
    @Test
    void aSecondCycleDoesNotDuplicateTheRequest() {
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        assertEquals(1, tasks.count());
    }

    /** Falta maior no ciclo seguinte não cria segunda tarefa do mesmo tipo. */
    @Test
    void aGrowingDeficitStillKeepsOneTask() {
        ColonyCycle.run(COLONY, owning(50), GOAL, tasks, workers);
        ColonyCycle.run(COLONY, owning(2), GOAL, tasks, workers);

        assertEquals(1, tasks.count());
    }

    /** Recursos diferentes em falta produzem tarefas diferentes. */
    @Test
    void eachMissingResourceGetsItsOwnTask() {
        Map<ResourceType, Integer> goal = Map.of(
                ResourceType.OAK_LOG, 64,
                ResourceType.COBBLESTONE, 32);

        ColonyCycle.run(COLONY, ResourceTally.empty(), goal, tasks, workers);

        assertEquals(2, tasks.count());
    }

    @Test
    void aColonyWithoutGoalsAsksForNothing() {
        ColonyCycle.run(COLONY, ResourceTally.empty(), Map.of(), tasks, workers);

        assertEquals(0, tasks.count());
    }

    /**
     * A tarefa some quando a falta acaba.
     *
     * <p>O jogador enche o baú, e o pedido perde o motivo. Deixá-lo na
     * fila mandaria um lenhador buscar madeira que a colônia já tem.
     */
    @Test
    void theTaskIsCancelledWhenTheDeficitIsGone() {
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);
        ColonyCycle.run(COLONY, owning(64), GOAL, tasks, workers);

        assertTrue(tasks.availableFor(COLONY).isEmpty());
    }

    /** Tarefa já em execução não é cancelada no meio. */
    @Test
    void aTaskBeingExecutedSurvivesTheDeficitEnding() {
        lumberjack();

        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        Task task = tasks.ofColony(COLONY).get(0);
        task.start();

        ColonyCycle.run(COLONY, owning(64), GOAL, tasks, workers);

        assertEquals(TaskState.EXECUTING, task.state());
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class,
                () -> ColonyCycle.run(null, ResourceTally.empty(), GOAL, tasks, workers));

        assertThrows(NullPointerException.class,
                () -> ColonyCycle.run(COLONY, null, GOAL, tasks, workers));

        assertThrows(NullPointerException.class,
                () -> ColonyCycle.run(COLONY, ResourceTally.empty(), null, tasks, workers));
    }

    /** Uma colônia não mexe na fila da outra. */
    @Test
    void oneColonyDoesNotTouchAnother() {
        UUID other = UUID.randomUUID();

        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);
        ColonyCycle.run(other, owning(64), GOAL, tasks, workers);

        assertEquals(1, tasks.ofColony(COLONY).size());
        assertEquals(0, tasks.ofColony(other).size());
    }
}
