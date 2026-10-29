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

    private WantedTree() {
    }

    /** A árvore pelas fontes de sempre — o bosque maduro, a zona, as conhecidas e a varredura — só da espécie dada. */
    static Optional<BlockPos> find(ServerWorld world, LumberjackWork.Job job, VillagerEntity villager,
            ResourceSearches.Plan search, Optional<com.villagecolony.core.colony.model.VillageBounds> zoned,
            java.util.function.Predicate<BlockPos> usable, java.util.function.Predicate<BlockPos> species) {
        java.util.function.Predicate<BlockPos> wanted = usable.and(species);

        // O bosque maduro primeiro, perto da vila (P7a); o anel de busca não vale para ele.
        Optional<BlockPos> tree = VillageGrove.matureTree(job.task.colonyId(), villager.getBlockPos(),
                log -> species.test(log) && !TreeClaims.isTaken(log) && !TreeMarks.isRejected(world, log)
                        && !TreeMarks.isOutOfReach(world, log), world);

        if (tree.isEmpty() && zoned.isPresent()) {
            tree = TreeChoice.byZone(world, job, villager, zoned.get()).filter(species);
        } else if (tree.isEmpty()) {
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
