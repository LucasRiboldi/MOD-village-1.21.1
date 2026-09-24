package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.construction.service.RemovalAudit;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.WaitingWork;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/** Cancela um canteiro comum quando o jogador marca sua area com uma tocha. */
public final class ConstructionCancellation {

    private ConstructionCancellation() {
    }

    /**
     * Cancela a obra comum que contem a posicao marcada.
     *
     * <p>A BigHouseMOD e uma fundacao obrigatoria e fica deliberadamente
     * fora desta regra. O projeto aberto, o projeto pendente e uma caixa
     * parcial abandonada liberam seu registro e suas tarefas para que o
     * planejador possa seguir sem carregar uma reserva morta.
     */
    public static boolean cancelAtSoulTorch(ServerWorld world, BlockPos marker) {
        if (world == null || marker == null
                || (!world.getBlockState(marker).isOf(Blocks.SOUL_TORCH)
                && !world.getBlockState(marker).isOf(Blocks.SOUL_WALL_TORCH))) {
            return false;
        }

        ColonyPos at = MinecraftTypeAdapter.toColonyPos(marker);

        for (ConstructionProject project : new ArrayList<>(VillageColonyMod.CONSTRUCTIONS.all())) {
            if (!project.state().isOpen()
                    || isBigHouse(project.blueprint().id())
                    || !Building.of(project).contains(at)) {
                continue;
            }

            cancelProject(project);
            return true;
        }

        for (ConstructionService.Pending pending :
                new ArrayList<>(VillageColonyMod.CONSTRUCTIONS.allPending())) {
            if (isBigHouse(pending.blueprint())) {
                continue;
            }

            Optional<com.villagecolony.core.construction.model.Blueprint> blueprint =
                    HousePlans.blueprintOf(world, pending.colonyId(), pending.blueprint(), pending.origin());
            if (blueprint.isEmpty()) {
                continue;
            }

            Building site = new Building(
                    pending.id(),
                    pending.colonyId(),
                    pending.blueprint(),
                    pending.origin(),
                    maxOf(pending.origin(), blueprint.get().size()),
                    false);
            if (!site.contains(at)) {
                continue;
            }

            VillageColonyMod.CONSTRUCTIONS.dropPending(pending.colonyId());
            cancelTasks(pending.colonyId());
            WaitingWork.forget(pending.id());
            VillageColonyMod.LOGGER.info(
                    "Construction {} was cancelled by a Soul Torch before resume", pending.id());
            return true;
        }

        for (Building building : new ArrayList<>(VillageColonyMod.BUILDINGS.all())) {
            if (building.finished()
                    || isBigHouse(building.blueprint())
                    || !building.contains(at)) {
                continue;
            }

            VillageColonyMod.BUILDINGS.remove(building.id());
            cancelTasks(building.colonyId());
            VillageColonyMod.LOGGER.info(
                    "Partial construction {} was cancelled by a Soul Torch", building.id());
            return true;
        }

        return false;
    }

    private static void cancelProject(ConstructionProject project) {
        VillageColonyMod.CONSTRUCTIONS.forget(project.id(), RemovalAudit.playerCancellation());
        WaitingWork.forget(project.id());
        cancelTasks(project.colonyId());
        VillageColonyMod.LOGGER.info(
                "Construction {} ({}) was cancelled by a Soul Torch",
                project.id(),
                project.blueprint().id());
    }

    private static void cancelTasks(UUID colonyId) {
        for (Task task : VillageColonyMod.TASKS.ofColony(colonyId)) {
            if (task.type() != TaskType.BUILD || !task.isOpen()) {
                continue;
            }

            task.executor().ifPresent(worker -> {
                BuilderWork.forget(worker);
                WorkTargets.clear(worker);
            });
            task.cancel();
        }
    }

    private static ColonyPos maxOf(ColonyPos origin, ColonyPos size) {
        return new ColonyPos(
                origin.x() + size.x() - 1,
                origin.y() + size.y() - 1,
                origin.z() + size.z() - 1);
    }

    private static boolean isBigHouse(com.villagecolony.core.type.ResourceId blueprint) {
        return StructureBlueprintReader.BIG_HOUSE_MOD.equals(blueprint);
    }
}
