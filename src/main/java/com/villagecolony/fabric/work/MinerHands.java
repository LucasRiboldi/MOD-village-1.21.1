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
 * As mãos do mineiro: cavar a pedra, fechar a tarefa, soltá-la ou desistir dela — separado de
 * {@link MinerWork} em 2026-09-24, quando ele passou de 500 linhas. Os
 * comentários vieram junto sem mudança.
 */
public final class MinerHands {

    private MinerHands() {
    }

    /** Quebra a pedra em curso, no tempo que ela pede. */
    static void mine(
            ServerWorld world, VillagerEntity villager, Job job, WorkerStorage storage) {

        BlockState state = world.getBlockState(job.target);

        if (job.required == 0) {
            job.required = BlockBreakTime.ticksFor(world, job.target, state, villager);
        }

        job.progress++;

        if (job.progress % MinerWork.SWING_INTERVAL == 1) {
            villager.swingHand(Hand.MAIN_HAND);
        }

        if (job.progress < job.required) {
            world.setBlockBreakingInfo(
                    villager.getId(), job.target, job.progress * MinerWork.BREAKING_STAGES / job.required);

            return;
        }

        world.setBlockBreakingInfo(villager.getId(), job.target, -1);

        List<ItemStack> drops = new ArrayList<>(
                Block.getDroppedStacks(state, world, job.target, null, null, ItemStack.EMPTY));

        // <b>A colônia avisa que foi ela</b> — 2026-09-16. Sem isto o
        // PlayerWorldChangeHandler via a própria picareta como edição do
        // jogador e reabria o ramal com a contagem de recusas zerada: o log
        // de 02:58 teve 19.193 linhas de "hit stone with nowhere to stand",
        // dez por segundo, e a mina nunca desceu. Ver ColonyEdits.
        ColonyEdits.remember(MinecraftTypeAdapter.toColonyPos(job.target));

        world.removeBlock(job.target, false);

        // A picareta pegou: a posição deixa de ser suspeita — E44. A
        // marca é por posição e o servidor vive dias; sem isto uma
        // recusa velha continuaria contando contra a pedra que veio
        // depois no mesmo lugar, quando a mina descer um nível.
        MineMarks.dug(job.target);

        // E a curva do ramal recomeça — 2026-09-11. Aqui, e não onde o
        // cursor escolhe a pedra: é este bloco saindo do mundo que prova
        // que a frente rende. Ver MineTrouble.pickaxeTook.
        MineTrouble.pickaxeTook(job.task.colonyId(), villager.getUuid());

        // <b>E se saiu água por ali, tapa antes de sair de perto</b> —
        // decisão do autor, 2026-09-03. Aqui, e não no ciclo seguinte: o
        // líquido corre por tique, e um ciclo de colônia é tempo de
        // sobra para ele descer a escada inteira. Ver MineFlooding.
        //
        // A galeria vira junto, que é a outra metade do pedido —
        // "seguir por outro caminho". Ver MineTrouble.flooded.
        if (MineFlooding.seal(world, job.target) > 0) {
            MineTrouble.flooded(job.task.colonyId(), villager.getUuid(), job.target);
        }

        // Regra 30: o minério que não é carvão vai para o baú da boca
        // da mina, e só transborda para o do mineiro quando aquele
        // lotar. Decidido aqui, com o bloco em mãos: no baú só
        // chegam itens, e minério cru não diz de que pedra veio.
        MinerHaul.Haul haul = MinerHaul.deposit(
                world,
                storage,
                drops,
                MinerHaul.treasureChestFor(world, job, state),
                job.target,
                MinecraftTypeAdapter.toItem(job.wanted).orElse(null));

        job.collected += haul.stored();
        job.toward += haul.wanted();

        // A linha que faltava. Trabalho mudo não se diagnostica — é o
        // §11, e foi ele que custou quatro sessões à Fase 10.
        VillageColonyMod.LOGGER.info(
                "Miner {} took {} from {} — {} this task",
                villager.getUuid(),
                haul.stored(),
                job.target.toShortString(),
                job.collected);

        if (job.toward >= job.task.amount()) {
            finishTask(villager.getUuid(), job);

            return;
        }

        // <b>Baú cheio encerra a tarefa</b> — 2026-09-22, visto em jogo. O
        // log do autor tem quarenta e sete linhas de
        // "Miner chest ... is full — dropped 1 of minecraft:cobblestone", e o
        // mineiro seguiu cavando as mesmas duas posições da boca da mina sem
        // parar, jogando pedra no chão a cada passagem. Do lado de fora isso
        // se vê como a mina sendo cavada para sempre sem render nada.
        //
        // A pedra saiu do mundo e não entrou em lugar nenhum: continuar é
        // gastar a vez do mineiro e sujar o chão. Encerrar devolve a vez ao
        // ciclo da colônia, que é quem sabe pedir baú novo — ver a Regra 30 e
        // MinerHaul.deposit, que já registra o transbordo.
        if (haul.stored() == 0 && !drops.isEmpty()) {
            VillageColonyMod.LOGGER.warn(
                    "Miner {} stops — the stone from {} had nowhere to go,"
                            + " the chest that serves him is full",
                    villager.getUuid().toString().substring(0, 8),
                    job.target.toShortString());

            finishTask(villager.getUuid(), job);

            return;
        }

        // Não solta o alvo ainda. A posição é a âncora da busca de quedas:
        // quando a areia de cima chegar, o próximo ciclo limpa esta marca e
        // escolhe a nova frente já assentada.
        job.approach = null;
        job.progress = 0;
        job.required = 0;
        job.settling = 1;
        WorkTargets.clear(villager.getUuid());
    }

    /**
     * Encerra a tarefa quando o pedido foi atendido — 2026-09-09.
     *
     * <p><b>Ela não terminava.</b> Nada em produção comparava o que o
     * mineiro trouxe com o que a tarefa pediu: {@code task.amount()} era
     * lido por um lugar só no mod inteiro, o {@code MinerReport}, para
     * escrever a linha do log. O número era um enfeite, e a sessão de
     * 2026-09-06 mostrou o enfeite crescendo — <b>496 amostras com a meta
     * ultrapassada</b>, 442 delas no mesmo mineiro em
     * <i>"105 of 32 so far"</i>, cavando pedra que a colônia já tinha.
     *
     * <p>O ciclo da colônia sabia parar e não alcançava: {@code
     * ColonyCycle.cancelSatisfied} tira da fila o pedido que perdeu o
     * motivo, mas só o que <b>ainda não começou</b> — "quem já começou
     * termina", e quem já começou não tinha como terminar.
     *
     * <p>Aqui, e não a cada tique: a pergunta é feita com a pedra já
     * depositada, que é a fronteira em que o lenhador e o fabricante
     * também param — a pedra da vez não é interrompida, e é o que aquela
     * decisão do ciclo protege.
     *
     * <p>Chega com a tarefa em RESERVED no caso comum, e a transição é a
     * mesma que {@code TreeFelling.finishTask} faz pelo mesmo motivo:
     * {@code Task.complete} exige EXECUTING, e completar direto lançava
     * dentro do tick do servidor.
     */
    static void finishTask(UUID workerId, Job job) {
        if (job.task.state() == TaskState.RESERVED) {
            job.task.start();
        }

        job.task.complete();

        VillageColonyMod.LOGGER.info(
                "Miner {} filled the order — {} {} of the {} asked, and stopped",
                workerId,
                job.toward,
                job.wanted.name().toLowerCase(java.util.Locale.ROOT),
                job.task.amount());

        release(workerId, job);
    }

    /** Larga a pedra de agora e volta a procurar. */
    static void release(UUID workerId, Job job) {
        job.target = null;
        job.approach = null;
        job.progress = 0;
        job.required = 0;
        job.settling = 0;
        job.stalled = 0;
        job.lease.reset();

        // O guarda de imobilidade sobrevive a largar a pedra — E36. Ver
        // MinerSteps.startNextStone: largar não é andar, e este caminho é o mais
        // percorrido de todos, porque toda pedra cavada passa por ele.
        // Quem zera de verdade é o ramo de trabalho, logo antes do mine.

        WorkTargets.clear(workerId);
    }

    /**
     * Devolve a tarefa quando o mineiro não chega à pedra.
     *
     * <p>O cursor da busca é esquecido junto: sem isso a passagem
     * seguinte reencontraria exatamente a mesma pedra inalcançável, que é
     * a roda que a Regra 9 fechou do lado do lenhador.
     */
    static void giveUp(ServerWorld world, UUID workerId, Job job, String why) {
        VillageColonyMod.LOGGER.info(
                "Miner {} gave up the stone at {} — {}. Task back to the queue. {}",
                workerId,
                job.target.toShortString(),
                why,
                world.getEntity(workerId) instanceof VillagerEntity villager
                        ? MinerReport.whyNotReached(world, villager, job.target)
                        : "the miner left the world");

        job.task.release();

        // <b>E a pedra ganha prazo</b> — E44, 2026-09-10. Segurar a
        // posição (logo abaixo) continua certo; segurar SEM PRAZO é o
        // laço que a sessão das 08:33 mediu: o mineiro gasta 2.400
        // tiques andando até ela, desiste, o outro assume o ramal e
        // recebe A MESMA pedra. Marcar vem antes de segurar de
        // propósito — quem lê a marca é a passagem seguinte, e ela
        // precisa achá-la já posta. Ver MineMarks.
        MineMarks.refuse(world, job.target);

        // A posição volta para o cursor da galeria — 2026-08-27. Sem
        // isto o mod marchava pela ordem de cavar com o mundo intacto.
        MineTrouble.couldNotReach(job.task.colonyId(), job.target);

        release(workerId, job);

        // E a vez na mina, se havia uma — 2026-08-29. Um mineiro preso
        // num poço devolvia a tarefa e a pegava de volta para sempre,
        // enquanto o outro esperava do lado de fora. Ver MineClaims.
        MineClaims.stepAside(job.task.colonyId(), workerId);

        // E a pedra descansa para ele — ADR-010. A vez na mina resolve
        // dois mineiros disputando uma escada; não resolve a colônia
        // inteira sem pedra alcançável, que é quando ele precisa ir
        // ajudar noutra coisa em vez de repetir a mesma parede.
        //
        // <b>E passa pela porta única desde 2026-09-10</b>: a contagem de
        // desistências que tira o trabalhador do ofício mora no rest, e
        // chamá-lo por fora do WorkerStrikes deixaria o mineiro sem a
        // linha do relatório que as outras seis têm.
        WorkerStrikes.gaveUp(workerId, job.task);

        // O cursor da varredura de areia sai junto: sem isso a passagem
        // seguinte reencontraria exatamente a mesma areia inalcançável,
        // que é a roda que a Regra 9 fechou do lado do lenhador.
        SandGathering.forget(workerId);
    }
}
