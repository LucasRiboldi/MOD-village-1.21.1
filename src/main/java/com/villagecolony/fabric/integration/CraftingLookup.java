package com.villagecolony.fabric.integration;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

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

    /**
     * A lista de compras de um item, resolvida contra o que existe.
     *
     * <p>O que a receita pede, com o ingrediente já escolhido: uma
     * receita aceita "qualquer tábua", e a conta só fecha depois de
     * saber <b>qual</b> tábua a colônia tem.
     *
     * @param result o que sai, e quantos saem de uma feitura
     * @param resultCount quantos saem de uma feitura
     * @param ingredients o que entra, por item e quantidade
     */
    public record Bill(Item result, int resultCount, Map<Item, Integer> ingredients) {
    }

    /**
     * Como fazer este item com o que a colônia tem — a Regra 10.
     *
     * <p>Procura a receita pelo <b>resultado</b>, que é o contrário de
     * {@link #resultOfOne}: ali a pergunta é "o que sai disto?", e aqui
     * é "o que faz isto?". A obra pede uma porta, e é esta a pergunta
     * que faltava na sessão de 2026-08-18 — a colônia tinha 154 tábuas
     * e nenhuma porta, e nada ligava uma coisa à outra.
     *
     * <p>Cada casa da receita vira um item concreto por
     * {@code available}: a receita da porta aceita tábua de qualquer
     * madeira, e escolher a que não existe no baú produziria uma lista
     * de compras impossível. Casa que nenhum item disponível satisfaz
     * derruba a receita inteira — a colônia não sabe fazer isto hoje, e
     * dizer isso é melhor que fabricar pela metade.
     *
     * <p>Só receita de bancada, como o resto desta classe. Uma porta
     * cabe na bancada; fundir vidro não, e é por isso que a janela
     * continua sendo material que o jogador guarda — ver a Regra 13.
     *
     * @param available responde se a colônia tem esse item para gastar
     * @return vazio quando nada faz este item, ou quando falta
     *     ingrediente que a colônia não tem
     */
    public static Optional<Bill> billFor(
            ServerWorld world, Item target, Predicate<Item> available) {

        for (RecipeEntry<CraftingRecipe> entry
                : world.getRecipeManager().listAllOfType(RecipeType.CRAFTING)) {

            ItemStack result = entry.value().getResult(world.getRegistryManager());

            if (!result.isOf(target) || result.isEmpty()) {
                continue;
            }

            Optional<Map<Item, Integer>> needed = resolve(entry.value(), available);

            if (needed.isPresent() && !needed.get().isEmpty()) {
                return Optional.of(new Bill(target, result.getCount(), needed.get()));
            }
        }

        return Optional.empty();
    }

    /**
     * Cada casa da receita vira um item que a colônia tem.
     *
     * <p>Vazio assim que uma casa não puder ser satisfeita: meia lista
     * de compras é pior que nenhuma, porque leva a tirar material do
     * baú para uma feitura que não vai acontecer.
     */
    private static Optional<Map<Item, Integer>> resolve(
            CraftingRecipe recipe, Predicate<Item> available) {

        Map<Item, Integer> needed = new LinkedHashMap<>();

        for (Ingredient slot : recipe.getIngredients()) {
            if (slot.isEmpty()) {
                // Casa vazia da grade. A porta ocupa seis de nove.
                continue;
            }

            Optional<Item> chosen = firstAvailable(slot, available);

            if (chosen.isEmpty()) {
                return Optional.empty();
            }

            needed.merge(chosen.get(), 1, Integer::sum);
        }

        return Optional.of(needed);
    }

    /** O primeiro item desta casa que a colônia tem para gastar. */
    private static Optional<Item> firstAvailable(Ingredient slot, Predicate<Item> available) {
        for (ItemStack candidate : slot.getMatchingStacks()) {
            if (!candidate.isEmpty() && available.test(candidate.getItem())) {
                return Optional.of(candidate.getItem());
            }
        }

        return Optional.empty();
    }
}
