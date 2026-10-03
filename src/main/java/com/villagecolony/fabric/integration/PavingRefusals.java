package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Por que as pontas de rua recusaram o calçamento — 2026-09-30.
 *
 * <p>O playtest de 30-09 terminou duas vezes em <i>"found no road end it
 * may pave — tried 22 of them"</i>, e a linha não dizia o que as 22 tinham
 * encontrado. {@code RoadPaving.pave} para em quatro situações diferentes —
 * degrau alto, bloco da vila gerada, bloco da colônia, chão que não é
 * natural, coisa em cima —, e cada uma pede uma correção diferente. É a
 * mesma lição de {@code LotRefusals}: contar o motivo antes de consertar.
 */
public final class PavingRefusals {

    static {
        ServerMemory.register(PavingRefusals.class, PavingRefusals::clearAll);
    }

    /** A última tentativa sem sucesso de cada colônia, para teste e diagnóstico. */
    private static final Map<UUID, Map<String, Integer>> LAST = new HashMap<>();

    private final Map<String, Integer> counts = new HashMap<>();

    void record(String reason) {
        counts.merge(reason, 1, Integer::sum);
    }

    boolean isEmpty() {
        return counts.isEmpty();
    }

    /** Os motivos do mais frequente ao menos frequente, numa linha. */
    String summary() {
        return counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> entry.getValue() + " " + entry.getKey())
                .collect(Collectors.joining("; "));
    }

    void publish(UUID colonyId) {
        LAST.put(colonyId, Map.copyOf(counts));
    }

    /** Os motivos da última vez em que nenhuma ponta desta colônia aceitou calçamento. */
    public static Map<String, Integer> lastOf(UUID colonyId) {
        return LAST.getOrDefault(colonyId, Map.of());
    }

    public static void clearAll() {
        LAST.clear();
    }
}
