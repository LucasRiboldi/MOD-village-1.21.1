package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.brain.WorkTargets;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O construtor espera a peça que falta sem largar a obra — regra do autor,
 * 2026-10-11.
 *
 * <p>Quando só restam peças postas de lado (esperando material ou apoio), o
 * construtor fica com a tarefa: a oficina já foi chamada
 * ({@code CraftsmanRequest}), a peça volta assim que o material chega aos baús, e
 * enquanto isso ele calça a rua ({@link BuilderPaving}). Antes ele soltava a
 * tarefa, e no playtest da 0.3.26 um pastor a pegou e a vila ficou três minutos
 * presa numa vidraça.
 *
 * <p>Teto de {@link #MOST_TICKS}: passado esse prazo ele solta a tarefa como
 * antes, e o ciclo a redistribui.
 */
final class BuildWait {

    /** De quanto em quanto tempo ele confere se a peça chegou. */
    static final int CHECK_EVERY = 100;

    /** Quanto tempo, no máximo, ele segura a obra parada: cinco minutos. */
    static final int MOST_TICKS = 6_000;

    /** Desde quando cada construtor espera. */
    private static final Map<UUID, Long> SINCE = new HashMap<>();

    static {
        ServerMemory.register(BuildWait.class, SINCE::clear);
    }

    private BuildWait() {
    }

    /**
     * Se o construtor segura a obra e espera a peça que falta.
     *
     * @return false quando ele deve soltá-la (é ajudante, não há peça de lado,
     *         ou passou do teto)
     */
    static boolean holds(ServerWorld world, ConstructionProject project, UUID workerId) {
        if (!HelperReach.isBuilder(workerId) || project.deferredPieces().isEmpty()) {
            return false;
        }

        long now = world.getTime();
        Long since = SINCE.putIfAbsent(workerId, now);

        if (since == null) {
            WorkTargets.clear(workerId);
            VillageColonyMod.LOGGER.info("Builder {} holds the build — {} pieces wait for material or support;"
                    + " it paves the street meanwhile", workerId.toString().substring(0, 8),
                    project.deferredPieces().size());

            return true;
        }

        if (now - since > MOST_TICKS) {
            stop(workerId);

            return false;
        }

        if (now % CHECK_EVERY == 0) {
            retryArrived(world, project);
        }

        return true;
    }

    /** As peças que esperavam material e cujo material já está nos baús voltam à fila. */
    static void retryArrived(ServerWorld world, ConstructionProject project) {
        for (ConstructionProject.DeferredPiece piece : project.deferredPieces()) {
            if (piece.reason() == SkipReason.WAITING_MATERIAL
                    && BuilderMaterials.isInTheChests(world, project, piece.block())
                    && project.retry(piece)) {
                VillageColonyMod.LOGGER.info("Project {} retries {} — the material arrived while the builder waited",
                        project.id(), piece.block());
            }
        }
    }

    /** Se o construtor está esperando peça — o calçamento o aceita mesmo com a tarefa na mão. */
    static boolean isWaiting(UUID workerId) {
        return SINCE.containsKey(workerId);
    }

    static void stop(UUID workerId) {
        SINCE.remove(workerId);
    }
}
