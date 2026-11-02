package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * A obra é exclusiva do construtor. Ele tenta os outros lados e tem o prazo
 * inteiro ({@link BuilderWork#STALL_LIMIT}).
 */
public final class HelperReach {

    /** Legado de persistência de tarefa; novos ajudantes de obra não existem. */
    public static final int HELPER_RADIUS = 32;

    /** Legado de persistência de tarefa; novos ajudantes de obra não existem. */
    static final int HELPER_STALL = 600;

    private HelperReach() {
    }

    /**
     * Só o construtor pode pegar tarefa de obra.
     */
    public static boolean mayTake(ServerWorld world, Worker worker, ConstructionProject project) {
        return isBuilder(worker.villagerId());
    }

    /**
     * Só o construtor pode começar uma obra. Sem construtor, o planejador não abre obra.
     */
    public static boolean mayStart(ServerWorld world, Worker worker, ConstructionProject project) {
        return isBuilder(worker.villagerId());
    }

    /** Quantos tiques andando sem chegar este trabalhador tem antes de soltar a obra. */
    static int stallLimit(UUID workerId) {
        return isBuilder(workerId) ? BuilderWork.STALL_LIMIT : HELPER_STALL;
    }

    /** O caminho até {@code spot} não alcança: o construtor tenta outro lado. */
    static boolean gaveUp(BuilderWork.Job job, UUID workerId, BlockPos spot, BlockPos target) {
        VillageColonyMod.LOGGER.info("Builder {} gives up standing at {} for {} — the path does not"
                + " reach it; it tries another side", workerId, spot.toShortString(), target.toShortString());
        job.stall.reset();
        // E se ele mora abaixo da obra, acaba mudando de casa para a altura dela (HomeMove).
        HomeMove.gaveUpOnTheWayTo(workerId, target);

        return true;
    }

    static boolean isBuilder(UUID workerId) {
        return VillageColonyMod.WORKERS.find(workerId)
                .flatMap(Worker::profession)
                .filter(ProfessionType.BUILDER::equals)
                .isPresent();
    }

    /** Distância horizontal, em blocos, até a caixa do terreno (zero dentro dela). */
    static int distanceToSite(BlockPos at, ConstructionProject project) {
        ColonyPos origin = project.origin();
        ColonyPos size = project.blueprint().size();
        int dx = outside(at.getX(), origin.x(), origin.x() + size.x() - 1);
        int dz = outside(at.getZ(), origin.z(), origin.z() + size.z() - 1);

        return Math.max(dx, dz);
    }

    private static int outside(int value, int low, int high) {
        return value < low ? low - value : Math.max(0, value - high);
    }
}
