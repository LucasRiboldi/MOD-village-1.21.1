package com.villagecolony.core.telemetry.model;

/**
 * O motivo controlado de um evento de traço — decisão 7B, 2026-09-24.
 *
 * <p>Espelha {@code core.coordination.IdleReason}, com o mesmo motivo que
 * levou {@link ActivityProfession} a espelhar {@code ProfessionType}:
 * {@code DependencyRuleTest.coreDomainsDoNotImportTheCoordinationLayer}
 * proíbe qualquer domínio do {@code core} — inclusive {@code telemetry} —
 * de importar {@code core/coordination}, mesmo sendo ela quem pode
 * importar todos os outros. A exceção da ADR-006 §6 vale numa direção só.
 *
 * <p>Ganha {@link #WORK_STALLED}, que {@code IdleReason} não tem: é o
 * motivo que {@code ActivityLog.abandoned}/{@code failed} já usam como
 * string fixa, fora do vocabulário de espera.
 */
public enum ControlledReason {
    NO_TASK,
    NO_EXECUTOR,
    ALREADY_OPEN,
    NO_WORKER,
    NO_STORAGE,
    STORAGE_FULL,
    NO_TARGET,
    SWEEP_INCOMPLETE,
    NOT_IN_GAME,
    MISSING_MATERIAL,
    COUNT_PARTIAL,

    /** O guarda de travamento fechou a tarefa por falta de progresso. */
    WORK_STALLED,

    /** Nenhum motivo controlado — o estado é {@link ActivityState#IDLE}. */
    NONE,

    UNKNOWN
}
