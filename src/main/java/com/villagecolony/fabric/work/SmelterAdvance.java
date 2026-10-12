package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.StockCeiling;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;

/**
 * O fundidor ocioso adianta a produção — regra do autor, 2026-10-12: <i>"deve ficar
 * produzindo blocos intermediários aos finais para ir adiantando produção e liberando
 * espaço para novos blocos crus"</i> (o pedido diz ferreiro; no mod o ofício do forno é
 * o fundidor). No playtest 0.3.35 os fundidores ficaram 58% esperando.
 *
 * <p>A cada ciclo da colônia, cada fundidor sem tarefa funde até {@link #PER_SMELTER} do
 * cru que sobra nos baús no produto seguinte da cadeia (o jogo diz qual,
 * {@link CraftingLookup#smelted}). Fica nos baús o que a obra aberta pede do cru e mais
 * {@link #KEEP}; o produto para em {@link StockCeiling#RAW}. Sem lugar para o produto,
 * o cru volta.
 */
public final class SmelterAdvance {

    /** Quantos cada fundidor ocioso funde por ciclo. */
    static final int PER_SMELTER = 8;

    /** Quanto do cru fica nos baús além do que a obra pede. */
    static final int KEEP = 16;

    /** Os crus que se adiantam, na ordem: minério, areia, pedra, argila, arenito. */
    static final List<Item> RAWS = List.of(
            Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD,
            Items.SAND, Items.RED_SAND,
            Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.STONE,
            Items.CLAY_BALL, Items.CLAY,
            Items.SANDSTONE, Items.RED_SANDSTONE);

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

        for (Item raw : RAWS) {
            Optional<ItemStack> product = CraftingLookup.smelted(world, new ItemStack(raw));

            if (product.isEmpty() || product.get().isEmpty()) {
                continue;
            }

            Item made = product.get().getItem();
            int spare = ColonyChests.countIn(world, chests, raw) - BuildReserve.buildNeedOf(colony, raw) - KEEP;
            int room = StockCeiling.RAW - ColonyChests.countIn(world, chests, made);
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
                        "Colony {} — an idle smelter advanced {} {} into {} to free room for raw blocks",
                        colony.id().toString().substring(0, 8), taken - left, raw, made);

                return taken - left;
            }
        }

        return 0;
    }
}
