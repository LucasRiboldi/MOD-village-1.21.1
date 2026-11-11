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

    /**
     * O que a colônia pede além da obra: com os baús cheios, o construtor — mais mãos na obra gastam o
     * estoque (escolha do autor, 2026-10-28). Só conta para o adulto que sobra ({@code spareHand}).
     */
    private static final Map<UUID, List<ProfessionType>> SPARE = new HashMap<>();

    /** O ofício que já sobra: não ganha vaga nova enquanto sobrar (Regra 233). */
    private static final Map<UUID, List<ProfessionType>> SURPLUS = new HashMap<>();

    /** O teto de cabeças de um ofício pelo que a vila tem para ele: o pastor pelo rebanho (Regra 235). */
    private static final Map<UUID, Map<ProfessionType, Integer>> CAPS = new HashMap<>();

    /** Quantos animais da vila cada pastor pede (Regra 235, autor 2026-11-11). */
    public static final int ANIMALS_PER_SHEPHERD = 16;

    private ProfessionDemand() {
    }

    /** Quantos pastores o rebanho sustenta: 1 a cada {@value #ANIMALS_PER_SHEPHERD} animais, e nunca menos de 1 (Regra 235). */
    public static int shepherdsFor(int animals) {
        return Math.max(1, Math.max(0, animals) / ANIMALS_PER_SHEPHERD);
    }

    /** Grava o teto deste ofício nesta colônia. */
    public static synchronized void setCap(UUID colonyId, ProfessionType type, int maximum) {
        CAPS.computeIfAbsent(colonyId, ignored -> new java.util.EnumMap<>(ProfessionType.class)).put(type, maximum);
    }

    /** Se o ofício já tem as cabeças que a vila sustenta: não ganha vaga nova (Regra 235). */
    public static synchronized boolean atCap(@org.jspecify.annotations.Nullable UUID colonyId, ProfessionType type, int heads) {
        Integer maximum = colonyId == null ? null : CAPS.getOrDefault(colonyId, Map.of()).get(type);

        return maximum != null && heads >= maximum;
    }

    /** Os ofícios que sobram nesta colônia: a pedra passou do teto, e o mineiro não ganha vaga nova. */
    public static synchronized void setSurplus(UUID colonyId, List<ProfessionType> trades) {
        if (trades.isEmpty()) {
            SURPLUS.remove(colonyId);
        } else {
            SURPLUS.put(colonyId, List.copyOf(trades));
        }
    }

    public static synchronized List<ProfessionType> surplusOf(@org.jspecify.annotations.Nullable UUID colonyId) {
        return colonyId == null ? List.of() : SURPLUS.getOrDefault(colonyId, List.of());
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

    /** Liga ou desliga o pedido de mãos a mais desta colônia. */
    public static synchronized void setSpare(UUID colonyId, List<ProfessionType> trades) {
        if (trades.isEmpty()) {
            SPARE.remove(colonyId);
        } else {
            SPARE.put(colonyId, List.copyOf(trades));
        }
    }

    /** O pedido de mãos a mais desta colônia. */
    public static synchronized List<ProfessionType> spareOf(UUID colonyId) {
        return colonyId == null ? List.of() : SPARE.getOrDefault(colonyId, List.of());
    }

    /** O que esta colônia pede agora; vazio quando nada falta à obra. */
    public static synchronized List<ProfessionType> of(UUID colonyId) {
        return colonyId == null ? List.of() : DEMAND.getOrDefault(colonyId, List.of());
    }

    public static synchronized void clearAll() {
        DEMAND.clear();
        SPARE.clear();
        SURPLUS.clear();
        CAPS.clear();
    }
}
