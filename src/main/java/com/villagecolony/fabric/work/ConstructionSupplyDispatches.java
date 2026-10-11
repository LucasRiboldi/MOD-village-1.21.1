package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import com.villagecolony.fabric.integration.ColonySupply;
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
 * O despacho transitório da peça que mantém uma obra em espera.
 *
 * <p>A contagem não é estado do mundo: ela só separa quatro ciclos reais de
 * suprimento de consultas feitas pelo construtor, pela placa ou pelo overlay.
 * Por isso não é gravada no save. Regra 40, ADR-040.
 */
public final class ConstructionSupplyDispatches {

    private record Dispatch(Item item, int attempts, long cycleTick) {
    }

    private record Need(ConstructionProject project, Item item, SupplyRoute route) {
    }

    private static final Map<UUID, Dispatch> BY_PROJECT = new HashMap<>();

    static {
        ServerMemory.register(ConstructionSupplyDispatches.class, ConstructionSupplyDispatches::clearAll);
    }

    private ConstructionSupplyDispatches() {
    }

    /** Um minuto de renovações sem entrega permite o suprimento terminal (regra do autor, 2026-10-11). */
    public static boolean fallbackDue(UUID projectId, Item item) {
        Dispatch dispatch = BY_PROJECT.get(projectId);
        return dispatch != null
                && dispatch.item() == item
                && dispatch.attempts() >= BiomeConstructionSupply.DISPATCHES_BEFORE_STOCKING;
    }

    /** Um item físico no estoque encerra a sequência, seja ele de profissão ou do jogador. */
    public static void delivered(UUID projectId, Item item) {
        Dispatch dispatch = BY_PROJECT.get(projectId);
        if (dispatch != null && dispatch.item() == item) {
            BY_PROJECT.remove(projectId);
        }
    }

    /**
     * Registra uma renovação de demanda. Chamadas repetidas no mesmo ciclo não
     * gastam outra tentativa, o que mantém overlay e planejador observadores.
     */
    public static void recordCycleDispatch(UUID projectId, Item item, long cycleTick) {
        Dispatch previous = BY_PROJECT.get(projectId);
        if (previous != null && previous.item() == item && previous.cycleTick() == cycleTick) {
            return;
        }

        int attempts = previous != null && previous.item() == item ? previous.attempts() + 1 : 1;
        BY_PROJECT.put(projectId, new Dispatch(item, attempts, cycleTick));
    }

    /** Torna urgentes, antes da distribuição, as tarefas disponíveis da rota da obra. */
    public static void prioritizeForCycle(ServerWorld world, Colony colony) {
        currentNeed(world, colony).ifPresent(need -> {
            prioritizeRoute(colony.id(), need.route());
        });
    }

    /**
     * Registra somente o ciclo que deixou uma tarefa executável aberta para a
     * rota. Consultas da obra e uma rota teórica sem executor não gastam o
     * prazo do suprimento terminal. Regra 40, ADR-040.
     */
    public static void renewForCycle(ServerWorld world, Colony colony) {
        currentNeed(world, colony)
                .filter(need -> hasOpenSupplyTask(colony.id(), need.route()))
                .ifPresent(need -> recordCycleDispatch(
                        need.project().id(), need.item(), world.getTime()));
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
