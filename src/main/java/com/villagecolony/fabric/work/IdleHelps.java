package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * As ajudas de quem está sem tarefa do ofício — item 8 das escolhas do autor (2026-10-27), cada ajuda com
 * um ofício fixo:
 * <ul>
 *   <li><b>o lenhador cuida do bosque</b>: vai até ele e repõe a muda que falta e põe farinha de osso,
 *       sem esperar o relógio do bosque, no máximo uma vez a cada {@link #GROVE_EVERY}.</li>
 * </ul>
 */
public final class IdleHelps {

    static {
        ServerMemory.register(IdleHelps.class, IdleHelps::clearAll);
    }

    /** Quanto ele anda até a lavoura antes de largar. */
    static final int GIVE_UP_AFTER = 600;

    /** Ajuda comum só começa depois de um turno sem tarefa do ofício. */
    static final long HELP_AFTER_IDLE = 1_200;

    private static final class Help {
        final UUID worker;
        final BlockPos target;
        int walked;

        Help(UUID worker, BlockPos target) {
            this.worker = worker;
            this.target = target;
        }
    }

    private static final Map<UUID, Long> IDLE_SINCE = new HashMap<>();

    /** De quanto em quanto tempo o lenhador sem tarefa cuida do bosque: meio minuto. */
    static final long GROVE_EVERY = 600;

    private static final Map<UUID, Help> GROVE = new HashMap<>();

    private static final Map<UUID, Long> GROVE_DONE = new HashMap<>();


    private IdleHelps() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();

            if (world.getTime() % 10 == 5) {
                tick(world);
            }
        });
    }

    /** Se este aldeão está numa ajuda agora — conta como trabalho no {@code WorkTime}. */
    public static boolean isHelping(UUID workerId) {
        return GROVE.values().stream().anyMatch(help -> help.worker.equals(workerId));
    }

    private static void tick(ServerWorld world) {
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (colony.isActive() && colony.isAttended(now)) {
                lumberjackInTheGrove(world, colony);
            }
        }
    }

    /** O lenhador sem tarefa vai ao bosque e cuida dele. */
    static void lumberjackInTheGrove(ServerWorld world, Colony colony) {
        long now = world.getTime();
        Help help = GROVE.get(colony.id());

        if (help != null && !(world.getEntity(help.worker) instanceof VillagerEntity helper && isFree(world, helper, IdleWorkers.Help.GROVE))) {
            stopGrove(colony.id());
            help = null;
        }

        if (help == null) {
            if (now - GROVE_DONE.getOrDefault(colony.id(), Long.MIN_VALUE / 2) < GROVE_EVERY) {
                return;
            }

            List<BlockPos> grove = VillageGrove.of(colony.id());
            BlockPos target = grove.isEmpty() ? MinecraftTypeAdapter.toBlockPos(colony.center()) : grove.getFirst();

            for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
                if (worker.profession().filter(ProfessionType.LUMBERJACK::equals).isPresent()
                        && world.getEntity(worker.villagerId()) instanceof VillagerEntity lumberjack
                        && isFree(world, lumberjack, IdleWorkers.Help.GROVE)) {
                    help = new Help(lumberjack.getUuid(), target);
                    GROVE.put(colony.id(), help);
                    break;
                }
            }
        }

        if (help == null || !(world.getEntity(help.worker) instanceof VillagerEntity lumberjack)) {
            return;
        }

        if (!lumberjack.getBlockPos().isWithinDistance(help.target, 4)) {
            help.walked += 10;

            if (help.walked > GIVE_UP_AFTER) {
                stopGrove(colony.id());
                GROVE_DONE.put(colony.id(), now);
                return;
            }

            WorkTargets.set(lumberjack.getUuid(), help.target.up(), 2);
            return;
        }

        lumberjack.swingHand(Hand.MAIN_HAND);
        int planted = VillageGrove.tendNow(world, colony);

        VillageColonyMod.LOGGER.info("Lumberjack {} tended the grove — {} sapling(s) planted, bone meal on the rest",
                lumberjack.getUuid().toString().substring(0, 8), planted);
        stopGrove(colony.id());
        GROVE_DONE.put(colony.id(), now);
    }

    private static void stopGrove(UUID colonyId) {
        Help help = GROVE.remove(colonyId);

        if (help != null) {
            WorkTargets.clear(help.worker);
        }
    }

    static boolean isFree(ServerWorld world, VillagerEntity villager, IdleWorkers.Help own) {
        UUID id = villager.getUuid();

        if (!IdleWorkers.isFree(world, villager, own)) {
            IDLE_SINCE.remove(id);
            return false;
        }

        long since = IDLE_SINCE.computeIfAbsent(id, ignored -> world.getTime());
        return world.getTime() - since >= HELP_AFTER_IDLE;
    }

    static void clearAll() {
        IDLE_SINCE.clear();
        GROVE.clear();
        GROVE_DONE.clear();

    }
}
