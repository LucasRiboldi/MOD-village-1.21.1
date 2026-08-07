package com.villagecolony.core.task.model;

/**
 * Em que ponto uma tarefa está.
 *
 * <p>Ver Simulation-Loop.md §5 a §8.
 *
 * <p>{@link #CANCELLED} não consta de MVP-Tasks.md, que lista quatro
 * estados. Foi acrescentado porque Simulation-Loop.md §"Tarefas podem
 * ser canceladas" exige o caso — aldeão morreu, construção removida,
 * recurso deixou de ser necessário — e sem um estado próprio a tarefa
 * cancelada teria de ser apagada. Apagar perde a diferença entre "foi
 * feita" e "deixou de fazer sentido", que é justamente o que a colônia
 * precisa saber ao reavaliar (§9).
 */
public enum TaskState {

    /** Criada, sem executor. É o que a colônia oferece. */
    AVAILABLE,

    /** Um trabalhador foi escolhido, mas ainda não começou. */
    RESERVED,

    /** Em execução pelo trabalhador. */
    EXECUTING,

    /** Terminada com sucesso. */
    COMPLETED,

    /** Abandonada antes de terminar. Não é fracasso do trabalhador. */
    CANCELLED;

    /** Se ainda há algo a fazer com esta tarefa. */
    public boolean isOpen() {
        return this != COMPLETED && this != CANCELLED;
    }
}
