package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.TerrainDefects;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.VillageFocus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A medida diária do terreno da vila — Regra 323: no fim do expediente, uma linha {@code VC_TERRAIN} por colônia com os
 * defeitos que a fila de quadrados ({@link TerrainQueue}) ainda tem na caixa inteira e as colunas que os aldeões mudaram
 * no dia. Só lê; quem arruma é a fila.
 */
public final class TerrainReport {

    /** O instante do dia em que a medida sai: o fim do expediente. */
    private static final long AT = WorkClock.WORK_END;

    private static final Map<UUID, Long> REPORTED_DAY = new HashMap<>();

    static {
        ServerMemory.register(TerrainReport.class, REPORTED_DAY::clear);
    }

    private TerrainReport() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey().equals(World.OVERWORLD) && world.getTime() % 100 == 0
                    && Math.floorMod(world.getTimeOfDay(), WorkClock.DAY) >= AT) {
                pass(world);
            }
        });
    }

    private static void pass(ServerWorld world) {
        long day = world.getTimeOfDay() / WorkClock.DAY;

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            if (VillageFocus.isWorking(world, colony.id())
                    && !Long.valueOf(day).equals(REPORTED_DAY.put(colony.id(), day))) {
                report(world, colony, day);
            }
        }
    }

    private static void report(ServerWorld world, Colony colony, long day) {
        Optional<VillageBounds> bounds = colony.bounds();

        if (bounds.isEmpty()) {
            return;
        }

        VillageBounds box = bounds.get();
        int across = (box.maxX() - box.minX()) / TerrainQueue.SQUARE + 1;
        int count = across * ((box.maxZ() - box.minZ()) / TerrainQueue.SQUARE + 1);
        int isolated = 0;
        int pathSteps = 0;
        int steps = 0;

        for (int index = 0; index < count; index++) {
            TerrainDefects.Survey survey = TerrainQueue.survey(world, colony.id(), box, across, index);

            isolated += survey.isolated().size();
            pathSteps += survey.pathSteps();
            steps += survey.steps();
        }

        VillageColonyMod.LOGGER.info("VC_TERRAIN version=1 colony={} day={} squares={} isolated={} pathSteps={} steps={} "
                        + "levelled={}", colony.id().toString().substring(0, 8), day, count, isolated, pathSteps, steps,
                TerrainQueue.levelledToday(colony.id(), day));
    }
}
