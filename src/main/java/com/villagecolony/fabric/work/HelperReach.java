package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O ajudante de obra (outro ofício com tarefa de obra) só fica com a peça que
 * alcança, só pega a obra quando está perto dela e desiste cedo — regras do
 * autor, 2026-10-11.
 *
 * <p>O construtor continua tentando os outros lados e tem o prazo inteiro
 * ({@link BuilderWork#STALL_LIMIT}). No playtest da 0.3.26 o pastor pegou a
 * última vidraça da casa, andou dois minutos e parou a 51 blocos dela, com a
 * vila inteira esperando aquela obra fechar.
 */
public final class HelperReach {

    /** Distância, em blocos, da obra até onde um ajudante pode pegá-la. */
    public static final int HELPER_RADIUS = 32;

    /** Tiques andando sem chegar antes de o ajudante soltar a obra: 30 s. */
    static final int HELPER_STALL = 600;

    private HelperReach() {
    }

    /**
     * Se este trabalhador pode pegar a tarefa desta obra: o construtor sempre;
     * outro ofício só a até {@link #HELPER_RADIUS} blocos do terreno. Quem não
     * está carregado no mundo não é recusado aqui — o passo da obra espera por
     * ele, como sempre.
     */
    public static boolean mayTake(ServerWorld world, Worker worker, ConstructionProject project) {
        if (isBuilder(worker.villagerId())) {
            return true;
        }

        Entity entity = world.getEntity(worker.villagerId());

        return entity == null || distanceToSite(entity.getBlockPos(), project) <= HELPER_RADIUS;
    }

    /**
     * Se este trabalhador pode <b>começar</b> a obra (a tarefa que o planejador abre):
     * só o construtor — regra do autor, 2026-10-11: <i>"o construtor sempre começa a
     * obra, podendo receber ajuda ou não"</i>. A ajuda entra depois, pela vaga de
     * ajudante ({@link BuildHelper}). Colônia sem construtor cai na regra do ajudante
     * perto ({@link #mayTake}), para a obra não ficar sem ninguém.
     */
    public static boolean mayStart(ServerWorld world, Worker worker, ConstructionProject project) {
        if (isBuilder(worker.villagerId())) {
            return true;
        }

        boolean colonyHasBuilder = VillageColonyMod.WORKERS.ofColony(worker.colonyId()).stream()
                .anyMatch(other -> other.profession().filter(ProfessionType.BUILDER::equals).isPresent());

        return !colonyHasBuilder && mayTake(world, worker, project);
    }

    /** Quantos tiques andando sem chegar este trabalhador tem antes de soltar a obra. */
    static int stallLimit(UUID workerId) {
        return isBuilder(workerId) ? BuilderWork.STALL_LIMIT : HELPER_STALL;
    }

    /**
     * O caminho até {@code spot} não alcança: o construtor tenta outro lado, o
     * ajudante solta a tarefa.
     *
     * @return o que o passo do construtor devolve (false encerra o trabalho)
     */
    static boolean gaveUp(BuilderWork.Job job, UUID workerId, BlockPos spot, BlockPos target) {
        if (!isBuilder(workerId)) {
            BuilderWork.finish(job, workerId, "a helper only takes pieces it can reach — "
                    + target.toShortString() + " is left to the builder");

            return false;
        }

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
