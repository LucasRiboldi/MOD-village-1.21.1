package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Produtos de valor quando o estoque básico está cheio — escolha do autor, 2026-10-28 (jogo da 0.3.99:
 * lenhador 74%, pedreiro 48% e fundidor 44% parados com o estoque cheio). O titular sem tarefa do ofício,
 * no expediente, faz um lote de {@link #BATCH} por minuto: o lenhador faz carvão vegetal da tora, o
 * pedreiro faz tijolo de pedra e pedra polida, o fundidor faz pedra, vidro e terracota. Só com o cru acima de
 * {@link #KEEP_RAW} e o produto abaixo de {@link #CAP}: o estoque das obras vem primeiro.
 */
public final class ValueGoods {

    static final int BATCH = 8;
    static final int KEEP_RAW = 64;
    static final int CAP = 128;

    /** O que cada ofício transforma, na ordem de preferência: cru → produto. */
    static final Map<ProfessionType, List<Item[]>> RECIPES = Map.of(
            ProfessionType.MASON, List.of(
                    new Item[] {Items.STONE, Items.STONE_BRICKS},
                    new Item[] {Items.ANDESITE, Items.POLISHED_ANDESITE},
                    new Item[] {Items.GRANITE, Items.POLISHED_GRANITE},
                    new Item[] {Items.DIORITE, Items.POLISHED_DIORITE}),
            ProfessionType.SMELTER, List.of(
                    new Item[] {Items.COBBLESTONE, Items.STONE},
                    new Item[] {Items.SAND, Items.GLASS},
                    new Item[] {Items.CLAY, Items.TERRACOTTA}));

    private ValueGoods() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();
            long now = world.getTime();

            if (now % 1200 != 300) {
                return;
            }

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                if (colony.isActive() && colony.isAttended(now)) {
                    pass(world, colony);
                }
            }
        });
    }

    /** Um lote por titular livre de cada ofício desta colônia. */
    static int pass(ServerWorld world, Colony colony) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        int made = 0;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            Optional<ProfessionType> trade = worker.profession();

            if (trade.isEmpty() || !(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager)
                    || !VillageColonyMod.TASKS.assignedTo(worker.villagerId()).isEmpty()
                    || !WorkHours.isWorkTime(world, villager) || villager.isSleeping()) {
                continue;
            }

            Optional<Item[]> recipe = recipeFor(world, chests, trade.get());

            if (recipe.isPresent() && batch(world, chests, recipe.get()[0], recipe.get()[1])) {
                villager.swingHand(Hand.MAIN_HAND);
                ActionJournal.action(worker.villagerId(), "CRAFTED", ActionJournal.idOf(recipe.get()[1]), BATCH, null,
                        "value goods");
                made++;
            }
        }

        return made;
    }

    /** O lote que este ofício pode fazer agora: cru acima da reserva e produto abaixo do teto. */
    static Optional<Item[]> recipeFor(ServerWorld world, List<ColonyPos> chests, ProfessionType trade) {
        List<Item[]> recipes = trade == ProfessionType.LUMBERJACK ? charcoalFromTheMostLogs(world, chests)
                : RECIPES.getOrDefault(trade, List.of());

        for (Item[] recipe : recipes) {
            if (ColonyChests.countIn(world, chests, recipe[0]) >= KEEP_RAW + BATCH
                    && ColonyChests.countIn(world, chests, recipe[1]) < CAP) {
                return Optional.of(recipe);
            }
        }

        return Optional.empty();
    }

    /** O carvão vegetal sai da tora de que a vila tem mais. */
    private static List<Item[]> charcoalFromTheMostLogs(ServerWorld world, List<ColonyPos> chests) {
        Item most = null;
        int mostCount = 0;

        for (Item item : Registries.ITEM) {
            if (item.getDefaultStack().isIn(ItemTags.LOGS_THAT_BURN)) {
                int count = ColonyChests.countIn(world, chests, item);

                if (count > mostCount) {
                    most = item;
                    mostCount = count;
                }
            }
        }

        return most == null ? List.of() : List.<Item[]>of(new Item[] {most, Items.CHARCOAL});
    }

    /** Tira {@link #BATCH} do cru e põe o mesmo tanto do produto no primeiro baú com lugar. */
    static boolean batch(ServerWorld world, List<ColonyPos> chests, Item raw, Item product) {
        int taken = ColonyChests.withdraw(world, chests, raw, BATCH);
        int left = taken;

        for (ColonyPos chest : chests) {
            if (left <= 0) {
                break;
            }

            left = ChestDepositor.deposit(world, chest, product, left);
        }

        if (left > 0 && !chests.isEmpty()) {
            ChestDepositor.deposit(world, chests.getFirst(), raw, left);
        }

        return taken - left > 0;
    }
}
