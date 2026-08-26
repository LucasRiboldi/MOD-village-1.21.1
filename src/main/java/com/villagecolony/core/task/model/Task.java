package com.villagecolony.core.task.model;

import com.villagecolony.core.type.Capability;
import com.villagecolony.core.type.ResourceType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Uma coisa a fazer, criada pela colônia.
 *
 * <p>Simulation-Loop.md §"Regras Importantes": a colônia nunca executa,
 * só cria tarefas; o aldeão nunca cria, só executa. Esta classe é o
 * papel que passa de uma mão para a outra.
 *
 * <p>Guarda estado e valida transição. Não escolhe executor, não decide
 * prioridade e não sabe se o recurso existe — quem decide é a colônia,
 * quem executa é o trabalhador.
 *
 * <p>A colônia e o trabalhador são referenciados por id, e não por
 * objeto: um domínio do Core não importa outro. É também por isso que a
 * tarefa exige uma {@link Capability} e não uma profissão — a capacidade
 * mora em {@code core.type}, e o domínio worker responde quem a tem.
 * Ver ADR-006 §6.
 */
public final class Task {

    private final UUID id;

    private final UUID colonyId;

    private final TaskType type;

    private final TaskPriority priority;

    private final ResourceType targetResource;

    private final int amount;

    private TaskState state;

    private UUID executorId;

    private Task(
            UUID id,
            UUID colonyId,
            TaskType type,
            TaskPriority priority,
            ResourceType targetResource,
            int amount,
            TaskState state,
            UUID executorId) {

        this.id = id;
        this.colonyId = colonyId;
        this.type = type;
        this.priority = priority;
        this.targetResource = targetResource;
        this.amount = amount;
        this.state = state;
        this.executorId = executorId;
    }

    /**
     * Cria uma tarefa disponível, sem executor.
     *
     * <p>Nasce {@link TaskState#AVAILABLE} porque criar e atribuir são
     * dois passos do loop, §5 e §6. Uma tarefa que já nascesse com dono
     * apagaria a diferença.
     *
     * @param targetResource o recurso em jogo. Uma tarefa de coleta
     *     traz madeira; uma de construção gasta tábua. Qual dos dois
     *     sentidos vale é o {@link TaskType} que diz.
     * @param amount quanto. Precisa ser positivo: uma tarefa de zero
     *     unidades ocuparia um trabalhador para nada
     */
    public static Task create(
            UUID colonyId,
            TaskType type,
            TaskPriority priority,
            ResourceType targetResource,
            int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException("Task amount must be positive: " + amount);
        }

        return new Task(
                UUID.randomUUID(),
                Objects.requireNonNull(colonyId, "colonyId"),
                Objects.requireNonNull(type, "type"),
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(targetResource, "targetResource"),
                amount,
                TaskState.AVAILABLE,
                null);
    }

    public UUID id() {
        return id;
    }

    public UUID colonyId() {
        return colonyId;
    }

    public TaskType type() {
        return type;
    }

    public TaskPriority priority() {
        return priority;
    }

    public ResourceType targetResource() {
        return targetResource;
    }

    public int amount() {
        return amount;
    }

    public TaskState state() {
        return state;
    }

    /** A capacidade que o executor precisa ter. Atalho para o tipo. */
    public Capability requiredCapability() {
        return type.required();
    }

    /** Vazio enquanto ninguém tiver assumido a tarefa. */
    public Optional<UUID> executor() {
        return Optional.ofNullable(executorId);
    }

    public boolean isOpen() {
        return state.isOpen();
    }

    /**
     * Entrega a tarefa a um trabalhador.
     *
     * <p>Simulation-Loop.md §"Uma tarefa possui apenas um executor":
     * reservar o que já está reservado é conflito, não substituição.
     * Deixar passar em silêncio poria dois aldeões a cortar a mesma
     * árvore, e cada um contaria a madeira do outro como sua.
     */
    public void reserveFor(UUID workerId) {
        Objects.requireNonNull(workerId, "workerId");

        require(TaskState.AVAILABLE, "reserve");

        this.executorId = workerId;
        this.state = TaskState.RESERVED;
    }

    /** O trabalhador chegou e começou. */
    public void start() {
        require(TaskState.RESERVED, "start");

        this.state = TaskState.EXECUTING;
    }

    /** Terminou. A colônia reavalia depois disto — §8 e §9. */
    public void complete() {
        require(TaskState.EXECUTING, "complete");

        this.state = TaskState.COMPLETED;
    }

    /**
     * Se a tarefa está na mão de alguém — reservada ou em execução.
     *
     * <p>São <b>exatamente</b> os dois estados que {@link #release()}
     * aceita, e é por isso que a pergunta mora aqui: quem for devolver
     * uma tarefa à fila precisa fazê-la antes, e ela não pode ser
     * reescrita do lado de fora.
     *
     * <p>Foi essa reescrita que derrubou o servidor em 2026-08-25. O
     * construtor guardava o {@code release} com "esta tarefa não está
     * encerrada?" — que é outra pergunta, e {@code AVAILABLE} passava
     * por ela. Um construtor morto por zumbi deixava a tarefa na fila,
     * e vinte ciclos depois a obra desistia e chamava {@code release}
     * numa tarefa que já estava lá.
     */
    public boolean isHeld() {
        return state == TaskState.RESERVED || state == TaskState.EXECUTING;
    }

    /**
     * Devolve a tarefa à fila, sem executor.
     *
     * <p>É o que acontece quando o trabalhador morre no meio: a tarefa
     * continua fazendo sentido, só perdeu quem a faria. Cancelar seria
     * errado — a colônia ainda precisa de madeira.
     */
    public void release() {
        if (!isHeld()) {
            throw new IllegalStateException(
                    "Cannot release a task that is " + state + ": " + id);
        }

        this.executorId = null;
        this.state = TaskState.AVAILABLE;
    }

    /**
     * Abandona a tarefa.
     *
     * <p>Para quando ela deixou de fazer sentido — construção removida,
     * recurso que não é mais necessário. Ver Simulation-Loop.md
     * §"Tarefas podem ser canceladas".
     *
     * <p>Uma tarefa já concluída não pode ser cancelada: o trabalho foi
     * feito, e desfazer o registro faria a colônia recontar o que já
     * entrou no baú.
     */
    public void cancel() {
        if (state == TaskState.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed task: " + id);
        }

        this.executorId = null;
        this.state = TaskState.CANCELLED;
    }

    public boolean belongsTo(UUID colonyId) {
        return this.colonyId.equals(colonyId);
    }

    private void require(TaskState expected, String action) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " a task that is " + state + ": " + id);
        }
    }

    /** Duas tarefas são a mesma quando têm o mesmo id. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Task task && id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Task[" + type
                + " " + amount + "x" + targetResource
                + ", " + priority
                + ", " + state
                + ", executor=" + (executorId == null ? "none" : executorId)
                + "]";
    }
}
