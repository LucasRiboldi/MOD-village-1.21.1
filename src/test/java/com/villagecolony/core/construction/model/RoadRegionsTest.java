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
import static org.junit.jupiter.api.Assertions.assertSame;
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
        Map<Long, Integer> before = RoadRegions.fingerprints(street, Set.of(), Set.of(), FLAT);
        Map<Long, Integer> again = RoadRegions.fingerprints(List.copyOf(street).reversed(), Set.of(), Set.of(), FLAT);
        Set<ColonyPos> bumped = new HashSet<>(street);

        bumped.add(at(35, 9));

        Map<Long, Integer> now = RoadRegions.fingerprints(bumped, Set.of(), Set.of(), FLAT);

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

        Map<Long, Integer> before = RoadRegions.fingerprints(street, Set.of(), Set.of(), FLAT);
        Map<Long, Integer> now = RoadRegions.fingerprints(street, Set.of(), Set.of(), levelled);

        assertEquals(Set.of(RoadRegions.regionOf(3, 8)), RoadRegions.dirty(before, now, Set.of()));
    }

    @Test
    void aMarkedSquareIsDirtyEvenWithTheSameFingerprint() {
        Map<Long, Integer> prints = RoadRegions.fingerprints(street(), Set.of(), Set.of(), FLAT);
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

    @Test
    void theSquareThatWasCleanComesOutExactlyAsTheGraphBefore() {
        Set<ColonyPos> street = street();

        for (int z = 0; z <= 16; z++) {
            street.add(at(5, z));
        }

        RoadLines.Result first = RoadLines.build(street, Set.of(), at(0, 8), Set.of(), Set.of(), FLAT, 0, 0);
        RoadGraph before = first.graph().orElseThrow();
        Set<Long> dirty = Set.of(RoadRegions.regionOf(35, 8));
        Set<ColonyPos> bumped = new HashSet<>(street);

        bumped.add(at(35, 9));
        bumped.add(at(35, 10));
        bumped.add(at(6, 9));

        RoadLines.Result built = RoadLines.build(bumped, Set.of(), at(0, 8), Set.of(), Set.of(), FLAT, 0, 0,
                RoadRegions.frozen(before, dirty));
        RoadLines.Result settled = RoadRegions.settle(built, before, dirty, at(0, 8), Set.of(), bumped);

        assertEquals(clean(RoadRegions.columnsOf(before), dirty), clean(RoadRegions.columnsOf(settled.graph().orElseThrow()), dirty),
                "Regra 319: um quadrado limpo saiu diferente do grafo anterior");
    }

    @Test
    void withoutAGraphBeforeTheBuildIsKept() {
        RoadLines.Result built = RoadLines.build(street(), Set.of(), at(0, 8), Set.of(), Set.of(), FLAT, 0, 0);

        assertSame(built, RoadRegions.settle(built, null, Set.of(), at(0, 8), Set.of(), street()));
    }

    @Test
    void aSquareIsRebuiltOnlyWhenItStaysChangedForTwoReads() {
        RoadRegions.Memory memory = new RoadRegions.Memory();
        long square = RoadRegions.regionOf(35, 8);
        Map<Long, Integer> first = RoadRegions.fingerprints(street(), Set.of(), Set.of(), FLAT);
        Set<ColonyPos> bumped = new HashSet<>(street());

        bumped.add(at(35, 9));

        Map<Long, Integer> changed = RoadRegions.fingerprints(bumped, Set.of(), Set.of(), FLAT);

        assertEquals(first.keySet(), RoadRegions.confirm(memory, first, Set.of(), true), "a primeira montagem refaz tudo");
        assertTrue(RoadRegions.confirm(memory, changed, Set.of(), false).isEmpty(), "Regra 319: mudou uma vez e já refez");
        assertEquals(Set.of(square), RoadRegions.confirm(memory, changed, Set.of(), false),
                "Regra 319: mudou em duas releituras seguidas e não refez");
        assertTrue(RoadRegions.confirm(memory, changed, Set.of(), false).isEmpty(), "Regra 319: refez o que já estava aceito");
    }

    @Test
    void aChangeThatGoesBackBeforeTheSecondReadIsNotRebuilt() {
        RoadRegions.Memory memory = new RoadRegions.Memory();
        Map<Long, Integer> first = RoadRegions.fingerprints(street(), Set.of(), Set.of(), FLAT);
        Set<ColonyPos> bumped = new HashSet<>(street());

        bumped.add(at(35, 9));
        RoadRegions.confirm(memory, first, Set.of(), true);
        RoadRegions.confirm(memory, RoadRegions.fingerprints(bumped, Set.of(), Set.of(), FLAT), Set.of(), false);

        assertTrue(RoadRegions.confirm(memory, first, Set.of(), false).isEmpty(), "Regra 319: a mudança passageira refez o quadrado");
    }

    @Test
    void aMarkedSquareIsRebuiltOnTheNextRead() {
        RoadRegions.Memory memory = new RoadRegions.Memory();
        Map<Long, Integer> prints = RoadRegions.fingerprints(street(), Set.of(), Set.of(), FLAT);
        long marked = RoadRegions.regionOf(20, 8);

        RoadRegions.confirm(memory, prints, Set.of(), true);

        assertTrue(RoadRegions.confirm(memory, prints, Set.of(marked), false).isEmpty());
        assertEquals(Set.of(marked), RoadRegions.confirm(memory, prints, Set.of(), false),
                "Regra 319: o bloco marcado não refez o quadrado na releitura seguinte");
    }

    private static Set<ColonyPos> clean(Set<ColonyPos> columns, Set<Long> dirty) {
        Set<ColonyPos> kept = new HashSet<>();

        columns.stream().filter(c -> !dirty.contains(RoadRegions.regionOf(c.x(), c.z()))).forEach(kept::add);

        return kept;
    }
}
