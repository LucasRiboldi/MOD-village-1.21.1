package com.villagecolony.core.telemetry.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Um evento único do traço de atividade — decisão 7B, 2026-09-24.
 *
 * <p>Seis campos, e nenhum outro: identidade do trabalhador, profissão,
 * o que ele fazia, o desfecho, o motivo controlado, a categoria do alvo,
 * e um contador de progresso. Nenhuma coordenada, nenhum nome de bloco,
 * nenhum texto livre — a mesma régua de privacidade/simplicidade que
 * {@code fabric.work.ActivityLog} já segue.
 *
 * <p><b>Imutável, como todo valor do {@code core}.</b> Quem monta um
 * evento novo a cada tique não reaproveita o antigo.
 */
public record ActivityTraceEvent(
        UUID workerId,
        ActivityProfession profession,
        ActivityKind activity,
        ActivityState state,
        ControlledReason reason,
        TargetKind target,
        int progress) {

    public ActivityTraceEvent {
        Objects.requireNonNull(workerId, "workerId");
        Objects.requireNonNull(profession, "profession");
        Objects.requireNonNull(activity, "activity");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(target, "target");

        if (progress < 0) {
            throw new IllegalArgumentException("progress must not be negative");
        }
    }
}
