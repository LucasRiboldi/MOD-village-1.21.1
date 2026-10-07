package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O pastor cuida do rebanho: faz as ovelhas procriarem — decisão do autor,
 * 2026-09-30.
 *
 * <p>Até aqui ele só tosquiava. A lã volta a crescer, mas o rebanho era o
 * que o mundo gerou: ovelha que morria ou se afastava não voltava, e sem lã
 * não há cama, que é o que faz a vila crescer.
 *
 * <p><b>Como no jogo.</b> Duas ovelhas adultas e sem espera comem um trigo
 * cada e entram no modo de acasalamento; quem faz o resto é o próprio
 * Vanilla. O trigo sai dos baús da colônia — o fazendeiro guarda uma
 * reserva crua para isso ({@link FarmerBakery#WHEAT_RESERVE}).
 *
 * <p><b>Até um rebanho de {@value #FLOCK_TARGET}</b> em volta da vila, e um
 * par a cada {@value #BETWEEN_PAIRS} tiques, que é a espera de procriação do
 * próprio jogo.
 */
public final class ShepherdFlock {

    static {
        ServerMemory.register(ShepherdFlock.class, ShepherdFlock::clearAll);
    }

    /** Ovelhas adultas que a vila mantém. */
    public static final int FLOCK_TARGET = 12;

    /** Tiques entre um par e o seguinte — cinco minutos. */
    static final int BETWEEN_PAIRS = 6_000;

    private static final int RADIUS = 32;

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private ShepherdFlock() {
    }

    /**
     * Põe um par para procriar, se a vila tem pastor, rebanho pequeno, par
     * disponível e trigo.
     *
     * @return {@code true} se um par entrou em acasalamento agora
     */
    public static boolean tend(ServerWorld world, Colony colony) {
        long now = world.getTime();
        Long last = LAST.get(colony.id());

        if (last != null && now - last < BETWEEN_PAIRS) {
            return false;
        }

        boolean hasShepherd = VillageColonyMod.WORKERS.ofColony(colony.id()).stream()
                .anyMatch(worker -> worker.profession()
                        .filter(ProfessionType.SHEPHERD::equals).isPresent());

        if (!hasShepherd) {
            return false;
        }

        LAST.put(colony.id(), now);

        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        List<SheepEntity> adults = world.getEntitiesByClass(
                SheepEntity.class, new Box(center).expand(RADIUS),
                sheep -> sheep.isAlive() && !sheep.isBaby());

        if (adults.size() >= FLOCK_TARGET) {
            return false;
        }

        List<SheepEntity> ready = new ArrayList<>();

        for (SheepEntity sheep : adults) {
            if (sheep.getBreedingAge() == 0 && !sheep.isInLove()) {
                ready.add(sheep);
            }

            if (ready.size() == 2) {
                break;
            }
        }

        if (ready.size() < 2) {
            return false;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        int wheat = ColonyChests.withdraw(world, chests, Items.WHEAT, 2);

        if (wheat < 2) {
            if (wheat > 0 && !chests.isEmpty()) {
                ChestDepositor.deposit(world, chests.getFirst(), Items.WHEAT, wheat);
            }

            VillageColonyMod.LOGGER.info(
                    "Colony {} — the shepherd has no wheat to breed the flock ({} adult sheep)",
                    colony.id().toString().substring(0, 8), adults.size());

            return false;
        }

        for (SheepEntity sheep : ready) {
            sheep.lovePlayer(null);
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} — the shepherd fed two sheep to breed ({} adult sheep, target {})",
                colony.id().toString().substring(0, 8), adults.size(), FLOCK_TARGET);

        return true;
    }

    /** O servidor fechou. */
    public static void clearAll() {
        LAST.clear();
    }
}
