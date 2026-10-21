package com.villagecolony.fabric.work;

import com.villagecolony.fabric.work.MinerWork.Job;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyEdits;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.MineFlooding;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.integration.MineMouth;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O passo do mineiro a cada tique e a escolha da próxima pedra — separado de
 * {@link MinerWork} em 2026-09-24, quando ele passou de 500 linhas. Os
 * comentários vieram junto sem mudança.
 */
final class MinerSteps {

    /** Quantos blocos abaixo do destino, sem desvio, tornam o mineiro encalhado. */
    static final int CLIMB_OUT_BELOW = 2;

    /** Desistências seguidas num ramal, sem pedra cavada entre elas, antes de ele acabar. */
    static final int UNREACHABLE_BEFORE_DONE = 3;

    private MinerSteps() {
    }

    /**
     * Um passo.
     *
     * @return se esta passagem gastou uma busca do orçamento do tique
     */
    static boolean step(
            ServerWorld world, UUID workerId, Job job, boolean maySearch) {

        Entity entity = world.getEntity(workerId);

        if (!(entity instanceof VillagerEntity villager)) {
            return false;
        }

        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(workerId);

        if (storage.isEmpty()) {
            return false;
        }

        MinerCaution.keepOutOfWater(villager);

        if (job.target == null) {
            if (!maySearch) {
                return false;
            }

            return startNextStone(world, workerId, job, villager);
        }

        // <b>Depois de quebrar, espera a frente assentar</b> — playtest de
        // 2026-09-20. A areia que cai ocupa o lugar do alvo no tique
        // seguinte. Sem este estado, o mineiro soltava a posição assim que
        // a pedra saía, escolhia de novo a mesma frente e entrava num ciclo
        // de tentativa, queda e bloqueio. A espera também existe quando
        // não há entidade caindo: ela dá ao jogo um tique para atualizar a
        // coluna, que é a marcha mais lenta pedida para o deserto.
        if (job.settling > 0) {
            if (MineSettling.waits(world, job.target, job.settling)) {
                job.settling++;

                return false;
            }

            VillageColonyMod.LOGGER.info(
                    "Miner {} let the mined front settle at {} after {} ticks — picking a target again",
                    villager.getUuid().toString().substring(0, 8),
                    job.target.toShortString(),
                    job.settling);

            MinerHands.release(workerId, job);

            return false;
        }

        BlockState state = world.getBlockState(job.target);

        if (state.isAir()) {
            // Alguém tirou a pedra entre planejar e chegar. Procura outra.
            MinerHands.release(workerId, job);

            return false;
        }

        // <b>O desvio anda antes de tudo</b> — ADR-025, fase 2. Enquanto ele
        // cava e põe bloco, os guardas de travamento esperam: o desvio tem
        // prazo próprio por passo. Ver MinerDetours.
        if (MinerDetours.tick(world, villager, job, storage.get())) {
            return false;
        }

        if (!MinerWork.isWithinReach(villager, job.target)) {
            // Só conta tique de expediente — 2026-08-27, e é o molde do
            // lenhador, que esta classe segue de propósito. O guarda pune
            // quem anda sem chegar; fora da hora o aldeão está PROIBIDO
            // de andar, porque a GoToWorkTargetTask nem começa. A sessão
            // de 08-26 queimou metade do orçamento com ele dormindo: o
            // contador foi de 886 a 2086 com o relatório dizendo
            // "off hours".
            if (WorkHours.isWorkTime(world, villager)) {
                job.stalled++;
            }

            // Chegou perto: a posição de trabalho é conferida uma vez, de onde ele
            // está, antes de gastar os 400 tiques do prazo nela — E1.
            if (!job.judged && MiningTarget.isNear(villager.getBlockPos(), job.target)) {
                job.judged = true;

                MiningTarget.Verdict verdict =
                        MiningTarget.judge(world, job.target, villager.getBlockPos());

                if (!verdict.eligible()) {
                    MinerHands.passOver(world, workerId, job, verdict.rejection());

                    return false;
                }
            }

            // Fora da mina, primeiro a boca de superfície e a descida nível por
            // nível; só no nível da pedra vale a perna de sempre (MineEntrance).
            // Na descida o prazo de aproximação não corre: ir até a boca pode
            // afastá-lo da pedra. Corre o prazo da descida, que aceita também
            // descer mais fundo como avanço (MineLease.outOfTimeOnDescent).
            Optional<BlockPos> down = MineEntrance.legDown(
                    world, job.task.colonyId(), villager.getBlockPos(), job.target);

            if (down.isPresent()) {
                job.lease.reset();

                // Nem o orçamento de caminhada: a descida inteira (vila, boca, três
                // níveis) passava dos 2.400 tiques (playtest de 08-10, 15:28–15:46).
                job.stalled = 0;
            } else {
                job.descent.reset();
            }

            guardOrWalk(world, workerId, job, villager, down);

            return false;
        }

        // <b>Ele chegou e vai bater na pedra</b> — E36, 2026-09-04. É
        // aqui que o guarda de imobilidade recomeça, e não ao pegar alvo:
        // trabalhar é a prova de que ele não está congelado. Mesmo lugar
        // em que o BuilderWork e o CraftingWork sempre zeraram.
        job.stall.reset();

        // <b>Ele espera a areia assentar antes de bater</b> — pedido do
        // autor, 2026-09-19. Cavar no meio da queda é cavar no escuro: o
        // alvo desce um bloco, o buraco se reenche, e a picareta bate no
        // ar. Ver MineSettling.
        //
        // O guarda de imobilidade já foi zerado acima, e é de propósito:
        // esperar a duna assentar é trabalho, não congelamento — o mesmo
        // argumento do lenhador parado cortando árvore.
        if (MineSettling.waits(world, job.target, job.settling)) {
            job.settling++;

            return false;
        }

        if (job.settling > 0) {
            // <b>Assentou: o alvo é recalculado antes da próxima batida</b>
            // — decisão do autor entre as duas opções. O que caiu ocupou o
            // lugar, então a pedra de antes pode estar soterrada; insistir
            // nela seria bater onde não há mais nada. Soltar o alvo faz a
            // passagem seguinte escolher de novo, e o que desceu é minério
            // que chegou sozinho até a mão dele.
            VillageColonyMod.LOGGER.info(
                    "Miner {} waited {} ticks for the sand to settle at {} — picking a target again",
                    villager.getUuid().toString().substring(0, 8),
                    job.settling,
                    job.target.toShortString());

            job.settling = 0;

            job.target = null;

            job.approach = null;

            job.progress = 0;

            job.required = 0;

            return false;
        }

        MinerHands.mine(world, villager, job, storage.get());

        return false;
    }

    /**
     * O mineiro ainda não alcança a pedra: ou um guarda de travamento desiste da tarefa, ou ele anda
     * em direção a ela. Extraído de {@link #step} em 2026-10-15 (F3 do plano) sem mudar o fluxo.
     *
     * <p>Os quatro guardas, na ordem: imobilidade ({@link WorkStall} — não saiu do bloco), aproximação
     * ({@link MineLease#outOfTime} — andou sem chegar mais perto), descida ({@link MineLease#outOfTimeOnDescent}
     * — a boca não encurtou e ele não desceu) e caminhada ({@link MinerWork#STALL_LIMIT} tiques sem
     * chegar). Nenhum disparou → {@link #walkToward}.
     */
    private static void guardOrWalk(
            ServerWorld world, UUID workerId, Job job, VillagerEntity villager, Optional<BlockPos> down) {

        // E se ele saiu do lugar — 2026-09-03. Ver WorkStall, que faz a pergunta do expediente sozinho.
        if (job.stall.stuck(world, villager)) {
            stuck(world, villager, job, "it has not moved a block in "
                    + job.stall.ticks() + " ticks of work time");
        } else if (job.lease.outOfTime(world, villager, job.target)) {
            // E se ele anda sem chegar mais perto — E44, 2026-09-10. O caso que os outros não pegam:
            // quem contorna sem fim sai do bloco (escapa do guarda de imobilidade) e ainda tem 2.400
            // tiques de orçamento pela frente. Ver MineLease.
            stuck(world, villager, job, "it got no closer than "
                    + String.format("%.1f", job.lease.closest())
                    + " blocks in " + job.lease.ticks()
                    + " ticks of work time");
        } else if (down.isPresent() && job.descent.outOfTimeOnDescent(world, villager, job.target)) {
            stuck(world, villager, job, "the way down got no closer than "
                    + String.format("%.1f", job.descent.closest())
                    + " blocks and no deeper in " + job.descent.ticks()
                    + " ticks of work time");
        } else if (job.stalled >= MinerWork.STALL_LIMIT) {
            stuck(world, villager, job, "it walked for "
                    + job.stalled + " ticks of work time without arriving");
        } else {
            walkToward(world, workerId, job, villager, down);
        }
    }

    /**
     * O mineiro ainda não alcança a pedra e nenhum guarda de travamento disparou: escolhe o alvo de
     * caminhada deste tique. Extraído de {@link #step} em 2026-10-15 (F3 do plano) sem mudar o fluxo.
     *
     * <p>Primeiro reconfere a aproximação guardada — quem caiu num buraco depois de escolher o alvo
     * fica com um destino acima do degrau que ele sobe, e a navegação nunca cumpre (sessão de 09-19).
     * Depois a cascata, da mais barata para a mais cara: caminho direto do jogo ({@link MinerDirectPath}),
     * rota em rede no nível ({@link NetworkRoute}), percurso por etapas até a boca ({@link MineApproachPath},
     * só na descida) e, por fim, a perna pela ordem de cavar ({@link MinerLeg}).
     */
    private static void walkToward(
            ServerWorld world, UUID workerId, Job job, VillagerEntity villager, Optional<BlockPos> down) {

        // E se ele desceu, a aproximação guardada não serve mais (09-19): o approach fica acima do
        // degrau, a navegação não cumpre, e o guarda de imobilidade devolve a tarefa. Recalcular custa
        // ~600 leituras que o cache evita, então só quando o cache está comprovadamente furado.
        if (job.approach.getY() - villager.getBlockPos().getY() > MinerWork.CLIMB) {
            BlockPos again = MinerApproach.approachTo(world, job.target, villager.getBlockPos());

            // Só troca por uma que ele alcance: sem nenhuma, o approachTo devolve a de antes ou a
            // própria pedra, e trocar seria rodar a busca a cada tique para chegar ao mesmo lugar.
            if (again.getY() - villager.getBlockPos().getY() <= MinerWork.CLIMB) {
                job.approach = again;
            }
        }

        // O jogo já acha o caminho até a pedra: direto, sem perna (MinerDirectPath). Guardado, não
        // recalculado — a busca custa ~600 leituras e isto roda todo tique enquanto ele caminha.
        if (MinerDirectPath.reaches(world, villager, job.approach)) {
            WorkTargets.set(workerId, job.approach, MinerReach.ARRIVAL);

            return;
        }

        // No nível em rede: de nó pronto em nó pronto até o pai do nó (ADR-042 D5).
        Optional<BlockPos> waypoint = NetworkRoute.waypoint(job.task.colonyId(), villager, job.target);

        if (waypoint.isPresent()) {
            WorkTargets.set(workerId, waypoint.get(), MinerReach.ARRIVAL);

            return;
        }

        if (down.isPresent()) {
            // Boca longe numa vila grande: anda por etapas até lá, por caminhos reais da navegação, em
            // vez de mirar a boca a ~95 blocos e ficar "walking to nowhere" (2026-10-14). A falha e a
            // liberação ficam com o guarda de descida (job.descent).
            WorkTargets.set(workerId,
                    MineApproachPath.toward(world, villager, down.get()), MinerReach.ARRIVAL);

            return;
        }

        // E32 (2026-09-02): a perna pela ordem de cavar, para quando a navegação não traça caminho por
        // dentro da rocha. O corredor é o do ramal que ele reservou (MineDigging.armToWalk).
        Optional<MineArm> corridor =
                MineDigging.armToWalk(job.task.colonyId(), workerId, villager.getBlockPos());

        BlockPos leg = MinerLeg.legTowards(
                villager.getBlockPos(),
                job.approach,
                corridor,
                MineDigging.leadsToTheTarget(job.task.colonyId(), workerId, corridor),
                MinerApproach.footingIn(world));

        WorkTargets.set(
                workerId,
                MinerApproach.climbableWalkTarget(world, villager.getBlockPos(), leg),
                MinerReach.ARRIVAL);
    }

    /**
     * Um guarda de travamento disparou: primeiro o desvio, depois a desistência
     * — ADR-025, fase 2. Ver MinerDetours.
     */
    private static void stuck(ServerWorld world, VillagerEntity villager, Job job, String why) {
        if (MinerDetours.tryDetour(world, villager, job, why)) {
            return;
        }

        Optional<BlockPos> way = WorkTargets.of(villager.getUuid())
                .filter(target -> target.getY() - villager.getBlockY() >= CLIMB_OUT_BELOW);
        boolean belowItsWay = way.isPresent();
        boolean network = NetworkDigging.galleryPhase(job.task.colonyId());
        Optional<MineArm> arm = MineDigging.armOf(job.task.colonyId(), villager.getUuid());

        // No nível em rede quem conta é o nó: três desistências o vedam (ADR-042).
        if (network) {
            NetworkDigging.gaveUp(job.task.colonyId(), villager.getUuid());
        }

        MinerHands.giveUp(world, villager.getUuid(), job, why);

        // O ramal cuja pedra ninguém alcança acaba: sem isso o nível não descia nunca e a
        // mina morria com um ramal só aberto (playtest 0.3.32, 11 desistências, 0 cavado).
        if (!network && arm.isPresent() && !arm.get().isDone() && arm.get().blockedAgain(UNREACHABLE_BEFORE_DONE)) {
            VillageColonyMod.LOGGER.info("Mine {} closes a branch after {} stones nobody could reach — {}",
                    job.task.colonyId().toString().substring(0, 8), UNREACHABLE_BEFORE_DONE, why);
        }

        // Dois ou mais abaixo de para onde ia, sem desvio: aldeão sobe um, e devolver a
        // tarefa o deixava girando no mesmo buraco a cada pega (playtest 0.3.31, "5 blocks
        // below it and unable to climb"). A fuga do encalhado o tira de lá.
        if (belowItsWay) {
            StrandedWorkers.strandBelow(villager.getUuid(), villager.getBlockPos(), way.get(),
                    "below its way and unable to climb");
        }
    }

    /**
     * Acha o próximo bloco, reserva-o e manda o aldeão andar até lá.
     *
     * <p><b>Dois caminhos, e quem decide é o recurso da tarefa.</b> Pedra
     * está em toda parte abaixo do chão e se busca descendo a escada da
     * Regra 29; areia mora na praia e na duna, e a vinte blocos não há
     * nenhuma fora do deserto. A mesma profissão, duas geografias.
     *
     * <p>A geometria de cada um saiu daqui em 2026-08-21 — ver
     * {@link MineDigging} e {@link SandGathering}. O que ficou é o que os
     * dois compartilham, que é o trabalho em si: a picareta, o baú, o
     * guarda de travamento e a tarefa.
     *
     * @return se esta passagem gastou uma busca do orçamento do tique
     */
    static boolean startNextStone(
            ServerWorld world, UUID workerId, Job job, VillagerEntity villager) {

        UUID colonyId = job.task.colonyId();

        boolean sand = job.task.targetResource().group() == ResourceGroup.SAND;

        int branches = VillageColonyMod.MINES.of(colonyId)
                .map(Mine::branchesOpenNow)
                .orElse(Mine.ARMS);

        Optional<BlockPos> found;

        if (!sand && !NetworkDigging.galleryPhase(colonyId) && MineClaims.heldByOther(colonyId, workerId, branches)) {
            // O ramal continua exclusivo, mas o mineiro barrado não fica
            // parado: ele raspa pedra exposta fora das células planejadas
            // da mina aberta, criando caminho de coleta enquanto espera.
            // Se nada aparecer, a recusa continua não sendo busca; o dono
            // ainda precisa da passagem para soltar ou avançar o ramal.
            found = MineDigging.exposedStoneFallback(world, workerId, colonyId, job.center);

            if (found.isEmpty()) {
                return false;
            }
        } else {
            found = sand
                    ? SandGathering.nextTarget(world, workerId, colonyId, job.center)
                    : MineDigging.nextTarget(world, workerId, colonyId, job.center);
        }

        if (found.isEmpty()) {
            return true;
        }

        job.target = found.get();

        // Alvo só com posição de trabalho válida e alcançável — E1, ver MiningTarget.
        MiningTarget.Verdict verdict = MiningTarget.judge(world, job.target, villager.getBlockPos());

        if (!verdict.eligible()) {
            MinerHands.passOver(world, workerId, job, verdict.rejection());

            return true;
        }

        job.judged = MiningTarget.isNear(villager.getBlockPos(), job.target);
        job.approach = verdict.workPosition() != null
                ? verdict.workPosition()
                : MinerApproach.approachTo(world, job.target, villager.getBlockPos());
        job.progress = 0;
        job.required = 0;
        job.stalled = 0;
        job.detours = 0;
        job.detour = null;

        // <b>E o prazo de aproximação recomeça</b> — E44, 2026-09-10, e
        // aqui alvo novo É motivo, ao contrário do guarda de
        // imobilidade logo abaixo. A régua do MineLease é a distância
        // ATÉ ESTA PEDRA; herdá-la da anterior faria ele desistir de uma
        // pedra mais distante sem ter dado um passo por ela.
        job.lease.reset();
        job.descent.reset();

        // <b>E o guarda de imobilidade NÃO é zerado aqui</b> — E36,
        // 2026-09-04. A pergunta que ele faz é <i>o aldeão saiu do
        // bloco?</i>, e ela não tem nada a ver com qual é o alvo: quem
        // estava congelado continua congelado depois de a pedra à frente
        // dele sumir. Zerar por alvo novo deixava <b>imune</b> quem troca
        // de alvo com frequência, e foi o que os mineiros travados da
        // sessão de 09-04 exibiram por vinte e cinco minutos com
        // {@code stall 0/2400, still 0/300} e nenhum passo dado.
        //
        // Quem zera é o movimento — o WorkStall vê sozinho — e o ramo em
        // que ele trabalha, que é o que o construtor e o fabricante
        // sempre fizeram. O de 2.400 continua por alvo, porque é isso que
        // ele mede: andei demais até ESTE alvo.

        WorkTargets.set(workerId, job.approach, MinerReach.ARRIVAL);

        return true;
    }
}
