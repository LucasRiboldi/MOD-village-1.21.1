package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.work.MinerWork.Job;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.Set;

/**
 * O mineiro que não chega cava o próprio caminho — ADR-025, fase 2.
 *
 * <p>Antes, os três guardas do {@code MinerSteps} (parado, sem se aproximar,
 * andando demais) levavam direto ao {@code giveUp}: a pedra ganhava marca, o
 * mineiro largava a tarefa, e se estava preso, ficava. Agora o primeiro
 * tropeço pede um desvio ao {@code DetourPlanner}: até algum lugar de onde a
 * picareta alcance a pedra, cavando rocha e pondo pedregulho onde falta chão.
 * Só quando nem o desvio chega é que a desistência de sempre vale.
 *
 * <p>Dois desvios por pedra, no máximo: o segundo cobre o mundo que mudou
 * durante o primeiro; um terceiro seria insistir na pedra que o mundo recusa,
 * e para isso já existe a marca do E44.
 */
final class MinerDetours {

    static final int PER_TARGET = 2;

    private MinerDetours() {
    }

    /**
     * Tenta um desvio até a pedra de agora.
     *
     * @return se o desvio começou — então quem chamou não desiste
     */
    static boolean tryDetour(ServerWorld world, VillagerEntity villager, Job job, String why) {
        if (job.target == null || job.detours >= PER_TARGET) {
            return false;
        }

        job.detours++;

        BlockPos stone = job.target;

        Optional<DetourWalker> walker = DetourWalker.plan(
                world,
                villager.getUuid(),
                villager.getBlockPos(),
                stone,
                feet -> !feet.equals(stone)
                        && MinerReach.isWithinReach(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, stone),
                Set.of(stone),
                false);

        if (walker.isEmpty()) {
            VillageColonyMod.LOGGER.info(
                    "Miner {} found no detour to {} within {} blocks — {}",
                    villager.getUuid().toString().substring(0, 8),
                    stone.toShortString(),
                    com.villagecolony.core.movement.DetourPlanner.RADIUS,
                    why);

            return false;
        }

        job.detour = walker.get();
        job.stall.reset();
        job.lease.reset();
        job.stalled = 0;

        VillageColonyMod.LOGGER.info(
                "Miner {} takes a detour of {} steps to {} — {}",
                villager.getUuid().toString().substring(0, 8),
                walker.get().steps(),
                stone.toShortString(),
                why);

        return true;
    }

    /**
     * Um tique do desvio em curso.
     *
     * @return se o desvio usou o tique — então quem chamou não anda nem cava
     */
    static boolean tick(ServerWorld world, VillagerEntity villager, Job job, WorkerStorage storage) {
        if (job.detour == null) {
            return false;
        }

        if (!WorkHours.isWorkTime(world, villager)) {
            return true;
        }

        DetourWalker walker = job.detour;
        DetourWalker.Status status = walker.tick(world, villager, storage.chestPosition());

        if (status == DetourWalker.Status.WALKING) {
            return true;
        }

        job.detour = null;
        job.stall.reset();
        job.lease.reset();
        job.stalled = 0;

        if (status == DetourWalker.Status.DONE) {
            VillageColonyMod.LOGGER.info(
                    "Miner {} is through its detour at {} — {} dug, {} placed",
                    villager.getUuid().toString().substring(0, 8),
                    villager.getBlockPos().toShortString(),
                    walker.dug(),
                    walker.placed());

            return false;
        }

        MinerHands.giveUp(world, villager.getUuid(), job, "its detour failed — " + walker.why());

        return true;
    }
}
