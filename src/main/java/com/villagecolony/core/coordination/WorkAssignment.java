package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.Task;
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

        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(tasks, "tasks");

        int assigned = 0;

        for (Worker worker : idleWorkers(colonyId, workers, tasks)) {
            if (takeOneTask(colonyId, worker, tasks)) {
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
     * Dá a este trabalhador a tarefa mais urgente que ele saiba fazer.
     *
     * <p>Percorre as capacidades da profissão porque um pedreiro que
     * também carrega madeira deve pegar madeira quando não há o que
     * construir. A ordem entre as capacidades de uma mesma profissão é a
     * do catálogo, e não uma decisão tomada aqui.
     *
     * @return true se alguma tarefa foi reservada
     */
    private static boolean takeOneTask(
            java.util.UUID colonyId, Worker worker, TaskService tasks) {

        Optional<ProfessionType> profession = worker.profession();

        if (profession.isEmpty()) {
            return false;
        }

        Profession catalogued = ProfessionRegistry.of(profession.get());

        for (Capability capability : catalogued.capabilities()) {
            Optional<Task> task = tasks.nextFor(colonyId, capability);

            if (task.isPresent()) {
                task.get().reserveFor(worker.villagerId());

                return true;
            }
        }

        return false;
    }
}
