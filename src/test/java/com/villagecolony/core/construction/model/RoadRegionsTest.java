package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regra 319 (ADR-057, modo 2): quadrados de 16 × 16 com impressão digital; só o sujo se refaz, o limpo entra fixo. */
class RoadRegionsTest {

    private static final int Y = 64;

    private static final PathTerrain FLAT = new PathTerrain() {
        @Override
        public OptionalInt groundY(int x, int z, int nearY) {
            return OptionalInt.of(Y);
        }

        @Override
        public boolean blocked(int x, int z) {
            return false;
        }
    };

    private static ColonyPos at(int x, int z) {
        return new ColonyPos(x, Y, z);
    }

    /** Uma rua de x 0 a 40 em z 8: passa por três quadrados (0, 1 e 2 em x). */
    private static Set<ColonyPos> street() {
        Set<ColonyPos> cells = new HashSet<>();

        for (int x = 0; x <= 40; x++) {
            cells.add(at(x, 8));
        }

        return cells;
    }

    @Test
    void theFingerprintDoesNotDependOnOrderAndOnlyTheChangedSquareIsDirty() {
        Set<ColonyPos> street = street();
        Map<Long, Integer> before = RoadRegions.fingerprints(street, Set.of(), Set.of(), Set.of(), FLAT);
        Map<Long, Integer> again = RoadRegions.fingerprints(List.copyOf(street).reversed(), Set.of(), Set.of(), Set.of(), FLAT);
        Set<ColonyPos> bumped = new HashSet<>(street);

        bumped.add(at(35, 9));

        Map<Long, Integer> now = RoadRegions.fingerprints(bumped, Set.of(), Set.of(), Set.of(), FLAT);

        assertEquals(before, again);
        assertEquals(Set.of(RoadRegions.regionOf(35, 9)), RoadRegions.dirty(before, now, Set.of()));
    }

    @Test
    void aChangedGroundHeightMakesTheSquareDirty() {
        Set<ColonyPos> street = street();
        PathTerrain levelled = new PathTerrain() {
            @Override
            public OptionalInt groundY(int x, int z, int nearY) {
                return OptionalInt.of(x == 3 ? Y - 1 : Y);
            }

            @Override
            public boolean blocked(int x, int z) {
                return false;
            }
        };

        Map<Long, Integer> before = RoadRegions.fingerprints(street, Set.of(), Set.of(), Set.of(), FLAT);
        Map<Long, Integer> now = RoadRegions.fingerprints(street, Set.of(), Set.of(), Set.of(), levelled);

        assertEquals(Set.of(RoadRegions.regionOf(3, 8)), RoadRegions.dirty(before, now, Set.of()));
    }

    @Test
    void aMarkedSquareIsDirtyEvenWithTheSameFingerprint() {
        Map<Long, Integer> prints = RoadRegions.fingerprints(street(), Set.of(), Set.of(), Set.of(), FLAT);
        long marked = RoadRegions.regionOf(20, 8);

        assertEquals(Set.of(marked), RoadRegions.dirty(prints, prints, Set.of(marked)));
    }

    @Test
    void theCleanSquaresKeepTheirLinesThroughTheBuild() {
        Set<ColonyPos> street = street();
        RoadGraph before = RoadLines.build(street, Set.of(), at(0, 8), Set.of(), Set.of(), FLAT, 0, 0).graph().orElseThrow();
        Set<Long> dirty = Set.of(RoadRegions.regionOf(35, 8));
        Set<ColonyPos> frozen = RoadRegions.frozen(before, dirty);

        assertFalse(frozen.isEmpty());
        assertTrue(frozen.stream().noneMatch(column -> dirty.contains(RoadRegions.regionOf(column.x(), column.z()))),
                "coluna de quadrado sujo ficou fixa: " + frozen);

        Set<ColonyPos> bumped = new HashSet<>(street);

        bumped.add(at(35, 9));
        bumped.add(at(35, 10));

        RoadGraph after = RoadLines.build(bumped, Set.of(), at(0, 8), Set.of(), Set.of(), FLAT, 0, 0, frozen)
                .graph().orElseThrow();

        assertTrue(RoadRegions.columnsOf(after).containsAll(frozen),
                "uma linha de quadrado limpo saiu do grafo depois de mudar só o quadrado sujo");
    }
}
