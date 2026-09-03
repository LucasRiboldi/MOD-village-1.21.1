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
     * Quem travou pega o que houver, e volta a produzir — ADR-010.
     *
     * <p>O mineiro cuja pedra acabou de travar continua mineiro: baú,
     * ferramenta e nome não mudam. O que muda é a tarefa que ele aceita
     * nesta passagem. Ver a ADR para por que trocar a profissão de
     * verdade sairia caro — baú, Regra 11 e a invariante da mão.
     */
    @Test
    void aWorkerWhoseWorkStalledBorrowsAnother() {
        Worker miner = workerWith(ProfessionType.MINER);
        miner.rest(Capability.COLLECT_STONE);

        stoneTask();
        Task wood = woodTask();

        assertEquals(1, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(Optional.of(miner.villagerId()), wood.executor());
    }

    /**
     * Mas nunca fica parado para honrar um descanso.
     *
     * <p>A terceira passagem, e ela é o que impede a regra de virar o
     * problema que conserta: sem nada emprestado ao alcance, a pedra
     * dele volta a valer mesmo descansando.
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
     * Obra não se empresta.
     *
     * <p>Coleta é andar até um bloco e trazê-lo; qualquer um com baú faz.
     * Obra tem projeto, cursor e barreira de teste, e um pedreiro
     * emprestado entrando no meio de uma casa é defeito, não ajuda.
     */
    @Test
    void buildingIsNotLent() {
        Worker miner = workerWith(ProfessionType.MINER);
        miner.rest(Capability.COLLECT_STONE);

        Task build = tasks.create(COLONY, TaskType.BUILD, TaskPriority.CONSTRUCTION,
                ResourceType.OAK_PLANKS, 1);

        assertEquals(0, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(TaskState.AVAILABLE, build.state());
    }

    /**
     * E quem não travou não pega o trabalho dos outros.
     *
     * <p>É o portão da regra inteira, e o que mantém de pé o
     * {@link #aFarmerDoesNotTakeTheWoodTask}: emprestar é consequência de
     * ter travado, e não de estar sem tarefa. Sem isto, toda profissão
     * ociosa viraria lenhadora na primeira passagem.
     */
    @Test
    void aWorkerWhoDidNotStallDoesNotBorrow() {
        workerWith(ProfessionType.MINER);

        Task wood = woodTask();

        assertEquals(0, WorkAssignment.assign(COLONY, workers, tasks));
        assertEquals(TaskState.AVAILABLE, wood.state());
    }

    /**
     * Uma mão emprestada por capacidade — 2026-09-02, sessão das 22:59.
     *
     * <p><b>A regra da mão emprestada esvaziou a profissão inteira.</b>
     * Os dois lenhadores da vila travaram na madeira no mesmo ciclo,
     * descansaram a capacidade, e os dois foram para a mina:
     *
     * <pre>
     * miners: 7b6909df (LUMBERJACK lending a hand) digging Diorito ...
     *         fad43afc (LUMBERJACK lending a hand) waiting for the shaft
     * </pre>
     *
     * <p>Dez minutos, zero pedra e <b>zero árvore</b> — a vila ficou sem
     * quem cortasse lenha. E o segundo emprestado nem cavou: a escada é
     * de um só, então ele trocou tentar outra árvore por ficar numa fila
     * de uma vaga. Emprestar tem que render, e entrar em fila não rende.
     *
     * <p>A ADR-010 previu o risco — <i>"o especialista some da
     * especialidade"</i> — e apostou que a 1ª passagem o seguraria. Não
     * segurou, porque os dois travaram juntos. O teto é por capacidade:
     * o segundo volta ao próprio trabalho pela 3ª passagem, que é onde
     * ele rende mais que parado.
     */
    @Test
    void onlyOneHandIsLentToTheSameCapability() {
        Worker first = workerWith(ProfessionType.LUMBERJACK);
        Worker second = workerWith(ProfessionType.LUMBERJACK);

        first.rest(Capability.COLLECT_WOOD);
        second.rest(Capability.COLLECT_WOOD);

        stoneTask();
        stoneTask();

        Task wood = woodTask();

        WorkAssignment.assign(COLONY, workers, tasks);

        assertTrue(
                wood.executor().isPresent(),
                "os dois lenhadores foram para a mina, e a vila ficou sem quem corte lenha");
    }
}
