package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.MaeRoadAccess;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** As colunas de caminho que o traçado de porta enxerga na vila nova — ADR-043, Regra 177. */
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
        // A releitura periódica publica o grafo (RoadGraphRefresh). A linha do grafo é a do meio da rua, e o traçado
        // de porta conta a rua inteira: o índice físico mais o grafo.
        Set<Long> roads = new HashSet<>(RoadIndex.roadsOf(colony.id()).map(ColonyRoads::columns).orElse(java.util.List.of()));

        store.graph(colony.id()).ifPresent(graph -> roads.addAll(columnsOf(graph)));

        return Optional.of(roads);
    }

    static Set<Long> columnsOf(RoadGraph graph) {
        Set<Long> columns = new HashSet<>();
        graph.nodes().forEach(node -> columns.add(ColonyRoads.column(node.position().x(), node.position().z())));
        graph.edges().forEach(edge -> edge.columns()
                .forEach(at -> columns.add(ColonyRoads.column(at.x(), at.z()))));
        return Set.copyOf(columns);
    }
}
