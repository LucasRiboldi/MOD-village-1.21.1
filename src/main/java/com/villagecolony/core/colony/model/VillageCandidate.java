package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Objects;

/**
 * Um aglomerado de camas que passou na validação de vila.
 *
 * <p>Ainda não é uma {@link Colony}: é o resultado da detecção, esperando
 * que alguém decida criar uma colônia nova ou atualizar uma existente.
 * Essa decisão é da TASK-010.
 *
 * @param center posição central; o sino tem prioridade sobre a média das
 *     camas, por ser o centro social real da vila Vanilla (ADR-003 §4)
 * @param bedCount camas do cluster
 * @param complete se esta observação provadamente não cortou cama
 *     alguma. Só ela autoriza a colônia a encolher — ver
 *     {@link Colony#observe} e {@code VillageDetector#evaluate}
 */
public record VillageCandidate(ColonyPos center, int bedCount, boolean complete) {

    public VillageCandidate {
        Objects.requireNonNull(center, "center");

        if (bedCount <= 0) {
            throw new IllegalArgumentException("bedCount must be positive: " + bedCount);
        }
    }

    /**
     * Observação sem prova de completude.
     *
     * <p>É o padrão seguro: cresce a colônia, não a encolhe. Quem sabe
     * de onde olhou usa o construtor de três argumentos.
     */
    public VillageCandidate(ColonyPos center, int bedCount) {
        this(center, bedCount, false);
    }
}
