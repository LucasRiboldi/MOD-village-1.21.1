package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Uma peça de material, tirada ou feita — a Regra 10.
 *
 * <p>Mora aqui, e não em {@code BuilderWork}, porque duas coisas
 * precisam dela: a obra em curso e a mobília que entra na casa já
 * terminada, da Regra 21. As duas fazem a mesma pergunta — "tem isto no
 * baú? e se não tiver, dá para fazer?" — e uma resposta diferente entre
 * elas seria uma casa que a obra diz não poder mobiliar e a mobília diz
 * que sim.
 */
public final class ColonySupply {

    /**
     * Quantos passos de receita a colônia desce atrás do que falta.
     *
     * <p><b>Por que ela não era zero, e por que não é infinita.</b> Até
     * 2026-08-21 a colônia só montava o que pudesse montar com
     * <b>todos</b> os ingredientes já no baú, e isso deixava duas peças
     * da casa presas por um degrau:
     *
     * <pre>
     * tocha     carvão e graveto. O carvão a mina passou a dar; o
     *           graveto cai das folhas por sorteio, e ninguém o fazia
     *           de tábua — a colônia tinha 154 tábuas e nenhuma tocha
     *
     * lampião   pepita e tocha. A pepita sai do lingote, que o fundidor
     *           faz do ferro cru; e a tocha é a de cima. São dois
     *           degraus, e é ele que fixa este número em dois
     * </pre>
     *
     * <p>Não é o {@code ItemRequest} do backlog, e não pretende ser: ali
     * o trabalhador <b>pede</b> o que lhe falta e a colônia decide quem
     * atende. Aqui é só a bancada indo um pouco mais fundo, e o teto
     * existe porque cada degrau custa uma varredura do livro de receitas
     * inteiro.
     */
    private static final int RECIPE_DEPTH = 2;

    private ColonySupply() {
    }

    /**
     * Tira uma peça deste item dos baús da colônia, fabricando se
     * preciso.
     *
     * <p>Os baús são percorridos do mais próximo de {@code near} para o
     * mais longe, e a retirada acumula entre eles — a decisão de
     * 2026-08-15.
     *
     * @param near de onde se mede a distância: a obra, ou a casa
     * @return true quando a peça está em mãos
     */
    public static boolean take(ServerWorld world, UUID colonyId, ColonyPos near, Item item) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(colonyId, near);

        if (ColonyChests.withdraw(world, chests, item, 1) > 0) {
            return true;
        }

        if (!craft(world, chests, item)) {
            return false;
        }

        return ColonyChests.withdraw(world, chests, item, 1) > 0;
    }

    /**
     * A colônia tem, ou consegue fazer, este item? Sem tirar nada.
     *
     * <p>Precisa concordar com {@link #take}: uma que dissesse "tem" e
     * outra que não achasse poria a obra a acordar e voltar a dormir
     * todo ciclo.
     */
    public static boolean canProvide(
            ServerWorld world, UUID colonyId, ColonyPos near, Item item) {

        List<ColonyPos> chests = ColonyChests.nearestFirst(colonyId, near);

        return ColonyChests.countIn(world, chests, item) > 0 || enoughFor(world, chests, item);
    }

    /**
     * Fabrica o item com o que os baús têm.
     *
     * <p>Três conferências antes de gastar qualquer coisa, e cada uma
     * evita destruir material do jogador: a receita existe com todos os
     * ingredientes em baú; há quantidade inteira de cada um; e o
     * resultado cabe em algum baú. Fabricar pela metade tranca material
     * num item que não serve, e fabricar sem onde guardar gasta o
     * ingrediente à toa.
     */
    private static boolean craft(ServerWorld world, List<ColonyPos> chests, Item item) {
        return craft(world, chests, item, RECIPE_DEPTH);
    }

    private static boolean craft(
            ServerWorld world, List<ColonyPos> chests, Item item, int depth) {

        Optional<CraftingLookup.Bill> found = billFor(world, chests, item);

        if (missing(world, chests, found) && depth > 0 && makeWhatIsMissing(
                world, chests, item, depth)) {

            // Alguma coisa entrou no baú. A pergunta muda, e vale
            // repeti-la: a receita que não fechava pode fechar agora.
            found = billFor(world, chests, item);
        }

        if (missing(world, chests, found)) {
            return false;
        }

        CraftingLookup.Bill bill = found.get();

        Optional<ColonyPos> room = ColonyChests.firstWithRoomFor(
                world, chests, bill.result(), bill.resultCount());

        if (room.isEmpty()) {
            VillageColonyMod.LOGGER.info(
                    "The colony could make {} and has nowhere to put it — every chest is full",
                    item);

            return false;
        }

        for (Map.Entry<Item, Integer> part : bill.ingredients().entrySet()) {
            ColonyChests.withdraw(world, chests, part.getKey(), part.getValue());
        }

        ChestDepositor.deposit(world, room.get(), bill.result(), bill.resultCount());

        VillageColonyMod.LOGGER.info(
                "The colony made {} {} out of {}", bill.resultCount(), item, bill.ingredients());

        return true;
    }

    private static Optional<CraftingLookup.Bill> billFor(
            ServerWorld world, List<ColonyPos> chests, Item item) {

        return CraftingLookup.billFor(
                world, item, ingredient -> ColonyChests.countIn(world, chests, ingredient) > 0);
    }

    /**
     * Faz primeiro o que falta para esta receita — 2026-08-21.
     *
     * <p>A receita é perguntada ao livro <b>sem</b> filtrar pelo baú: é
     * assim que se descobre o que falta, e não só que alguma coisa falta.
     * Depois, cada ingrediente que o baú não cobre é tentado pela mesma
     * porta, um degrau mais raso.
     *
     * <p>Falhar num ingrediente não desiste dos outros: uma receita de
     * duas casas pode ter uma que a colônia sabe fazer e outra que não, e
     * fazer a primeira aproxima a segunda de valer a pena — na volta
     * seguinte o material dela pode ter chegado.
     *
     * @return se alguma coisa foi realmente feita
     */
    private static boolean makeWhatIsMissing(
            ServerWorld world, List<ColonyPos> chests, Item item, int depth) {

        Optional<CraftingLookup.Bill> recipe =
                CraftingLookup.billFor(world, item, anything -> true);

        if (recipe.isEmpty()) {
            return false;
        }

        boolean made = false;

        for (Map.Entry<Item, Integer> part : recipe.get().ingredients().entrySet()) {
            if (ColonyChests.countIn(world, chests, part.getKey()) >= part.getValue()) {
                continue;
            }

            if (craft(world, chests, part.getKey(), depth - 1)) {
                VillageColonyMod.LOGGER.info(
                        "The colony made {} because {} needs it", part.getKey(), item);

                made = true;
            }
        }

        return made;
    }

    /** Não há receita, ou há e falta ingrediente para ela. */
    private static boolean missing(
            ServerWorld world, List<ColonyPos> chests, Optional<CraftingLookup.Bill> bill) {

        return bill.isEmpty() || !enough(world, chests, bill.get());
    }

    private static boolean enoughFor(ServerWorld world, List<ColonyPos> chests, Item item) {
        return enoughFor(world, chests, item, RECIPE_DEPTH);
    }

    /**
     * A colônia conseguiria fazer isto, sem tirar nada do baú?
     *
     * <p>Anda pelos mesmos degraus que {@link #craft}, e tem de andar:
     * uma resposta que dissesse "não" onde o {@code craft} faria poria a
     * obra a esperar por peça que a colônia sabe montar, e uma que
     * dissesse "sim" onde ele falha a poria a acordar e voltar a dormir
     * todo ciclo.
     *
     * <p><b>É uma aproximação, e a mesma dos dois lados:</b> conta o que
     * falta, e não quantas feituras cada falta custaria. Uma receita que
     * pedisse trinta gravetos diria "sim" com tábua para dez — e o
     * {@code craft} faria os dez e pararia, que é o mesmo engano na mesma
     * direção. O que não pode é os dois discordarem.
     */
    private static boolean enoughFor(
            ServerWorld world, List<ColonyPos> chests, Item item, int depth) {

        if (!missing(world, chests, billFor(world, chests, item))) {
            return true;
        }

        if (depth <= 0) {
            return false;
        }

        Optional<CraftingLookup.Bill> recipe =
                CraftingLookup.billFor(world, item, anything -> true);

        if (recipe.isEmpty()) {
            return false;
        }

        for (Map.Entry<Item, Integer> part : recipe.get().ingredients().entrySet()) {
            if (ColonyChests.countIn(world, chests, part.getKey()) >= part.getValue()) {
                continue;
            }

            if (!enoughFor(world, chests, part.getKey(), depth - 1)) {
                return false;
            }
        }

        return true;
    }

    private static boolean enough(
            ServerWorld world, List<ColonyPos> chests, CraftingLookup.Bill bill) {

        for (Map.Entry<Item, Integer> part : bill.ingredients().entrySet()) {
            if (ColonyChests.countIn(world, chests, part.getKey()) < part.getValue()) {
                return false;
            }
        }

        return true;
    }
}
