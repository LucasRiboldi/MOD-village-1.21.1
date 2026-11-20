package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.type.ServerMemory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * A espera por motivo — Regra 339: cada segundo de espera do {@link WorkTime} conta para o pedido que o aldeão segura
 * parado ({@code TIPO:ALVO}), ou para {@code HELP} quando ele espera numa ajuda sem pedido. A cada janela sai a linha
 * {@code VC_TIME_WAIT} com os {@value #TOP} motivos mais longos de cada ofício; o {@code time_ledger.py} os soma.
 */
final class WaitReasons {

    /** Quantos motivos cada ofício mostra por janela. */
    static final int TOP = 3;

    /** Segundos de espera por motivo, por ofício, por colônia, na janela atual. */
    private static final Map<UUID, Map<String, Map<String, Integer>>> WINDOW = new HashMap<>();

    static {
        ServerMemory.register(WaitReasons.class, WINDOW::clear);
    }

    private WaitReasons() {
    }

    /** Um segundo de espera deste aldeão. */
    static void sample(UUID colonyId, String profession, UUID id) {
        WINDOW.computeIfAbsent(colonyId, ignored -> new TreeMap<>())
                .computeIfAbsent(profession, ignored -> new HashMap<>())
                .merge(reasonOf(id), 1, Integer::sum);
    }

    /** O pedido que ele segura, ou {@code HELP}. */
    static String reasonOf(UUID id) {
        List<Task> held = VillageColonyMod.TASKS.assignedTo(id);

        if (held.isEmpty()) {
            return "HELP";
        }

        return held.get(0).type().name() + ":" + held.get(0).targetResource().name();
    }

    /** Fecha a janela: a linha de cada colônia, e a conta recomeça. */
    static void report(long ticks) {
        for (Map.Entry<UUID, Map<String, Map<String, Integer>>> colony : WINDOW.entrySet()) {
            StringBuilder line = new StringBuilder();

            colony.getValue().forEach((profession, reasons) -> line.append(' ').append(describe(profession, reasons)));

            VillageColonyMod.LOGGER.info("VC_TIME_WAIT version=1 colony={} window={}s{}",
                    colony.getKey().toString().substring(0, 8), ticks / 20, line);
        }

        WINDOW.clear();
    }

    /** {@code SMELTER[s=88 SMELT_MATERIAL:IRON_INGOT=80 SMELT_MATERIAL:GLASS=5 HELP=2]}: segundos, os mais longos. */
    static String describe(String profession, Map<String, Integer> reasons) {
        int total = reasons.values().stream().mapToInt(Integer::intValue).sum();
        StringBuilder out = new StringBuilder(profession).append("[s=").append(total);

        reasons.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP)
                .forEach(reason -> out.append(' ').append(reason.getKey()).append('=').append(reason.getValue()));

        return out.append(']').toString();
    }
}
