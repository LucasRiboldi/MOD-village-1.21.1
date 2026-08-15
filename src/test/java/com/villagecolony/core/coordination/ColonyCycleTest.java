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
        lumberjack();

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
        lumberjack();

        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        assertEquals(1, tasks.count());
    }

    /** Falta maior no ciclo seguinte não cria segunda tarefa do mesmo tipo. */
    @Test
    void aGrowingDeficitStillKeepsOneTask() {
        lumberjack();

        ColonyCycle.run(COLONY, owning(50), GOAL, tasks, workers);
        ColonyCycle.run(COLONY, owning(2), GOAL, tasks, workers);

        assertEquals(1, tasks.count());
    }

    /** Recursos diferentes em falta produzem tarefas diferentes. */
    @Test
    void eachMissingResourceGetsItsOwnTask() {
        lumberjack();

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
        lumberjack();

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

    /**
     * Cada lenhador ganha o seu pedido.
     *
     * <p>Uma tarefa tem um executor só. Com um pedido de madeira e cinco
     * lenhadores, quatro ficariam olhando o quinto trabalhar — e desde a
     * Regra 1, que faz a tarefa durar até o baú encher, ficariam olhando
     * por muito tempo.
     */
    @Test
    void everyCapableWorkerGetsATaskOfItsOwn() {
        lumberjack();
        lumberjack();
        lumberjack();

        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        assertEquals(3, tasks.count());

        for (Task task : tasks.ofColony(COLONY)) {
            assertEquals(TaskState.RESERVED, task.state(), "sobrou tarefa sem dono");
        }
    }

    /** A falta é repartida entre os pedidos abertos. */
    @Test
    void theDeficitIsSplitBetweenTheWorkers() {
        lumberjack();
        lumberjack();

        ColonyCycle.run(COLONY, owning(4), GOAL, tasks, workers);

        for (Task task : tasks.ofColony(COLONY)) {
            assertEquals(30, task.amount(), "esperava metade dos 60 que faltam");
        }
    }

    /**
     * O ciclo seguinte não abre uma segunda rodada.
     *
     * <p>O teto é o número de trabalhadores capazes, e quem já está
     * executando segura um dos pedidos — contá-lo como ocioso faria a
     * colônia abrir uma tarefa nova por ciclo para quem já trabalha, que
     * é o E1 de volta por outra porta.
     */
    @Test
    void aSecondCycleDoesNotAddASecondRoundOfTasks() {
        lumberjack();
        lumberjack();

        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        assertEquals(2, tasks.count());
    }

    /**
     * Um lenhador novo entra e ganha trabalho.
     *
     * <p>É o outro lado do teto: ele acompanha a colônia. Um aldeão que
     * vira lenhador no meio da partida não precisa esperar a fila
     * esvaziar.
     */
    @Test
    void aNewWorkerGetsATaskInTheNextCycle() {
        lumberjack();

        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        assertEquals(1, tasks.count());

        lumberjack();

        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        assertEquals(2, tasks.count());
    }

    /**
     * Sem ninguém que saiba fazer, não se abre pedido.
     *
     * <p>Uma tarefa que nenhuma profissão da colônia pode executar fica
     * na fila para sempre: nada a cancela, porque a falta continua, e
     * nada a conclui, porque não há executor. Era assim que a meta de
     * pedra sobrevivia.
     */
    @Test
    void nobodyCapableMeansNoRequest() {
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        assertEquals(0, tasks.count());
    }

    /** Uma colônia não mexe na fila da outra. */
    @Test
    void oneColonyDoesNotTouchAnother() {
        UUID other = UUID.randomUUID();

        lumberjack();

        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);
        ColonyCycle.run(other, owning(64), GOAL, tasks, workers);

        assertEquals(1, tasks.ofColony(COLONY).size());
        assertEquals(0, tasks.ofColony(other).size());
    }

    // ── A tarefa de obra não pertence ao ciclo ─────────────────────────
    //
    // A sessão de 2026-08-15 mostrou que nada em produção criava tarefa
    // BUILD, e a correção pôs ConstructionPlanner para criá-la. Os dois
    // testes abaixo guardam as duas armadilhas que essa tarefa
    // encontraria aqui dentro — as duas silenciosas, e as duas fatais
    // para a obra.

    /** Uma tarefa de obra, como o ConstructionPlanner a abre. */
    private Task buildTask() {
        return tasks.create(
                COLONY,
                TaskType.BUILD,
                com.villagecolony.core.task.model.TaskPriority.CONSTRUCTION,
                ResourceType.OAK_PLANKS,
                151);
    }

    /**
     * O recurso de uma tarefa de obra é nominal. Se o ciclo a cancelasse
     * por aquele recurso não estar em falta, a casa sairia da fila porque
     * o estoque de tábua subiu — que é o contrário do que se quer, já que
     * é a tábua que a casa consome.
     */
    @Test
    void aBuildTaskIsNotCancelledWhenItsNominalResourceIsNotMissing() {
        lumberjack();

        Task build = buildTask();

        // Meta só de madeira: OAK_PLANKS não está em falta nenhuma.
        ColonyCycle.run(COLONY, owning(10), GOAL, tasks, workers);

        assertEquals(
                TaskState.AVAILABLE,
                build.state(),
                "o ciclo cancelou a obra por causa do estoque de tábua");
    }

    /**
     * E ela não pode ocupar a vaga de um pedido de verdade: contada como
     * pedido de tábua, faria a colônia deixar de fabricar exatamente o
     * que a obra consome.
     */
    @Test
    void aBuildTaskDoesNotCountAsAnOpenRequestForItsNominalResource() {
        workers.register(UUID.randomUUID(), COLONY).assign(ProfessionType.MANUFACTURER);

        buildTask();

        ColonyCycle.run(
                COLONY,
                ResourceTally.of(Map.of(ResourceType.OAK_PLANKS, 0)),
                Map.of(ResourceType.OAK_PLANKS, 32),
                tasks,
                workers);

        long crafting = tasks.ofColony(COLONY).stream()
                .filter(task -> task.type() == TaskType.CRAFT_MATERIAL)
                .count();

        assertTrue(
                crafting > 0,
                "a obra ocupou a vaga do pedido de tábua, e a colônia não vai fabricar");
    }
}
