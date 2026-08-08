package com.villagecolony.core.coordination;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.resource.service.ResourceDemand;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.service.WorkerService;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * O ciclo em que a colônia pensa — TASK-020 e ADR-002.
 *
 * <p>Três passos, nesta ordem:
 *
 * <ol>
 *   <li>comparar o que a colônia tem com o que ela quer;
 *   <li>pedir o que falta, e retirar o pedido que perdeu o motivo;
 *   <li>entregar os pedidos a quem sabe atendê-los.
 * </ol>
 *
 * <p>Existe porque {@code ColonyResources} sabia responder "o que
 * tenho" e {@code ResourceDemand} sabia responder "o que falta" sem que
 * houvesse onde perguntar. O loop da ADR-002 nunca tinha sido escrito, e
 * pendurar a pergunta na detecção faria a colônia pensar só quando
 * alguém passasse perto.
 *
 * <p>Não guarda estado nem conta ticks: recebe a fotografia do momento e
 * age sobre ela. Quem chama decide a cadência — hoje o ciclo longo de
 * {@code VillageDetector.CYCLE_TICKS}, e é ele que sabe quais colônias
 * estão ACTIVE.
 */
public final class ColonyCycle {

    private ColonyCycle() {
    }

    /**
     * Roda um ciclo de uma colônia.
     *
     * @param owned o que a colônia tem, tipicamente o total de
     *     {@code ColonyResources}. Contagem parcial produz pedido a
     *     mais — ver {@code ChestInventoryReader.ChestSurvey}
     * @param goal quanto ela quer ter de cada recurso
     * @return quantas tarefas foram reservadas por trabalhadores agora
     */
    public static int run(
            UUID colonyId,
            ResourceTally owned,
            Map<ResourceType, Integer> goal,
            TaskService tasks,
            WorkerService workers) {

        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(owned, "owned");
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(tasks, "tasks");
        Objects.requireNonNull(workers, "workers");

        Map<ResourceType, Integer> missing = ResourceDemand.deficit(goal, owned);

        cancelSatisfied(colonyId, missing, tasks);
        requestMissing(colonyId, missing, tasks);

        return WorkAssignment.assign(colonyId, workers, tasks);
    }

    /**
     * Retira da fila o pedido que perdeu o motivo.
     *
     * <p>O jogador enche o baú e a falta acaba; deixar o pedido mandaria
     * um lenhador buscar madeira que a colônia já tem.
     *
     * <p>Só cancela tarefa ainda disponível. Quem já começou termina: um
     * trabalhador a meio caminho da árvore não é interrompido porque a
     * contagem mudou, e a contagem muda o tempo todo.
     */
    private static void cancelSatisfied(
            UUID colonyId, Map<ResourceType, Integer> missing, TaskService tasks) {

        for (Task task : tasks.availableFor(colonyId)) {
            if (!missing.containsKey(task.targetResource())) {
                task.cancel();
            }
        }
    }

    /**
     * Cria um pedido por recurso em falta, se ainda não houver um.
     *
     * <p>Um por recurso, e não um por ciclo: o ciclo roda a cada trinta
     * segundos, e uma colônia com falta permanente acumularia uma tarefa
     * por ciclo até a fila crescer sem limite.
     *
     * <p>A quantidade é o que falta agora. Um pedido já aberto não é
     * reescrito quando a falta cresce — mexer no alvo de uma tarefa que
     * alguém pode estar executando é outra decisão, e o MVP não precisa
     * dela: o ciclo seguinte à conclusão pede o resto.
     */
    private static void requestMissing(
            UUID colonyId, Map<ResourceType, Integer> missing, TaskService tasks) {

        for (Map.Entry<ResourceType, Integer> entry : missing.entrySet()) {
            if (hasOpenRequestFor(colonyId, entry.getKey(), tasks)) {
                continue;
            }

            tasks.create(
                    colonyId,
                    typeFor(entry.getKey()),
                    TaskPriority.PRODUCTION,
                    entry.getKey(),
                    entry.getValue());
        }
    }

    private static boolean hasOpenRequestFor(
            UUID colonyId, ResourceType resource, TaskService tasks) {

        for (Task task : tasks.ofColony(colonyId)) {
            if (task.targetResource() == resource && task.state() != TaskState.CANCELLED
                    && task.state() != TaskState.COMPLETED) {

                return true;
            }
        }

        return false;
    }

    /**
     * Quem produz cada recurso.
     *
     * <p>É a cadeia produtiva de Resource-System.md: o que se tira do
     * mundo é coletado, o que se faz a partir de outro é fabricado, e o
     * que uma obra consome é erguido.
     */
    private static TaskType typeFor(ResourceType resource) {
        return switch (resource.category()) {
            case NATURAL -> TaskType.COLLECT_WOOD;
            case PROCESSED -> TaskType.CRAFT_MATERIAL;
            case CONSTRUCTION -> TaskType.BUILD;
        };
    }
}
