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
import java.util.function.Predicate;

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

        return run(colonyId, owned, goal, tasks, workers, worker -> true);
    }

    /**
     * O mesmo, sabendo quem tem baú.
     *
     * <p>Quem chama em jogo é o ciclo da camada Fabric, que conhece o
     * registro de baús. Sem essa consulta a distribuição entrega tarefa
     * de colher e de fabricar a trabalhador sem baú, que a devolve no
     * primeiro tick — ver {@link TaskType#needsOwnStorage()}.
     *
     * @param hasStorage responde se um trabalhador tem baú próprio
     */
    public static int run(
            UUID colonyId,
            ResourceTally owned,
            Map<ResourceType, Integer> goal,
            TaskService tasks,
            WorkerService workers,
            Predicate<UUID> hasStorage) {

        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(owned, "owned");
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(tasks, "tasks");
        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(hasStorage, "hasStorage");

        Map<ResourceType, Integer> missing = ResourceDemand.deficit(goal, owned);

        cancelSatisfied(colonyId, missing, tasks);
        requestMissing(colonyId, missing, tasks, workers);

        return WorkAssignment.assign(colonyId, workers, tasks, hasStorage);
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
            if (!task.type().isResourceRequest()) {
                // A tarefa de obra não é pedido de recurso, e o recurso
                // dela é nominal. Cancelá-la aqui tiraria da fila uma
                // casa por conta do estoque de tábua ter subido — ver
                // TaskType.isResourceRequest.
                continue;
            }

            if (!missing.containsKey(task.targetResource())) {
                task.cancel();
            }
        }
    }

    /**
     * Pede o que falta, um pedido por mão que a colônia tem.
     *
     * <p>Um por trabalhador capaz, e não um só por recurso. Uma tarefa
     * tem um executor — Simulation-Loop.md §"Uma tarefa possui apenas um
     * executor" —, então uma colônia com cinco lenhadores e um pedido de
     * madeira põe quatro deles a olhar o quinto trabalhar. Enquanto a
     * tarefa durava uma árvore isso quase não aparecia, porque ela girava
     * entre os aldeões a cada ciclo; com a Regra 1, que a faz durar até o
     * baú encher, um lenhador só trabalharia por muito tempo.
     *
     * <p>O teto é o número de trabalhadores capazes, e não o de ociosos.
     * Quem já está executando segura uma das tarefas abertas e por isso
     * já está contado; olhar só os ociosos abriria uma tarefa nova a cada
     * ciclo para quem já está trabalhando, que é o E1 de volta por outra
     * porta.
     *
     * <p>E não é um por ciclo: uma colônia com falta permanente
     * acumularia uma tarefa por ciclo até a fila crescer sem limite.
     *
     * <p>A quantidade é a falta repartida entre os pedidos abertos. É uma
     * divisão de fachada e vale dizer por quê: quem de fato encerra o
     * trabalho é o espaço no baú <em>daquele</em> trabalhador, conferido
     * a cada árvore por {@code LumberjackWork}. O número na tarefa serve
     * para o log e para o dia em que houver recurso cuja coleta não passe
     * por baú próprio.
     *
     * <p>Um pedido já aberto não é reescrito quando a falta cresce —
     * mexer no alvo de uma tarefa que alguém pode estar executando é
     * outra decisão, e o MVP não precisa dela.
     */
    private static void requestMissing(
            UUID colonyId,
            Map<ResourceType, Integer> missing,
            TaskService tasks,
            WorkerService workers) {

        for (Map.Entry<ResourceType, Integer> entry : missing.entrySet()) {
            ResourceType resource = entry.getKey();
            TaskType type = typeFor(resource);

            int hands = WorkAssignment.countCapableOf(colonyId, type.required(), workers);

            if (hands == 0) {
                // Ninguém sabe fazer. Abrir a tarefa mesmo assim a
                // deixaria na fila para sempre, sem executor possível.
                continue;
            }

            int open = countOpenRequestsFor(colonyId, resource, tasks);
            int share = Math.max(1, entry.getValue() / hands);

            for (int i = open; i < hands; i++) {
                tasks.create(colonyId, type, TaskPriority.PRODUCTION, resource, share);
            }
        }
    }

    /** Quantos pedidos deste recurso ainda estão de pé. */
    private static int countOpenRequestsFor(
            UUID colonyId, ResourceType resource, TaskService tasks) {

        int open = 0;

        for (Task task : tasks.ofColony(colonyId)) {
            if (!task.type().isResourceRequest()) {
                // Idem: a de obra não é pedido, e contá-la aqui faria a
                // colônia deixar de fabricar a tábua que a obra consome.
                continue;
            }

            if (task.targetResource() == resource && task.state() != TaskState.CANCELLED
                    && task.state() != TaskState.COMPLETED) {

                open++;
            }
        }

        return open;
    }

    /**
     * Quem produz cada recurso.
     *
     * <p>É a cadeia produtiva de Resource-System.md: o que se tira do
     * mundo é coletado, o que se faz a partir de outro é fabricado, e o
     * que uma obra consome é erguido.
     */
    private static TaskType typeFor(ResourceType resource) {
        // Pelo grupo, e não pela categoria — 2026-08-20. A categoria
        // separa natural de processado, e isso bastava enquanto o único
        // natural era madeira: pedra, areia e lã entraram e todas caíram
        // em COLLECT_WOOD, que mandaria o lenhador buscar pedregulho.
        if (resource == ResourceType.GLASS) {
            // A exceção nominal: vidro é processado, mas não por receita
            // de bancada. Quem o faz é a fornalha.
            return TaskType.SMELT_MATERIAL;
        }

        return switch (resource.group()) {
            case WOOD -> TaskType.COLLECT_WOOD;
            case STONE, SAND -> TaskType.COLLECT_STONE;
            case WOOL -> TaskType.COLLECT_WOOL;
            case PLANKS -> TaskType.CRAFT_MATERIAL;
            case NONE -> switch (resource.category()) {
                case NATURAL -> TaskType.COLLECT_WOOD;
                case PROCESSED -> TaskType.CRAFT_MATERIAL;
                case CONSTRUCTION -> TaskType.BUILD;
            };
        };
    }
}
