package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.data.save.MaeWorkStore;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Execução de rua chamada exclusivamente pelo writer; publicação incerta nunca é repetida — MAE Fase 2. */
final class MaeRoadExecution {
    static final String TYPE = "road_v2";
    private MaeRoadExecution() { }

    static void validate(ServerWorld world, NbtCompound region, NbtCompound job) {
        if (!TYPE.equals(job.getString("type")) || !"terrain_v2".equals(region.getString("policy"))
                || job.getInt("contract") != 1 || !job.contains("hub", NbtElement.LONG_TYPE)
                || job.getBoolean("pendingPublication")) throw new IllegalStateException("unknown_or_pending_road_contract");
        if (!networkRevision(world, region.getUuid("colony")).equals(job.getString("networkBefore"))) {
            throw new IllegalStateException("road_network_changed");
        }
        var readOnly = job.getCompound("readOnly");
        MaeRoadEvidence.revalidate(world, region, readOnly);
        MaeProtection.inspectTerrain(world, region.getUuid("colony"), MaeProtection.margin(MaeWorldWriter.area(job)),
                MaeRoadEvidence.floors(readOnly));
        Set<ColonyPos> positions = surfaces(job);
        var plan = job.getList("plan", NbtElement.COMPOUND_TYPE);
        if (plan.size() != positions.size()) throw new IllegalStateException("invalid_road_plan");
        Set<ColonyPos> planned = new HashSet<>();
        for (var element : plan) {
            var step = (NbtCompound) element;
            BlockPos at = BlockPos.fromLong(step.getLong("position"));
            requireWritable(at, readOnly);
            if (!step.contains("position", NbtElement.LONG_TYPE) || !"pave".equals(step.getString("purpose"))
                    || !positions.contains(MinecraftTypeAdapter.toColonyPos(at))
                    || !planned.add(MinecraftTypeAdapter.toColonyPos(at))
                    || !MaeWorldWriter.decodeState(step.getCompound("after")).equals(StreetSurface.center(world, at,
                            MaeWorldWriter.decodeState(step.getCompound("before"))))) {
                throw new IllegalStateException("invalid_road_plan");
            }
        }
    }

    static void requireWritable(BlockPos at, NbtCompound readOnly) {
        if (MaeRoadEvidence.floors(readOnly).stream().anyMatch(road -> road.getX() == at.getX() && road.getZ() == at.getZ())) {
            throw new IllegalStateException("anchor_write");
        }
    }

    static Set<ColonyPos> surfaces(NbtCompound job) {
        if (!job.contains("surfaces", NbtElement.LIST_TYPE)) throw new IllegalStateException("missing_road_surfaces");
        var list = job.getList("surfaces", NbtElement.COMPOUND_TYPE);
        if (list.isEmpty() || list.size() > 4096 || list.size() != ((NbtList) job.get("surfaces")).size()) {
            throw new IllegalStateException("invalid_road_surfaces");
        }
        Set<ColonyPos> positions = new HashSet<>();
        for (var element : list) {
            var entry = (NbtCompound) element;
            if (!entry.contains("position", NbtElement.LONG_TYPE)) throw new IllegalStateException("invalid_road_surface");
            ColonyPos position = MinecraftTypeAdapter.toColonyPos(BlockPos.fromLong(entry.getLong("position")));
            if (!MaeWorldWriter.area(job).contains(position) || !positions.add(position)) {
                throw new IllegalStateException("invalid_road_surface");
            }
        }
        return positions;
    }

    static boolean accepts(BlockState after) {
        return StreetSurface.isStreetMaterial(after) && !after.isAir() && after.getFluidState().isEmpty();
    }

    static void checkSurface(ServerWorld world, BlockPos at, boolean paved) {
        var floor = world.getBlockState(at);
        if (!world.isChunkLoaded(at) || floor.hasBlockEntity() || !floor.getFluidState().isEmpty()
                || !(paved ? accepts(floor) : MaeProtection.terrain(floor))
                || floor.getCollisionShape(world, at).isEmpty()) throw new IllegalStateException("invalid_road_floor");
        for (int layer = 1; layer <= 2; layer++) {
            BlockPos support = at.down(layer);
            var state = world.getBlockState(support);
            if (!MaeProtection.terrain(state) || !state.isFullCube(world, support)) {
                throw new IllegalStateException("road_support");
            }
        }
        for (int height = 1; height <= 2; height++) {
            BlockPos head = at.up(height);
            var state = world.getBlockState(head);
            if (!state.getFluidState().isEmpty() || !state.getCollisionShape(world, head).isEmpty()) {
                throw new IllegalStateException("road_clearance");
            }
        }
    }

    static String networkRevision(ServerWorld world, UUID colony) {
        var store = ColonySavedData.get(world.getServer()).roadGraphs();
        if (!store.versionSupported(colony)) throw new IllegalStateException("unknown_road_graph");
        String graph = store.graph(colony).map(value -> value.nodes().stream()
                .map(node -> node.position() + ":" + node.roles().stream().map(Enum::name).sorted().toList())
                .sorted().toList() + "\n" + value.edges().stream().map(Object::toString).sorted().toList()).orElse("absent");
        String index = RoadIndex.roadsOf(colony).map(roads -> roads.columns().stream().sorted().toList().toString()).orElse("absent");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((graph + "\n" + index).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    static RoadGraph publicationGraph(ServerWorld world, UUID colonyId, NbtCompound job) {
        var colony = VillageColonyMod.COLONIES.find(colonyId).orElseThrow();
        Set<ColonyPos> positions = new HashSet<>(surfaces(job));
        ColonyPos hub = MinecraftTypeAdapter.toColonyPos(BlockPos.fromLong(job.getLong("hub")));
        var existing = ColonySavedData.get(world.getServer()).roadGraphs().graph(colonyId);
        if (existing.isPresent()) {
            if (existing.get().nodes().stream().noneMatch(node -> node.position().equals(hub)
                    && node.roles().contains(RoadGraph.Role.HUB))) throw new IllegalStateException("road_hub_changed");
            for (ColonyPos at : allPositions(existing.get())) {
                BlockPos pos = MinecraftTypeAdapter.toBlockPos(at);
                checkSurface(world, pos, true);
                if (RoadIndex.roadsOf(colonyId).stream().noneMatch(index -> index.columns().contains(ColonyRoads.column(at.x(), at.z())))) {
                    throw new IllegalStateException("road_index_diverged");
                }
                positions.add(at);
            }
        } else if (!positions.contains(hub) || Math.max(Math.abs((long) hub.x() - colony.center().x()),
                Math.abs((long) hub.z() - colony.center().z())) > RoadNetwork.SEED_RADIUS
                || RoadIndex.roadsOf(colonyId).isPresent()) throw new IllegalStateException("unproven_road_hub");
        if (positions.size() > RoadNetwork.MAX_COLUMNS) throw new IllegalStateException("road_graph_budget");
        Set<ColonyPos> entrances = new HashSet<>();
        existing.ifPresent(graph -> graph.nodes().stream().filter(node -> node.roles().contains(RoadGraph.Role.ENTRANCE))
                .forEach(node -> entrances.add(node.position())));
        RoadGraph graph = RoadGraph.from(positions, hub, entrances);
        if (graph.edges().stream().anyMatch(edge -> edge.roadClass() != RoadGraph.RoadClass.PRIMARY)) {
            throw new IllegalStateException("road_not_connected_to_hub");
        }
        // Nós isolados não produzem aresta; exigir componente físico único também nesse caso — ADR-043.
        Set<ColonyPos> reached = new HashSet<>();
        var pending = new java.util.ArrayDeque<ColonyPos>();
        reached.add(hub); pending.add(hub);
        while (!pending.isEmpty()) {
            ColonyPos at = pending.removeFirst();
            for (Direction side : Direction.Type.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
                ColonyPos next = new ColonyPos(at.x() + side.getOffsetX(), at.y() + dy, at.z() + side.getOffsetZ());
                if (positions.contains(next) && reached.add(next)) pending.add(next);
            }
        }
        if (reached.size() != positions.size()) throw new IllegalStateException("road_not_connected_to_hub");
        return graph;
    }

    static Set<ColonyPos> allPositions(RoadGraph graph) {
        Set<ColonyPos> positions = new HashSet<>();
        graph.nodes().forEach(node -> positions.add(node.position()));
        graph.edges().forEach(edge -> positions.addAll(edge.columns()));
        return positions;
    }

    static void deliver(ServerWorld world, UUID colony, NbtCompound record, BlockPos at, BlockState after) {
        MaeTerrainExecution.deliverLoot(world, colony, record, at);
        Supplies.record(SupplyException.SHAPED_GROUND, colony, Registries.BLOCK.getId(after.getBlock()).toString(), 1);
        record.putString("materials", "confirmed");
    }

    static void finish(ServerWorld world, NbtCompound region, NbtCompound job, MaeWorkStore store) {
        UUID colony = region.getUuid("colony");
        for (ColonyPos at : surfaces(job)) checkSurface(world, MinecraftTypeAdapter.toBlockPos(at), true);
        if (!networkRevision(world, colony).equals(job.getString("networkBefore"))) throw new IllegalStateException("road_network_changed");
        RoadGraph graph = publicationGraph(world, colony, job);
        NbtCompound publication = new NbtCompound();
        publication.putUuid("operation", job.getUuid("id"));
        publication.putString("purpose", "publication");
        publication.putString("networkBefore", job.getString("networkBefore"));
        publication.putString("result", "pending");
        NbtList history = region.getList("history", NbtElement.COMPOUND_TYPE);
        if (history.size() >= 1024) throw new IllegalStateException("history_budget_exceeded");
        history.add(publication);
        job.putBoolean("pendingPublication", true);
        region.put("history", history);
        region.put("job", job);
        store.update(region);
        // Diário precede os dois stores; falha entre eles fica pendente, sem retry automático — MAE Fase 2.
        ColonySavedData.get(world.getServer()).roadGraphs().update(colony, graph);
        var columns = allPositions(graph).stream().map(at -> ColonyRoads.column(at.x(), at.z())).distinct().sorted().toList();
        SweepState.ROADS.put(colony, new ColonyRoads(colony, VillageColonyMod.COLONIES.find(colony).orElseThrow().center(), columns));
        publication.putString("result", "confirmed");
        history.set(history.size() - 1, publication);
        job.putBoolean("pendingPublication", false);
        region.put("history", history);
    }
}
