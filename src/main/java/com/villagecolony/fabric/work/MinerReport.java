package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.fabric.brain.WorkHours;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A linha que o mineiro deixa no log.
 *
 * <p><b>Ela não existia, e a sessão de 2026-08-22 pagou por isso.</b>
 * Dois mineiros reivindicaram baú às 03:50:07 e passaram treze minutos
 * sem produzir uma linha sequer — nem "abriu mina", nem "pegou", nem
 * motivo de ociosidade. Zero minas no save, zero arenito nos baús, e do
 * lado de fora não havia como distinguir o mineiro que anda do mineiro
 * que está parado, do que não tem picareta, do que não achou pedra.
 *
 * <p>O lenhador ganhou a dele em 2026-08-12 e o construtor em 08-18,
 * pelo mesmo motivo e depois do mesmo tipo de sessão perdida. Esta é a
 * terceira vez que o §11 cobra a mesma conta, e a última profissão do
 * ciclo de trabalho que estava muda.
 *
 * <p>Silenciosa quando não há mineiro com trabalho: quem responde nesse
 * caso é {@code MinerWork.reportIdle}, e ele fala uma vez por motivo e
 * não uma vez por ciclo.
 */
public final class MinerReport {

    private MinerReport() {
    }

    /**
     * Uma linha por ciclo dizendo o que cada mineiro está fazendo.
     *
     * <p>As perguntas que ela precisa responder são as que a sessão de
     * 08-22 não pôde: <b>o quê</b> ele procura, <b>onde</b> está a pedra
     * de agora e a que distância, se é <b>hora</b> de trabalhar, quanto
     * já juntou do que a tarefa pede, e há quanto tempo não sai do lugar.
     *
     * @return a linha escrita, ou vazio quando não houve o que dizer.
     *     Devolver em vez de só registrar é o que dá ao teste de jogo
     *     como afirmar que a linha existe — e a ausência dela foi o
     *     defeito inteiro
     */
    public static Optional<String> report(ServerWorld world, Colony colony) {
        StringBuilder line = new StringBuilder();
        int reported = 0;

        for (Map.Entry<UUID, MinerWork.Job> entry : MinerWork.JOBS.entrySet()) {
            MinerWork.Job job = entry.getValue();

            if (!job.task.belongsTo(colony.id())) {
                continue;
            }

            if (reported++ > 0) {
                line.append("; ");
            }

            line.append(shortId(entry.getKey()))
                    .append(" ")
                    .append(describe(world, entry.getKey(), job));
        }

        if (reported == 0) {
            return Optional.empty();
        }

        VillageColonyMod.LOGGER.info("Colony {} miners: {}", colony.id(), line);

        return Optional.of(line.toString());
    }

    /**
     * O que este mineiro está fazendo, numa frase.
     *
     * <p><b>A distinção que importa</b> é entre não ter pedra e não
     * chegar nela: a primeira é a busca, a segunda é o caminho, e as
     * duas se pareciam de fora. É a mesma lição da linha do construtor,
     * que passou oito minutos "andando" sem que ninguém soubesse para
     * onde.
     */
    private static String describe(ServerWorld world, UUID workerId, MinerWork.Job job) {
        StringBuilder text = new StringBuilder();

        boolean sand = job.task.targetResource().group() == ResourceGroup.SAND;

        if (job.target == null) {
            text.append(sand ? "looking for sand" : "looking for stone");
        } else {
            text.append("digging ")
                    .append(world.getBlockState(job.target).getBlock().getName().getString())
                    .append(" at ")
                    .append(job.target.toShortString())
                    .append(", ")
                    .append(distanceOf(world, workerId, job.target))
                    .append(", ")
                    .append(job.progress)
                    .append("/")
                    .append(job.required)
                    .append(" ticks");
        }

        text.append(", ").append(hoursOf(world, workerId));

        text.append(", wants ").append(job.wanted.path());

        text.append(", ").append(job.collected).append(" of ")
                .append(job.task.amount()).append(" so far");

        text.append(", stall ").append(job.stalled).append("/").append(MinerWork.STALL_LIMIT);

        return text.toString();
    }

    /**
     * A quantos blocos o mineiro está da pedra.
     *
     * <p>"fora do mundo" quer dizer aldeão em chunk descarregado — o
     * caso em que o trabalho espera por ele e nada está errado.
     */
    private static String distanceOf(ServerWorld world, UUID workerId, BlockPos target) {
        if (!(world.getEntity(workerId) instanceof VillagerEntity villager)) {
            return "out of the world";
        }

        return (int) Math.sqrt(villager.getBlockPos().getSquaredDistance(target)) + " blocks away";
    }

    private static String hoursOf(ServerWorld world, UUID workerId) {
        if (!(world.getEntity(workerId) instanceof VillagerEntity villager)) {
            return "out of the world";
        }

        return WorkHours.isWorkTime(world, villager) ? "work time" : "off hours";
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
