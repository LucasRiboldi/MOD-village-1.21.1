package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.VillageFocus;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * À noite, quem não acha caminho até a cama vai para ela — ADR-036 item 15.
 * Ver {@link BedTeleport}.
 */
final class NightHome {

    static {
        ServerMemory.register(NightHome.class, NightHome::clearAll);
    }

    /** De quanto em quanto tempo cada aldeão tem o caminho de casa conferido. */
    static final int CHECK_EVERY = 600;

    /** Já está em casa: a esta distância da cama não se procura caminho. */
    private static final int AT_HOME = 2;

    private static final Map<UUID, Long> NEXT_CHECK = new HashMap<>();

    private NightHome() {
    }

    static void clearAll() {
        NEXT_CHECK.clear();
    }

    /** Uma passagem por todos os trabalhadores das colônias que trabalham. */
    static void pass(ServerWorld world) {
        if (!WorkClock.isRestTime(world.getTimeOfDay())) {
            return;
        }

        long now = world.getTime();

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            if (!VillageFocus.isWorking(world, colony.id())) {
                continue;
            }

            for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
                UUID id = worker.villagerId();

                // Quem deitou fecha a porta da casa — ADR-037 V2.
                if (world.getEntity(id) instanceof VillagerEntity sleeper && sleeper.isSleeping()) {
                    sleeper.getSleepingPosition()
                            .ifPresent(bed -> HouseDoors.closeOnce(world, sleeper, bed));
                }

                if (now < NEXT_CHECK.getOrDefault(id, Long.MIN_VALUE)
                        || !(world.getEntity(id) instanceof VillagerEntity villager)
                        || !villager.isAlive() || villager.isSleeping()) {
                    continue;
                }

                NEXT_CHECK.put(id, now + CHECK_EVERY);
                check(world, villager, id);
            }
        }
    }

    /** Confere um aldeão: sem caminho até a cama, ele vai para ela. */
    static boolean check(ServerWorld world, VillagerEntity villager, UUID workerId) {
        Optional<BlockPos> bed = BedTeleport.bedOf(world, villager);

        if (bed.isEmpty() || villager.getBlockPos().isWithinDistance(bed.get(), AT_HOME)) {
            return false;
        }

        Path path = villager.getNavigation().findPathTo(bed.get(), 1);

        if (path != null && path.reachesTarget()) {
            return false;
        }

        return BedTeleport.toBed(world, villager, workerId, "no path home at night");
    }
}
