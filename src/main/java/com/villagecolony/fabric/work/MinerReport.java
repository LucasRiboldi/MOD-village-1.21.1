package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
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
                    .append(LentHand.mark(
                            VillageColonyMod.WORKERS.find(entry.getKey())
                                    .flatMap(Worker::profession),
                            ProfessionType.MINER))
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
     * Por que este mineiro está sem pedra — 2026-08-28.
     *
     * <p><b>"Procurando" e "barrado" não são a mesma coisa</b>, e a
     * linha as escrevia igual. A mina é de um mineiro só desde 08-28, e
     * o segundo sai de {@code nextTarget} sem alvo: uma sessão inteira
     * dele "looking for stone" mandaria o autor investigar a busca, que
     * está certa.
     *
     * <p>É o E31 aplicado antes de custar sessão — relatório que afirma
     * o que não mediu é pior que relatório que cala. Ver
     * {@link MineClaims}.
     */
    private static String waitingFor(MinerWork.Job job, UUID workerId, boolean sand) {
        if (sand) {
            // A areia não passa pela mina: cada um tem seu cursor de
            // espiral, e dois catando duna não se atrapalham.
            return "looking for sand";
        }

        Optional<UUID> digger = MineClaims.diggerIn(job.task.colonyId());

        if (digger.isPresent() && !digger.get().equals(workerId)) {
            return "waiting for the shaft — " + shortId(digger.get()) + " is in it";
        }

        return "looking for stone";
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
            text.append(waitingFor(job, workerId, sand));
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

        // E há quantos tiques ele não sai do bloco — 2026-09-03. Os dois
        // contadores lado a lado separam as duas frases que a sessão
        // precisa distinguir e que "stall" sozinho confundia: <i>andando
        // devagar</i> tem "still" perto de zero; <i>travado</i> tem os
        // dois subindo juntos. Toda sessão registrada era o segundo caso,
        // e o log não sabia dizer.
        text.append(", still ").append(job.still).append("/").append(MinerWork.STILL_LIMIT);

        return text.toString();
    }

    /**
     * A quantos blocos o mineiro está da pedra, e se dali ele alcança.
     *
     * <p>"fora do mundo" quer dizer aldeão em chunk descarregado — o
     * caso em que o trabalho espera por ele e nada está errado.
     *
     * <p><b>A mesma conta do alcance desde 2026-08-27</b>, e com uma
     * casa decimal. Media-se com {@code getBlockPos()}, inteiro, e ainda
     * se truncava a raiz — qualquer distância entre 4,0 e 4,99 saía como
     * <i>"4 blocks away"</i>, e o alcance é 4. A sessão das 22:19 passou
     * dois mil e quatrocentos tiques dizendo que o mineiro estava a
     * quatro blocos da pedra que ele não alcançava.
     *
     * <p>O "out of reach" é dito por extenso porque é a diferença que
     * importa: parado perto e parado longe têm correções diferentes.
     */
    private static String distanceOf(ServerWorld world, UUID workerId, BlockPos target) {
        if (!(world.getEntity(workerId) instanceof VillagerEntity villager)) {
            return "out of the world";
        }

        double away = MinerWork.distanceTo(villager, target);

        if (away <= MinerWork.REACH) {
            return String.format("%.1f blocks away", away);
        }

        // Fora de alcance: onde ele está e para onde foi mandado entram
        // na linha — 2026-08-27.
        //
        // Estavam só na frase de desistência, e ela sai depois de 2400
        // tiques de expediente. A sessão das 23:18 durou três minutos, o
        // guarda parou em 1177, e a sessão inteira passou sem que a única
        // linha capaz de responder chegasse a ser escrita. O estado que
        // interessa é o do travamento, não o do fim dele.
        return String.format("%.1f blocks away (out of reach", away)
                + ", he is at " + villager.getBlockPos().toShortString()
                + trapped(villager, workerId)
                + ", walking to " + sentTo(workerId, target)
                + ")";
    }

    /**
     * Se ele está <b>abaixo</b> do destino, e por quanto — 2026-08-29.
     *
     * <p><b>A sessão das 04:40.</b> O mineiro passou seis minutos com a
     * mesma linha, e ela dizia só <i>"out of reach"</i>:
     *
     * <pre>
     * he is at 757, 42, 877, 5,4 blocks away;
     * it was walking to 758, 44, 878;
     * </pre>
     *
     * <p>Oito leituras, todas na <b>mesma posição</b>, sem andar um
     * bloco. E o número que respondia estava ali sem ser lido: y=42
     * contra y=44 do lugar de ficar de pé. <b>Dois blocos abaixo</b>, e
     * aldeão sobe um. Ele não estava longe nem perdido — estava num poço.
     *
     * <p>"Fora de alcance" cobre três coisas com correções diferentes:
     * longe demais, caminho que a navegação não traça, e <b>preso</b>. A
     * terceira passou a ter nome, e é a que o log não sabia dizer.
     *
     * <p>Um bloco de diferença não é nada: o aldeão sobe um degrau. Do
     * segundo em diante ele não sobe, e nenhuma correção de destino o
     * tira de lá.
     */
    private static String trapped(VillagerEntity villager, UUID workerId) {
        Optional<BlockPos> destination = WorkTargets.of(workerId);

        if (destination.isEmpty()) {
            return "";
        }

        int below = destination.get().getY() - villager.getBlockPos().getY();

        return below < 2 ? "" : ", " + below + " blocks below it and unable to climb";
    }

    /**
     * Para onde este mineiro foi mandado — lido, e não recalculado.
     *
     * <p><b>A linha recomputava o destino</b>, e é a mesma família do
     * E30 e do E31: instrumento que reporta o que recalculou em vez do
     * que aconteceu. Ela chamava {@code approachTo} de novo na hora de
     * escrever e imprimia <b>esse</b> resultado.
     *
     * <p>Enquanto os dois coincidem ninguém percebe. Eles deixam de
     * coincidir exatamente no caso que interessa: quando o
     * {@link MinerReach#legTowards} manda o mineiro à <b>boca da mina</b>
     * porque a pedra está longe demais para a navegação traçar um
     * caminho. A sessão de 2026-08-28 saiu com o segundo mineiro parado
     * na superfície, <i>"walking to 758, 44, 878"</i>, sem que desse
     * para saber se a perna tinha sequer disparado — e essa é a pergunta
     * do E35.
     *
     * <p>Sai mais barato junto: {@code approachTo} são umas seiscentas
     * leituras de bloco, gastas uma vez por mineiro por ciclo para
     * reimprimir um dado que já estava guardado.
     */
    private static String sentTo(UUID workerId, BlockPos target) {
        Optional<BlockPos> destination = WorkTargets.of(workerId);

        if (destination.isEmpty()) {
            // Sem destino ele é Vanilla: a task nem começa. É estado
            // legítimo — fora do expediente, ou tarefa recém-solta.
            return "nowhere";
        }

        if (destination.get().equals(target)) {
            // O approachTo não achou lugar de ficar de pé e devolveu a
            // própria pedra, que a navegação nunca cumpre.
            return "the stone itself";
        }

        return MinerWork.mouthFor(workerId)
                .filter(mouth -> mouth.equals(destination.get()))
                .map(mouth -> "the mine mouth at " + mouth.toShortString())
                .orElseGet(() -> destination.get().toShortString());
    }

    /**
     * Por que o mineiro não chegou, dito em uma frase — 2026-08-27.
     *
     * <p>É o molde do {@code BuilderApproach.whyNotReached}, e existe
     * pela mesma razão: <i>"não chegou"</i> tanto pode ser aldeão longe
     * demais para a navegação, destino que a navegação não cumpre, túnel
     * alagado, ou aldeão do outro lado de uma parede — e as quatro têm
     * correções diferentes.
     *
     * <p><b>Duas sessões seguidas sem um bloco cavado</b> pediram isto.
     * O relatório dizia a distância — 7,9 numa, 21,5 na outra, sempre
     * congeladas — e distância sozinha não escolhe entre as quatro. As
     * três coisas que escolhem são <b>onde ele está</b>, <b>para onde
     * foi mandado</b> e <b>o que há lá</b>.
     *
     * <p>A frase mais importante é <i>"the stone itself"</i>: quer dizer
     * que o {@code approachTo} não achou lugar de ficar de pé e mandou o
     * aldeão para dentro da rocha, que a navegação nunca cumpre.
     */
    public static String whyNotReached(
            ServerWorld world, VillagerEntity villager, BlockPos target) {

        // <b>Duas perguntas, e elas não são a mesma</b> — 2026-08-29.
        // Para onde ele <b>foi mandado</b> é o destino que a task recebeu,
        // e só o WorkTargets sabe; onde <b>haveria</b> de ficar de pé é o
        // approachTo, e é ele que diz "não há lugar nenhum". A frase
        // antiga misturava as duas e imprimia a segunda como se fosse a
        // primeira — ver sentTo.
        BlockPos spot = MinerWork.approachTo(world, target);

        String stand = spot.equals(target)
                ? "the stone itself (no free neighbour to stand on)"
                : spot.toShortString()
                        + (BuilderApproach.standable(world, spot) ? "" : ", which is not standable")
                        + (world.getBlockState(spot).getFluidState().isEmpty()
                                ? "" : ", which is flooded");

        return "the miner is at " + villager.getBlockPos().toShortString()
                + trapped(villager, villager.getUuid())
                + ", " + String.format("%.1f", MinerWork.distanceTo(villager, target))
                + " blocks away; it was walking to " + sentTo(villager.getUuid(), target)
                + "; the place to stand is " + stand
                + "; the stone at " + target.toShortString() + " is "
                + world.getBlockState(target).getBlock().getName().getString();
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
