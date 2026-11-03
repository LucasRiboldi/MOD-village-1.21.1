package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.integration.RoadGraphMigration;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.MaeRoadAccess;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Ativa o grafo somente depois de confirmar no mundo todo o índice legado — ADR-043. */
final class RoadGraphActivation {
    private RoadGraphActivation() {
    }

    static Optional<Set<Long>> roads(ServerWorld world, Colony colony) {
        if (!MaeRoadAccess.metadataAllowed(world, colony.id())) {
            var confirmed = ColonySavedData.get(world.getServer()).roadGraphs();
            return Optional.of(confirmed.versionSupported(colony.id())
                    ? confirmed.graph(colony.id()).map(RoadGraphActivation::columnsOf).orElse(Set.of()) : Set.of());
        }
        if (!MineHomePlans.isCurrentVillage(world, colony.center())) {
            return Optional.empty();
        }
        var store = ColonySavedData.get(world.getServer()).roadGraphs();
        if (!store.versionSupported(colony.id())) {
            return Optional.of(Set.of());
        }
        Optional<ColonyRoads> legacy = RoadIndex.roadsOf(colony.id());
        if (legacy.isEmpty()) {
            return Optional.of(Set.of());
        }
        var inspection = RoadGraphMigration.inspect(world, legacy.get(), Set.of());
        if (inspection.graph().isEmpty()) {
            return Optional.of(Set.of());
        }
        RoadGraph graph = inspection.graph().orElseThrow();
        if (!store.graph(colony.id()).equals(Optional.of(graph))) {
            store.update(colony.id(), graph);
        }
        return Optional.of(columnsOf(graph));
    }

    static Set<Long> columnsOf(RoadGraph graph) {
        Set<Long> columns = new HashSet<>();
        graph.nodes().forEach(node -> columns.add(ColonyRoads.column(node.position().x(), node.position().z())));
        graph.edges().forEach(edge -> edge.columns()
                .forEach(at -> columns.add(ColonyRoads.column(at.x(), at.z()))));
        return Set.copyOf(columns);
    }
}
