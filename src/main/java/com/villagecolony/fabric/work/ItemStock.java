package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.coordination.CraftFamily;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.integration.CraftingLookup;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Um pouco de tudo no estoque — regra do autor, 2026-10-11: <i>"a produção de blocos
 * de carpintaria e fundidor devem produzir um pouco de todos os itens possíveis para
 * criar um estoque de todos que futuras profissões vão precisar"</i>.
 *
 * <p>Os itens são os que as plantas do estilo da vila usam e os ingredientes deles,
 * descendo a receita até {@link #DEPTH} degraus, e todas as peças de madeira do jogo
 * ({@link #isWoodPiece}, autor 2026-10-12: "um pouco de todas as peças de madeira"); ficam os que a bancada faz e que são
 * da madeira (carpinteiro) ou do metal e do vidro (fundidor), pela divisão do
 * {@link CraftFamily}. Cada um vai até {@link #EACH}, em rodízio, sempre abaixo do
 * teto de 64 ({@code StockCeiling}). A obra vem antes: a ordem de estoque é uma
 * tarefa de fabricação, e o artesão atende primeiro a peça que a obra espera
 * ({@code CraftingSteps.produceForWork}).
 *
 * <p>A ordem é nominal ({@link #NOMINAL}), como o pedido de peça de obra: o item mora
 * aqui, não no {@code ResourceType}, que declara só 68 recursos.
 */
public final class ItemStock {

    static {
        ServerMemory.register(ItemStock.class, ItemStock::clearAll);
    }

    /** Quantos de cada item. */
    public static final int EACH = 8;

    /** Os ofícios que guardam um pouco de tudo. */
    static final List<TaskType> FAMILIES = List.of(TaskType.CRAFT_WOOD_MATERIAL, TaskType.CRAFT_METAL_MATERIAL);

    /** Alvo nominal da ordem — outro que o do pedido de peça de obra, para um não calar o outro. */
    static final ResourceType NOMINAL = ResourceType.DIRT;

    /** Degraus de receita abaixo das peças das plantas. */
    static final int DEPTH = 3;

    /** Releitura das plantas; o estilo da vila não muda. */
    static final long REFRESH = 6_000;

    /** Quanto tempo o item que não se conseguiu fazer fica fora do rodízio. */
    static final long SKIP_FOR = 6_000;

    private static final Map<UUID, Map<TaskType, List<Item>>> ITEMS = new HashMap<>();

    private static final Map<UUID, Long> READ_AT = new HashMap<>();

    private static final Map<UUID, Item> ORDERS = new HashMap<>();

    private static final Map<String, Integer> CURSORS = new HashMap<>();

    private static final Map<Item, Long> SKIPPED = new HashMap<>();

    private ItemStock() {
    }

    static void clearAll() {
        ITEMS.clear();
        READ_AT.clear();
        ORDERS.clear();
        CURSORS.clear();
        SKIPPED.clear();
    }

    /** Os itens de estoque desta colônia, por oficina (para o teste e o relatório). */
    static List<Item> itemsOf(UUID colonyId, TaskType family) {
        return ITEMS.getOrDefault(colonyId, Map.of()).getOrDefault(family, List.of());
    }

    /** Uma vez por ciclo: abre a ordem de estoque da oficina que está sem tarefa. */
    public static void openOrders(ServerWorld world, Colony colony) {
        long now = world.getTime();

        ORDERS.keySet().removeIf(id -> VillageColonyMod.TASKS.find(id).filter(Task::isOpen).isEmpty());

        if (!READ_AT.containsKey(colony.id()) || now - READ_AT.get(colony.id()) >= REFRESH) {
            READ_AT.put(colony.id(), now);
            ITEMS.put(colony.id(), read(world, colony));
        }

        for (TaskType family : FAMILIES) {
            boolean busy = VillageColonyMod.TASKS.ofColony(colony.id()).stream()
                    .anyMatch(task -> task.type() == family && task.isOpen());

            if (busy || WorkAssignment.countCapableOf(colony.id(), family.required(), VillageColonyMod.WORKERS) == 0) {
                continue;
            }

            next(world, colony, family, now).ifPresent(item -> {
                Task order = VillageColonyMod.TASKS.create(colony.id(), family, TaskPriority.PRODUCTION, NOMINAL, EACH);

                ORDERS.put(order.id(), item);
                VillageColonyMod.LOGGER.info("Colony {} asks the {} to stock {} of {}",
                        colony.id().toString().substring(0, 8), CraftFamily.subjectOf(family), EACH, pathOf(item));
            });
        }
    }

    /**
     * Um passo da ordem de estoque, dentro do {@code CraftingWork}.
     *
     * @return vazio quando a tarefa não é ordem de estoque; senão, se o trabalho segue
     */
    static Optional<Boolean> step(ServerWorld world, CraftingWork.Job job, UUID workerId) {
        Item item = ORDERS.get(job.task.id());
        Optional<Colony> colony = VillageColonyMod.COLONIES.find(job.task.colonyId());

        if (item == null || colony.isEmpty()) {
            return Optional.empty();
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.get().id(), colony.get().center());

        if (ColonyChests.countIn(world, chests, item) >= EACH) {
            ORDERS.remove(job.task.id());
            CraftingWork.finish(job, workerId, "stocked " + EACH + " of " + pathOf(item));

            return Optional.of(false);
        }

        Optional<ColonyPos> own = VillageColonyMod.STORAGES.of(workerId).map(WorkerStorage::chestPosition);
        boolean made = own.isPresent()
                ? ColonySupply.stockToward(world, colony.get().id(), colony.get().center(), item, EACH, own.get())
                : ColonySupply.stockToward(world, colony.get().id(), colony.get().center(), item, EACH);

        if (!made) {
            SKIPPED.put(item, world.getTime());
            ORDERS.remove(job.task.id());
            CraftingWork.finish(job, workerId, "could not make " + pathOf(item) + " for the stock");

            return Optional.of(false);
        }

        job.crafted++;

        return Optional.of(true);
    }

    /** O próximo item da oficina, em rodízio, abaixo de {@link #EACH} e que a colônia consegue fazer. */
    private static Optional<Item> next(ServerWorld world, Colony colony, TaskType family, long now) {
        List<Item> items = itemsOf(colony.id(), family);

        if (items.isEmpty()) {
            return Optional.empty();
        }

        String key = colony.id() + "|" + family;
        int start = CURSORS.getOrDefault(key, 0);
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());

        for (int step = 0; step < items.size(); step++) {
            int index = Math.floorMod(start + step, items.size());
            Item item = items.get(index);
            Long skipped = SKIPPED.get(item);

            if ((skipped != null && now - skipped < SKIP_FOR)
                    || ColonyChests.countIn(world, chests, item) >= EACH
                    || !ColonySupply.canProvide(world, colony.id(), colony.center(), item)) {
                continue;
            }

            CURSORS.put(key, index + 1);

            return Optional.of(item);
        }

        return Optional.empty();
    }

    /** Os itens das plantas do estilo da vila e os ingredientes deles, por oficina. */
    static Map<TaskType, List<Item>> read(ServerWorld world, Colony colony) {
        String style = HousePlans.paletteOf(world, colony.center()).style();
        Set<Item> seen = new LinkedHashSet<>();
        List<Item> plans = new ArrayList<>();

        for (ResourceId id : VillageStructures.housesFor(style)) {
            HousePlans.READ.computeIfAbsent(id, missing -> StructureBlueprintReader.read(world, missing))
                    .map(Blueprint::blocks)
                    .ifPresent(blocks -> blocks.forEach(block -> MinecraftTypeAdapter.toBlock(block.block())
                            .map(Block::asItem)
                            .filter(item -> item != Items.AIR && seen.add(item))
                            .ifPresent(plans::add)));
        }

        List<Item> layer = plans;

        for (int depth = 0; depth < DEPTH && !layer.isEmpty(); depth++) {
            List<Item> below = new ArrayList<>();

            for (Item item : layer) {
                CraftingLookup.billFor(world, item, any -> true).ifPresent(bill -> bill.ingredients().keySet()
                        .forEach(ingredient -> {
                            if (seen.add(ingredient)) {
                                below.add(ingredient);
                            }
                        }));
            }

            layer = below;
        }

        // Todas as peças de madeira do jogo, além das do estilo — autor, 2026-10-12.
        Registries.ITEM.stream().filter(ItemStock::isWoodPiece).forEach(seen::add);

        Map<TaskType, List<Item>> byFamily = new HashMap<>();

        for (Item item : seen) {
            TaskType family = CraftFamily.of(pathOf(item));

            if (FAMILIES.contains(family) && CraftingLookup.billFor(world, item, any -> true).isPresent()) {
                byFamily.computeIfAbsent(family, ignored -> new ArrayList<>()).add(item);
            }
        }

        return byFamily;
    }

    /** As famílias de peça de madeira que o carpinteiro guarda um pouco de cada. */
    private static final List<TagKey<Item>> WOOD_PIECES = List.of(
            ItemTags.WOODEN_STAIRS, ItemTags.WOODEN_SLABS, ItemTags.WOODEN_FENCES, ItemTags.FENCE_GATES,
            ItemTags.WOODEN_DOORS, ItemTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_PRESSURE_PLATES,
            ItemTags.WOODEN_BUTTONS, ItemTags.SIGNS);

    /** Se este item é peça de madeira: das famílias acima, ou escada de mão. */
    static boolean isWoodPiece(Item item) {
        ItemStack stack = new ItemStack(item);

        return item == Items.LADDER || WOOD_PIECES.stream().anyMatch(stack::isIn);
    }

    static String pathOf(Item item) {
        return Registries.ITEM.getId(item).getPath();
    }
}
