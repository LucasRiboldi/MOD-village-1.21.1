package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.Capability;
import com.villagecolony.core.worker.model.Profession;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionRegistry;
import com.villagecolony.core.worker.service.WorkerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Quem faz o quê — TASK-023.
 *
 * <p>Percorre os trabalhadores ociosos de uma colônia, lê a profissão de
 * cada um e procura na fila a tarefa mais urgente que ele saiba fazer.
 *
 * <p>Não guarda estado. Os dois lados que ele casa já existiam e não se
 * conheciam: {@code TaskType} declara a {@link Capability} que exige, e
 * {@link Profession} diz quais o trabalhador tem. O que faltava era um
 * lugar legítimo para o código que lê os dois — ver a emenda da ADR-006
 * §6, que criou este pacote.
 *
 * <p>A ordem entre trabalhadores é a que {@code WorkerService} devolve;
 * a ordem entre tarefas é a prioridade, e quem a decide é o
 * {@code TaskService}. Nada disso é escolhido aqui de novo.
 */
public final class WorkAssignment {

    /**
     * O que responder quando ninguém perguntou por baú.
     *
     * <p>Preserva o comportamento de antes de {@link
     * TaskType#needsOwnStorage()} existir: sem informação de baú, todo
     * trabalhador é elegível. É o que a sobrecarga de três argumentos usa.
     */
    private static final Predicate<java.util.UUID> ANY_WORKER_HAS_STORAGE = worker -> true;

    private WorkAssignment() {
    }

    /**
     * Distribui as tarefas disponíveis entre os trabalhadores ociosos.
     *
     * <p>Um trabalhador por tarefa e uma tarefa por trabalhador: a Fase 8
     * vai mandar o aldeão andar até o local, e quem tivesse duas tarefas
     * andaria para dois lugares.
     *
     * <p>Trabalhador sem profissão é pulado, não é erro — bebê e nitwit
     * são o caso comum, e a atribuição de profissão é passo separado.
     * Ver TASK-014.
     *
     * @return quantas tarefas foram reservadas agora
     */
    public static int assign(
            java.util.UUID colonyId, WorkerService workers, TaskService tasks) {

        return assign(colonyId, workers, tasks, ANY_WORKER_HAS_STORAGE);
    }

    /**
     * O mesmo, sabendo quem tem baú.
     *
     * <p>A sobrecarga acima existe para quem não tem essa informação —
     * o Core não conhece baú de Minecraft, e boa parte dos testes de
     * unidade não precisa dele. Em jogo quem chama é o ciclo, que conhece
     * o registro de baús e passa a consulta aqui.
     *
     * @param hasStorage responde se um trabalhador tem baú próprio. Ver
     *     {@link TaskType#needsOwnStorage()} para o que depende disso
     */
    public static int assign(
            java.util.UUID colonyId,
            WorkerService workers,
            TaskService tasks,
            Predicate<java.util.UUID> hasStorage) {

        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(tasks, "tasks");
        Objects.requireNonNull(hasStorage, "hasStorage");

        int assigned = 0;

        for (Worker worker : idleWorkers(colonyId, workers, tasks)) {
            if (takeOneTask(colonyId, worker, tasks, hasStorage)) {
                assigned++;
            }
        }

        return assigned;
    }

    /**
     * Os trabalhadores da colônia que não estão executando nada.
     *
     * <p>Ocioso é quem não tem tarefa aberta, e não quem está parado: um
     * trabalhador a caminho da árvore continua ocupado, e a tarefa é o
     * que sabe disso.
     */
    public static List<Worker> idleWorkers(
            java.util.UUID colonyId, WorkerService workers, TaskService tasks) {

        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(tasks, "tasks");

        List<Worker> idle = new ArrayList<>();

        for (Worker worker : workers.ofColony(colonyId)) {
            if (tasks.assignedTo(worker.villagerId()).isEmpty()) {
                idle.add(worker);
            }
        }

        return List.copyOf(idle);
    }

    /**
     * Quantos trabalhadores da colônia sabem fazer isto.
     *
     * <p>É quantas mãos a colônia tem para uma capacidade, e não quantas
     * estão livres agora: quem já está executando conta, porque a tarefa
     * dele é uma das que a colônia abriu. Contar só os ociosos faria a
     * colônia abrir uma tarefa nova a cada ciclo para quem já está
     * trabalhando.
     *
     * <p>Mora aqui, e não em {@code ColonyCycle}, pelo mesmo motivo que
     * {@link #takeOneTask}: ler profissão e traduzir para capacidade é o
     * que esta classe faz, e fazê-lo em dois lugares abriria espaço para
     * as duas leituras divergirem.
     */
    public static int countCapableOf(
            java.util.UUID colonyId, Capability capability, WorkerService workers) {

        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(workers, "workers");

        int capable = 0;

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.profession()
                    .map(ProfessionRegistry::of)
                    .filter(catalogued -> catalogued.capabilities().contains(capability))
                    .isPresent()) {

                capable++;
            }
        }

        return capable;
    }

    /**
     * Dá a este trabalhador a tarefa mais urgente que ele saiba fazer.
     *
     * <p>Percorre as capacidades da profissão porque um pedreiro que
     * também carrega madeira deve pegar madeira quando não há o que
     * construir. A ordem entre as capacidades de uma mesma profissão é a
     * do catálogo, e não uma decisão tomada aqui.
     *
     * <p>Uma tarefa que exige baú próprio é pulada quando o trabalhador
     * não tem um. Não é filtro de conveniência: quem a executasse a
     * soltaria no primeiro tick, e o ciclo seguinte a daria ao mesmo
     * trabalhador — a fila giraria sem que nada fosse produzido, e a
     * tarefa nunca chegaria a quem tem baú. Ver
     * {@link TaskType#needsOwnStorage()}.
     *
     * @return true se alguma tarefa foi reservada
     */
    private static boolean takeOneTask(
            java.util.UUID colonyId,
            Worker worker,
            TaskService tasks,
            Predicate<java.util.UUID> hasStorage) {

        Optional<ProfessionType> profession = worker.profession();

        if (profession.isEmpty()) {
            return false;
        }

        Profession catalogued = ProfessionRegistry.of(profession.get());

        for (Capability capability : catalogued.capabilities()) {
            Optional<Task> task = tasks.nextFor(colonyId, capability);

            if (task.isEmpty()) {
                continue;
            }

            if (task.get().type().needsOwnStorage()
                    && !hasStorage.test(worker.villagerId())) {

                continue;
            }

            task.get().reserveFor(worker.villagerId());

            return true;
        }

        return false;
    }
}
