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
        Optional<CraftingLookup.Bill> found = billFor(world, chests, item);

        if (found.isEmpty() || !enough(world, chests, found.get())) {
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

    private static boolean enoughFor(ServerWorld world, List<ColonyPos> chests, Item item) {
        return billFor(world, chests, item)
                .map(bill -> enough(world, chests, bill))
                .orElse(false);
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
