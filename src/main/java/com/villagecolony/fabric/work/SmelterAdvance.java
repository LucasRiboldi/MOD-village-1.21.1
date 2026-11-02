package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.BlockItem;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;

/**
 * O fundidor ocioso adianta a produção — regra do autor, 2026-10-12: <i>"deve ficar
 * produzindo blocos intermediários aos finais para ir adiantando produção e liberando
 * espaço para novos blocos crus"</i> (o pedido diz ferreiro; no mod o ofício do forno é
 * o fundidor). No playtest 0.3.35 os fundidores ficaram 58% esperando.
 *
 * <p>A cada ciclo, o fundidor sem tarefa repõe blocos de receitas de fornalha até
 * {@link ItemStock#EACH} por tipo. Depois reserva somente lingotes de minérios físicos,
 * até {@link #INGOT_RESERVE} por tipo. Fica nos baús o que a obra aberta pede do cru e
 * mais {@link #KEEP}. Sem lugar para o produto, o cru volta.
 */
public final class SmelterAdvance {

    /** Quantos cada fundidor ocioso funde por ciclo. */
    static final int PER_SMELTER = 8;

    /** Quanto do cru fica nos baús além do que a obra pede. */
    static final int KEEP = 16;

    /**
     * O pedregulho que fica para a mina, além da obra: é o enchimento do piso e da
     * beirada (playtest 0.3.36: a vila fundiu e cortou todo pedregulho e a mina parou).
     */
    static final int MINE_FILL = 64;

    /** Somente minérios físicos viram reserva ociosa; demais itens seguem estoque padrão. */
    static final List<Item> INGOT_RAWS = List.of(Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD);

    /** Meta máxima de cada lingote mineral disponível à vila. */
    static final int INGOT_RESERVE = 64;

    private SmelterAdvance() {
    }

    /**
     * Uma fornada por fundidor sem tarefa.
     *
     * @return quantos itens foram fundidos
     */
    public static int smeltForIdleSmelters(ServerWorld world, Colony colony) {
        int smelted = 0;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (worker.profession().filter(ProfessionType.SMELTER::equals).isPresent()
                    && VillageColonyMod.TASKS.assignedTo(worker.villagerId()).isEmpty()) {
                smelted += smeltOnce(world, colony);
            }
        }

        return smelted;
    }

    /** Uma fornada: o primeiro cru com sobra cujo produto ainda cabe no teto. */
    static int smeltOnce(ServerWorld world, Colony colony) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());

        for (var entry : world.getRecipeManager().listAllOfType(RecipeType.SMELTING)) {
            Item made = entry.value().getResult(world.getRegistryManager()).getItem();
            if (!(made instanceof BlockItem) || ColonyChests.countIn(world, chests, made) >= ItemStock.EACH) {
                continue;
            }
            for (var ingredient : entry.value().getIngredients()) {
                for (ItemStack option : ingredient.getMatchingStacks()) {
                    Item raw = option.getItem();
                    int keep = raw == Items.COBBLESTONE || raw == Items.COBBLED_DEEPSLATE ? MINE_FILL : KEEP;
                    int spare = ColonyChests.countIn(world, chests, raw) - BuildReserve.buildNeedOf(colony, raw) - keep;
                    int room = ItemStock.EACH - ColonyChests.countIn(world, chests, made);
                    int amount = Math.min(PER_SMELTER, Math.min(spare, room));
                    if (amount <= 0) {
                        continue;
                    }
                    int taken = ColonyChests.withdraw(world, chests, raw, amount);
                    int left = ColonyChests.deposit(world, chests, made, taken);
                    if (left > 0) {
                        ColonyChests.deposit(world, chests, raw, left);
                    }
                    if (taken > left) {
                        return taken - left;
                    }
                }
            }
        }

        for (Item raw : INGOT_RAWS) {
            Optional<ItemStack> product = CraftingLookup.smelted(world, new ItemStack(raw));

            if (product.isEmpty() || product.get().isEmpty()) {
                continue;
            }

            Item made = product.get().getItem();
            if (made instanceof BlockItem) {
                continue;
            }
            int spare = ColonyChests.countIn(world, chests, raw) - BuildReserve.buildNeedOf(colony, raw) - KEEP;
            int room = INGOT_RESERVE - ColonyChests.countIn(world, chests, made);
            int amount = Math.min(PER_SMELTER, Math.min(spare, room));

            if (amount <= 0) {
                continue;
            }

            int taken = ColonyChests.withdraw(world, chests, raw, amount);
            int left = ColonyChests.deposit(world, chests, made, taken);

            if (left > 0) {
                // Sem lugar para o produto: o cru que não virou volta.
                ColonyChests.deposit(world, chests, raw, left);
            }

            if (taken - left > 0) {
                VillageColonyMod.LOGGER.info(
                        "Colony {} — an idle smelter reserved {} {} as {}",
                        colony.id().toString().substring(0, 8), taken - left, raw, made);

                return taken - left;
            }
        }

        return 0;
    }
}
