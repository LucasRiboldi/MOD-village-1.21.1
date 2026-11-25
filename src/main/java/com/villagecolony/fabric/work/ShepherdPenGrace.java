package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Regra 355: o pastor com tosquia reservada, dentro de um cercado, está trabalhando e não preso. O socorro do curral
 * ({@link PenEscape}) espera {@link #GRACE_TICKS} desde a primeira vez que o vê cercado assim; passado o prazo, age
 * como para qualquer aldeão. Cada início e cada fim de prazo saem no log como {@code VC_PEN_WORKING}.
 */
final class ShepherdPenGrace {

    /** 30 segundos dentro do cercado com a tosquia reservada. */
    static final int GRACE_TICKS = 600;

    /** Desde quando cada pastor está cercado com a tosquia reservada. */
    private static final Map<UUID, Long> SINCE = new ConcurrentHashMap<>();

    static {
        ServerMemory.register(ShepherdPenGrace.class, SINCE::clear);
    }

    private ShepherdPenGrace() {
    }

    /** Se o socorro deve esperar: pastor cercado, com a tosquia reservada, dentro do prazo. */
    static boolean holds(ServerWorld world, MobEntity villager) {
        UUID id = villager.getUuid();
        if (!isShearing(id)) {
            SINCE.remove(id);
            return false;
        }
        long now = world.getTime();
        Long since = SINCE.putIfAbsent(id, now);
        if (since == null) {
            VillageColonyMod.LOGGER.info("VC_PEN_WORKING villager={} at={} grace={} state=started",
                    shortId(id), villager.getBlockPos().toShortString(), GRACE_TICKS);
            return true;
        }
        if (now - since <= GRACE_TICKS) {
            return true;
        }
        VillageColonyMod.LOGGER.info("VC_PEN_WORKING villager={} at={} grace={} state=expired after={}",
                shortId(id), villager.getBlockPos().toShortString(), GRACE_TICKS, now - since);
        SINCE.remove(id);
        return false;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    /** Esquece o prazo: ele saiu do cercado ou largou a tosquia. */
    static void forget(UUID villagerId) {
        SINCE.remove(villagerId);
    }

    /** Para os GameTests: o prazo começou {@code ticks} atrás. */
    static void backdate(UUID villagerId, long ticks) {
        SINCE.computeIfPresent(villagerId, (id, since) -> since - ticks);
    }

    static boolean isShearing(UUID villagerId) {
        boolean shepherd = VillageColonyMod.WORKERS.find(villagerId).flatMap(Worker::profession)
                .filter(ProfessionType.SHEPHERD::equals).isPresent();
        return shepherd && VillageColonyMod.TASKS.assignedTo(villagerId).stream()
                .anyMatch(task -> task.type() == TaskType.COLLECT_WOOL
                        && (task.state() == TaskState.RESERVED || task.state() == TaskState.EXECUTING));
    }
}
