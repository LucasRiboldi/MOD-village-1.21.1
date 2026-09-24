package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.List;
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
 *     alguma. Ver {@link Colony#observe} e
 *     {@code VillageDetector#evaluate}
 * @param anchor de onde a varredura partiu, ou {@code null} quando não
 *     se sabe. Duas varreduras da mesma âncora enxergam a mesma fatia
 *     da vila, e é isso que permite à colônia encolher
 * @param beds posições físicas observadas nesta varredura; ficam vazias
 *     quando o candidato foi criado fora do adaptador Minecraft
 */
public record VillageCandidate(
        ColonyPos center, int bedCount, boolean complete, ColonyPos anchor,
        List<ColonyPos> beds) {

    public VillageCandidate {
        Objects.requireNonNull(center, "center");

        if (bedCount <= 0) {
            throw new IllegalArgumentException("bedCount must be positive: " + bedCount);
        }

        beds = List.copyOf(Objects.requireNonNull(beds, "beds"));

        if (!beds.isEmpty() && beds.size() != bedCount) {
            throw new IllegalArgumentException(
                    "observed beds must match bedCount: " + beds.size() + " != " + bedCount);
        }
    }

    /** Observação sem as posições físicas das camas. */
    public VillageCandidate(ColonyPos center, int bedCount, boolean complete, ColonyPos anchor) {
        this(center, bedCount, complete, anchor, List.of());
    }

    /**
     * Observação sem prova de completude e sem âncora.
     *
     * <p>É o padrão seguro: cresce a colônia, não a encolhe.
     */
    public VillageCandidate(ColonyPos center, int bedCount) {
        this(center, bedCount, false, null, List.of());
    }

    /** Observação com prova geométrica, sem âncora. */
    public VillageCandidate(ColonyPos center, int bedCount, boolean complete) {
        this(center, bedCount, complete, null, List.of());
    }
}
