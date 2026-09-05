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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TASK-023 — quem faz o quê.
 *
 * <p>Os dois lados já existiam e não se conheciam: {@link TaskType}
 * declara a {@link com.villagecolony.core.type.Capability} que exige, e
 * a profissão do trabalhador diz quais ele tem. Faltava quem os casasse,
 * e não havia lugar legítimo para esse código até a emenda da ADR-006
 * §6.
 */
class WorkAssignmentTest {

    private static final UUID COLONY = UUID.randomUUID();

    private WorkerService workers;

    private TaskService tasks;

    @BeforeEach
    void setUp() {
        workers = new WorkerService();
        tasks = new TaskService();
    }

    private Worker workerWith(ProfessionType profession) {
        Worker worker = workers.register(UUID.randomUUID(), COLONY);
        worker.assign(profession);

        return worker;
    }

    private Task woodTask() {
        return tasks.create(COLONY, TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION,
                ResourceType.OAK_LOG, 64);
    }

    @Test
    void aLumberjackTakesTheWoodTask() {
        Worker lumberjack = workerWith(ProfessionType.LUMBERJACK);
        Task task = woodTask();

        int assigned = WorkAssignment.assign(COLONY, workers, tasks);

        assertEquals(1, assigned);
        assertEquals(TaskState.RESERVED, task.state());
        assertEquals(Optional.of(lumberjack.villagerId()), task.executor());
    }

    /** A capacidade é o critério, não o nome da profissão. */
    @Test
    void aFarmerDoesNotTakeTheWoodTask() {
        workerWith(ProfessionType.FARMER);
        Task task = woodTask();

        assertEquals(0, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(TaskState.AVAILABLE, task.state());
    }

    @Test
    void aWorkerWithoutAProfessionTakesNothing() {
        workers.register(UUID.randomUUID(), COLONY);
        Task task = woodTask();

        assertEquals(0, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(TaskState.AVAILABLE, task.state());
    }

    /**
     * Um trabalhador por vez.
     *
     * <p>Sem isto o mesmo lenhador pegaria a fila inteira, e a Fase 8 o
     * mandaria andar para dois lugares ao mesmo tempo.
     */
    @Test
    void aBusyWorkerDoesNotTakeASecondTask() {
        workerWith(ProfessionType.LUMBERJACK);
        woodTask();
        woodTask();

        assertEquals(1, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(0, WorkAssignment.assign(COLONY, workers, tasks));
    }

    @Test
    void twoLumberjacksTakeTwoTasks() {
        workerWith(ProfessionType.LUMBERJACK);
        workerWith(ProfessionType.LUMBERJACK);
        woodTask();
        woodTask();

        assertEquals(2, WorkAssignment.assign(COLONY, workers, tasks));
    }

    @Test
    void moreWorkersThanTasksLeavesTheRestIdle() {
        workerWith(ProfessionType.LUMBERJACK);
        workerWith(ProfessionType.LUMBERJACK);
        woodTask();

        assertEquals(1, WorkAssignment.assign(COLONY, workers, tasks));
    }

    /** A fila respeita a prioridade que o TaskService já ordena. */
    @Test
    void theUrgentTaskGoesFirst() {
        workerWith(ProfessionType.LUMBERJACK);

        tasks.create(COLONY, TaskType.COLLECT_WOOD, TaskPriority.CONSTRUCTION,
                ResourceType.OAK_LOG, 64);

        Task urgent = tasks.create(COLONY, TaskType.COLLECT_WOOD, TaskPriority.SURVIVAL,
                ResourceType.OAK_LOG, 64);

        WorkAssignment.assign(COLONY, workers, tasks);

        assertEquals(TaskState.RESERVED, urgent.state());
    }

    /** Trabalhador de outra colônia não atende esta fila. */
    @Test
    void aWorkerOfAnotherColonyIsNotConsidered() {
        Worker stranger = workers.register(UUID.randomUUID(), UUID.randomUUID());
        stranger.assign(ProfessionType.LUMBERJACK);

        Task task = woodTask();

        assertEquals(0, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(TaskState.AVAILABLE, task.state());
    }

    @Test
    void nothingToDoIsNotAnError() {
        workerWith(ProfessionType.LUMBERJACK);

        assertEquals(0, WorkAssignment.assign(COLONY, workers, tasks));
    }

    @Test
    void idleWorkersAreTheOnesWithoutATask() {
        Worker busy = workerWith(ProfessionType.LUMBERJACK);
        Worker free = workerWith(ProfessionType.BUILDER);

        woodTask();
        WorkAssignment.assign(COLONY, workers, tasks);

        List<Worker> idle = WorkAssignment.idleWorkers(COLONY, workers, tasks);

        assertEquals(1, idle.size());
        assertEquals(free.villagerId(), idle.get(0).villagerId());
        assertFalse(idle.contains(busy));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class,
                () -> WorkAssignment.assign(null, workers, tasks));

        assertThrows(NullPointerException.class,
                () -> WorkAssignment.assign(COLONY, null, tasks));

        assertThrows(NullPointerException.class,
                () -> WorkAssignment.assign(COLONY, workers, null));
    }

    /**
     * Reatribuir depois de o trabalhador soltar a tarefa.
     *
     * <p>O caminho de quem morre no meio do trabalho: o §15 registra que
     * a profissão e o baú já são liberados; a tarefa também precisa
     * voltar para a fila e achar outro dono.
     */
    @Test
    void aReleasedTaskFindsAnotherWorker() {
        Worker first = workerWith(ProfessionType.LUMBERJACK);
        Worker second = workerWith(ProfessionType.LUMBERJACK);

        Task task = woodTask();

        WorkAssignment.assign(COLONY, workers, tasks);

        UUID owner = task.executor().orElseThrow();

        tasks.releaseAllOf(owner);
        workers.remove(owner);

        assertEquals(1, WorkAssignment.assign(COLONY, workers, tasks));

        UUID survivor = owner.equals(first.villagerId())
                ? second.villagerId()
                : first.villagerId();

        assertEquals(Optional.of(survivor), task.executor());
        assertTrue(task.state() == TaskState.RESERVED);
    }

    /**
     * A sessão de 2026-08-15, e o motivo de {@code needsOwnStorage}
     * existir.
     *
     * <p>Dois lenhadores e dois fabricantes ficaram doze minutos sem
     * baú. A cada ciclo a distribuição lhes dava a tarefa, e a cada tick
     * {@code LumberjackWork} a devolvia à fila por não ter onde guardar:
     * dezenas de linhas "has no chest — wood task returned to the queue"
     * e nada produzido. A tarefa girava e nunca chegava a quem tinha baú.
     */
    @Test
    void aWorkerWithoutAChestDoesNotTakeATaskThatNeedsOne() {
        workerWith(ProfessionType.LUMBERJACK);

        Task task = woodTask();

        assertEquals(0, WorkAssignment.assign(COLONY, workers, tasks, worker -> false));
        assertEquals(TaskState.AVAILABLE, task.state());
        assertTrue(task.executor().isEmpty());
    }

    /** E a tarefa recusada sobra para quem pode atendê-la. */
    @Test
    void theTaskGoesToTheLumberjackWhoHasAChest() {
        Worker without = workerWith(ProfessionType.LUMBERJACK);
        Worker with = workerWith(ProfessionType.LUMBERJACK);

        Task task = woodTask();

        assertEquals(
                1,
                WorkAssignment.assign(
                        COLONY, workers, tasks, worker -> worker.equals(with.villagerId())));

        assertEquals(Optional.of(with.villagerId()), task.executor());
        assertFalse(task.executor().equals(Optional.of(without.villagerId())));
    }

    /**
     * Construir não guarda nada — o material sai do baú da colônia e
     * vira bloco. Exigir baú próprio do construtor travaria a obra por um
     * motivo que não existe.
     */
    @Test
    void aBuilderWithoutAChestStillTakesTheBuildTask() {
        workerWith(ProfessionType.BUILDER);

        Task task = tasks.create(
                COLONY, TaskType.BUILD, TaskPriority.CONSTRUCTION, ResourceType.OAK_PLANKS, 151);

        assertEquals(1, WorkAssignment.assign(COLONY, workers, tasks, worker -> false));
        assertTrue(task.executor().isPresent());
    }

    /** Sem o argumento, o comportamento é o de antes: baú não é olhado. */
    @Test
    void theOverloadWithoutStorageKnowledgeAssignsAsBefore() {
        workerWith(ProfessionType.LUMBERJACK);
        woodTask();

        assertEquals(1, WorkAssignment.assign(COLONY, workers, tasks));
    }

    private Task stoneTask() {
        return tasks.create(COLONY, TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE, 64);
    }

    /**
     * <b>O mineiro travado não vira lenhador</b> — decisão do autor,
     * 2026-09-05, depois da sessão de 22:37 de 04-09.
     *
     * <p>Era o contrário até aqui: a 2ª passagem da ADR-010 dava a ele a
     * tarefa de madeira, e o log da sessão mostrou o resultado nos dois
     * sentidos ao mesmo tempo — {@code (MINER lending a hand)} na árvore
     * e {@code (LUMBERJACK lending a hand)} na galeria.
     *
     * <p>O que ele faz agora é voltar para a pedra dele pela 2ª passagem,
     * que é a antiga terceira. A madeira fica para quem sabe cortá-la.
     */
    @Test
    void aStalledMinerDoesNotTakeTheWoodTask() {
        Worker miner = workerWith(ProfessionType.MINER);
        miner.rest(Capability.COLLECT_STONE);

        Task stone = stoneTask();
        Task wood = woodTask();

        assertEquals(1, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(Optional.of(miner.villagerId()), stone.executor());
        assertEquals(
                TaskState.AVAILABLE,
                wood.state(),
                "o mineiro pegou a madeira, e a separação de funções é do autor");
    }

    /** E o inverso, que é a outra metade da mesma frase. */
    @Test
    void aStalledLumberjackDoesNotTakeTheStoneTask() {
        Worker lumberjack = workerWith(ProfessionType.LUMBERJACK);
        lumberjack.rest(Capability.COLLECT_WOOD);

        Task stone = stoneTask();
        Task wood = woodTask();

        assertEquals(1, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(Optional.of(lumberjack.villagerId()), wood.executor());
        assertEquals(
                TaskState.AVAILABLE,
                stone.state(),
                "o lenhador foi para a mina, que é o defeito visto em jogo");
    }

    /**
     * Nunca fica parado para honrar um descanso.
     *
     * <p>A 2ª passagem, e ela é o que impede o descanso de virar o
     * problema que conserta: sem mais nada da profissão dele ao alcance,
     * a pedra volta a valer mesmo descansando.
     *
     * <p>Sobreviveu inteira à saída da mão emprestada — era a 3ª
     * passagem, e é a mesma prova.
     */
    @Test
    void theRestNeverLeavesTheWorkerIdle() {
        Worker miner = workerWith(ProfessionType.MINER);
        miner.rest(Capability.COLLECT_STONE);

        Task stone = stoneTask();

        assertEquals(1, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(Optional.of(miner.villagerId()), stone.executor());
    }

    /**
     * E obra continua sendo só do construtor.
     *
     * <p>Valia pela lista do que se emprestava; vale agora pela regra
     * inteira, que é mais forte. O teste fica porque a afirmação é a
     * mesma e o autor a cobra: um pedreiro emprestado entrando no meio
     * de uma casa é defeito, não ajuda.
     */
    @Test
    void buildingStaysWithTheBuilder() {
        Worker miner = workerWith(ProfessionType.MINER);
        miner.rest(Capability.COLLECT_STONE);

        Task build = tasks.create(COLONY, TaskType.BUILD, TaskPriority.CONSTRUCTION,
                ResourceType.OAK_PLANKS, 1);

        assertEquals(0, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(TaskState.AVAILABLE, build.state());
    }

    /**
     * Dois lenhadores travados continuam os dois na madeira.
     *
     * <p>É a sessão de 2026-09-02 22:59 relida com a regra de hoje. Lá
     * os dois foram para a mina e a vila passou dez minutos com zero
     * pedra e <b>zero árvore</b>. Aqui os dois voltam para a árvore, e a
     * pedra espera o mineiro — que é quem sabe descer a escada.
     */
    @Test
    void bothStalledLumberjacksStayOnWood() {
        Worker first = workerWith(ProfessionType.LUMBERJACK);
        Worker second = workerWith(ProfessionType.LUMBERJACK);

        first.rest(Capability.COLLECT_WOOD);
        second.rest(Capability.COLLECT_WOOD);

        Task stone = stoneTask();
        Task firstWood = woodTask();
        Task secondWood = woodTask();

        assertEquals(2, WorkAssignment.assign(COLONY, workers, tasks));

        assertTrue(firstWood.executor().isPresent());
        assertTrue(secondWood.executor().isPresent());
        assertEquals(
                TaskState.AVAILABLE,
                stone.state(),
                "a vila ficou sem quem corte lenha, que é o defeito de 09-02 ao contrario");
    }
}
