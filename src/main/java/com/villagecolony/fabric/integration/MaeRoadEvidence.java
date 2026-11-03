package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Contexto congelado de leitura; índice ou aparência não substituem a cadeia de geração — MAE Fase 2. */
final class MaeRoadEvidence {
    private MaeRoadEvidence() { }

    static NbtCompound capture(ServerWorld world, NbtCompound region, WorkArea inspection, ColonyPos anchor) {
        UUID colony = region.getUuid("colony");
        RoadGraph graph = graph(world, colony, anchor);
        Set<ColonyPos> roads = new HashSet<>();
        NbtList cells = new NbtList();
        for (ColonyPos position : MaeRoadExecution.allPositions(graph)) {
            if (!inspection.contains(position)) continue;
            BlockPos at = MinecraftTypeAdapter.toBlockPos(position);
            certify(world, region, at);
            roads.add(position);
            for (int height = -2; height <= 3; height++) {
                BlockPos cell = at.up(height);
                if (!MaeWorldWriter.area(region).contains(MinecraftTypeAdapter.toColonyPos(cell))) {
                    throw new IllegalStateException("anchor_context_outside_region");
                }
                NbtCompound snapshot = new NbtCompound();
                snapshot.putLong("position", cell.asLong());
                snapshot.put("state", NbtHelper.fromBlockState(world.getBlockState(cell)));
                cells.add(snapshot);
            }
        }
        if (!roads.contains(anchor)) throw new IllegalStateException("anchor_outside_inspection");
        NbtList floors = new NbtList();
        for (ColonyPos position : roads) {
            NbtCompound floor = new NbtCompound();
            floor.putLong("position", MinecraftTypeAdapter.toBlockPos(position).asLong());
            floors.add(floor);
        }
        NbtCompound context = new NbtCompound();
        context.putInt("version", 1);
        context.putLong("anchor", MinecraftTypeAdapter.toBlockPos(anchor).asLong());
        context.put("floors", floors);
        context.put("cells", cells);
        return context;
    }

    static Set<BlockPos> floors(NbtCompound context) {
        if (context.isEmpty()) return Set.of();
        if (context.getInt("version") != 1 || !context.contains("anchor", NbtElement.LONG_TYPE)
                || !context.contains("floors", NbtElement.LIST_TYPE) || !context.contains("cells", NbtElement.LIST_TYPE)) {
            throw new IllegalStateException("unknown_anchor_context");
        }
        var list = context.getList("floors", NbtElement.COMPOUND_TYPE);
        if (list.isEmpty() || list.size() > 4096 || list.size() != ((NbtList) context.get("floors")).size()) {
            throw new IllegalStateException("invalid_anchor_context");
        }
        Set<BlockPos> floors = new HashSet<>();
        for (var element : list) {
            var entry = (NbtCompound) element;
            if (!entry.contains("position", NbtElement.LONG_TYPE)
                    || !floors.add(BlockPos.fromLong(entry.getLong("position")))) throw new IllegalStateException("invalid_anchor_context");
        }
        if (!floors.contains(BlockPos.fromLong(context.getLong("anchor")))) throw new IllegalStateException("missing_anchor_context");
        return Set.copyOf(floors);
    }

    static void revalidate(ServerWorld world, NbtCompound region, NbtCompound context) {
        if (context.isEmpty()) return;
        UUID colony = region.getUuid("colony");
        ColonyPos anchor = MinecraftTypeAdapter.toColonyPos(BlockPos.fromLong(context.getLong("anchor")));
        RoadGraph graph = graph(world, colony, anchor);
        Set<ColonyPos> positions = MaeRoadExecution.allPositions(graph);
        for (BlockPos at : floors(context)) {
            if (!positions.contains(MinecraftTypeAdapter.toColonyPos(at))) throw new IllegalStateException("anchor_graph_changed");
            certify(world, region, at);
        }
        var cells = context.getList("cells", NbtElement.COMPOUND_TYPE);
        Set<BlockPos> expected = new HashSet<>();
        for (BlockPos floor : floors(context)) for (int y = -2; y <= 3; y++) expected.add(floor.up(y));
        if (cells.size() != expected.size() || cells.size() != ((NbtList) context.get("cells")).size()) {
            throw new IllegalStateException("invalid_anchor_cells");
        }
        for (var element : cells) {
            var entry = (NbtCompound) element;
            if (!entry.contains("position", NbtElement.LONG_TYPE) || !entry.contains("state", NbtElement.COMPOUND_TYPE)) {
                throw new IllegalStateException("invalid_anchor_cells");
            }
            BlockPos at = BlockPos.fromLong(entry.getLong("position"));
            if (!expected.remove(at)) throw new IllegalStateException("invalid_anchor_cells");
            if (!world.isChunkLoaded(at) || !NbtHelper.fromBlockState(world.getBlockState(at)).equals(entry.getCompound("state"))) {
                throw new IllegalStateException("anchor_state_changed");
            }
        }
    }

    private static RoadGraph graph(ServerWorld world, UUID colony, ColonyPos anchor) {
        var store = ColonySavedData.get(world.getServer()).roadGraphs();
        if (!store.versionSupported(colony)) throw new IllegalStateException("unknown_road_graph");
        RoadGraph graph = store.graph(colony).orElseThrow(() -> new IllegalStateException("missing_road_graph"));
        boolean primary = graph.edges().stream().anyMatch(edge -> edge.roadClass() == RoadGraph.RoadClass.PRIMARY
                && edge.columns().contains(anchor)) || graph.nodes().stream().anyMatch(node -> node.position().equals(anchor)
                && node.roles().contains(RoadGraph.Role.HUB));
        if (!primary) throw new IllegalStateException("anchor_not_primary");
        return graph;
    }

    private static void certify(ServerWorld world, NbtCompound region, BlockPos at) {
        UUID colony = region.getUuid("colony");
        long column = ColonyRoads.column(at.getX(), at.getZ());
        if (!MaeWorldWriter.area(region).contains(MinecraftTypeAdapter.toColonyPos(at))
                || RoadIndex.roadsOf(colony).stream().noneMatch(index -> index.columns().contains(column))
                || SweepState.ROADS.values().stream().anyMatch(index -> !index.colonyId().equals(colony)
                        && index.columns().contains(column))) throw new IllegalStateException("anchor_index_diverged");
        MaeRoadExecution.checkSurface(world, at, true);
        NbtList history = region.getList("history", NbtElement.COMPOUND_TYPE);
        for (var element : history) {
            var record = (NbtCompound) element;
            if ("pave".equals(record.getString("purpose")) && record.getLong("position") == at.asLong()
                    && "applied".equals(record.getString("result")) && "confirmed".equals(record.getString("materials"))
                    && record.containsUuid("operation") && NbtHelper.fromBlockState(world.getBlockState(at)).equals(record.getCompound("after"))) {
                for (var publicationElement : history) {
                    var publication = (NbtCompound) publicationElement;
                    if ("publication".equals(publication.getString("purpose")) && publication.containsUuid("operation")
                            && publication.getUuid("operation").equals(record.getUuid("operation"))
                            && "confirmed".equals(publication.getString("result"))) return;
                }
            }
        }
        // Rua de geração permanece válida após mudanças vizinhas próprias confirmadas; nunca importar outro hash.
        if (history.stream().map(entry -> (NbtCompound) entry).anyMatch(entry ->
                entry.contains("position", NbtElement.LONG_TYPE) && entry.getLong("position") == at.asLong())) {
            throw new IllegalStateException("uncertified_road");
        }
        WorkArea cell = new WorkArea(MinecraftTypeAdapter.toColonyPos(at), MinecraftTypeAdapter.toColonyPos(at));
        MaeWorldWriter.validateEvidence(world, cell, MaeGenerationEvidence.inspect(world, cell, false, 1),
                region.getCompound("ownedSections"));
    }
}
