package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.integration.ChestDepositor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Onde vai o que o mineiro cava — a Regra 30.
 *
 * <p>Saiu de {@code MinerWork} em 2026-08-22, quando ele cruzou as
 * quinhentas linhas. É uma pergunta inteira e separada de "o que cavar"
 * e "como cavar".
 *
 * <p>Era <i>"o minério vai para o baú da boca da mina, e o resto para o
 * baú do mineiro na vila"</i> — a Regra 30, ADR-009 §6. <b>Desde
 * 2026-09-15 é um destino só</b>, por decisão do autor: <i>"retire o baú
 * da boca da mina, use só o baú de cada mineiro"</i>. Ver
 * {@link #treasureChestFor}.
 */
final class MinerHaul {

    private MinerHaul() {
    }

    /**
     * Sempre nulo: tudo o que o mineiro cava vai para o baú dele.
     *
     * <p><b>Decisão do autor, 2026-09-15</b>: <i>"retire o baú da boca da
     * mina, use só o baú de cada mineiro"</i>. Revoga o desvio do tesouro
     * que a Regra 30 criou em 2026-08-22.
     *
     * <p>Era: minério que não fosse carvão ia para o baú da boca, e o
     * resto para o do mineiro. Passa a ser um destino só — o mesmo que a
     * pedra e o carvão sempre tiveram, e de onde a obra e a fornalha já
     * tiram o que consomem.
     *
     * <p><b>Fica como método, e nulo, em vez de sumir</b>: ele é a porta
     * única por onde o depósito pergunta pelo desvio, e o
     * {@link #deposit} já trata o nulo como "sem desvio" desde 2026-08-22
     * — é o caminho que a mina sem baú sempre percorreu. Apagá-lo
     * espalharia a decisão pelo corpo do laço, que é onde ela seria a
     * próxima a discordar de si mesma.
     */
    static ColonyPos treasureChestFor(
            ServerWorld world, MinerWork.Job job, BlockState state) {

        return null;
    }

    /**
     * O que entrou no baú, e quanto disso era o que a tarefa pediu.
     *
     * <p><b>São dois números porque são duas perguntas</b>, e confundi-las
     * foi o E3 da sessão de 2026-09-06: o relatório do mineiro dizia
     * <i>"105 of 32 so far"</i> comparando <b>tudo o que ele guardou</b>
     * — pedregulho, terra, carvão, minério — com a meta de <b>um</b>
     * recurso. Os dois lados da frase nem falavam da mesma coisa.
     *
     * @param stored tudo o que coube no baú, de qualquer item
     * @param wanted quanto disso era o recurso que a tarefa pediu
     */
    record Haul(int stored, int wanted) {
    }

    /**
     * Guarda o que caiu no baú do mineiro ou, se for minério, no baú da
     * boca da mina. O excedente fica como item no mundo, sem contaminar o
     * baú de outro trabalhador.
     *
     * <p>O baú da boca mantém prioridade pela Regra 30; toda outra saída
     * pertence ao trabalhador. A retirada de insumos continua compartilhada
     * e é independente deste destino de produção.
     *
     * @param wanted o item que a tarefa pediu, para a conta sair separada
     *     — nulo quando o pedido não vira item deste jogo, e aí o
     *     {@link Haul#wanted()} sai zero
     * @return o que entrou, separado em tudo e no que foi pedido
     */
    static Haul deposit(
            ServerWorld world,
            WorkerStorage storage,
            List<ItemStack> drops,
            ColonyPos treasure,
            BlockPos dropPosition,
            Item wanted) {

        ColonyPos chest = storage.chestPosition();

        int stored = 0;
        int asked = 0;

        for (ItemStack drop : drops) {
            // Antes de o laço mexer no stack: o desvio do tesouro o
            // reescreve com o que sobrou, e contar depois perderia a
            // parte que foi para o baú da boca da mina.
            boolean isAsked = wanted != null && drop.isOf(wanted);
            int before = stored;
            if (treasure != null) {
                // O baú da boca primeiro, e o do mineiro com o que sobrar
                // — a Regra 30 dita por inteiro.
                int rejected = ChestDepositor.deposit(
                        world, treasure, drop.getItem(), drop.getCount());

                stored += drop.getCount() - rejected;

                if (rejected == 0) {
                    continue;
                }

                drop = new ItemStack(drop.getItem(), rejected);
            }

            // Devolve quantos **não** couberam, e não quantos entraram.
            // Ler ao contrário foi o defeito que este mineiro cometeu no
            // primeiro teste dele: todo pedregulho guardado virava uma
            // linha de "filled up" com o baú vazio ao lado.
            //
            int leftOver = ChestDepositor.deposit(
                    world, chest, drop.getItem(), drop.getCount());

            stored += drop.getCount() - leftOver;

            if (leftOver > 0) {
                world.spawnEntity(new ItemEntity(
                        world,
                        dropPosition.getX() + 0.5,
                        dropPosition.getY() + 0.5,
                        dropPosition.getZ() + 0.5,
                        new ItemStack(drop.getItem(), leftOver)));

                VillageColonyMod.LOGGER.warn(
                        "Miner chest at {} is full — dropped {} of {} at {}",
                        chest,
                        leftOver,
                        drop.getItem(),
                        dropPosition);
            }

            if (isAsked) {
                asked += stored - before;
            }
        }

        return new Haul(stored, asked);
    }
}
