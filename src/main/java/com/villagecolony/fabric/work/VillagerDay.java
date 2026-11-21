package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.VillageFocus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * O dia de cada aldeão de profissão — Regra 344, telemetria T5. A cada amostra do {@link WorkTime} (uma por segundo, de
 * dia e de noite) conta os segundos no expediente, dormindo, acordado fora do expediente e sem cama, e a menor vida
 * vista. No fim do expediente sai a linha {@code VC_VILLAGER_DAY} da colônia (as 24 horas desde a anterior); a morte sai
 * na hora, com a causa, em {@code VC_VILLAGER_DEATH}.
 */
public final class VillagerDay {

    private static final int WORK = 0;

    private static final int SLEEP = 1;

    private static final int AWAKE_OFF = 2;

    private static final int NO_BED = 3;

    private static final int MIN_HEALTH = 4;

    /** Segundos por amostra: o {@link WorkTime} amostra a cada 20 tiques. */
    private static final int SECONDS = 1;

    private static final Map<UUID, int[]> DAY = new HashMap<>();

    private static final Map<UUID, Long> REPORTED_DAY = new HashMap<>();

    static {
        ServerMemory.register(VillagerDay.class, () -> {
            DAY.clear();
            REPORTED_DAY.clear();
        });
    }

    private VillagerDay() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.getRegistryKey().equals(World.OVERWORLD) || world.getTime() % 100 != 0
                    || Math.floorMod(world.getTimeOfDay(), WorkClock.DAY) < WorkClock.WORK_END) {
                return;
            }

            long day = world.getTimeOfDay() / WorkClock.DAY;

            for (Colony colony : VillageColonyMod.COLONIES.all()) {
                if (VillageFocus.isWorking(world, colony.id())
                        && !Long.valueOf(day).equals(REPORTED_DAY.put(colony.id(), day))) {
                    VillageColonyMod.LOGGER.info(line(colony.id(), day));
                    VillageColonyMod.WORKERS.ofColony(colony.id()).forEach(worker -> DAY.remove(worker.villagerId()));
                }
            }
        });
    }

    /** Uma amostra deste aldeão; {@code working} é o expediente dele. */
    static void sample(VillagerEntity villager, boolean working) {
        int[] day = DAY.computeIfAbsent(villager.getUuid(), ignored -> new int[] {0, 0, 0, 0, Integer.MAX_VALUE});

        if (working) {
            day[WORK] += SECONDS;
        } else if (villager.isSleeping()) {
            day[SLEEP] += SECONDS;
        } else {
            day[AWAKE_OFF] += SECONDS;
        }

        if (villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME).isEmpty()) {
            day[NO_BED] += SECONDS;
        }

        day[MIN_HEALTH] = Math.min(day[MIN_HEALTH], (int) Math.ceil(villager.getHealth()));
    }

    /** A morte, na hora, com a causa e o lugar. */
    public static void died(VillagerEntity villager, String cause) {
        VillageColonyMod.WORKERS.find(villager.getUuid()).ifPresent(worker -> VillageColonyMod.LOGGER.info(
                "VC_VILLAGER_DEATH version=1 colony={} worker={} prof={} cause={} at={}",
                worker.colonyId().toString().substring(0, 8), villager.getUuid().toString().substring(0, 8),
                worker.profession().map(Enum::name).orElse("NONE"), cause, villager.getBlockPos().toShortString()));
        DAY.remove(villager.getUuid());
    }

    /** {@code VC_VILLAGER_DAY}: por aldeão, {@code ofício,expediente,sono,acordado fora,sem cama,menor vida} (segundos). */
    static String line(UUID colonyId, long day) {
        List<String> entries = new ArrayList<>();
        Map<String, String> sorted = new TreeMap<>();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            int[] counts = DAY.get(worker.villagerId());

            if (counts != null) {
                sorted.put(worker.villagerId().toString().substring(0, 8), worker.profession().map(Enum::name).orElse("NONE")
                        + "," + counts[WORK] + "," + counts[SLEEP] + "," + counts[AWAKE_OFF] + "," + counts[NO_BED] + ","
                        + (counts[MIN_HEALTH] == Integer.MAX_VALUE ? 0 : counts[MIN_HEALTH]));
            }
        }

        sorted.forEach((id, value) -> entries.add(id + "=" + value));

        return "VC_VILLAGER_DAY version=1 colony=" + colonyId.toString().substring(0, 8) + " day=" + day
                + " villagers=" + entries.size() + (entries.isEmpty() ? "" : " " + String.join(" ", entries));
    }

    /** A linha do dia até agora, sem zerar a conta — para o {@code /vc salvar} (Regra 346). */
    public static String peek(java.util.UUID colonyId, long day) {
        return line(colonyId, day);
    }

    /** Para o teste. */
    static void forget(UUID villagerId) {
        DAY.remove(villagerId);
    }
}
