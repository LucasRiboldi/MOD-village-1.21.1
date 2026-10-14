package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A obra aberta reserva o cru que ainda vai pôr — P3b, autossuficiência, 2026-10-11.
 *
 * <p>Peça de estoque (a que a obra aberta não pede) e fornalha só gastam o cru
 * acima do que a obra ainda precisa dele. No playtest de 11-10 o pedreiro lavrou
 * 318 lajes e o fundidor assou 186 pedregulhos enquanto a obra deixava o
 * pedregulho de lado 252 vezes.
 */
final class BuildReserve {

    private BuildReserve() {
    }

    /** Quanto deste item a obra aberta da colônia ainda vai pôr como bloco. */
    static int buildNeedOf(Colony colony, Item item) {
        return ConstructionDemand.materialNeededBy(MinecraftTypeAdapter.toResourceId(item), colony);
    }

    /**
     * O ingrediente que esta peça de estoque tiraria da obra aberta, se houver.
     *
     * <p>Vazio quando a própria obra pede a peça (fazê-la é trabalhar para a
     * obra) ou quando a receita só gasta o que sobra acima da reserva.
     */
    static Optional<Item> takenFromTheBuild(
            ServerWorld world, Colony colony, List<ColonyPos> chests, Item piece) {

        if (buildNeedOf(colony, piece) > 0) {
            return Optional.empty();
        }

        Optional<CraftingLookup.Bill> bill = CraftingLookup.billFor(
                world, piece, item -> ColonyChests.countIn(world, chests, item) > 0);

        if (bill.isEmpty()) {
            return Optional.empty();
        }

        for (Map.Entry<Item, Integer> ingredient : bill.get().ingredients().entrySet()) {
            int need = buildNeedOf(colony, ingredient.getKey());

            if (need > 0 && ColonyChests.countIn(world, chests, ingredient.getKey()) - ingredient.getValue() < need) {
                return Optional.of(ingredient.getKey());
            }
        }

        return Optional.empty();
    }
}
