package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regra 322: só a aresta igual em 3 releituras seguidas é calçada; a que muda recomeça a conta. */
class RoadStabilityTest {

    private static final PathTerrain FLAT = new PathTerrain() {
        @Override
        public OptionalInt groundY(int x, int z, int nearY) {
            return OptionalInt.of(64);
        }

        @Override
        public boolean blocked(int x, int z) {
            return false;
        }
    };

    private static RoadGraph street(int length) {
        Set<ColonyPos> cells = new HashSet<>();

        for (int x = 0; x <= length; x++) {
            cells.add(new ColonyPos(x, 64, 0));
        }

        return RoadLines.build(cells, Set.of(), new ColonyPos(0, 64, 0), Set.of(), Set.of(), FLAT, 0, 0).graph().orElseThrow();
    }

    @Test
    void anEdgeIsPavedOnlyAfterThreeEqualReads() {
        RoadStability.Memory memory = new RoadStability.Memory();
        RoadGraph graph = street(20);
        long middle = ColonyRoads.column(10, 0);

        assertFalse(RoadStability.stableColumns(memory, graph).contains(middle), "calçou na primeira releitura");
        assertFalse(RoadStability.stableColumns(memory, graph).contains(middle), "calçou na segunda releitura");
        assertTrue(RoadStability.stableColumns(memory, graph).contains(middle), "não calçou na terceira releitura igual");
    }

    @Test
    void aChangedEdgeStartsOver() {
        RoadStability.Memory memory = new RoadStability.Memory();

        RoadStability.stableColumns(memory, street(20));
        RoadStability.stableColumns(memory, street(20));

        assertFalse(RoadStability.stableColumns(memory, street(24)).contains(ColonyRoads.column(10, 0)),
                "a aresta que mudou na terceira releitura já ficou estável");
    }
}
