package com.villagecolony.core.colony.model;

/**
 * Quantos baús a observação alcançou — decisão 11A, 2026-09-24.
 *
 * <p>Espelha o que {@code fabric.integration.ChestInventoryReader.ChestSurvey}
 * já mede, pela mesma razão de sempre: {@code core/colony} não pode
 * importar Fabric (ADR-005/ADR-006). Quem monta esta observação traduz o
 * {@code ChestSurvey} real para este valor.
 */
public record ChestCoverage(int chestsRead, int chestsUnreachable) {

    public ChestCoverage {
        if (chestsRead < 0) {
            throw new IllegalArgumentException("chestsRead must not be negative");
        }
        if (chestsUnreachable < 0) {
            throw new IllegalArgumentException("chestsUnreachable must not be negative");
        }
    }

    /** Se algum baú reconhecido ficou fora do alcance desta fotografia. */
    public boolean isPartial() {
        return chestsUnreachable > 0;
    }

    /** Quantos baús a colônia conhece: os lidos mais os que faltaram. */
    public int chestsKnown() {
        return chestsRead + chestsUnreachable;
    }
}
