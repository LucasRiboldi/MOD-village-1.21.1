package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.OreVein;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

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
     * Guarda o que caiu no baú do mineiro.
     *
     * <p>O que não couber é perdido, e é o mesmo E3 do lenhador: o bloco
     * já saiu do mundo. Fica em WARN para não sumir em silêncio.
     *
     * @return quantas peças entraram
     */
    static int deposit(
            ServerWorld world, WorkerStorage storage, List<ItemStack> drops, ColonyPos treasure) {

        ColonyPos chest = storage.chestPosition();
        int stored = 0;

        for (ItemStack drop : drops) {
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
            int leftOver = ChestDepositor.deposit(
                    world, chest, drop.getItem(), drop.getCount());

            stored += drop.getCount() - leftOver;

            if (leftOver > 0) {
                VillageColonyMod.LOGGER.warn(
                        "Chest of miner at {} filled up — {} of {} lost",
                        chest,
                        leftOver,
                        drop.getCount());
            }
        }

        return stored;
    }
}
