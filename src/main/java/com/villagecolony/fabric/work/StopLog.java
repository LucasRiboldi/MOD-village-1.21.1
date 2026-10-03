package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A linha de parada, sem repetir a mesma frase a cada segundo — 2026-10-03.
 *
 * <p>Desde 30-09 o fundidor escreveu {@code stopped — none of 12 colony chests
 * had minecraft:sand} 6.391 vezes: ele reserva, não acha o cru, solta, e o
 * ciclo seguinte repete. A frase só diz algo novo quando o motivo muda, e é
 * então que ela sai — junto com quantas vezes a anterior se repetiu. A cada
 * cem repetições ela sai de novo, para a parada longa continuar visível.
 */
final class StopLog {

    static {
        ServerMemory.register(StopLog.class, StopLog::clearAll);
    }

    /** De quantas em quantas repetições a mesma frase volta ao log. */
    static final int EVERY = 100;

    private record Last(String why, int repeats) {
    }

    private static final Map<UUID, Last> LAST = new HashMap<>();

    private StopLog() {
    }

    /** Escreve a parada, se ela disser algo novo; devolve se a linha foi escrita. */
    static boolean stopped(String role, UUID workerId, String why) {
        Last last = LAST.get(workerId);

        if (last != null && last.why().equals(why)) {
            int repeats = last.repeats() + 1;

            LAST.put(workerId, new Last(why, repeats));

            if (repeats % EVERY != 0) {
                return false;
            }

            VillageColonyMod.LOGGER.info("{} {} stopped — {} (the same {} times in a row)", role, workerId, why, repeats);

            return true;
        }

        String before = last == null || last.repeats() == 0
                ? ""
                : " (the previous reason repeated " + last.repeats() + " times)";

        LAST.put(workerId, new Last(why, 0));
        VillageColonyMod.LOGGER.info("{} {} stopped — {}{}", role, workerId, why, before);

        return true;
    }

    static void clearAll() {
        LAST.clear();
    }
}
