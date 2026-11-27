package com.villagecolony.core.worker.service;

import com.villagecolony.core.type.ServerMemory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quanto o Zelador nivela e quantos Zeladores a vila tem — ADR-063 §3 (autor, 2026-11-25, decisão 1 🅲): o orçamento do
 * dia cresce com as colunas que o plano de alturas ainda tem a mexer, e a vila abre uma segunda vaga de Zelador quando
 * esse atraso não cai em {@value #STALL_DAYS} dias. Emenda o item 2 da ADR-059 (uma vaga por vila).
 */
public final class GradeCrew {

    /** O orçamento de um Zelador sem atraso: o mesmo da fila de quadrados que o plano substituiu. */
    public static final int BASE = 256;

    /** O teto do orçamento por dia. */
    public static final int CAP = 1_024;

    /** Uma coluna a mais no dia para cada tantas pendentes. */
    public static final int PER_PENDING = 4;

    /** Quantos dias sem o atraso cair abrem a segunda vaga. */
    public static final int STALL_DAYS = 3;

    /** O máximo de Zeladores numa vila. */
    public static final int MAX_KEEPERS = 2;

    private static final Map<UUID, List<Integer>> HISTORY = new HashMap<>();
    private static final Map<UUID, Integer> SLOTS = new HashMap<>();

    static {
        ServerMemory.register(GradeCrew.class, GradeCrew::clearAll);
    }

    private GradeCrew() {
    }

    /** Quantas colunas o Zelador nivela hoje, com {@code pending} colunas fora do alvo. */
    public static int perDay(int pending) {
        return Math.min(CAP, BASE + Math.max(0, pending) / PER_PENDING);
    }

    /** Se o atraso não caiu nos últimos {@value #STALL_DAYS} dias: o de hoje não é menor que o de três dias atrás. */
    public static boolean stalled(List<Integer> pendingByDay) {
        int size = pendingByDay.size();

        if (size <= STALL_DAYS) {
            return false;
        }

        int today = pendingByDay.get(size - 1);

        return today > 0 && today >= pendingByDay.get(size - 1 - STALL_DAYS);
    }

    /** O fim do dia desta colônia, com as colunas pendentes: decide a vaga do segundo Zelador. */
    public static synchronized int endOfDay(UUID colonyId, int pending) {
        List<Integer> history = HISTORY.computeIfAbsent(colonyId, ignored -> new ArrayList<>());

        history.add(pending);

        if (history.size() > STALL_DAYS + 1) {
            history.removeFirst();
        }

        int slots = pending == 0 ? 1 : stalled(history) ? MAX_KEEPERS : keepers(colonyId);

        SLOTS.put(colonyId, slots);

        return slots;
    }

    /** Quantas vagas de Zelador a colônia tem; sem colônia ou sem medida, uma (Regra 330). */
    public static synchronized int keepers(@Nullable UUID colonyId) {
        return colonyId == null ? 1 : SLOTS.getOrDefault(colonyId, 1);
    }

    static synchronized void clearAll() {
        HISTORY.clear();
        SLOTS.clear();
    }
}
