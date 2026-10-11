package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O ajudante de obra só fica com a peça que alcança — regra do autor,
 * 2026-10-11. No playtest de 11-10 o pastor ajudando a obra desistiu 33 vezes
 * de um lugar de pé que o caminho não alcançava, tentando lado após lado: 26%
 * do expediente bloqueado. O construtor continua tentando os outros lados; o
 * ajudante (outro ofício com tarefa de obra) larga a peça para o construtor.
 */
final class HelperReach {

    private HelperReach() {
    }

    /**
     * O caminho até {@code spot} não alcança: o construtor tenta outro lado, o
     * ajudante solta a tarefa.
     *
     * @return o que o passo do construtor devolve (false encerra o trabalho)
     */
    static boolean gaveUp(BuilderWork.Job job, UUID workerId, BlockPos spot, BlockPos target) {
        boolean helper = VillageColonyMod.WORKERS.find(workerId)
                .flatMap(Worker::profession)
                .filter(ProfessionType.BUILDER::equals)
                .isEmpty();

        if (helper) {
            BuilderWork.finish(job, workerId, "a helper only takes pieces it can reach — "
                    + target.toShortString() + " is left to the builder");

            return false;
        }

        VillageColonyMod.LOGGER.info("Builder {} gives up standing at {} for {} — the path does not"
                + " reach it; it tries another side", workerId, spot.toShortString(), target.toShortString());
        job.stall.reset();

        return true;
    }
}
