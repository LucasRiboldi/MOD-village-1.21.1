package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.PieceRescue;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A obra que só tem peças de lado não fica presa — Regra 113.
 *
 * <p>A cada ciclo da colônia, se a obra em {@link ConstructionState#BUILDING} não tem mais nada a pôr e
 * ainda não terminou, toda peça de lado volta à fila, seja qual for o motivo (material, apoio ou
 * alcance); o planejador então reabre a tarefa de obra. Depois de {@link PieceRescue#TRIES} voltas a peça
 * surge no lugar, com a linha {@code VC_SUPPLY} da exceção {@link SupplyException#STUCK_PIECE}.
 */
final class StuckPieces {

    private static final Map<UUID, PieceRescue> RESCUES = new HashMap<>();

    static {
        ServerMemory.register(StuckPieces.class, RESCUES::clear);
    }

    private StuckPieces() {
    }

    static void rescue(ServerWorld world, ConstructionProject project) {
        if (project.state() != ConstructionState.BUILDING || project.isFinished()
                || project.nextBlock().isPresent() || project.deferredPieces().isEmpty()) {
            return;
        }

        PieceRescue rescue = RESCUES.computeIfAbsent(project.id(), id -> new PieceRescue());

        for (ConstructionProject.DeferredPiece piece : project.deferredPieces()) {
            if (rescue.next(piece.position()) == PieceRescue.Verdict.SURGE) {
                UnreachablePieces.surge(world, project, piece, SupplyException.STUCK_PIECE);
            } else if (project.retry(piece)) {
                VillageColonyMod.LOGGER.info("Project {} brings back {} at {} — try {} of {}, nothing else is left",
                        project.id(), piece.block(), piece.position(), rescue.triesAt(piece.position()),
                        PieceRescue.TRIES);
            }
        }

        if (project.isFinished()) {
            RESCUES.remove(project.id());
            close(project);
        }
    }

    /** Sem peça por pôr, a obra fecha aqui: pode não haver construtor com a tarefa para fechá-la. */
    private static void close(ConstructionProject project) {
        project.moveTo(ConstructionState.COMPLETED);
        VillageColonyMod.BUILDINGS.registerOrMerge(Building.of(project, true));

        for (Task task : VillageColonyMod.TASKS.ofColony(project.colonyId())) {
            if (task.type() == TaskType.BUILD && task.isOpen()) {
                task.executor().ifPresent(BuilderWork::forget);
                task.cancel();
            }
        }

        VillageColonyMod.LOGGER.info("Colony {} finished {} at {} — the last pieces set aside came up in place"
                + " (Regra 113), now colony infrastructure", project.colonyId(), project.blueprint().id(),
                project.origin());
    }
}
