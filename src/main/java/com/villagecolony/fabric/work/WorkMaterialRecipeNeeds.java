package com.villagecolony.fabric.work;

import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.Optional;

/** Calcula a parcela da cadeia de fornalha escondida sob receitas do pedreiro. */
final class WorkMaterialRecipeNeeds {

    private WorkMaterialRecipeNeeds() {
    }

    /**
     * Acrescenta materiais de fornalha consumidos por um bloco produzido pelo pedreiro.
     *
     * <p>Terracota vermelha, por exemplo, usa terracota neutra assada pelo fundidor.
     * Ingredientes automáticos, como corantes, ficam fora da busca física.
     */
    static void addSmeltedIngredients(
            ServerWorld world,
            ResourceId material,
            int needed,
            Map<ResourceType, Integer> wanted) {
        if (world == null || needed <= 0) {
            return;
        }

        Optional<ResourceType> target = MinecraftTypeAdapter.toBlock(material)
                .flatMap(WorkMaterials::firstKnownAlternative);
        if (target.isEmpty() || target.get().production() != Production.CRAFTED_STONE) {
            return;
        }

        MinecraftTypeAdapter.toItem(target.get())
                .flatMap(item -> CraftingLookup.billFor(world, item, any -> true))
                .ifPresent(bill -> {
                    int batches = (needed + bill.resultCount() - 1) / bill.resultCount();
                    bill.ingredients().forEach((ingredient, amount) -> MinecraftTypeAdapter.toResourceType(ingredient)
                            .filter(type -> type.production() == Production.SMELTED)
                            .ifPresent(type -> wanted.merge(type, batches * amount, Integer::sum)));
                });
    }
}
