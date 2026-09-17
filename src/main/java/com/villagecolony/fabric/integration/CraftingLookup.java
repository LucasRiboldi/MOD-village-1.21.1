package com.villagecolony.fabric.integration;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;

import java.util.LinkedHashMap;
import java.util.ArrayList;
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
     * O que a fornalha faz com este item — 2026-08-20.
     *
     * <p>A exceção honesta que a Regra 10 registrou em 08-18: a vidraça
     * pede vidro, vidro pede fundir, e a colônia não fundia. Agora funde.
     *
     * <p>Pergunta ao próprio jogo, como {@link #resultOfOne} faz com a
     * bancada: quem sabe que areia vira vidro é o Minecraft, e escrever
     * a tabela aqui a faria envelhecer no primeiro datapack.
     */
    public static Optional<ItemStack> smelted(ServerWorld world, ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }

        return world.getRecipeManager()
                .getFirstMatch(
                        RecipeType.SMELTING,
                        new net.minecraft.recipe.input.SingleStackRecipeInput(input),
                        world)
                .map(entry -> entry.value().craft(
                        new net.minecraft.recipe.input.SingleStackRecipeInput(input),
                        world.getRegistryManager()))
                .filter(result -> !result.isEmpty());
    }

    /**
     * O que entra na fornalha para sair isto — 2026-08-22.
     *
     * <p>O inverso de {@link #smelted}, e ele existe para tirar do mod a
     * última tabela de "quem vira o quê". Até aqui o fundidor tinha duas
     * linhas escritas à mão — areia dá vidro, ferro cru dá lingote — e
     * o arenito liso seria a terceira. A ADR-009 pede o contrário: quem
     * sabe o que a fornalha faz é o livro de receitas do jogo.
     *
     * <p><b>Todos os candidatos, e não o primeiro.</b> Lingote de ferro
     * sai de três coisas — minério, minério de ardósia e ferro cru — e
     * qual delas serve depende do que está no baú, não da ordem em que o
     * livro as devolve. Devolver só a primeira seria escolher por acaso.
     *
     * <p>Vazio quando a fornalha não faz isto — datapack que mudou,
     * material que não é de fornalha —, e vazio aqui é o fundidor
     * encerrando a tarefa em vez de queimar o que não devia.
     */
    public static List<Item> smeltingInputsFor(ServerWorld world, Item wanted) {
        List<Item> inputs = new ArrayList<>();

        for (RecipeEntry<SmeltingRecipe> entry
                : world.getRecipeManager().listAllOfType(RecipeType.SMELTING)) {

            if (!entry.value().getResult(world.getRegistryManager()).isOf(wanted)) {
                continue;
            }

            for (Ingredient slot : entry.value().getIngredients()) {
                for (ItemStack option : slot.getMatchingStacks()) {
                    if (!option.isEmpty() && !inputs.contains(option.getItem())) {
                        inputs.add(option.getItem());
                    }
                }
            }
        }

        return inputs;
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
                Bill bench = new Bill(target, result.getCount(), needed.get());

                // <b>E o cortador, se ele sair mais barato</b> —
                // 2026-09-17. A escada de pedregulho custa seis por
                // quatro na bancada e um por um no cortador; a laje,
                // três por seis contra um por dois. Preferir a bancada
                // sempre era gastar pedra tendo a ferramenta certa no
                // baú do pedreiro. Ver cutFor.
                return Optional.of(cheaperOf(bench, cutFor(world, target, available)));
            }
        }

        return cutFor(world, target, available);
    }

    /**
     * Entre a bancada e o cortador, a que gasta menos por peça.
     *
     * <p>Compara <b>ingrediente por resultado</b>, e não ingrediente
     * solto: uma receita que pede seis e devolve quatro custa 1,5 por
     * peça, e uma que pede um e devolve um custa 1,0. Comparar só o
     * total escolheria a errada toda vez que a bancada rendesse mais.
     *
     * <p>Empate fica com a bancada, que é o caminho que o mod já andava
     * — mudar o que já funciona precisa de motivo, e empate não é.
     */
    private static Bill cheaperOf(Bill bench, Optional<Bill> cut) {
        if (cut.isEmpty()) {
            return bench;
        }

        return costPerPiece(cut.get()) < costPerPiece(bench) ? cut.get() : bench;
    }

    private static double costPerPiece(Bill bill) {
        int total = 0;

        for (int amount : bill.ingredients().values()) {
            total += amount;
        }

        return bill.resultCount() <= 0 ? total : (double) total / bill.resultCount();
    }

    /**
     * A mesma peça, no cortador de pedra — 2026-09-17.
     *
     * <p><b>Consultado sempre, e escolhido só quando é mais barato</b> —
     * ver {@link #cheaperOf}. A primeira versão o punha como segunda
     * pergunta, respondida só quando a bancada falhasse, e o gametest
     * mostrou que isso não economizava nada: para a escada de pedregulho
     * a bancada <b>responde</b>, a seis por quatro, e o cortador nunca
     * era alcançado. A ordem foi corrigida pela medição.
     *
     * <p><b>Por que existe.</b> O pedreiro recebe {@code Items.STONECUTTER}
     * do {@code ChestMarker}, e o mod nunca consultava
     * {@code RecipeType.STONECUTTING}: só {@code CRAFTING} e
     * {@code SMELTING}. A escada de pedregulho custa <b>seis por quatro</b>
     * na bancada e <b>um por um</b> no cortador, e a laje custa três por
     * seis contra um por dois — a colônia gastava pedra que não precisava
     * gastar, tendo a ferramenta certa no baú.
     *
     * <p><b>Uma entrada só, e é o que torna isto barato.</b>
     * {@code StonecuttingRecipe} estende {@code CuttingRecipe} e carrega
     * um {@code Ingredient} único — não é grade. Conferido por
     * {@code javap} no JAR de 1.21.1.
     */
    private static Optional<Bill> cutFor(
            ServerWorld world, Item target, Predicate<Item> available) {

        for (RecipeEntry<StonecuttingRecipe> entry
                : world.getRecipeManager().listAllOfType(RecipeType.STONECUTTING)) {

            ItemStack result = entry.value().getResult(world.getRegistryManager());

            if (!result.isOf(target) || result.isEmpty()) {
                continue;
            }

            List<Ingredient> slots = entry.value().getIngredients();

            if (slots.isEmpty()) {
                continue;
            }

            Optional<Item> chosen = firstAvailable(slots.get(0), available);

            if (chosen.isEmpty()) {
                continue;
            }

            return Optional.of(new Bill(
                    target, result.getCount(), Map.of(chosen.get(), 1)));
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
