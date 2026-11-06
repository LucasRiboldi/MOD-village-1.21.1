package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.TreeScanner;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * A árvore da espécie que a obra pede — T1 (autor, 2026-10-29: "o lenhador derruba a espécie que a obra pede"). O
 * {@link TreeChoice} procura primeiro só a espécie da tarefa, pelas fontes de sempre; sem ela no alcance, a árvore
 * mais perto, como antes. A obra que pede uma espécie aceita outra na parede (Regra 27, madeira intercambiável).
 */
final class WantedTree {

    /** A largura de cada faixa em volta do centro da vila. */
    private static final int BAND = 32;

    private WantedTree() {
    }

    /** A árvore pelas fontes de sempre — miolo, margem, bosque e varredura — só da espécie dada. */
    static Optional<BlockPos> find(ServerWorld world, LumberjackWork.Job job, VillagerEntity villager,
            ResourceSearches.Plan search, Optional<com.villagecolony.core.colony.model.VillageBounds> zoned,
            java.util.function.Predicate<BlockPos> usable, java.util.function.Predicate<BlockPos> species) {
        java.util.function.Predicate<BlockPos> wanted = usable.and(species);

        Optional<BlockPos> tree = zoned.map(box -> byZone(world, job, villager.getBlockPos(), box, wanted))
                .orElseGet(Optional::empty);

        if (tree.isEmpty()) {
            // O bosque é reserva: árvores no miolo agora têm prioridade para liberar a vila.
            tree = VillageGrove.matureTree(job.task.colonyId(), job.center,
                    log -> species.test(log) && !TreeClaims.isTaken(log) && !TreeMarks.isRejected(world, log)
                            && !TreeMarks.isOutOfReach(world, log), world);
        }

        if (tree.isEmpty() && zoned.isEmpty()) {
            // Primeiro a árvore conhecida mais perto dele; só sem nenhuma, a varredura — ADR-038 P3a.
            tree = com.villagecolony.fabric.integration.VillageTrees.nearest(
                    world, job.task.colonyId(), villager.getBlockPos(), wanted);

            if (tree.isEmpty()) {
                tree = TreeScanner.findNearestLog(world, search.origin(), search.radius(), wanted);
                tree.ifPresent(found -> com.villagecolony.fabric.integration.VillageTrees.rememberTree(
                        job.task.colonyId(), found));
            }
        }

        return tree;
    }

    /**
     * A árvore do interior da vila primeiro; só sem ela a busca alcança a borda — Regra 198.
     *
     * <p>Faixas de {@value #BAND} blocos em volta do centro, de dentro para fora; dentro da faixa, a árvore conhecida mais
     * perto dele, sem cruzar a vila (Regra 189). Cada busca dá uma olhada em espiral, que parte do centro e só ensina uma
     * árvore nova: quem decide é a faixa.
     */
    static Optional<BlockPos> byZone(ServerWorld world, LumberjackWork.Job job, BlockPos here,
            com.villagecolony.core.colony.model.VillageBounds box,
            java.util.function.Predicate<BlockPos> wanted) {
        java.util.UUID colonyId = job.task.colonyId();
        int margin = ResourceSearches.marginOf(colonyId, com.villagecolony.core.worker.model.ProfessionType.LUMBERJACK);
        java.util.function.Predicate<BlockPos> usable = log -> com.villagecolony.core.coordination.VillageZone
                        .of(box, log.getX(), log.getZ(), margin).withinReach()
                && !TreeClaims.isTaken(log)
                && !TreeMarks.isRejected(world, log)
                && !TreeMarks.isOutOfReach(world, log)
                && wanted.test(log);
        java.util.function.Predicate<BlockPos> central = log -> box.containsColumn(log.getX(), log.getZ())
                && usable.test(log);
        int coreRadius = Math.max(box.sizeX(), box.sizeZ()) / 2;

        TreeScanner.findNearestLog(world, job.center, coreRadius, central).ifPresent(found ->
                com.villagecolony.fabric.integration.VillageTrees.rememberTree(colonyId, found));

        Optional<BlockPos> tree = innermost(world, job, here, coreRadius, central);

        if (tree.isEmpty()) {
            tree = com.villagecolony.fabric.integration.VillageTrees.nearest(world, colonyId, here, usable);
        }

        if (tree.isEmpty()) {
            tree = TreeScanner.findNearestLog(world, job.center, coreRadius + margin, usable);
            tree.ifPresent(found -> com.villagecolony.fabric.integration.VillageTrees.rememberTree(colonyId, found));
        }

        return tree;
    }

    private static Optional<BlockPos> innermost(ServerWorld world, LumberjackWork.Job job, BlockPos here, int coreRadius,
            java.util.function.Predicate<BlockPos> central) {
        for (int reach = BAND; reach < coreRadius; reach += BAND) {
            long limit = (long) reach * reach;
            Optional<BlockPos> tree = com.villagecolony.fabric.integration.VillageTrees.nearest(
                    world, job.task.colonyId(), here, log -> flatDistance(log, job.center) <= limit && central.test(log));

            if (tree.isPresent()) {
                return tree;
            }
        }

        return com.villagecolony.fabric.integration.VillageTrees.nearest(world, job.task.colonyId(), here, central);
    }

    private static long flatDistance(BlockPos log, BlockPos center) {
        long dx = (long) log.getX() - center.getX();
        long dz = (long) log.getZ() - center.getZ();

        return dx * dx + dz * dz;
    }

    /**
     * A tora da espécie que a tarefa pede — T1 (autor, 2026-10-29: o lenhador derruba a espécie que a obra pede). Tarefa
     * que não pede tora de uma espécie aceita qualquer uma.
     */
    static java.util.function.Predicate<BlockPos> species(ServerWorld world, com.villagecolony.core.task.model.Task task) {
        java.util.Optional<net.minecraft.block.Block> wanted = com.villagecolony.fabric.adapter.MinecraftTypeAdapter
                .toItem(task.targetResource())
                .map(net.minecraft.block.Block::getBlockFromItem)
                .filter(block -> block.getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.LOGS));

        return wanted.<java.util.function.Predicate<BlockPos>>map(block -> log -> world.getBlockState(log).isOf(block))
                .orElse(log -> true);
    }

}
