package com.villagecolony.fabric.work;

import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * O prazo da coleta do pastor e o fim dela sem chegar — Regra 269 (autor, 2026-11-13): o prazo é o
 * {@link ShepherdHerding#TIMEOUT} mais {@value #TICKS_PER_BLOCK} tiques por bloco do caminho (do pastor ao animal e do
 * animal ao curral ou ao poste), medido quando a coleta começa; e o animal que estava na corda fica amarrado na cerca
 * mais perto, em vez de solto.
 */
final class HerdDeadline {

    /** Tiques a mais por bloco de caminho: o animal na corda anda devagar e fica para trás. */
    static final int TICKS_PER_BLOCK = 20;

    private HerdDeadline() {
    }

    /** O prazo desta coleta, medido no primeiro passo e guardado nela. */
    static long of(ShepherdHerding.Herd herd, VillagerEntity shepherd, AnimalEntity animal) {
        if (herd.deadline == 0) {
            BlockPos goal = herd.pen != null ? herd.pen.outside() : herd.fence;

            herd.deadline = ticksFor(shepherd.getBlockPos(), animal.getBlockPos(), goal);
        }

        return herd.deadline;
    }

    static long ticksFor(BlockPos shepherd, BlockPos animal, BlockPos goal) {
        return ShepherdHerding.TIMEOUT + (long) TICKS_PER_BLOCK * (flat(shepherd, animal) + flat(animal, goal));
    }

    private static int flat(BlockPos from, BlockPos to) {
        return Math.abs(from.getX() - to.getX()) + Math.abs(from.getZ() - to.getZ());
    }

    /** Amarra o animal na cerca mais perto; sem cerca ao alcance, ele fica solto. */
    static boolean tieNearby(ServerWorld world, AnimalEntity animal) {
        Optional<BlockPos> fence = ShepherdHerding.nearestFence(world, animal.getBlockPos());

        if (fence.isEmpty()) {
            return false;
        }

        animal.attachLeash(LeashKnotEntity.getOrCreate(world, fence.get()), true);

        return true;
    }
}
