package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

/**
 * A terceira busca vazia põe o material no baú de quem o usa — pedido do
 * autor, 2026-10-03.
 *
 * <p><b>O que se via.</b> No playtest das 01:02 a vila das planícies disse
 * {@code has no sand anywhere in the radius} e os dois fundidores pararam
 * por areia 64 vezes em meia hora; a obra esperou a vidraça até a barreira
 * de teste riscá-la. A busca respondia "não há", o castigo dobrava, e nada
 * mais acontecia — a mesma lição de "rota na receita não é rota no mundo".
 *
 * <p><b>A regra.</b> Achar no alcance da vila continua sendo trabalho da
 * profissão, que vai buscar. Três varreduras completas e vazias seguidas
 * querem dizer que o alcance não tem: o material aparece no baú da
 * profissão que o consome — o fundidor, se a fornalha o transforma; o
 * construtor, se não —, ou no primeiro baú da vila com espaço. Qualquer
 * baú da vila serve para quem precisa, inclusive o do jogador
 * ({@code ColonyChests.nearestFirst}).
 */
final class LocateFallback {

    /** Varreduras vazias seguidas antes de o material aparecer. */
    static final int ATTEMPTS = 3;

    private LocateFallback() {
    }

    /**
     * Uma varredura completa não achou o recurso da tarefa.
     *
     * @param empty quantas seguidas não acharam, contando esta
     * @return se o material foi posto no baú
     */
    static boolean afterEmptySearch(ServerWorld world, Task task, int empty) {
        if (empty < ATTEMPTS) {
            return false;
        }

        Optional<Item> item = MinecraftTypeAdapter.toItem(task.targetResource());

        if (item.isEmpty()) {
            return false;
        }

        int amount = Math.clamp(task.amount(), 1, item.get().getMaxCount());
        ProfessionType user = userOf(world, item.get());
        boolean stocked = VillageColonyMod.COLONIES.find(task.colonyId())
                .map(colony -> BiomeConstructionSupply.stockAfterEmptySearches(
                        world, colony.id(), colony.center(), item.get(), amount, user))
                .orElse(false);

        if (stocked) {
            // A contagem recomeça: se o baú esvaziar, a profissão volta a
            // procurar antes de o material aparecer de novo.
            EmptySweeps.found(task.colonyId(), task.targetResource());
        }

        return stocked;
    }

    /** Quem consome: a fornalha transforma areia, argila e cacto; o resto vai para a obra. */
    static ProfessionType userOf(ServerWorld world, Item item) {
        return CraftingLookup.smelted(world, new ItemStack(item)).isPresent()
                ? ProfessionType.SMELTER
                : ProfessionType.BUILDER;
    }
}
