package com.villagecolony.core.telemetry.model;

/**
 * O que o trabalhador estava fazendo — decisão 7B, 2026-09-24.
 *
 * <p>Espelha as categorias que {@code fabric.work.ActivityLog.activityFor}
 * já grava como texto solto (<code>"MINING"</code>,
 * <code>"HARVESTING"</code>, ...) desde a entrega de telemetria de
 * 2026-09-23. Vira enum aqui porque um valor pode ser testado e contado;
 * texto solto só pode ser comparado, e é assim que um motivo muda de
 * forma sem ninguém perceber — a mesma razão que fez {@code IdleReason}
 * existir.
 */
public enum ActivityKind {
    MINING,
    HARVESTING,
    CRAFTING,
    SMELTING,
    FARMING,
    SHEPHERDING,
    BUILDING,
    COORDINATION,
    UNKNOWN
}
