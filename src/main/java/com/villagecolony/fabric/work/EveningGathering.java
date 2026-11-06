package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageFocus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A reunião do fim do dia — Regra 195 (autor, 2026-11-05): com o expediente encerrado às {@link WorkClock#WORK_END}, os
 * aldeões vão ao sino da vila, ou ao centro, até {@link WorkClock#DUSK}; depois o Vanilla os leva para casa.
 *
 * <p>Só dá o destino: quem anda é o cérebro do jogo. Fica de fora quem está encalhado, em pânico, dormindo, longe demais ou
 * fundo na mina, que perderia a volta para casa tentando subir.
 */
public final class EveningGathering {

    private static final int EVERY = 100;

    /** A esta distância do ponto ele já está na reunião. */
    private static final int AT_THE_SPOT = 6;

    /** O caminho longo até a reunião não vale: à noite ele ainda precisa chegar em casa. */
    private static final int MAX_DISTANCE = 96;

    private static final int MAX_DEPTH = 8;

    private static final float SPEED = 0.5F;

    /** A última passada da janela, de onde sai a linha do dia. */
    private static final long LAST_PASS = WorkClock.DUSK - EVERY;

    private static final Map<UUID, Long> REPORTED_DAY = new HashMap<>();

    static {
        ServerMemory.register(EveningGathering.class, REPORTED_DAY::clear);
    }

    private EveningGathering() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey().equals(World.OVERWORLD) && world.getTime() % EVERY == 0
                    && WorkClock.isGatheringTime(world.getTimeOfDay())) {
                pass(world);
            }
        });
    }

    private static void pass(ServerWorld world) {
        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            if (VillageFocus.isWorking(world, colony.id())) {
                gather(world, colony);
            }
        }
    }

    private static void gather(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        int called = 0;
        int there = 0;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (!(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager) || !mayGather(villager, worker)) {
                continue;
            }

            BlockPos spot = spotOf(world, villager, center);
            BlockPos at = villager.getBlockPos();

            if (Math.abs(at.getY() - spot.getY()) > MAX_DEPTH || !at.isWithinDistance(spot, MAX_DISTANCE)) {
                continue;
            }

            called++;

            if (at.isWithinDistance(spot, AT_THE_SPOT)) {
                there++;
            } else if (!villager.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET)) {
                villager.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, SPEED, AT_THE_SPOT - 1));
            }
        }

        report(world, colony, called, there);
    }

    private static boolean mayGather(VillagerEntity villager, Worker worker) {
        return villager.isAlive() && !villager.isBaby() && !villager.isSleeping()
                && !StrandedWorkers.isStranded(worker.villagerId())
                && !villager.getBrain().hasActivity(Activity.PANIC) && !villager.getBrain().hasActivity(Activity.HIDE);
    }

    /** O sino que o aldeão conhece, quando fica na vila; senão, o centro. */
    private static BlockPos spotOf(ServerWorld world, VillagerEntity villager, BlockPos center) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.MEETING_POINT)
                .filter(point -> point.dimension().equals(world.getRegistryKey()))
                .map(point -> point.pos())
                .filter(bell -> bell.isWithinDistance(center, MAX_DISTANCE))
                .orElse(center);
    }

    private static void report(ServerWorld world, Colony colony, int called, int there) {
        long day = world.getTimeOfDay() / WorkClock.DAY;

        if (Math.floorMod(world.getTimeOfDay(), WorkClock.DAY) >= LAST_PASS
                && !Long.valueOf(day).equals(REPORTED_DAY.put(colony.id(), day))) {
            VillageColonyMod.LOGGER.info("VC_GATHER colony={} day={} there={} of={}",
                    colony.id().toString().substring(0, 8), day, there, called);
        }
    }
}
