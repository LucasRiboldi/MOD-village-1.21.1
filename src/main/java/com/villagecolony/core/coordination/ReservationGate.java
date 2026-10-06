package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.TaskType;

/**
 * A trava que uma tarefa precisa vencer antes de ser reservada — ADR-035 §4.
 *
 * <p>Qual tarefa tem trava é decisão pura e mora aqui; se a trava está aberta
 * depende do mundo e é respondido na borda Fabric
 * ({@code ColonyCycleRunner.canReserveTask}).
 */
public enum ReservationGate {

    /** Reserva livre. */
    NONE,

    /**
     * O raio já foi varrido inteiro sem achar o recurso; a tarefa espera o
     * descanso da varredura (F-1, {@code EmptySweeps}).
     */
    EMPTY_SWEEP,

    /** A obra só é reservada com ponto de apoio ao alcance do próximo bloco. */
    BUILD_SITE;

    public static ReservationGate of(TaskType type) {
        return switch (type) {
            case COLLECT_SURFACE_RESOURCE, COLLECT_SOIL, COLLECT_WOOL -> EMPTY_SWEEP;
            case BUILD -> BUILD_SITE;
            default -> NONE;
        };
    }
}
