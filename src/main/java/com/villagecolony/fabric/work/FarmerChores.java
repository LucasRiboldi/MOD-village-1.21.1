package com.villagecolony.fabric.work;

import com.villagecolony.fabric.work.FarmerWork.Chore;
import com.villagecolony.fabric.work.FarmerWork.Job;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.CropPatch;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * As tarefas do fazendeiro no canteiro: achar o que fazer, colher, semear e
 * guardar — separado de {@link FarmerWork} em 2026-09-24, quando ele passou de
 * 500 linhas.
 *
 * <p>O {@code FarmerWork} cuida do ciclo do trabalhador (tarefa, passo,
 * guarda de travamento); esta classe é o que ele faz com as mãos. Os
 * comentários vieram junto sem mudança.
 */
final class FarmerChores {

    private FarmerChores() {
    }

    /**
     * A lavoura madura mais perto do centro da vila.
     *
     * <p>Do centro para fora, e não do aldeão: a lavoura da vila é da
     * vila, e dois fazendeiros que buscassem cada um a partir de si
     * acabariam em cantos opostos do mesmo campo.
     */
    static void findWork(
            ServerWorld world, UUID workerId, Job job, WorkerStorage storage) {

        UUID colonyId = job.task.colonyId();

        // <b>Volta inteira sem nada compra silêncio</b> — P1.5, 2026-09-11.
        // O motivo já foi dito quando a volta fechou; aqui não se fala de
        // novo, senão o descanso vira a enxurrada que ele evita. Ver
        // FieldRest para por que ele existe e por que é curto.
        if (FieldRest.isResting(colonyId, world.getTime())) {
            return;
        }

        CropPatch.Field field = CropPatch.survey(world, colonyId, job.center, FarmerWork.searchRadius);

        Optional<BlockPos> found = field.ripe();
        Chore chore = Chore.HARVEST;

        if (found.isEmpty()) {
            // <b>Sem semente não há o que semear nem por que arar</b> —
            // 2026-09-05, e é este gate que dá teto ao campo sem uma
            // constante inventada. Os dois trabalhos gastam semente, e a
            // semente só sobra quando a colheita sobra: a roça cresce no
            // ritmo em que a lavoura paga por ela, e para de crescer
            // quando o baú seca.
            if (ChestWithdrawer.seedIn(world, storage.chestPosition()).isPresent()) {
                found = field.emptyPlot();
                chore = Chore.SOW;
            }
        }

        if (found.isEmpty()) {
            // <b>Pelo recordAt, e não pelo record</b> — o molde do P0.6.
            // Este método roda por tique e os dois motivos alternam por
            // construção: toda volta termina em NO_TARGET e a seguinte
            // recomeça em SWEEP_INCOMPLETE. A regra de transição sozinha
            // deixou 4.389 linhas num log de 6.117 na areia.
            //
            // E são dois motivos, não um: até 2026-09-11 esta linha saía
            // sempre como NO_TARGET, cujo texto é <i>"nothing to work on
            // in the whole radius"</i> — e o raio inteiro nunca tinha
            // sido olhado. O enum afirmava a cobertura que a varredura
            // truncada não entregava. Ver CropPatch#survey.
            IdleLog.recordAt(
                    colonyId,
                    FarmerWork.SUBJECT,
                    field.incomplete() ? IdleReason.SWEEP_INCOMPLETE : IdleReason.NO_TARGET,
                    "nothing ripe and no empty plot within "
                            + FarmerWork.searchRadius + " blocks of the village",
                    world.getTime());

            if (!field.incomplete()) {
                // <b>E aí ele planta árvore na borda</b> — habilidade
                // nova, decisão do autor de 2026-09-19. Aqui, e só aqui:
                // é o ponto em que o fazendeiro varreu o raio inteiro e
                // não achou nada de lavoura para fazer. Plantar é o que
                // sobra de útil, e a vila precisa — a obra de 13:04
                // parou esperando jungle_door num deserto cuja colônia
                // tinha dez toras ao todo. Ver TreeNursery.
                FarmerNursery.plantIfItIsTime(world, colonyId, job.center);

                FieldRest.sweptAndFoundNothing(colonyId, world.getTime());
            }

            return;
        }

        // Achou: o campo voltou a render, e o descanso não vale mais.
        FieldRest.thereIsWorkAgain(colonyId);

        IdleLog.clear(colonyId, FarmerWork.SUBJECT);

        job.target = found.get();
        job.chore = chore;
        job.stalled = 0;

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

        WorkTargets.set(workerId, job.target);
    }

    /**
     * Planta a semente do baú no canteiro — 2026-09-05.
     *
     * <p><b>A semente sai antes de a muda entrar, e volta se não
     * entrar.</b> É a mesma conta do fundidor: tirar do baú e não
     * entregar nada seria a colônia destruindo material do jogador.
     */
    static void sow(
            ServerWorld world, VillagerEntity villager, Job job, WorkerStorage storage) {

        Optional<Item> seed = ChestWithdrawer.seedIn(world, storage.chestPosition());

        if (seed.isEmpty()
                || !ChestWithdrawer.takeSeed(world, storage.chestPosition(), seed.get())) {

            FarmerWork.release(villager.getUuid(), job);

            return;
        }

        villager.swingHand(Hand.MAIN_HAND);

        if (CropPatch.sow(world, job.target, seed.get())) {
            VillageColonyMod.LOGGER.info(
                    "Farmer {} sowed {} at {}",
                    villager.getUuid(),
                    seed.get(),
                    job.target.toShortString());
        } else {
            ChestDepositor.deposit(world, storage.chestPosition(), seed.get(), 1);
        }

        FarmerWork.release(villager.getUuid(), job);
    }

    /**
     * Colhe, replanta e guarda.
     *
     * <p>A ordem importa: a semente sai da própria colheita, então é
     * preciso ter o que caiu em mãos antes de replantar. O que sobra vai
     * para o baú.
     */
    static void harvest(
            ServerWorld world, VillagerEntity villager, Job job, WorkerStorage storage) {

        BlockState state = world.getBlockState(job.target);

        villager.swingHand(Hand.MAIN_HAND);

        List<ItemStack> drops = new ArrayList<>(
                Block.getDroppedStacks(state, world, job.target, null, null, ItemStack.EMPTY));

        // Replantar antes de guardar — a Regra 7, onde ela nasceu. O
        // jogo devolve a semente junto com a comida, e ela sai do que
        // caiu em vez de sair do baú.
        boolean replanted = CropPatch.replant(world, job.target, state, drops);

        int took = store(world, storage, drops);

        job.collected += took;

        VillageColonyMod.LOGGER.info(
                "Farmer {} harvested {} at {} — {} this task, {}",
                villager.getUuid(),
                took,
                job.target.toShortString(),
                job.collected,
                replanted ? "replanted" : "nothing left to replant");

        FarmerWork.release(villager.getUuid(), job);
    }

    /**
     * Guarda no baú do fazendeiro o que sobrou depois de replantar.
     *
     * <p>Devolve quantos <b>entraram</b>. O {@code ChestDepositor}
     * devolve quantos não couberam, e ler ao contrário foi o defeito que
     * o mineiro cometeu no primeiro teste dele — todo item guardado
     * virava uma linha de "baú cheio" com o baú vazio ao lado.
     */
    static int store(
            ServerWorld world, WorkerStorage storage, List<ItemStack> drops) {

        int stored = 0;

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }

            int leftOver = ChestDepositor.deposit(
                    world, storage.chestPosition(), drop.getItem(), drop.getCount());

            stored += drop.getCount() - leftOver;

            if (leftOver > 0) {
                VillageColonyMod.LOGGER.warn(
                        "Chest of farmer at {} filled up — {} of {} lost",
                        storage.chestPosition(),
                        leftOver,
                        drop.getCount());
            }
        }

        return stored;
    }
}
