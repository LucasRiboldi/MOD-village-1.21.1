package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Distância física ao centro sem conectar componentes por proximidade — ADR-043. */
class RoadGraphRoutesTest {
    private static ColonyPos at(int x, int z) {
        return new ColonyPos(x, 64, z);
    }

    @Test
    void countsInteriorColumnsAndBranchesThroughContractedEdges() {
        Set<ColonyPos> roads = new HashSet<>();
        for (int x = 0; x <= 6; x++) {
            roads.add(at(x, 0));
        }
        roads.add(at(3, 1));
        roads.add(at(3, 2));
        roads.add(at(8, 0));
        var distances = RoadGraphRoutes.distancesToHub(RoadGraph.from(roads, at(0, 0), Set.of()));
        assertEquals(0, distances.get(at(0, 0)));
        assertEquals(2, distances.get(at(2, 0)));
        assertEquals(5, distances.get(at(3, 2)));
        assertEquals(6, distances.get(at(6, 0)));
        assertFalse(distances.containsKey(at(8, 0)));
        assertThrows(UnsupportedOperationException.class, () -> distances.put(at(8, 0), 8));
    }

    @Test
    void takesTheShorterSideOfACycleAtEveryInteriorColumn() {
        Set<ColonyPos> ring = Set.of(at(0, 0), at(1, 0), at(2, 0), at(2, 1),
                at(2, 2), at(1, 2), at(0, 2), at(0, 1));
        var distances = RoadGraphRoutes.distancesToHub(RoadGraph.from(ring, at(0, 0), Set.of()));
        assertEquals(8, distances.size());
        assertEquals(1, distances.get(at(0, 1)));
        assertEquals(3, distances.get(at(1, 2)));
        assertEquals(4, distances.get(at(2, 2)));
    }

    @Test
    void missingHubDoesNotInventConnectionAndIsolatedHubStillExists() {
        assertTrue(RoadGraphRoutes.distancesToHub(
                RoadGraph.from(Set.of(at(1, 0)), at(0, 0), Set.of())).isEmpty());
        assertEquals(java.util.Map.of(at(0, 0), 0), RoadGraphRoutes.distancesToHub(
                RoadGraph.from(Set.of(at(0, 0)), at(0, 0), Set.of())));
    }

    @Test
    void preservesHeightInsteadOfMergingOverlappingHorizontalColumns() {
        ColonyPos raised = new ColonyPos(1, 65, 0);
        ColonyPos disconnected = new ColonyPos(0, 67, 0);
        var distances = RoadGraphRoutes.distancesToHub(RoadGraph.from(
                Set.of(at(0, 0), raised, disconnected), at(0, 0), Set.of()));
        assertEquals(1, distances.get(raised));
        assertFalse(distances.containsKey(disconnected));
    }
}
