package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ligação local da obra à rua: o trajeto do piso da entrada até o grafo de ruas MAE já publicado ou, sem
 * grafo nem índice de ruas na colônia, até a semente perto do centro — ADR-045 e ADR-046, item 1 do fim do plano.
 *
 * <p>Só planeja, sem escrever: o trajeto vai para {@link MaeRoadPreparation} e pelo writer, que revalidam
 * tudo. Fica dentro da região reservada, contorna a pegada e só passa por chão de terreno com apoio e
 * passagem livre, sem corte nem aterro (rua pede terreno nivelado, ADR-045).
 */
final class MaeStructureRoad {
    /** Teto da busca, o mesmo do plano de rua. */
    static final int MAX_CELLS = 4096;

    private MaeStructureRoad() { }

    /** O que a preparação de rua recebe: trajeto em ordem, hub e, ligando a um grafo, a célula certificada. */
    record Plan(List<BlockPos> route, BlockPos hub, @Nullable BlockPos connection) { }

    static Optional<Plan> plan(ServerWorld world, UUID colonyId, WorkArea region, Building lot, BlockPos entry) {
        // A preparação lê dois abaixo e três acima de cada célula, mais a margem de proteção de dois.
        WorkArea usable = new WorkArea(
                new ColonyPos(region.min().x() + 2, region.min().y() + 4, region.min().z() + 2),
                new ColonyPos(region.max().x() - 2, region.max().y() - 5, region.max().z() - 2));
        var graph = ColonySavedData.get(world.getServer()).roadGraphs().graph(colonyId);
        Set<BlockPos> network = new HashSet<>();
        BlockPos hub = null;
        if (graph.isPresent()) {
            for (ColonyPos at : MaeRoadExecution.allPositions(graph.get())) network.add(MinecraftTypeAdapter.toBlockPos(at));
            hub = graph.get().nodes().stream().filter(node -> node.roles().contains(RoadGraph.Role.HUB))
                    .map(node -> MinecraftTypeAdapter.toBlockPos(node.position())).findFirst().orElse(null);
            if (hub == null) return Optional.empty();
        } else if (RoadIndex.roadsOf(colonyId).isPresent()) {
            return Optional.empty();
        }
        ColonyPos center = VillageColonyMod.COLONIES.find(colonyId).orElseThrow().center();
        if (!roadable(world, usable, lot, network, entry)) return Optional.empty();
        Map<BlockPos, BlockPos> parents = new HashMap<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        parents.put(entry, entry);
        pending.add(entry);
        while (!pending.isEmpty() && parents.size() <= MAX_CELLS) {
            BlockPos at = pending.removeFirst();
            if (network.isEmpty() && seed(at, center)) {
                return Optional.of(new Plan(path(parents, at), at, null));
            }
            for (Direction side : Direction.Type.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
                BlockPos next = at.offset(side).up(dy);
                if (network.contains(next)) return Optional.of(new Plan(path(parents, at), hub, next));
                if (parents.containsKey(next) || !roadable(world, usable, lot, network, next)) continue;
                parents.put(next, at);
                pending.addLast(next);
            }
        }
        return Optional.empty();
    }

    private static boolean seed(BlockPos at, ColonyPos center) {
        return Math.max(Math.abs((long) at.getX() - center.x()), Math.abs((long) at.getZ() - center.z()))
                <= RoadNetwork.SEED_RADIUS;
    }

    /** Chão de terreno firme, dois de apoio, dois de passagem; fora da pegada, de rua alheia e da rede. */
    private static boolean roadable(ServerWorld world, WorkArea usable, Building lot, Set<BlockPos> network, BlockPos at) {
        if (!usable.contains(MinecraftTypeAdapter.toColonyPos(at)) || network.contains(at)
                || (at.getX() >= lot.min().x() && at.getX() <= lot.max().x()
                    && at.getZ() >= lot.min().z() && at.getZ() <= lot.max().z())
                || !world.isChunkLoaded(at) || RoadIndex.isLaidByAColony(at)) return false;
        BlockState floor = world.getBlockState(at);
        if (!MaeProtection.terrain(floor) || floor.isAir() || !floor.isFullCube(world, at)
                || StreetSurface.center(world, at, floor).equals(floor)) return false;
        for (int layer = 1; layer <= 2; layer++) {
            BlockState support = world.getBlockState(at.down(layer));
            if (!MaeProtection.terrain(support) || !support.isFullCube(world, at.down(layer))) return false;
        }
        for (int height = 1; height <= 2; height++) {
            BlockState head = world.getBlockState(at.up(height));
            if (!head.getFluidState().isEmpty() || !head.getCollisionShape(world, at.up(height)).isEmpty()) return false;
        }
        return true;
    }

    private static List<BlockPos> path(Map<BlockPos, BlockPos> parents, BlockPos end) {
        var route = new ArrayList<BlockPos>();
        BlockPos at = end;
        while (true) {
            route.add(at.toImmutable());
            BlockPos parent = parents.get(at);
            if (parent.equals(at)) break;
            at = parent;
        }
        Collections.reverse(route);
        return route;
    }
}
