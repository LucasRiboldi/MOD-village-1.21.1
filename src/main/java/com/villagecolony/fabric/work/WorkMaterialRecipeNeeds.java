package com.villagecolony.fabric.work;

import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        Optional<Item> piece;
        Set<ResourceType> countedElsewhere = Set.of();
        if (target.isPresent()) {
            if (target.get().production() != Production.CRAFTED_STONE) {
                return;
            }
            piece = MinecraftTypeAdapter.toItem(target.get());
        } else {
            // Lingote e vidro já têm a conta deles (WorkMaterials.iron e .glass).
            countedElsewhere = OWN_COUNT;
            // A peça sem recurso declarado também desce pela receita: os tijolos
            // (bricks) pedem tijolo assado (brick) e não eram vistos — a obra do
            // playtest 0.3.32 esperou 10 min por bricks sem tarefa nenhuma aberta.
            piece = MinecraftTypeAdapter.toBlock(material).map(Block::asItem).filter(item -> item != Items.AIR);
        }
        Set<ResourceType> skip = countedElsewhere;
        piece.ifPresent(item -> addSmeltedOf(world, item, needed, wanted, skip, DEPTH));
    }

    /** Assados que a meta já pede por conta própria, peça por peça. */
    private static final Set<ResourceType> OWN_COUNT = Set.of(ResourceType.IRON_INGOT, ResourceType.GLASS);

    /** Quantos degraus de receita a busca do assado desce (escada de tijolo → tijolos → tijolo). */
    static final int DEPTH = 3;

    private static void addSmeltedOf(
            ServerWorld world, Item item, int needed, Map<ResourceType, Integer> wanted, Set<ResourceType> skip,
            int depth) {
        if (depth <= 0) {
            return;
        }
        CraftingLookup.billFor(world, item, any -> true).ifPresent(bill -> {
            int batches = (needed + bill.resultCount() - 1) / bill.resultCount();
            bill.ingredients().forEach((ingredient, amount) -> {
                Optional<ResourceType> type = MinecraftTypeAdapter.toResourceType(ingredient);
                if (type.isPresent() && type.get().production() == Production.SMELTED && !skip.contains(type.get())) {
                    wanted.merge(type.get(), batches * amount, Integer::sum);
                } else if (type.isEmpty()) {
                    addSmeltedOf(world, ingredient, batches * amount, wanted, skip, depth - 1);
                }
            });
        });
    }
}
