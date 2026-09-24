package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.type.Capability;
import com.villagecolony.core.worker.model.Worker;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/** Regras que tornam um trabalhador elegivel para reservar uma tarefa. */
final class WorkEligibility {

    private WorkEligibility() {
    }

    /**
     * Descanso e baú próprio são requisitos de reserva, não preferências
     * da passagem que está distribuindo a fila.
     *
     * <p><b>E estar solto também — E47, 2026-09-24.</b> O encalhado não
     * reserva nada, em nenhuma capacidade: mandar quem está preso num buraco
     * construir do outro lado da vila só gera desistência, e desistência
     * acumulada tira o ofício dele.
     */
    static boolean canReserve(
            Worker worker,
            Capability capability,
            Task task,
            Predicate<UUID> hasStorage) {

        Objects.requireNonNull(worker, "worker");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(hasStorage, "hasStorage");

        return !worker.isStranded()
                && !worker.isResting(capability)
                && (!task.type().needsOwnStorage() || hasStorage.test(worker.villagerId()));
    }
}
