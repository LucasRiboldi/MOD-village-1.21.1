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

            // E se ele saiu do lugar — 2026-09-03. Ver WorkStall, que faz
            // a pergunta do expediente por conta própria.
            if (job.stall.stuck(world, villager)) {
                MinerHands.giveUp(world, workerId, job, "it has not moved a block in "
                        + job.stall.ticks() + " ticks of work time");
            } else if (job.lease.outOfTime(world, villager, job.target)) {
                // <b>E se ele anda sem chegar mais perto</b> — E44,
                // 2026-09-10. Este é o caso que os outros dois não
                // pegam: quem contorna sem fim sai do bloco (escapa do
                // guarda de imobilidade) e ainda tem 2.400 tiques de
                // orçamento pela frente. Ver MineLease.
                MinerHands.giveUp(world, workerId, job, "it got no closer than "
                        + String.format("%.1f", job.lease.closest())
                        + " blocks in " + job.lease.ticks()
                        + " ticks of work time");
            } else if (job.stalled >= MinerWork.STALL_LIMIT) {
                MinerHands.giveUp(world, workerId, job, "it walked for "
                        + job.stalled + " ticks of work time without arriving");
            } else {
                // <b>E se ele desceu, a aproximação guardada não serve
                // mais</b> — sessão de jogo de 2026-09-19.
                //
                // O `approachTo` de três mãos filtra por MinerWork.CLIMB, mas
                // responde para a posição em que ele estava <b>na hora
                // de escolher o alvo</b>. Quem cai num buraco depois
                // disso fica com um destino dois acima da cabeça, e
                // aldeão sobe um: a navegação não cumpre, ele não sai do
                // lugar, e o guarda de imobilidade devolve a tarefa. O
                // ciclo reabre a mesma pedra e escolhe a mesma
                // aproximação, porque a pergunta é feita de onde ele
                // está — e ele está no buraco.
                //
                // O log de 09-19 mediu o laço fechado: 21 das 25 leituras
                // com o mineiro em -841, 44, 1374 e toda aproximação em
                // y=46. Dezoito das trinta e duas desistências dizem
                // "2 blocks below it and unable to climb", e a sessão
                // terminou com 103 pedras quebradas e ZERO entregues.
                //
                // Recalcular custa as seiscentas leituras que o cache
                // evita, então só se paga quando o cache está
                // comprovadamente furado — a condição abaixo é a mesma
                // que o relatório usa para acusar o degrau.
                if (job.approach.getY() - villager.getBlockPos().getY() > MinerWork.CLIMB) {
                    BlockPos again =
                            MinerApproach.approachTo(world, job.target, villager.getBlockPos());

                    // Só troca por uma que ele alcance: sem nenhuma, o
                    // `approachTo` devolve a de antes ou a própria pedra,
                    // e trocar seria rodar a busca a cada tique para
                    // chegar ao mesmo lugar.
                    if (again.getY() - villager.getBlockPos().getY() <= MinerWork.CLIMB) {
                        job.approach = again;
                    }
                }

                // O mesmo destino da primeira vez, e pelo mesmo motivo:
                // repor a pedra aqui era repor a rocha maciça, e a
                // navegação não tem como cumprir isso — ver MinerApproach.approachTo.
                //
                // Guardado, e não recalculado: a busca custa umas
                // seiscentas leituras de bloco, e isto roda todo tique
                // enquanto ele caminha.
                //
                // E por pernas — 2026-08-28. Ver MinerLeg.legTowards:
                // a navegação não traça um caminho de vinte blocos por
                // dentro da rocha, e ele ficava parado na superfície
                // acima da galeria.
                // E32 — 2026-09-02. A perna entregava um bloco cru da ordem
                // de cavar, sem perguntar se dava para ficar de pé nele. Ordem
                // não é lista de lugares onde se fica de pé: duas de cada três
                // posições da escada são a cabeça, e as que o cursor entregou
                // podem não ter sido cavadas. Alvo sólido não faz a navegação
                // desistir — o MobNavigation SOBE o alvo até sair da rocha, e
                // dentro de uma mina isso é a superfície. Ver
                // docs/research/E32-miner-walk-target.md.
                // <b>O corredor é o de onde ele está</b> — 2026-09-05, e
                // era o do ramal que ele reservou. Ver
                // MineDigging.armToWalk: os dois deixaram de ser o mesmo
                // quando a mina ganhou quatro rumos, e a tarefa de areia
                // nunca reservou rumo nenhum.
                Optional<MineArm> corridor =
                        MineDigging.armToWalk(
                                job.task.colonyId(), workerId, villager.getBlockPos());

                BlockPos leg = MinerLeg.legTowards(
                        villager.getBlockPos(),
                        job.approach,
                        corridor,
                        MineDigging.leadsToTheTarget(
                                job.task.colonyId(), workerId, corridor),
                        MinerApproach.footingIn(world));

                WorkTargets.set(
                        workerId,
                        MinerApproach.climbableWalkTarget(world, villager.getBlockPos(), leg),
                        MinerReach.ARRIVAL);
            }

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

        if (!sand && MineClaims.heldByOther(colonyId, workerId, branches)) {
            // <b>Recusa não é busca</b> — 2026-09-04. Quem é barrado no
            // portão da escada não varre coluna nenhuma, e cobrar do
            // orçamento o que não gastou foi o impasse daquele dia: o
            // barrado vinha antes no mapa, levava a única busca do tique,
            // e o dono ficava sem a passagem em que soltaria a mina por
            // não achar pedra — a saída de 2026-09-02, que nunca chegava
            // a rodar. Vinte e cinco minutos assim, uma pedra na colônia,
            // e os dois guardas de travamento em zero porque ninguém
            // andava para lugar nenhum.
            //
            // A areia não passa por aqui: ela não usa a escada, e o
            // dono dela é o cursor de cada mineiro.
            return false;
        }

        Optional<BlockPos> found = sand
                ? SandGathering.nextTarget(world, workerId, colonyId, job.center)
                : MineDigging.nextTarget(world, workerId, colonyId, job.center);

        if (found.isEmpty()) {
            return true;
        }

        job.target = found.get();
        job.approach = MinerApproach.approachTo(world, job.target, villager.getBlockPos());
        job.progress = 0;
        job.required = 0;
        job.stalled = 0;

        // <b>E o prazo de aproximação recomeça</b> — E44, 2026-09-10, e
        // aqui alvo novo É motivo, ao contrário do guarda de
        // imobilidade logo abaixo. A régua do MineLease é a distância
        // ATÉ ESTA PEDRA; herdá-la da anterior faria ele desistir de uma
        // pedra mais distante sem ter dado um passo por ela.
        job.lease.reset();

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
