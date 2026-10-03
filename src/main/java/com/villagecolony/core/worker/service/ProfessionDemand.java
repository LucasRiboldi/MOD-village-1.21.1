package com.villagecolony.core.worker.service;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.UUID;

/**
 * As profissões de que a obra depende agora, por colônia — decisão do
 * autor, 2026-09-30.
 *
 * <p><i>"Focar primeiro na profissão que está dependente na demanda,
 * depois olha para a lista."</i> Até aqui a contratação só contava cabeças
 * ({@link ProfessionAssigner#mostNeeded}) e seguia a ordem fixa: uma vila
 * travada por tábua podia contratar um pastor, porque o pastor estava em
 * menor número.
 *
 * <p>Quem escreve é o ciclo da colônia, que é onde a falta da obra é
 * medida; quem lê é {@link ProfessionAssigner#vacancyFor}. A ordem é a de
 * urgência: a maior falta primeiro.
 */
public final class ProfessionDemand {

    static {
        ServerMemory.register(ProfessionDemand.class, ProfessionDemand::clearAll);
    }

    private static final Map<UUID, List<ProfessionType>> DEMAND = new HashMap<>();

    private ProfessionDemand() {
    }

    /** Substitui o que esta colônia pede, na ordem de urgência. */
    public static synchronized void set(UUID colonyId, List<ProfessionType> urgentFirst) {
        Objects.requireNonNull(colonyId, "colonyId");
        SequencedSet<ProfessionType> distinct = new LinkedHashSet<>(urgentFirst);

        if (distinct.isEmpty()) {
            DEMAND.remove(colonyId);
        } else {
            DEMAND.put(colonyId, List.copyOf(distinct));
        }
    }

    /** O que esta colônia pede agora; vazio quando nada falta à obra. */
    public static synchronized List<ProfessionType> of(UUID colonyId) {
        return colonyId == null ? List.of() : DEMAND.getOrDefault(colonyId, List.of());
    }

    public static synchronized void clearAll() {
        DEMAND.clear();
    }
}
