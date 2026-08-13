package com.villagecolony.fabric.integration;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;

/**
 * O que um item vira, segundo o próprio jogo — Fase 9.
 *
 * <p>A receita não mora no mod. É a mesma escolha da tabela de loot da
 * colheita, e pelo mesmo motivo: escrever "um tronco dá quatro tábuas"
 * aqui seria inventar uma segunda verdade sobre o jogo, que passaria a
 * divergir dele no dia em que um datapack ou uma versão nova mudasse a
 * receita. Quem responde é o {@code RecipeManager} do servidor.
 *
 * <p>Só receita de bancada de uma casa. É o que o MVP precisa — tronco
 * vira tábua, e essa receita cabe na mão, sem mesa — e é o que dá para
 * fazer sem decidir onde a colônia fabrica. A pergunta "onde se
 * fabrica?" só precisa de resposta quando existir receita que exija
 * bancada, e aí ela será uma decisão do autor.
 */
public final class CraftingLookup {

    private CraftingLookup() {
    }

    /**
     * O que sai de um item só, se alguma receita o aceitar sozinho.
     *
     * <p>Vazio quando nada é feito só com ele — que é o caso da maioria
     * das coisas, e não é erro.
     *
     * <p>A quantidade do resultado é a da receita: quatro tábuas por
     * tronco, hoje, porque é o que o jogo diz. O mod não conta com esse
     * número em lugar nenhum.
     */
    public static Optional<ItemStack> resultOfOne(ServerWorld world, ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }

        // Uma casa só: é assim que o jogador faz tábua no próprio
        // inventário, sem mesa de trabalho.
        CraftingRecipeInput grid = CraftingRecipeInput.create(
                1, 1, List.of(new ItemStack(input.getItem(), 1)));

        Optional<RecipeEntry<CraftingRecipe>> recipe =
                world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, grid, world);

        return recipe.map(entry -> entry.value().craft(grid, world.getRegistryManager()))
                .filter(result -> !result.isEmpty());
    }
}
