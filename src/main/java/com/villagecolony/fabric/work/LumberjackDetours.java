package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.Set;

/**
 * O lenhador parado no caminho tenta um desvio antes de desistir da árvore —
 * P7b, autossuficiência, 2026-10-11 (o mesmo {@link DetourWalker} do mineiro,
 * ADR-025).
 *
 * <p>No playtest de 11-10 o lenhador passou 27% do expediente parado com
 * destino: a perna da caminhada ({@code WalkLegs}) parava num barranco ou num
 * tufo de folhas, e o guarda de imobilidade largava a árvore. O desvio abre o
 * caminho cavando, e o tronco da árvore nunca é cavado.
 */
final class LumberjackDetours {

    /** Desvios por árvore antes de o guarda decidir. */
    static final int PER_TREE = 2;

    private LumberjackDetours() {
    }

    /**
     * Tenta um desvio até a perna ou o pé da árvore de agora.
     *
     * @return se o desvio começou — então quem chamou não desiste
     */
    static boolean tryDetour(ServerWorld world, VillagerEntity villager, LumberjackWork.Job job, String why) {
        if (job.plan == null) {
            return false;
        }

        BlockPos tree = job.plan.base();

        if (!tree.equals(job.detourTree)) {
            job.detourTree = tree;
            job.detours = 0;
        }

        if (job.detours >= PER_TREE) {
            return false;
        }

        job.detours++;

        BlockPos aim = WorkTargets.of(villager.getUuid())
                .filter(target -> !target.equals(tree))
                .orElse(TreeChoice.approachTo(world, tree));
        boolean leg = !aim.equals(TreeChoice.approachTo(world, tree));

        Optional<DetourWalker> walker = DetourWalker.plan(world, villager.getUuid(), villager.getBlockPos(), aim,
                feet -> leg ? feet.equals(aim) : feet.isWithinDistance(tree, LumberjackWork.REACH),
                Set.of(tree), false);

        if (walker.isEmpty()) {
            return false;
        }

        job.detour = walker.get();
        job.stall.reset();
        job.stalled = 0;

        VillageColonyMod.LOGGER.info("Lumberjack {} takes a detour of {} steps to {} — {}",
                villager.getUuid().toString().substring(0, 8), walker.get().steps(), aim.toShortString(), why);

        return true;
    }

    /**
     * Um tique do desvio em curso.
     *
     * @return se o desvio usou o tique — então quem chamou não anda nem corta
     */
    static boolean tick(ServerWorld world, VillagerEntity villager, LumberjackWork.Job job, WorkerStorage storage) {
        if (job.detour == null) {
            return false;
        }

        if (!WorkHours.isWorkTime(world, villager)) {
            return true;
        }

        DetourWalker.Status status = job.detour.tick(world, villager, storage.chestPosition());

        if (status == DetourWalker.Status.WALKING) {
            return true;
        }

        VillageColonyMod.LOGGER.info("Lumberjack {} {} its detour at {} — {} dug, {} placed",
                villager.getUuid().toString().substring(0, 8),
                status == DetourWalker.Status.DONE ? "is through" : "gave up",
                villager.getBlockPos().toShortString(), job.detour.dug(), job.detour.placed());

        job.detour = null;
        job.stall.reset();
        job.stalled = 0;

        return false;
    }
}
