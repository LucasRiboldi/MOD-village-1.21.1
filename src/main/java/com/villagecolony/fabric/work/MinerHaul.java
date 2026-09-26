package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.integration.ChestDepositor;
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
 * <p>Desde 2026-09-15 é um destino só, por decisão do autor: tudo o que o
 * mineiro cava vai diretamente para o seu próprio baú.
 */
final class MinerHaul {

    private MinerHaul() {
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
     * Guarda o que caiu no baú do mineiro. O excedente fica como item no
     * mundo, sem contaminar o baú de outro trabalhador.
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
            BlockPos dropPosition,
            Item wanted) {

        ColonyPos chest = storage.chestPosition();

        int stored = 0;
        int asked = 0;

        for (ItemStack drop : drops) {
            boolean isAsked = wanted != null && drop.isOf(wanted);
            int before = stored;
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
