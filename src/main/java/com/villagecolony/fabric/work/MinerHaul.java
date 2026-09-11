package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.OreVein;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.UUID;

/**
 * Onde vai o que o mineiro cava — a Regra 30.
 *
 * <p>Saiu de {@code MinerWork} em 2026-08-22, quando ele cruzou as
 * quinhentas linhas. É uma pergunta inteira e separada de "o que cavar"
 * e "como cavar": <b>o minério vai para o baú da boca da mina, e o resto
 * para o baú do mineiro na vila</b>.
 *
 * <p>O corte é por responsabilidade, e não por contagem — ADR-009 §6.
 */
final class MinerHaul {

    private MinerHaul() {
    }

    /**
     * O baú da boca da mina desta colônia, quando o que caiu é tesouro.
     *
     * <p>Regra 30, 2026-08-22. Nulo em tudo o mais: pedra, terra e
     * carvão vão direto para o baú do mineiro na vila, que é de onde a
     * obra e a fornalha tiram o que consomem.
     *
     * <p>Nulo também quando a mina não tem baú — boca em encosta, chunk
     * fora de memória —, e aí o tesouro segue o caminho de sempre. É o
     * lado seguro do erro: guardado no lugar errado, nunca perdido.
     */
    static ColonyPos treasureChestFor(
            ServerWorld world, MinerWork.Job job, BlockState state) {

        if (!OreVein.isTreasure(state)) {
            return null;
        }

        return VillageColonyMod.MINES.of(job.task.colonyId())
                .map(mine -> MinecraftTypeAdapter.toBlockPos(mine.shaft().entry()))
                .flatMap(mouth -> MineMouth.chestAt(world, mouth))
                .map(MinecraftTypeAdapter::toColonyPos)
                .orElse(null);
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
     * Guarda o que caiu no baú do mineiro, e na colônia o que não coube.
     *
     * <p><b>O transbordo entrou em 2026-09-11, e fecha a outra metade do
     * E3.</b> Até aqui o que não coubesse no baú do mineiro era
     * destruído — o bloco já tinha saído do mundo, e o WARN só contava a
     * perda. O lenhador deixou de destruir em 2026-09-04, quando ganhou
     * {@link ColonyChests#ownFirst}; o mineiro ficou para trás, e o
     * {@code TODO.md} registrava a metade aberta desde então.
     *
     * <p>A ordem é a mesma do lenhador, e é a que faz o relatório de um
     * mineiro falar do mineiro: o baú dele primeiro, o resto da colônia
     * como transbordo. A boca da mina continua antes de tudo, porque é a
     * Regra 30 e ela vale enquanto houver minério.
     *
     * <p><b>Ainda se perde com a colônia inteira cheia</b>, e aí o WARN é
     * a notícia certa: a essa altura o jogador precisa esvaziar alguma
     * coisa, e precisa poder descobrir isso. O que deixou de acontecer é
     * perder tendo espaço a vinte blocos.
     *
     * @param colonyId de quem são os baús do transbordo
     * @param wanted o item que a tarefa pediu, para a conta sair separada
     *     — nulo quando o pedido não vira item deste jogo, e aí o
     *     {@link Haul#wanted()} sai zero
     * @return o que entrou, separado em tudo e no que foi pedido
     */
    static Haul deposit(
            ServerWorld world,
            UUID colonyId,
            WorkerStorage storage,
            List<ItemStack> drops,
            ColonyPos treasure,
            Item wanted) {

        ColonyPos chest = storage.chestPosition();

        // Uma vez, e não por bloco: a lista sai de percorrer trabalhadores
        // e registros, e isto roda a cada picareta — Performance-Rules §6.
        List<ColonyPos> colonyChests = ColonyChests.ownFirst(colonyId, chest);

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
            // E a lista inteira, não só o baú dele: é o E3 do lenhador,
            // que atravessa os baús da colônia antes de desistir.
            int leftOver = ColonyChests.deposit(
                    world, colonyChests, drop.getItem(), drop.getCount());

            stored += drop.getCount() - leftOver;

            if (leftOver > 0) {
                VillageColonyMod.LOGGER.warn(
                        "Colony chests of miner at {} are full — {} of {} lost"
                                + " ({} chests tried)",
                        chest,
                        leftOver,
                        drop.getCount(),
                        colonyChests.size());
            }

            if (isAsked) {
                asked += stored - before;
            }
        }

        return new Haul(stored, asked);
    }
}
