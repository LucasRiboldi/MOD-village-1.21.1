package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.VillageFocus;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * À noite, quem está preso longe da cama vai para ela — ADR-036 item 15, Regra 142 (autor 2026-10-28).
 *
 * <p>Na taiga simulada (10 dias, 0.3.102) todo aldeão era teleportado toda noite: a busca do jogo para em
 * 48 blocos e 768 nós, e quem dormia a mais que isso nunca tinha caminho — 66 dos 114 teleportes — e a
 * conferência teleportava na primeira volta, antes de o aldeão dar um passo. Agora a busca vai a
 * {@link #HOME_RANGE} blocos com {@link #NODE_MULTIPLIER}× os nós, e sem caminho o aldeão só vai à cama se
 * ficou parado (menos de {@link #MOVED} blocos) em {@link #STILL_CHECKS} conferências seguidas. Ver
 * {@link BedTeleport}.
 */
final class NightHome {

    static {
        ServerMemory.register(NightHome.class, NightHome::clearAll);
    }

    /** De quanto em quanto tempo cada aldeão tem o caminho de casa conferido. */
    static final int CHECK_EVERY = 600;

    /** Já está em casa: a esta distância da cama não se procura caminho. */
    private static final int AT_HOME = 2;

    /** Até onde a busca do caminho de casa vai — o jogo dá 48 ao aldeão. */
    static final double HOME_RANGE = 128;

    /** Quantas vezes os nós da busca do jogo a busca de casa pode visitar. */
    static final float NODE_MULTIPLIER = 4;

    /** Andou ao menos isto desde a conferência anterior: está indo, não está preso. */
    static final int MOVED = 3;

    /** Quantas conferências seguidas parado e sem caminho levam à cama. */
    static final int STILL_CHECKS = 2;

    private static final Map<UUID, Long> NEXT_CHECK = new HashMap<>();

    /** Onde cada aldeão sem caminho estava na conferência anterior, e quantas seguidas ficou ali. */
    private static final Map<UUID, Still> STILL = new HashMap<>();

    private record Still(BlockPos at, int checks) {
    }

    private NightHome() {
    }

    static void clearAll() {
        NEXT_CHECK.clear();
        STILL.clear();
    }

    /** Uma passagem por todos os trabalhadores das colônias que trabalham. */
    static void pass(ServerWorld world) {
        if (!WorkClock.isRestTime(world.getTimeOfDay())) {
            STILL.clear();

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

    /** Confere um aldeão: sem caminho até a cama e parado, ele vai para ela. */
    static boolean check(ServerWorld world, VillagerEntity villager, UUID workerId) {
        Optional<BlockPos> bed = BedTeleport.bedOf(world, villager);

        if (bed.isEmpty() || villager.getBlockPos().isWithinDistance(bed.get(), AT_HOME)) {
            STILL.remove(workerId);

            return false;
        }

        if (hasPathHome(villager, bed.get())) {
            STILL.remove(workerId);

            return false;
        }

        BlockPos here = villager.getBlockPos();
        Still before = STILL.get(workerId);
        int checks = before != null && before.at().isWithinDistance(here, MOVED) ? before.checks() + 1 : 1;

        if (checks < STILL_CHECKS) {
            // Sem caminho inteiro, mas talvez andando: o cérebro do jogo vai por trechos. Espera a próxima.
            STILL.put(workerId, new Still(here, checks));

            return false;
        }

        STILL.remove(workerId);

        return BedTeleport.toBed(world, villager, workerId, "stuck with no path home at night");
    }

    /** O caminho até a cama, buscado até {@link #HOME_RANGE} blocos; o atributo e os nós voltam ao normal. */
    private static boolean hasPathHome(VillagerEntity villager, BlockPos bed) {
        EntityAttributeInstance follow = villager.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
        double usual = follow == null ? 0 : follow.getBaseValue();

        try {
            if (follow != null) {
                follow.setBaseValue(Math.max(usual, HOME_RANGE));
            }
            villager.getNavigation().setRangeMultiplier(NODE_MULTIPLIER);

            Path path = villager.getNavigation().findPathTo(bed, 1);

            return path != null && path.reachesTarget();
        } finally {
            if (follow != null) {
                follow.setBaseValue(usual);
            }
            villager.getNavigation().resetRangeMultiplier();
        }
    }
}
