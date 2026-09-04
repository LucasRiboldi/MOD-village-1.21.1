package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.TreeHarvester;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.world.chunk.WorldChunk;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Derrubar o bloco e guardar o que ele deu.
 *
 * <p>Saiu de {@code LumberjackWork} em 2026-08-20, quando ele passou de
 * mil e duzentas linhas. É o único lugar do lenhador que <b>escreve no
 * mundo</b>, e isso é o que o torna um assunto: escolher árvore, andar
 * até ela e contar o que se está fazendo não derrubam nada.
 *
 * <p>A Regra 2 mora aqui — a velocidade de um jogador com machado de
 * ferro — e a Regra 1 encosta nela: o baú confere espaço antes de o
 * bloco sair do mundo, porque um tronco derrubado sem onde caber é item
 * perdido (o E3 do §17).
 */
public final class TreeFelling {

    private TreeFelling() {
    }

    /**
     * A ferramenta que o tempo de quebra assume.
     *
     * <p>É a Regra 2 ao pé da letra: ferramenta de ferro. Isto é o
     * relógio da colheita, não um inventário.
     *
     * <p>Desde 2026-08-13 o lenhador <b>carrega</b> um machado de
     * madeira, entregue por {@code WorkerEquipment} porque
     * Profession-System.md manda entregá-lo. E mesmo assim esta linha
     * continua de ferro, de propósito: perguntar ao trabalhador o que ele
     * tem na mão tornaria a colheita mais lenta do que a Regra 2 manda —
     * seria trocar uma regra do autor por uma consequência de
     * implementação. O dia de perguntar é o dia em que a evolução de
     * ferramenta existir, e ela não pertence ao MVP.
     */
    private static final Item TOOL = Items.IRON_AXE;

    /**
     * Quantos estágios de rachadura o cliente conhece.
     *
     * <p>Vanilla desenha de 0 a 9. Sem isso a Regra 2 seria invisível:
     * o jogador veria um aldeão parado ao lado de uma árvore que some
     * sozinha meio minuto depois.
     */
    private static final int BREAKING_STAGES = 10;

    /** De quantos em quantos ticks o braço balança. */
    private static final int SWING_INTERVAL = 5;

    /**
     * Um tick de machado no bloco da vez.
     *
     * <p>O contador sobe; quando alcança o que o bloco pede, o bloco cai
     * e o que ele deu vai direto para o baú. É o único ponto do mod que
     * escreve no mundo a cada tick, e escreve um bloco por lenhador —
     * o custo por tick tem de continuar cabendo num tick.
     */
    static void chop(
            ServerWorld world, VillagerEntity villager, LumberjackWork.Job job, WorkerStorage storage) {

        BlockPos pos = job.currentBlock();
        BlockState state = stateAt(world, pos);

        if (state == null) {
            // Chunk descarregado no meio da colheita. Esperar é melhor
            // que pular: o bloco continua lá, e o aldeão também.
            return;
        }

        if (job.required == 0) {
            job.required = BlockBreakTime.ticksFor(world, pos, state, TOOL);
        }

        job.progress++;

        if (job.progress % SWING_INTERVAL == 1) {
            villager.swingHand(Hand.MAIN_HAND);
        }

        if (job.progress < job.required) {
            world.setBlockBreakingInfo(
                    villager.getId(), pos, job.progress * BREAKING_STAGES / job.required);

            return;
        }

        // Rachadura apagada antes de o bloco sair: um estágio deixado
        // para trás fica desenhado no ar até o cliente recarregar.
        world.setBlockBreakingInfo(villager.getId(), pos, -1);

        List<ItemStack> drops = TreeHarvester.breakOne(world, job.plan, pos);

        job.collected += countLogs(drops, job.plan);

        deposit(world, job, storage, drops);

        job.index++;
        job.progress = 0;
        job.required = 0;
        job.stalled = 0;
    }

    /**
     * Encerra a tarefa por baú cheio.
     *
     * <p>É o fim previsto pela Regra 1, e não uma falha. A linha diz
     * quanto a tarefa rendeu porque é a única prova em jogo de que o
     * trabalho contínuo aconteceu — sem ela, um lenhador que trabalhou
     * dez minutos e um que nunca achou árvore produzem o mesmo silêncio.
     *
     * <p>Pode chegar aqui com a tarefa ainda RESERVED, e é o caso comum
     * da Regra 1: o baú termina quase cheio, o ciclo seguinte abre um
     * pedido do tamanho do espaço que sobrou, e a primeira árvore que o
     * lenhador olha já não cabe. Ele encerra sem ter derrubado nada, e
     * {@code Task.complete} exige EXECUTING — completar direto lançava
     * dentro do tick do servidor e derrubava o mundo. A transição é a
     * mesma que {@code startNextTree} faz ao começar uma árvore.
     */
    static void finishTask(LumberjackWork.Job job, UUID workerId, WorkerStorage storage, int room) {
        if (job.task.state() == TaskState.RESERVED) {
            job.task.start();
        }

        job.task.complete();

        // Tarefa cumprida, aldeão liberado. É a cessão imediata da
        // ADR-004 §5: sem destino, a task do Brain para e ele volta à
        // rotina Vanilla no mesmo tick.
        WorkTargets.clear(workerId);

        VillageColonyMod.LOGGER.info(
                "Worker {} filled the chest — {} logs collected, {} more would fit",
                storage.workerId(),
                job.collected,
                room);
    }

    /**
     * Põe nos baús da colônia tudo o que o bloco deu.
     *
     * <p>Tronco, muda, maçã, graveto: o que a tabela de loot der. A
     * colônia só conta os troncos, e os outros ficam no baú sem contagem
     * — o que não é perda, é a regra de sempre: item fora da lista
     * continua no baú, apenas não é contado.
     *
     * <p><b>Mas não contado é diferente de inofensivo, e foi o que
     * 2026-09-04 ensinou.</b> Vara e maçã não são {@code ResourceType}
     * nenhum, então nenhum trabalhador as retira e nenhuma meta as
     * enxerga — e cada uma ocupa um slot para sempre. O baú do lenhador
     * assoreia, o espaço de madeira dele só desce, e ao chegar a zero ele
     * morre em definitivo: naquela sessão foram cinquenta e nove ciclos
     * sem derrubar nada e vinte e quatro troncos destruídos aqui dentro.
     *
     * <p>Transbordar para a colônia tira o lenhador do buraco sem mover o
     * assoreamento de lugar — o tronco tem consumidor em qualquer baú.
     * <b>O assoreamento em si continua de pé:</b> nada esvazia vara e
     * maçã de baú nenhum, e baú que só enche acaba cheio. Dar consumidor
     * ou descarte a esses itens é decisão de projeto, e está em aberto.
     */
    private static void deposit(
            ServerWorld world, LumberjackWork.Job job, WorkerStorage storage,
            List<ItemStack> drops) {

        // O baú do próprio primeiro, os da colônia depois. A retirada já
        // percorre a colônia inteira desde 2026-08-14; o depósito ficou
        // para trás, e a sessão de 2026-09-04 cobrou o outro lado — vinte
        // e quatro troncos destruídos porque o baú do lenhador tinha
        // assoreado de vara e maçã, que nada retira de baú nenhum.
        //
        // Transbordar não move o assoreamento de lugar: o tronco tem
        // consumidor em qualquer baú, porque o fabricante retira de
        // todos.
        List<ColonyPos> chests =
                ColonyChests.ownFirst(job.task.colonyId(), storage.chestPosition());

        for (ItemStack stack : drops) {
            int leftOver = ColonyChests.deposit(
                    world, chests, stack.getItem(), stack.getCount());

            if (leftOver == 0) {
                continue;
            }

            // Agora só se chega aqui com a colônia inteira cheia, e aí é
            // notícia de verdade: o jogador precisa esvaziar alguma
            // coisa, e o item já saiu do mundo.
            VillageColonyMod.LOGGER.warn(
                    "Colony of worker {} had no room mid-harvest — {} of {} were lost"
                            + " across {} chests",
                    storage.workerId(),
                    leftOver,
                    stack.getItem(),
                    chests.size());
        }
    }

    /** Quantos troncos havia no que este bloco deu. */
    private static int countLogs(List<ItemStack> drops, TreeHarvester.Plan plan) {
        Item log = plan.species().log().asItem();
        int logs = 0;

        for (ItemStack stack : drops) {
            if (stack.isOf(log)) {
                logs += stack.getCount();
            }
        }

        return logs;
    }

    /**
     * O estado de um bloco, ou {@code null} se o chunk não está
     * carregado.
     *
     * <p>Nunca {@code world.getBlockState} direto. Ele carrega o chunk
     * que faltar, e do tick do servidor isso significa gerar terreno
     * dentro do laço. Ver §11.
     */
    static BlockState stateAt(ServerWorld world, BlockPos pos) {
        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);

        return chunk == null ? null : chunk.getBlockState(pos);
    }
}
