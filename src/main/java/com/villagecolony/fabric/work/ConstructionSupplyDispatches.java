package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.integration.SupplyGapLedger;
import com.villagecolony.fabric.integration.SupplyRoute;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O vigia das peças que travam a obra — Regra 40, ADR-040, e o G2 do autor (2026-10-20): <i>"se
 * não tiver material nos baús, se não tiver rota ou a rota para adquirir travar, registrar o
 * travamento e fazer todos blocos necessários aparecerem no baú do construtor"</i>.
 *
 * <p>Vigia <b>cada</b> peça que trava o construtor — a da vez e as que ele pôs de lado por falta
 * de material —, cada uma com o próprio contador. Travou é passar
 * {@link BiomeConstructionSupply#DISPATCHES_BEFORE_STOCKING} ciclos sem progresso: a peça não
 * aumentou nos baús e a obra não assentou mais dela. Rota sem tarefa aberta também conta: ninguém
 * a está executando. Ao travar, tudo o que a obra ainda pede da peça aparece no baú do
 * construtor, e o caso vai para o registro permanente ({@link SupplyGapLedger}).
 *
 * <p>A contagem não é estado do mundo, e não vai para o save.
 */
public final class ConstructionSupplyDispatches {

    /**
     * O vigia de uma peça.
     *
     * @param misses ciclos seguidos sem progresso
     * @param cycleTick o tique do último ciclo contado, para o mesmo ciclo não contar duas vezes
     * @param progress quanto da peça há nos baús menos quanto a obra ainda pede: sobe com entrega
     *     e com assentamento
     */
    private record Watch(int misses, long cycleTick, int progress) {
    }

    private record Need(ConstructionProject project, Item item, SupplyRoute route) {
    }

    private static final Map<UUID, Map<Item, Watch>> BY_PROJECT = new HashMap<>();

    static {
        ServerMemory.register(ConstructionSupplyDispatches.class, ConstructionSupplyDispatches::clearAll);
    }

    private ConstructionSupplyDispatches() {
    }

    /** Um minuto de ciclos sem progresso permite o suprimento terminal (regra do autor, 2026-10-11). */
    public static boolean fallbackDue(UUID projectId, Item item) {
        Watch watch = BY_PROJECT.getOrDefault(projectId, Map.of()).get(item);
        return watch != null && watch.misses() >= BiomeConstructionSupply.DISPATCHES_BEFORE_STOCKING;
    }

    /** Um item físico no estoque encerra a sequência dele, seja de profissão ou do jogador. */
    public static void delivered(UUID projectId, Item item) {
        Map<Item, Watch> watches = BY_PROJECT.get(projectId);
        if (watches != null) {
            watches.remove(item);
            if (watches.isEmpty()) {
                BY_PROJECT.remove(projectId);
            }
        }
    }

    /**
     * Conta um ciclo sem progresso desta peça. Chamadas repetidas no mesmo ciclo não gastam outra
     * tentativa, o que mantém overlay e planejador observadores; peças diferentes não se zeram.
     */
    public static void recordCycleDispatch(UUID projectId, Item item, long cycleTick) {
        Map<Item, Watch> watches = BY_PROJECT.computeIfAbsent(projectId, any -> new HashMap<>());
        Watch previous = watches.get(item);
        if (previous != null && previous.cycleTick() == cycleTick) {
            return;
        }

        watches.put(item, previous == null
                ? new Watch(1, cycleTick, Integer.MIN_VALUE)
                : new Watch(previous.misses() + 1, cycleTick, previous.progress()));
    }

    /** Torna urgentes, antes da distribuição, as tarefas disponíveis da rota da obra. */
    public static void prioritizeForCycle(ServerWorld world, Colony colony) {
        currentNeed(world, colony).ifPresent(need -> {
            prioritizeRoute(colony.id(), need.route());
        });
    }

    /**
     * Um ciclo da colônia: cada peça que trava a obra avança ou conta um ciclo sem progresso, e a
     * que travou aparece inteira no baú do construtor — G2, 2026-10-20.
     *
     * <p>Rota sem tarefa aberta conta: ninguém a está executando (o sílex da bancada de
     * flecheiro, sem receita e sem fonte, deixou a obra parada a sessão inteira no playtest
     * 0.3.36; a rota de papel sem executor, a 0.3.69 inteira na bancada de biomas).
     */
    public static void renewForCycle(ServerWorld world, Colony colony) {
        VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).ifPresent(project -> {
            for (Block block : blockingBlocks(project)) {
                watch(world, colony, project, block);
            }
        });
    }

    /** As peças que travam o construtor agora: a da vez, se a obra espera, e as postas de lado por material. */
    private static Set<Block> blockingBlocks(ConstructionProject project) {
        Set<Block> blocks = new java.util.LinkedHashSet<>();

        if (project.state() == ConstructionState.WAITING_RESOURCES) {
            project.nextBlock().map(BlueprintBlock::block).flatMap(MinecraftTypeAdapter::toBlock)
                    .ifPresent(blocks::add);
        }

        for (ConstructionProject.DeferredPiece piece : project.deferredPieces()) {
            if (piece.reason() == SkipReason.WAITING_MATERIAL) {
                MinecraftTypeAdapter.toBlock(piece.block()).ifPresent(blocks::add);
            }
        }

        return blocks;
    }

    private static void watch(ServerWorld world, Colony colony, ConstructionProject project, Block block) {
        List<Item> choices = MaterialChoice.forBlock(block);

        if (choices.isEmpty()) {
            return;
        }

        Item item = choices.getFirst();

        if (choices.stream().anyMatch(choice ->
                ColonySupply.canProvide(world, colony.id(), project.origin(), choice))) {
            delivered(project.id(), item);
            return;
        }

        int progress = ColonyChests.countIn(world, ColonyChests.nearestFirst(world, colony.id(), project.origin()), item)
                - project.remainingMaterials().getOrDefault(MinecraftTypeAdapter.toResourceId(block), 0);
        Map<Item, Watch> watches = BY_PROJECT.computeIfAbsent(project.id(), any -> new HashMap<>());
        Watch previous = watches.get(item);

        if (previous != null && progress > previous.progress()) {
            watches.put(item, new Watch(0, world.getTime(), progress)); // entregou ou assentou
            return;
        }

        recordCycleDispatch(project.id(), item, world.getTime());
        Watch counted = watches.get(item);
        watches.put(item, new Watch(counted.misses(), counted.cycleTick(), progress));

        if (counted.misses() < BiomeConstructionSupply.DISPATCHES_BEFORE_STOCKING) {
            return;
        }

        SupplyRoute route = BiomeConstructionSupply.routeInBiome(world, colony.id(), item);
        String reason = !route.available() ? SupplyGapLedger.NO_ROUTE
                : hasOpenSupplyTask(colony.id(), route) ? SupplyGapLedger.STALLED
                : SupplyGapLedger.NO_TASK;

        VillageColonyMod.LOGGER.info("Colony {} — {} stalled the construction for {} cycles ({}): it appears"
                        + " in the builder's chest", colony.id(), item, counted.misses(), reason);

        if (BiomeConstructionSupply.stockForConstruction(world, colony.id(), project.origin(), item, reason)) {
            delivered(project.id(), item);
        }
    }

    private static Optional<Need> currentNeed(ServerWorld world, Colony colony) {
        return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .filter(project -> project.state() == ConstructionState.WAITING_RESOURCES)
                .flatMap(project -> project.nextBlock()
                        .map(BlueprintBlock::block)
                        .flatMap(MinecraftTypeAdapter::toBlock)
                        .flatMap(block -> missingNeed(world, project, block)));
    }

    private static Optional<Need> missingNeed(
            ServerWorld world, ConstructionProject project, Block block) {

        List<Item> choices = MaterialChoice.forBlock(block);
        if (choices.stream().anyMatch(item ->
                ColonySupply.canProvide(world, project.colonyId(), project.origin(), item))) {
            return Optional.empty();
        }

        return choices.stream().findFirst().map(item -> new Need(
                project, item, BiomeConstructionSupply.routeInBiome(world, project.colonyId(), item)));
    }

    private static void prioritizeRoute(UUID colonyId, SupplyRoute route) {
        Set<ResourceType> resources = new HashSet<>();
        collectResources(route, resources);

        if (resources.isEmpty()) {
            return;
        }

        VillageColonyMod.TASKS.availableFor(colonyId).stream()
                .filter(task -> resources.contains(task.targetResource()))
                .forEach(task -> task.reprioritize(TaskPriority.CONSTRUCTION_MATERIAL));
    }

    private static boolean hasOpenSupplyTask(UUID colonyId, SupplyRoute route) {
        Set<ResourceType> resources = new HashSet<>();
        collectResources(route, resources);

        return VillageColonyMod.TASKS.ofColony(colonyId).stream().anyMatch(task ->
                task.isOpen()
                        && task.type().isResourceRequest()
                        && resources.contains(task.targetResource()));
    }

    private static void collectResources(SupplyRoute route, Set<ResourceType> resources) {
        MinecraftTypeAdapter.toResourceType(route.item()).ifPresent(resources::add);
        route.inputs().forEach(input -> collectResources(input, resources));
    }

    static void clearAll() {
        BY_PROJECT.clear();
    }
}
