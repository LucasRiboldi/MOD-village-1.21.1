package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.VillageFocus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A medida das travas pequenas de cru — Regra 343, fase 4 do plano de ofícios e reservas (ADR-062). Cada trava (lã,
 * farinha de osso, trigo, semente, cru da fornalha, pedregulho da mina, lingote, cru dos produtos de valor, manutenção)
 * conta no dia quantas vezes foi consultada, quantas vezes barrou o ofício e o menor estoque visto. No fim do expediente
 * sai a linha {@code VC_KEEP}; o {@code stock_report.py} diz qual trava nunca age, qual segura demais e qual está bem.
 * Nenhuma trava muda aqui: a decisão de subir, descer ou tirar é do autor, depois do jogo.
 */
public final class KeepLedger {

    private static final int CHECKS = 0;

    private static final int BARRED = 1;

    private static final int LOWEST = 2;

    private static final int LIMIT = 3;

    private static final Map<String, long[]> DAY = new TreeMap<>();

    private static long reportedDay = Long.MIN_VALUE;

    static {
        ServerMemory.register(KeepLedger.class, () -> {
            DAY.clear();
            reportedDay = Long.MIN_VALUE;
        });
    }

    private KeepLedger() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.getRegistryKey().equals(World.OVERWORLD) || world.getTime() % 100 != 0
                    || Math.floorMod(world.getTimeOfDay(), WorkClock.DAY) < WorkClock.WORK_END) {
                return;
            }

            long day = world.getTimeOfDay() / WorkClock.DAY;
            boolean working = false;

            for (Colony colony : VillageColonyMod.COLONIES.all()) {
                working |= VillageFocus.isWorking(world, colony.id());
            }

            if (working && day != reportedDay) {
                reportedDay = day;
                VillageColonyMod.LOGGER.info(line(day));
                clear();
            }
        });
    }

    /** A trava barra quando o estoque está abaixo do limite. */
    static boolean below(String lock, int have, int limit) {
        return saw(lock, have, limit, have < limit);
    }

    /** A trava barra quando o estoque não passa do limite. */
    static boolean atMost(String lock, int have, int limit) {
        return saw(lock, have, limit, have <= limit);
    }

    /** A trava barra quando o estoque já chegou ao teto. */
    static boolean atLeast(String lock, int have, int limit) {
        return saw(lock, have, limit, have >= limit);
    }

    private static boolean saw(String lock, int have, int limit, boolean barred) {
        long[] counts = DAY.computeIfAbsent(lock, ignored -> new long[] {0, 0, Long.MAX_VALUE, limit});

        counts[CHECKS]++;
        counts[BARRED] += barred ? 1 : 0;
        counts[LOWEST] = Math.min(counts[LOWEST], have);
        counts[LIMIT] = limit;

        return barred;
    }

    /** As contas do dia: trava → {consultas, barradas, menor estoque, limite}. */
    static Map<String, long[]> today() {
        return DAY;
    }

    static void clear() {
        DAY.clear();
    }

    /** {@code upkeep=36,2,1,20}: limite, consultas, barradas, menor estoque. */
    static String entry(String lock, long[] counts) {
        return lock + "=" + counts[LIMIT] + "," + counts[CHECKS] + "," + counts[BARRED] + "," + counts[LOWEST];
    }

    /** A linha {@code VC_KEEP} do dia. */
    static String line(long day) {
        List<String> entries = new ArrayList<>();

        DAY.forEach((lock, counts) -> entries.add(entry(lock, counts)));

        return "VC_KEEP version=1 day=" + day + " locks=" + entries.size()
                + (entries.isEmpty() ? "" : " " + String.join(" ", entries));
    }
}
