package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.brain.WorkHours;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Os desvios do encalhado — ADR-025, fase 2. Separado do {@link StrandedEscape},
 * que passaria de 500 linhas; ele pede, esta classe anda.
 */
final class StrandedDetours {

    static {
        ServerMemory.register(StrandedDetours.class, StrandedDetours::clearAll);
    }

    /** Quantos desvios uma fuga tenta quando a escada natural não existe. */
    static final int MAX_DETOURS = 3;

    private static final Map<UUID, DetourWalker> DETOURS = new HashMap<>();

    private static final Map<UUID, Integer> DETOURS_TRIED = new HashMap<>();

    private StrandedDetours() {
    }

    static boolean isWalking(UUID workerId) {
        return DETOURS.containsKey(workerId);
    }

    /**
     * Sem escada natural, o desvio que cava e põe bloco — ADR-025, fase 2.
     *
     * <p>A escada do E47 só sobe por terreno que já tem chão à frente; caverna,
     * vão e bolsão de água a derrubavam em "no natural, dry way up". O desvio
     * procura até o nível do terreno ({@link StrandedEscape#isOut}), e quando o raio não
     * basta, aceita o trecho que mais se aproxima: a passagem seguinte parte
     * de mais perto.
     */
    static boolean begin(ServerWorld world, UUID workerId, BlockPos feet, BlockPos home) {
        int tried = DETOURS_TRIED.getOrDefault(workerId, 0);

        if (tried >= MAX_DETOURS) {
            return false;
        }

        DETOURS_TRIED.put(workerId, tried + 1);

        Optional<DetourWalker> walker = DetourWalker.plan(
                world, workerId, feet, home, at -> StrandedEscape.isOut(world, at), Set.of(), true);

        if (walker.isEmpty()) {
            return false;
        }

        DETOURS.put(workerId, walker.get());

        VillageColonyMod.LOGGER.info(
                "Stranded worker {} has no natural way up at {} — taking a detour of {} steps{}",
                workerId.toString().substring(0, 8),
                feet.toShortString(),
                walker.get().steps(),
                walker.get().reachesGoal() ? " out" : " toward the surface (the way out is farther)");

        return true;
    }

    /** Um tique de cada desvio em curso. */
    static void walk(ServerWorld world) {
        if (DETOURS.isEmpty()) {
            return;
        }

        for (Iterator<Map.Entry<UUID, DetourWalker>> it = DETOURS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, DetourWalker> entry = it.next();
            UUID workerId = entry.getKey();

            if (!(world.getEntity(workerId) instanceof VillagerEntity villager) || !villager.isAlive()) {
                continue;
            }

            if (!WorkHours.isWorkTime(world, villager)) {
                continue;
            }

            DetourWalker.Status status = entry.getValue().tick(world, villager,
                    VillageColonyMod.STORAGES.of(workerId).map(WorkerStorage::chestPosition).orElse(null));

            if (status == DetourWalker.Status.WALKING) {
                continue;
            }

            it.remove();

            VillageColonyMod.LOGGER.info(
                    "Stranded worker {} {} its detour at {} — {} dug, {} placed{}",
                    workerId.toString().substring(0, 8),
                    status == DetourWalker.Status.DONE ? "finished" : "stopped",
                    villager.getBlockPos().toShortString(),
                    entry.getValue().dug(),
                    entry.getValue().placed(),
                    status == DetourWalker.Status.DONE ? "" : " (" + entry.getValue().why() + ")");
        }
    }

    static void forget(UUID workerId) {
        DETOURS.remove(workerId);
        DETOURS_TRIED.remove(workerId);
    }

    static void clearAll() {
        DETOURS.clear();
        DETOURS_TRIED.clear();
    }
}
