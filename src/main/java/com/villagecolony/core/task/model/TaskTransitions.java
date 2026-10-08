package com.villagecolony.core.task.model;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Quem quiser saber de cada passo de uma tarefa — o diário de ações
 * ({@code ActionJournal}), pedido do autor em 2026-10-08.
 *
 * <p>Um observador só, e por padrão nenhum: o núcleo não sabe de arquivo nem
 * de relógio de jogo, só avisa que a tarefa mudou de estado e na mão de quem.
 */
public final class TaskTransitions {

    /** Uma mudança de estado; {@code worker} é quem a tinha (ou passa a ter). */
    @FunctionalInterface
    public interface Listener {
        void moved(Task task, TaskState from, TaskState to, UUID worker);
    }

    private static final Listener NONE = (task, from, to, worker) -> { };

    private static volatile Listener listener = NONE;

    private TaskTransitions() {
    }

    /** Liga o observador; {@link #stopObserving()} volta a nenhum. */
    public static void observe(Listener next) {
        listener = Objects.requireNonNull(next, "next");
    }

    public static void stopObserving() {
        listener = NONE;
    }

    /** Avisa sem deixar o observador derrubar a tarefa. */
    static void moved(Task task, TaskState from, TaskState to, @Nullable UUID worker) {
        if (worker == null) {
            return;
        }

        try {
            listener.moved(task, from, to, worker);
        } catch (RuntimeException ignored) {
            // O diário nunca derruba o jogo.
        }
    }
}
