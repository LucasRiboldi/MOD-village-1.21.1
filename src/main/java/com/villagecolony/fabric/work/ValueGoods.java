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
 * Produtos de valor com o titular sem tarefa — Regra 133 (autor, 2026-10-28; jogo da 0.3.99: lenhador 74%,
 * pedreiro 48% e fundidor 44% parados com o estoque cheio) e Regra 146 (autor, 2026-10-28; taiga simulada:
 * fundidor 80% e pedreiro 64% parados com 1.209 pedras, porque o produto batia no teto de 128). O titular
 * sem tarefa do ofício, no expediente, faz um lote por minuto: o lenhador faz carvão vegetal da tora; o
 * pedreiro faz tijolo, pedra polida, arenito cortado e as peças de pedregulho das obras; o fundidor funde o
 * minério cru, assa a carne, e faz pedra, pedra lisa, vidro, tijolo e terracota. Cada receita guarda uma
 * reserva do cru ({@link Recipe#keep}) e para com o produto em {@link #CAP}: o estoque das obras vem primeiro.
 */
public final class ValueGoods {

    /** Quantos do cru um lote gasta, no máximo. */
    static final int BATCH = 8;

    /** A reserva de cru das matérias que as obras também usam. */
    static final int KEEP_RAW = 64;

    /** O produto para de ser feito com tanto nos baús. */
    static final int CAP = 512;

    /** Uma receita: {@code in} do cru dão {@code out} do produto; o cru nunca desce de {@code keep}. */
    record Recipe(Item raw, int in, Item product, int out, int keep) {

        static Recipe oneToOne(Item raw, Item product) {
            return new Recipe(raw, 1, product, 1, KEEP_RAW);
        }

        /** Minério cru e carne: nada os usa crus, não há reserva. */
        static Recipe allOf(Item raw, Item product) {
            return new Recipe(raw, 1, product, 1, 0);
        }
    }

    /** O que cada ofício transforma, na ordem de preferência. */
    static final Map<ProfessionType, List<Recipe>> RECIPES = Map.of(
            ProfessionType.MASON, List.of(
                    Recipe.oneToOne(Items.STONE, Items.STONE_BRICKS),
                    new Recipe(Items.COBBLESTONE, 6, Items.COBBLESTONE_STAIRS, 4, KEEP_RAW),
                    new Recipe(Items.COBBLESTONE, 3, Items.COBBLESTONE_SLAB, 6, KEEP_RAW),
                    new Recipe(Items.COBBLESTONE, 6, Items.COBBLESTONE_WALL, 6, KEEP_RAW),
                    Recipe.oneToOne(Items.STONE_BRICKS, Items.CHISELED_STONE_BRICKS),
                    Recipe.oneToOne(Items.SANDSTONE, Items.CUT_SANDSTONE),
                    Recipe.oneToOne(Items.RED_SANDSTONE, Items.CUT_RED_SANDSTONE),
                    Recipe.oneToOne(Items.ANDESITE, Items.POLISHED_ANDESITE),
                    Recipe.oneToOne(Items.GRANITE, Items.POLISHED_GRANITE),
                    Recipe.oneToOne(Items.DIORITE, Items.POLISHED_DIORITE),
                    Recipe.oneToOne(Items.COBBLED_DEEPSLATE, Items.POLISHED_DEEPSLATE)),
            ProfessionType.SMELTER, List.of(
                    Recipe.allOf(Items.RAW_IRON, Items.IRON_INGOT),
                    Recipe.allOf(Items.RAW_GOLD, Items.GOLD_INGOT),
                    Recipe.allOf(Items.RAW_COPPER, Items.COPPER_INGOT),
                    Recipe.allOf(Items.BEEF, Items.COOKED_BEEF),
                    Recipe.allOf(Items.PORKCHOP, Items.COOKED_PORKCHOP),
                    Recipe.allOf(Items.MUTTON, Items.COOKED_MUTTON),
                    Recipe.allOf(Items.CHICKEN, Items.COOKED_CHICKEN),
                    Recipe.allOf(Items.RABBIT, Items.COOKED_RABBIT),
                    Recipe.oneToOne(Items.COBBLESTONE, Items.STONE),
                    Recipe.oneToOne(Items.STONE, Items.SMOOTH_STONE),
                    Recipe.oneToOne(Items.SAND, Items.GLASS),
                    Recipe.oneToOne(Items.CLAY_BALL, Items.BRICK),
                    Recipe.oneToOne(Items.CLAY, Items.TERRACOTTA),
                    Recipe.oneToOne(Items.SANDSTONE, Items.SMOOTH_SANDSTONE)));

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

            Optional<Recipe> recipe = recipeFor(world, chests, trade.get());

            if (recipe.isPresent()) {
                int crafted = batch(world, chests, recipe.get());

                if (crafted > 0) {
                    villager.swingHand(Hand.MAIN_HAND);
                    ActionJournal.action(worker.villagerId(), "CRAFTED", ActionJournal.idOf(recipe.get().product()),
                            crafted, null, "value goods");
                    made++;
                }
            }
        }

        return made;
    }

    /** O lote que este ofício pode fazer agora: cru acima da reserva e produto abaixo do teto. */
    static Optional<Recipe> recipeFor(ServerWorld world, List<ColonyPos> chests, ProfessionType trade) {
        List<Recipe> recipes = trade == ProfessionType.LUMBERJACK ? charcoalFromTheMostLogs(world, chests)
                : RECIPES.getOrDefault(trade, List.of());

        for (Recipe recipe : recipes) {
            if (ColonyChests.countIn(world, chests, recipe.raw()) >= recipe.keep() + recipe.in()
                    && ColonyChests.countIn(world, chests, recipe.product()) < CAP) {
                return Optional.of(recipe);
            }
        }

        return Optional.empty();
    }

    /** O carvão vegetal sai da tora de que a vila tem mais. */
    private static List<Recipe> charcoalFromTheMostLogs(ServerWorld world, List<ColonyPos> chests) {
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

        return most == null ? List.of() : List.of(Recipe.oneToOne(most, Items.CHARCOAL));
    }

    /**
     * Faz quantas vezes a receita couber em {@link #BATCH} do cru, acima da reserva, e põe o produto no
     * primeiro baú com lugar; o cru que não virou produto volta.
     *
     * @return quanto do produto saiu
     */
    static int batch(ServerWorld world, List<ColonyPos> chests, Recipe recipe) {
        int spare = ColonyChests.countIn(world, chests, recipe.raw()) - recipe.keep();
        int crafts = Math.min(BATCH, spare) / recipe.in();

        if (crafts <= 0) {
            return 0;
        }

        int taken = ColonyChests.withdraw(world, chests, recipe.raw(), crafts * recipe.in());
        crafts = taken / recipe.in();

        int made = crafts * recipe.out();
        int left = made;

        for (ColonyPos chest : chests) {
            if (left <= 0) {
                break;
            }

            left = ChestDepositor.deposit(world, chest, recipe.product(), left);
        }

        // O que não coube volta como cru, na proporção da receita, junto com a sobra que não fez receita.
        int rawBack = taken - crafts * recipe.in() + (left * recipe.in()) / recipe.out();

        if (rawBack > 0 && !chests.isEmpty()) {
            ChestDepositor.deposit(world, chests.getFirst(), recipe.raw(), rawBack);
        }

        return made - left;
    }
}
