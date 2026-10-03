package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O que o fundidor faz enquanto o pedido não tem matéria-prima — F2 e pedido
 * do autor, 2026-09-30.
 *
 * <p>No playtest das 02:45 os fundidores pararam 193 vezes sem argila nem
 * areia: soltavam a tarefa, a pegavam de volta no ciclo seguinte e paravam de
 * novo, sem nunca produzir nada. A regra do autor: depois de
 * {@link #MISSES_BEFORE_FALLBACK} paradas pelo mesmo pedido, o fundidor funde
 * uma peça de outro item que a obra aberta vai precisar — ou, sem obra
 * aberta, que as plantas da vila vão precisar — e volta a tentar o pedido,
 * sem abandoná-lo.
 *
 * <p>"Vai precisar" desce um degrau da receita: a vidraça não sai da
 * fornalha, mas o vidro dela sai, e é ele que conta.
 */
final class SmelterFallback {

    static {
        ServerMemory.register(SmelterFallback.class, SmelterFallback::clearAll);
    }

    /** Paradas seguidas pelo mesmo pedido antes de trabalhar para a obra. */
    static final int MISSES_BEFORE_FALLBACK = 5;

    /** Paradas por tarefa, pelo id dela: a tarefa sai e volta da fila. */
    private static final Map<UUID, Integer> MISSES = new HashMap<>();

    /** A lista de cada colônia vale um ciclo: montá-la percorre receitas. */
    private static final Map<UUID, Cached> WANTED = new HashMap<>();

    private record Cached(List<Item> items, long at) {
    }

    private SmelterFallback() {
    }

    /** Mais uma parada por falta de matéria-prima; devolve quantas já são. */
    static int missed(UUID taskId) {
        return MISSES.merge(taskId, 1, Integer::sum);
    }

    /** O pedido achou matéria-prima: a contagem recomeça. */
    static void found(UUID taskId) {
        MISSES.remove(taskId);
    }

    /** Esquece a contagem de tarefas que já não existem ou foram encerradas. */
    static void forgetClosedTasks() {
        MISSES.keySet().removeIf(taskId -> VillageColonyMod.TASKS.find(taskId)
                .map(task -> task.isOpen() || task.isHeld())
                .map(alive -> !alive)
                .orElse(true));
    }

    /** Se já é hora de trabalhar para a obra em vez de soltar o pedido. */
    static boolean isDue(UUID taskId) {
        return MISSES.getOrDefault(taskId, 0) >= MISSES_BEFORE_FALLBACK;
    }

    /**
     * O que sai da fornalha e a vila vai usar, na ordem de preferência:
     * primeiro o que a obra aberta ainda pede; se não houver obra aberta, o
     * que as plantas da vila pedem.
     */
    static List<Item> wanted(ServerWorld world, Colony colony) {
        Cached cached = WANTED.get(colony.id());

        if (cached != null && world.getTime() - cached.at() < VillageDetector.CYCLE_TICKS) {
            return cached.items();
        }

        Set<Item> items = new LinkedHashSet<>();

        for (ConstructionProject project : VillageColonyMod.CONSTRUCTIONS.all()) {
            if (project.colonyId().equals(colony.id()) && project.state().isOpen()) {
                addSmeltedFrom(world, project.remainingMaterials().keySet(), items);
            }
        }

        if (items.isEmpty()) {
            for (Blueprint plan : HousePlans.plansFor(world, colony)) {
                addSmeltedFrom(world, plan.blocks().stream().map(BlueprintBlock::block).toList(), items);
            }
        }

        List<Item> list = List.copyOf(items);

        WANTED.put(colony.id(), new Cached(list, world.getTime()));

        return list;
    }

    /** O item de fornalha de cada material, ou do ingrediente dele. */
    private static void addSmeltedFrom(
            ServerWorld world, Collection<ResourceId> materials, Set<Item> into) {

        for (ResourceId material : materials) {
            Optional<Item> item = itemOf(material);

            if (item.isEmpty()) {
                continue;
            }

            if (isSmelted(world, item.get())) {
                addWithCharcoal(item.get(), into);

                continue;
            }

            CraftingLookup.billFor(world, item.get(), any -> true).ifPresent(bill -> {
                for (Item ingredient : bill.ingredients().keySet()) {
                    if (isSmelted(world, ingredient)) {
                        addWithCharcoal(ingredient, into);
                    }
                }
            });
        }
    }

    /**
     * O carvão vem com o carvão vegetal logo atrás — B-4, 2026-10-02. A
     * reanálise viu a obra esperar a tocha e o fundidor parado sem minério: a
     * tocha aceita os dois carvões, e o vegetal sai da tora, sem mineiro.
     */
    private static void addWithCharcoal(Item item, Set<Item> into) {
        into.add(item);

        if (item == Items.COAL) {
            into.add(Items.CHARCOAL);
        }
    }

    private static boolean isSmelted(ServerWorld world, Item item) {
        return !CraftingLookup.smeltingInputsFor(world, item).isEmpty();
    }

    /** O item de um bloco da planta: a tocha de parede é a tocha. */
    private static Optional<Item> itemOf(ResourceId block) {
        Identifier id = Identifier.of(block.namespace(), block.path());

        if (!Registries.BLOCK.containsId(id)) {
            return Optional.empty();
        }

        Block found = Registries.BLOCK.get(id);
        Item item = found.asItem();

        return item == Items.AIR ? Optional.empty() : Optional.of(item);
    }

    static void clearAll() {
        MISSES.clear();
        WANTED.clear();
    }
}
