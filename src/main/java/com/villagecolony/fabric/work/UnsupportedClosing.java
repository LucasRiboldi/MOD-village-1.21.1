package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.UnsupportedRelease;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceId;

import java.util.List;

/**
 * Fecha a obra que só esperava peças sem apoio ({@link UnsupportedRelease}): larga
 * as peças, encerra a obra como a última peça encerraria, e deixa a linha
 * {@code VC_UNSUPPORTED} no log para o relatório contar.
 */
final class UnsupportedClosing {

    private UnsupportedClosing() {
    }

    /** Fecha a obra se ela só esperava as poucas peças sem apoio; diz se fechou. */
    static boolean close(ConstructionProject project) {
        List<ResourceId> released = UnsupportedRelease.release(project);

        if (released.isEmpty()) {
            return false;
        }

        if (project.state() == ConstructionState.WAITING_RESOURCES) {
            project.moveTo(ConstructionState.BUILDING);
        }

        project.moveTo(ConstructionState.COMPLETED);
        VillageColonyMod.BUILDINGS.registerOrMerge(Building.of(project, true));

        for (Task task : VillageColonyMod.TASKS.ofColony(project.colonyId())) {
            if (task.type() == TaskType.BUILD && task.isOpen()) {
                task.executor().ifPresent(BuilderWork::forget);
                task.cancel();
            }
        }

        VillageColonyMod.LOGGER.warn(
                "VC_UNSUPPORTED version=1 project={} blueprint={} pieces={} reason=nothing holds them"
                        + " after two patience windows",
                project.id(), project.blueprint().id(), released);
        VillageColonyMod.LOGGER.info(
                "Colony {} finished {} at {} — the last {} pieces had nothing to hold them, now colony infrastructure",
                project.colonyId(), project.blueprint().id(), project.origin(), released.size());

        return true;
    }
}
