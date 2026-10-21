package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.coordination.StockCeiling;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.ColonyChests;
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
 * <p>Quando falta argila ou areia, a tarefa permanece com o fundidor para não
 * voltar à fila sem nenhuma produção. Depois de
 * {@link #MISSES_BEFORE_FALLBACK} faltas pelo mesmo pedido, o fundidor funde
 * uma peça de outro item que a obra aberta vai precisar — ou, sem obra aberta,
 * que as plantas da vila vão precisar — e volta a tentar o pedido, sem
 * abandoná-lo.
 *
 * <p>"Vai precisar" desce um degrau da receita: a vidraça não sai da
 * fornalha, mas o vidro dela sai, e é ele que conta.
 */
final class SmelterFallback {

    static {
        ServerMemory.register(SmelterFallback.class, SmelterFallback::clearAll);
    }

    /** Paradas seguidas pelo mesmo pedido antes de trabalhar para a obra. */
    static final int MISSES_BEFORE_FALLBACK = 2;

    /** Faltas seguidas por tarefa, pelo id dela. */
    private static final Map<UUID, Integer> MISSES = new HashMap<>();

    /** A lista de cada colônia vale um ciclo: montá-la percorre receitas. */
    private static final Map<UUID, Cached> WANTED = new HashMap<>();

    private record Cached(List<Item> items, long at) {
    }

    private SmelterFallback() {
    }

    /** Mais uma falta de matéria-prima; devolve quantas já são. */
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

    /** Se já é hora de trabalhar para a obra enquanto o pedido espera. */
    static boolean isDue(UUID taskId) {
        return MISSES.getOrDefault(taskId, 0) >= MISSES_BEFORE_FALLBACK;
    }

    /**
     * O mesmo, e já na primeira falta quando a obra aberta está esperando uma peça de fornalha —
     * autor, 2026-10-21: no deserto a obra pôs o arenito liso de lado 67 vezes enquanto o fundidor
     * segurava o pedido de ferro sem minério, e só fazia o arenito liso "enquanto o ferro espera".
     */
    static boolean isDue(ServerWorld world, com.villagecolony.core.task.model.Task task) {
        int misses = MISSES.getOrDefault(task.id(), 0);

        return misses >= MISSES_BEFORE_FALLBACK || (misses >= 1 && !waitedFor(world, task.colonyId()).isEmpty());
    }

    /**
     * As peças de fornalha que a obra aberta desta colônia espera agora: as postas de lado por falta de
     * material, e a da vez quando a obra está parada nela.
     */
    static Set<Item> waitedFor(ServerWorld world, UUID colonyId) {
        Set<ResourceId> pieces = new LinkedHashSet<>();

        for (ConstructionProject project : VillageColonyMod.CONSTRUCTIONS.all()) {
            if (!project.colonyId().equals(colonyId) || !project.state().isOpen()) {
                continue;
            }

            for (ConstructionProject.DeferredPiece piece : project.deferredPieces()) {
                if (piece.reason() == com.villagecolony.core.construction.model.SkipReason.WAITING_MATERIAL) {
                    pieces.add(piece.block());
                }
            }

            if (project.state() == com.villagecolony.core.construction.model.ConstructionState.WAITING_RESOURCES) {
                project.nextBlock().ifPresent(next -> pieces.add(next.block()));
            }
        }

        Set<Item> items = new LinkedHashSet<>();
        addSmeltedFrom(world, pieces, items);

        return items;
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

        // Primeiro o que a obra espera agora (waitedFor), depois o resto que ela ainda pede.
        Set<Item> items = new LinkedHashSet<>(waitedFor(world, colony.id()));

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

    /**
     * Se a vila já guarda este produto até o teto do estoque ({@link StockCeiling#CRAFTED}):
     * aí o fundidor não o faz "para as obras". O carvão vegetal conta junto com o
     * mineral, que serve às mesmas receitas — no playtest 0.3.31 ele fez 238 carvões
     * vegetais com 446 de carvão nos baús.
     */
    static boolean stocked(ServerWorld world, List<ColonyPos> chests, Item product) {
        int have = ColonyChests.countIn(world, chests, product)
                + (product == Items.CHARCOAL ? ColonyChests.countIn(world, chests, Items.COAL) : 0);

        return have >= StockCeiling.CRAFTED;
    }

    /** O item de fornalha de cada material, ou do ingrediente dele. */
    static void addSmeltedFrom(
            ServerWorld world, Collection<ResourceId> materials, Set<Item> into) {

        for (ResourceId material : materials) {
            Optional<Item> item = itemOf(material);

            if (item.isEmpty()) {
                continue;
            }

            if (isSmelted(world, item.get())) {
                addSmelted(world, item.get(), into);

                continue;
            }

            CraftingLookup.billFor(world, item.get(), any -> true).ifPresent(bill -> {
                for (Item ingredient : bill.ingredients().keySet()) {
                    if (isSmelted(world, ingredient)) {
                        addSmelted(world, ingredient, into);
                    }
                }
            });
        }
    }

    /**
     * O item e, logo atrás, o degrau de fornalha que vem antes dele — B-4,
     * 2026-10-02: a pedra lisa sai da pedra, e a pedra sai do pedregulho. Sem
     * este degrau a obra que pede pedra lisa deixava o fundidor parado com o
     * baú cheio de pedregulho, porque o cru da pedra lisa é a pedra, e pedra
     * ninguém trazia. Um degrau só: a pedra entra na lista, e o pedregulho
     * dela é cru comum.
     */
    private static void addSmelted(ServerWorld world, Item item, Set<Item> into) {
        addWithCharcoal(item, into);

        for (Item input : CraftingLookup.smeltingInputsFor(world, item)) {
            if (isSmelted(world, input)) {
                addWithCharcoal(input, into);
            }
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
