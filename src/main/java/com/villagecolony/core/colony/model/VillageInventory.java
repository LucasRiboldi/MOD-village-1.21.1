package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ResourceId;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Um raio-x imutável de uma colônia — decisão 11A, 2026-09-24.
 *
 * <p><b>Observa, nunca decide.</b> Esta classe nunca chama
 * {@code ConstructionPlanner.plan}, nunca abre tarefa, nunca muda o
 * estado de nenhum registro. Ela existe para responder "como a colônia
 * está agora", de fora, sem que a pergunta em si mude a resposta da
 * próxima pergunta — é a mesma garantia que
 * {@code fabric.integration.VillageInventoryObserver.observe} prova em
 * {@code observingInventoryDoesNotChangeHouseAlternation}.
 *
 * <p><b>Uma fotografia, não um fluxo.</b> Não é serializada, não é
 * gravada no save, não sobrevive ao ciclo em que foi tirada. Quem
 * precisar de histórico usa {@code core.telemetry.model.ActivityTrace}
 * (decisão 7B); esta classe responde só "agora".
 */
public record VillageInventory(
        UUID colonyId,
        int adults,
        int beds,
        Map<ColonyProfession, Integer> professions,
        int completedBuildings,
        int activeBuildings,
        ChestCoverage chestCoverage,
        Map<ResourceId, Integer> observedResources) {

    public VillageInventory {
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(chestCoverage, "chestCoverage");
        professions = Map.copyOf(professions);
        observedResources = Map.copyOf(observedResources);

        if (adults < 0) {
            throw new IllegalArgumentException("adults must not be negative");
        }
        if (beds < 0) {
            throw new IllegalArgumentException("beds must not be negative");
        }
        if (completedBuildings < 0) {
            throw new IllegalArgumentException("completedBuildings must not be negative");
        }
        if (activeBuildings < 0) {
            throw new IllegalArgumentException("activeBuildings must not be negative");
        }
    }

    /** Quantos trabalhadores desta profissão a colônia tem agora. */
    public int countOf(ColonyProfession profession) {
        return professions.getOrDefault(profession, 0);
    }

    /** Quanto deste recurso a fotografia viu nos baús alcançados. */
    public int observedAmountOf(ResourceId resourceId) {
        return observedResources.getOrDefault(resourceId, 0);
    }
}
