package com.villagecolony.core.task.service;

import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.Capability;
import com.villagecolony.core.type.ResourceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * As tarefas abertas da partida.
 *
 * <p>Chamado {@code TaskManager} em MVP-Tasks.md. A ADR-006 §5 removeu
 * <em>manager</em> como camada, então vale a ADR — mesma decisão da
 * TASK-006, onde {@code ColonyManager} virou {@code ColonyService}.
 *
 * <p>Cria, encontra e encerra tarefas. Não decide quem executa: o
 * serviço sabe qual capacidade cada tarefa exige, e quem tem essa
 * capacidade é pergunta do domínio worker. Ver ADR-006 §6 e §10 do
 * Project-State.
 *
 * <p><b>Thread safety:</b> nenhuma. Acessado apenas pela thread do
 * servidor, que é única.
 */
public final class TaskService {

    /** Ordem de inserção preservada para logs reproduzíveis. */
    private final Map<UUID, Task> tasks = new LinkedHashMap<>();

    /** Cria uma tarefa disponível e a registra. */
    public Task create(
            UUID colonyId,
            TaskType type,
            TaskPriority priority,
            ResourceType targetResource,
            int amount) {

        Task task = Task.create(colonyId, type, priority, targetResource, amount);
        tasks.put(task.id(), task);

        return task;
    }

    public Optional<Task> find(UUID taskId) {
        if (taskId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(tasks.get(taskId));
    }

    /** Todas as tarefas de uma colônia, encerradas inclusive. */
    public List<Task> ofColony(UUID colonyId) {
        Objects.requireNonNull(colonyId, "colonyId");

        List<Task> result = new ArrayList<>();

        for (Task task : tasks.values()) {
            if (task.belongsTo(colonyId)) {
                result.add(task);
            }
        }

        return List.copyOf(result);
    }

    /**
     * O que está esperando alguém, da mais urgente para a menos.
     *
     * <p>Ordenada aqui, e não por quem chama: a prioridade é regra da
     * colônia, e deixar cada chamador ordenar abriria espaço para dois
     * pontos do código escolherem tarefas em ordens diferentes.
     *
     * <p>Empate mantém a ordem de criação. Entre duas tarefas de
     * produção igualmente urgentes, a que a colônia pediu primeiro é
     * atendida primeiro — {@code sort} é estável e é disso que depende.
     */
    public List<Task> availableFor(UUID colonyId) {
        List<Task> available = new ArrayList<>();

        for (Task task : ofColony(colonyId)) {
            if (task.state() == TaskState.AVAILABLE) {
                available.add(task);
            }
        }

        available.sort((a, b) -> a.priority().compareTo(b.priority()));

        return List.copyOf(available);
    }

    /**
     * A tarefa mais urgente que exige uma dada capacidade.
     *
     * <p>É o que a colônia pergunta ao ter um trabalhador ocioso em
     * mãos: "há algo que este aqui saiba fazer?". Quem sabe se o
     * trabalhador tem a capacidade é o domínio worker; aqui só se
     * responde quais tarefas a pedem.
     */
    public Optional<Task> nextFor(UUID colonyId, Capability capability) {
        if (capability == null) {
            return Optional.empty();
        }

        for (Task task : availableFor(colonyId)) {
            if (task.requiredCapability() == capability) {
                return Optional.of(task);
            }
        }

        return Optional.empty();
    }

    /** As tarefas que um trabalhador assumiu e ainda não encerrou. */
    public List<Task> assignedTo(UUID workerId) {
        Objects.requireNonNull(workerId, "workerId");

        List<Task> result = new ArrayList<>();

        for (Task task : tasks.values()) {
            if (task.isOpen() && task.executor().filter(workerId::equals).isPresent()) {
                result.add(task);
            }
        }

        return List.copyOf(result);
    }

    /**
     * Devolve à fila tudo o que um trabalhador tinha.
     *
     * <p>Chamado quando o trabalhador some — morreu, virou zumbi. A
     * tarefa continua fazendo sentido; só perdeu quem a faria. Ver
     * {@code Task.release} e {@code VillagerLifecycleHandler}.
     *
     * @return quantas voltaram para a fila
     */
    public int releaseAllOf(UUID workerId) {
        int released = 0;

        for (Task task : assignedTo(workerId)) {
            task.release();
            released++;
        }

        return released;
    }

    /**
     * Esquece as tarefas já encerradas.
     *
     * <p>Sem isto o registro cresce para sempre: uma colônia produzindo
     * por horas acumula milhares de tarefas concluídas que ninguém mais
     * consulta. Elas não são persistidas, então a limpeza não perde
     * histórico que alguém fosse ler depois.
     *
     * @return quantas foram removidas
     */
    public int purgeClosed() {
        int before = tasks.size();

        tasks.values().removeIf(task -> !task.isOpen());

        return before - tasks.size();
    }

    /** Todas as tarefas. Somente leitura. */
    public Collection<Task> all() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    public int count() {
        return tasks.size();
    }

    public boolean remove(UUID taskId) {
        return taskId != null && tasks.remove(taskId) != null;
    }

    /** Esvazia o registro. Usado ao descarregar o mundo. */
    public void clear() {
        tasks.clear();
    }
}
