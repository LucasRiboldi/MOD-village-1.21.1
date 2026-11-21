package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.movement.DetourMoves;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.work.MinerWork.Job;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
public final class MinerDetours {

    static final int PER_TARGET = 2;

    /**
     * Os desvios por pedra em vigor — o teste do prazo (E44) os zera para medir
     * só o prazo: desde 2026-10-02 (A-5) o mineiro cria a pedra que falta, e o
     * desvio que antes falhava por falta de pedregulho agora anda.
     */
    private static int perTarget = PER_TARGET;

    /** Para a bateria: quantos desvios por pedra. */
    public static void allowDetoursPerTarget(int detours) {
        perTarget = detours;
    }

    public static void restoreDetours() {
        perTarget = PER_TARGET;
    }

    private MinerDetours() {
    }

    /**
     * Tenta um desvio até a pedra de agora.
     *
     * @return se o desvio começou — então quem chamou não desiste
     */
    static boolean tryDetour(ServerWorld world, VillagerEntity villager, Job job, String why) {
        if (job.target == null || job.detours >= perTarget) {
            return false;
        }

        job.detours++;

        BlockPos stone = job.target;
        BlockPos aim = aimFor(villager.getUuid(), stone);

        // Regra do autor de 2026-10-22: nada de piso no ar para fora da montanha (MinerTerrain), e se
        // precisar construir, desce antes de ir de lado (DetourMoves.Costs.MINER). Nos níveis do topo, nada de
        // túnel fora do desenho da mina (Regra 348).
        MineDigLimit limit = MineDigLimit.of(world, job.task.colonyId());
        Optional<DetourWalker> walker = DetourWalker.plan(
                world,
                villager.getUuid(),
                villager.getBlockPos(),
                aim,
                feet -> reached(feet, aim, stone),
                Set.of(stone),
                false,
                at -> limit.over(MinerTerrain.of(at)),
                false,
                DetourMoves.Costs.MINER);

        if (walker.isEmpty()) {
            VillageColonyMod.LOGGER.info(
                    "Miner {} found no detour to {} within {} blocks — {}",
                    villager.getUuid().toString().substring(0, 8),
                    aim.toShortString(),
                    com.villagecolony.core.movement.DetourPlanner.RADIUS,
                    why);

            return false;
        }

        job.detour = walker.get();
        job.stall.reset();
        job.lease.reset();
        job.descent.reset();
        job.stalled = 0;

        VillageColonyMod.LOGGER.info(
                "Miner {} takes a detour of {} steps to {} — {}",
                villager.getUuid().toString().substring(0, 8),
                walker.get().steps(),
                aim.toShortString(),
                why);

        return true;
    }

    /** O desvio e local: primeiro alcanca a perna atual, depois a pedra. */
    static BlockPos aimFor(UUID workerId, BlockPos stone) {
        return WorkTargets.of(workerId)
                .filter(target -> !target.equals(stone))
                .orElse(stone);
    }

    private static boolean reached(BlockPos feet, BlockPos aim, BlockPos stone) {
        if (!aim.equals(stone)) {
            return feet.equals(aim);
        }

        return !feet.equals(stone)
                && MinerReach.isWithinReach(
                        feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, stone);
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
        job.descent.reset();
        job.stalled = 0;

        boolean access = job.accessDetour;

        job.accessDetour = false;

        if (status == DetourWalker.Status.DONE) {
            VillageColonyMod.LOGGER.info(
                    "Miner {} is through its detour at {} — {} dug, {} placed",
                    villager.getUuid().toString().substring(0, 8),
                    villager.getBlockPos().toShortString(),
                    walker.dug(),
                    walker.placed());

            if (access) {
                MineAccess.dug(job.task.colonyId(), villager.getBlockPos());
            }

            return false;
        }

        if (access) {
            MineAccess.failed(job.task.colonyId(), "the tunnel stopped — " + walker.why());
        }

        MinerHands.giveUp(world, villager.getUuid(), job, "its detour failed — " + walker.why());

        return true;
    }

    /**
     * A descida que ele não alcança andando vira descida cavada (Regra 336): o alvo da descida é o próprio pé, fica
     * embaixo dele ou dentro da rocha, e ele cava o próximo degrau rumo à pedra.
     *
     * @return se a descida cavada começou
     */
    static boolean digTheWayDown(ServerWorld world, VillagerEntity villager, Job job, BlockPos leg) {
        BlockPos feet = villager.getBlockPos();

        if (!MineStepDown.cannotWalkTo(world, feet, leg)) {
            return false;
        }

        MineDigLimit limit = MineDigLimit.of(world, job.task.colonyId());
        Optional<MineStepDown> descent = MineStepDown.start(world, feet, job.target,
                MineStepDown.heading(feet, job.target, villager.getHorizontalFacing()),
                cell -> limit.mayCut(MinecraftTypeAdapter.toColonyPos(cell)));

        if (descent.isEmpty()) {
            return false;
        }

        job.stepDown = descent.get();
        WorkTargets.clear(villager.getUuid());

        VillageColonyMod.LOGGER.info(
                "Miner {} digs the way down from {} toward the stone at {} — the walk led to {}",
                villager.getUuid().toString().substring(0, 8), feet.toShortString(), job.target.toShortString(),
                leg.toShortString());

        return true;
    }

    /**
     * Um tique da descida cavada em curso. Enquanto ele cava, os guardas de travamento esperam: cavar é sair do lugar.
     *
     * @return se a descida usou o tique
     */
    static boolean stepDown(ServerWorld world, VillagerEntity villager, Job job, WorkerStorage storage) {
        if (job.stepDown == null) {
            return false;
        }

        if (!WorkHours.isWorkTime(world, villager)) {
            return true;
        }

        MineStepDown descent = job.stepDown;
        MineStepDown.Status status = descent.tick(world, villager, storage.chestPosition());

        job.stall.reset();
        job.lease.reset();
        job.descent.reset();
        job.stalled = 0;

        if (status == MineStepDown.Status.DIGGING) {
            return true;
        }

        job.stepDown = null;

        if (status == MineStepDown.Status.DONE) {
            ActionJournal.action(villager.getUuid(), "DUG_STAIRS", "stairs", descent.steps(), villager.getBlockPos(),
                    descent.dug() + " dug, " + descent.placed() + " floor");
            VillageColonyMod.LOGGER.info("Miner {} dug {} steps down to {} — {} dug, {} floor placed",
                    villager.getUuid().toString().substring(0, 8), descent.steps(),
                    villager.getBlockPos().toShortString(), descent.dug(), descent.placed());

            return false;
        }

        MinerHands.giveUp(world, villager.getUuid(), job, "its dug way down stopped — " + descent.why());

        return true;
    }
}
