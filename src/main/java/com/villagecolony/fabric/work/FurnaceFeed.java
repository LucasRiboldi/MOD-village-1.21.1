package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.CraftFamily;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O insumo da fornalha feito pelo pedreiro — B1, pedido do autor de 2026-10-21.
 *
 * <p>No deserto o fundidor parou 41 vezes sem arenito para o arenito liso da obra, com areia
 * nos baús: o arenito é {@link Production#MINED}, a rota "mina" vence a receita
 * ({@code SupplyRoutes}) e ninguém o fazia de 4 areias. Quando falta o cru da fornalha e ele é
 * peça de pedreiro ({@link CraftFamily}) com receita que os baús da vila pagam sem tirar da obra
 * aberta ({@link BuildReserve}), o fundidor pede ao pedreiro — o molde do {@link Crusher}.
 */
public final class FurnaceFeed {

    /** Quanto um pedido ao pedreiro pede. */
    static final int ORDER = 8;

    private FurnaceFeed() {
    }

    /**
     * Abre o pedido ao pedreiro para o primeiro cru que ele consegue fazer agora.
     *
     * @return se há pedido aberto para um dos crus (já havia, ou abriu agora)
     */
    static boolean askTheMason(ServerWorld world, Colony colony, List<Item> raws) {
        if (WorkAssignment.countCapableOf(
                colony.id(), TaskType.CRAFT_STONE_MATERIAL.required(), VillageColonyMod.WORKERS) == 0) {
            return false;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());

        for (Item raw : raws) {
            Optional<ResourceType> type = MinecraftTypeAdapter.toResourceType(raw);

            if (type.isEmpty() || !isFeed(type.get()) || !canMake(world, colony, chests, raw)) {
                continue;
            }

            if (VillageColonyMod.TASKS.ofColony(colony.id()).stream().noneMatch(task -> isOrderFor(task, type.get()))) {
                VillageColonyMod.TASKS.create(
                        colony.id(), TaskType.CRAFT_STONE_MATERIAL, TaskPriority.CONSTRUCTION_MATERIAL, type.get(), ORDER);

                VillageColonyMod.LOGGER.info(
                        "Colony {} has no {} for the furnace — the mason makes {} by the recipe",
                        colony.id().toString().substring(0, 8), name(raw), ORDER);
            }

            return true;
        }

        return false;
    }

    /**
     * Se este pedido de pedreiro é o de insumo da fornalha (o alvo é cru de mina, não peça fabricada).
     *
     * <p>O pedido nominal de peça de obra leva {@link CraftsmanRequest#NOMINAL}, que também é cru de
     * mina da família do pedreiro (pedregulho): ele é do {@code CraftingSteps.produceForWork}, e não daqui.
     */
    static boolean isOrder(Task task) {
        return task.type() == TaskType.CRAFT_STONE_MATERIAL
                && task.targetResource() != CraftsmanRequest.NOMINAL
                && isFeed(task.targetResource());
    }

    /**
     * Um passo do pedreiro no pedido: uma receita, com material de toda a vila, para o baú dele.
     *
     * @return false quando o pedido acabou
     */
    static boolean step(ServerWorld world, CraftingWork.Job job, UUID workerId, ColonyPos own) {
        Optional<Colony> colony = VillageColonyMod.COLONIES.find(job.task.colonyId());
        Optional<Item> item = MinecraftTypeAdapter.toItem(job.task.targetResource());

        if (colony.isEmpty() || item.isEmpty()) {
            CraftingWork.finish(job, workerId, "the furnace input is not an item of this game");

            return false;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.get().id(), colony.get().center());

        if (job.goalTarget < 0) {
            job.goalTarget = ColonyChests.countIn(world, chests, item.get()) + job.task.amount();
        }

        if (!makeOne(world, colony.get(), item.get(), job.goalTarget, own)) {
            CraftingWork.finish(job, workerId, "nothing above what the open build needs is left to make " + name(item.get()));

            return false;
        }

        if (++job.crafted >= job.task.amount()) {
            CraftingWork.finish(job, workerId, "made the " + name(item.get()) + " the furnace was missing");

            return false;
        }

        return true;
    }

    /**
     * Uma receita do cru, com material de toda a vila, para o baú {@code own}.
     *
     * @return false quando a receita tiraria ingrediente da obra aberta, ou não há com que fazê-la
     */
    static boolean makeOne(ServerWorld world, Colony colony, Item raw, int goal, ColonyPos own) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());

        return canMake(world, colony, chests, raw)
                && ColonySupply.stockToward(world, colony.id(), colony.center(), raw, goal, own);
    }

    /** Cru de mina que é peça da família do pedreiro: arenito, arenito vermelho. */
    private static boolean isFeed(ResourceType type) {
        return type.production() == Production.MINED
                && CraftFamily.of(type.name().toLowerCase(java.util.Locale.ROOT)) == TaskType.CRAFT_STONE_MATERIAL;
    }

    private static boolean isOrderFor(Task task, ResourceType raw) {
        return task.isOpen() && task.type() == TaskType.CRAFT_STONE_MATERIAL && task.targetResource() == raw;
    }

    /** A receita existe, os baús têm cada ingrediente inteiro, e nenhum sai da obra aberta. */
    private static boolean canMake(ServerWorld world, Colony colony, List<ColonyPos> chests, Item raw) {
        Optional<CraftingLookup.Bill> bill = CraftingLookup.billFor(
                world, raw, ingredient -> ColonyChests.countIn(world, chests, ingredient) > 0);

        if (bill.isEmpty()) {
            return false;
        }

        for (Map.Entry<Item, Integer> ingredient : bill.get().ingredients().entrySet()) {
            int spare = ColonyChests.countIn(world, chests, ingredient.getKey())
                    - BuildReserve.buildNeedOf(colony, ingredient.getKey());

            if (spare < ingredient.getValue()) {
                return false;
            }
        }

        return true;
    }

    private static String name(Item item) {
        return Registries.ITEM.getId(item).getPath();
    }
}
