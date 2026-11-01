package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoadGraphTest {
    private static ColonyPos p(int x, int y, int z) {
        return new ColonyPos(x, y, z);
    }

    @Test
    void contractsStraightRoadButPreservesEveryPhysicalColumn() {
        var a = p(0, 64, 0);
        var b = p(1, 64, 0);
        var c = p(2, 64, 0);
        var graph = RoadGraph.from(Set.of(a, b, c), a, Set.of(c));
        assertEquals(2, graph.nodes().size());
        assertEquals(List.of(a, b, c), graph.edges().getFirst().columns());
        assertEquals(RoadGraph.RoadClass.PRIMARY, graph.edges().getFirst().roadClass());
    }

    @Test
    void splitsAtEntranceInsideRoadAndAtTJunction() {
        var a = p(0, 64, 0);
        var door = p(1, 64, 0);
        var junction = p(2, 64, 0);
        var graph = RoadGraph.from(Set.of(a, door, junction, p(3, 64, 0), p(2, 64, 1)),
                a, Set.of(door));
        assertEquals(4, graph.edges().size());
        assertTrue(graph.nodes().stream().anyMatch(n -> n.position().equals(door)
                && n.roles().contains(RoadGraph.Role.ENTRANCE)));
        assertTrue(graph.nodes().stream().anyMatch(n -> n.position().equals(junction)
                && n.roles().contains(RoadGraph.Role.JUNCTION)));
    }

    @Test
    void doesNotConnectDiagonalOrTwoBlockCliff() {
        var a = p(0, 64, 0);
        var graph = RoadGraph.from(Set.of(a, p(1, 64, 1), p(1, 66, 0)), a, Set.of());
        assertEquals(3, graph.nodes().size());
        assertTrue(graph.edges().isEmpty());
    }

    @Test
    void preservesBendAndOneBlockSteps() {
        var a = p(0, 64, 0);
        var b = p(1, 65, 0);
        var c = p(1, 66, 1);
        var graph = RoadGraph.from(Set.of(a, b, c), a, Set.of());
        assertEquals(List.of(a, b, c), graph.edges().getFirst().columns());
    }

    @Test
    void preservesCycleWithoutEndpoints() {
        var a = p(0, 64, 0);
        var graph = RoadGraph.from(Set.of(a, p(1, 64, 0), p(1, 64, 1), p(0, 64, 1)),
                p(20, 64, 20), Set.of());
        assertEquals(1, graph.nodes().size());
        var edge = graph.edges().getFirst();
        assertEquals(edge.from(), edge.to());
        assertEquals(5, edge.columns().size());
        assertEquals(4, Set.copyOf(edge.columns()).size());
        assertEquals(RoadGraph.RoadClass.SECONDARY, edge.roadClass());
    }

    @Test
    void unknownEntranceAndHubDoNotCreatePhysicalRoads() {
        var a = p(0, 64, 0);
        var unknown = p(20, 64, 20);
        var graph = RoadGraph.from(Set.of(a), unknown, Set.of(unknown));
        assertEquals(1, graph.nodes().size());
        assertEquals(a, graph.nodes().getFirst().position());
        assertTrue(graph.edges().isEmpty());
    }

    @Test
    void inputOrderDoesNotChangeGraphAndResultsAreImmutable() {
        var a = p(0, 64, 0);
        var b = p(1, 64, 0);
        var c = p(2, 64, 0);
        var first = RoadGraph.from(new LinkedHashSet<>(List.of(c, a, b)), a, Set.of(c));
        var second = RoadGraph.from(new LinkedHashSet<>(List.of(b, c, a)), a, Set.of(c));
        assertEquals(first, second);
        assertThrows(UnsupportedOperationException.class, () -> first.nodes().clear());
        assertThrows(UnsupportedOperationException.class, () -> first.edges().clear());
        assertThrows(UnsupportedOperationException.class, () -> first.edges().getFirst().columns().clear());
    }
}
