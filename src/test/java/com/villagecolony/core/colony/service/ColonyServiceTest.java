package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColonyServiceTest {

    private static final ColonyPos ORIGIN = new ColonyPos(0, 64, 0);

    private ColonyService service;

    @BeforeEach
    void setUp() {
        service = new ColonyService();
    }

    @Test
    void startsEmpty() {
        assertEquals(0, service.count());
        assertTrue(service.all().isEmpty());
    }

    @Test
    void createColonyRegistersIt() {
        Colony colony = service.createColony(ORIGIN);

        assertEquals(1, service.count());
        assertEquals(Optional.of(colony), service.find(colony.id()));
    }

    @Test
    void createColonyGivesDistinctIds() {
        Colony one = service.createColony(ORIGIN);
        Colony other = service.createColony(ORIGIN);

        assertEquals(2, service.count());
        assertFalse(one.id().equals(other.id()));
    }

    @Test
    void createColonyRejectsNullCenter() {
        assertThrows(NullPointerException.class, () -> service.createColony(null));
    }

    @Test
    void registerAcceptsRestoredColony() {
        Colony restored = Colony.restore(
                UUID.randomUUID(), ORIGIN, ColonyState.EXPANSION, ColonyLifecycle.DORMANT);

        service.register(restored);

        assertSame(restored, service.find(restored.id()).orElseThrow());
    }

    /** Sobrescrever em silêncio esconderia save corrompido. */
    @Test
    void registerRejectsDuplicateId() {
        Colony colony = service.createColony(ORIGIN);

        assertThrows(IllegalStateException.class, () -> service.register(colony));
        assertEquals(1, service.count());
    }

    @Test
    void findMissingIdIsEmpty() {
        assertEquals(Optional.empty(), service.find(UUID.randomUUID()));
    }

    @Test
    void findNullIdIsEmpty() {
        assertEquals(Optional.empty(), service.find(null));
    }

    @Test
    void findNearestPicksTheClosest() {
        service.createColony(new ColonyPos(500, 64, 0));
        Colony near = service.createColony(new ColonyPos(30, 64, 0));
        service.createColony(new ColonyPos(-800, 64, 0));

        assertEquals(Optional.of(near), service.findNearest(ORIGIN, 100));
    }

    @Test
    void findNearestIgnoresColoniesOutsideRadius() {
        service.createColony(new ColonyPos(500, 64, 0));

        assertEquals(Optional.empty(), service.findNearest(ORIGIN, 100));
    }

    @Test
    void findNearestIncludesColonyExactlyOnRadius() {
        Colony onEdge = service.createColony(new ColonyPos(100, 64, 0));

        assertEquals(Optional.of(onEdge), service.findNearest(ORIGIN, 100));
    }

    /** Altura não deve afastar uma colônia: a distância é no plano. */
    @Test
    void findNearestIgnoresHeight() {
        Colony hilltop = service.createColony(new ColonyPos(10, 250, 0));

        assertEquals(Optional.of(hilltop), service.findNearest(ORIGIN, 20));
    }

    @Test
    void findNearestOnEmptyRegistryIsEmpty() {
        assertEquals(Optional.empty(), service.findNearest(ORIGIN, 1000));
    }

    @Test
    void findNearestWithNegativeRadiusIsEmpty() {
        service.createColony(ORIGIN);

        assertEquals(Optional.empty(), service.findNearest(ORIGIN, -1));
    }

    @Test
    void findNearestRejectsNullPosition() {
        assertThrows(NullPointerException.class, () -> service.findNearest(null, 100));
    }

    @Test
    void allKeepsInsertionOrder() {
        Colony first = service.createColony(new ColonyPos(1, 64, 0));
        Colony second = service.createColony(new ColonyPos(2, 64, 0));
        Colony third = service.createColony(new ColonyPos(3, 64, 0));

        assertEquals(List.of(first, second, third), List.copyOf(service.all()));
    }

    @Test
    void allIsUnmodifiable() {
        Colony colony = service.createColony(ORIGIN);

        assertThrows(
                UnsupportedOperationException.class,
                () -> service.all().remove(colony));
    }

    @Test
    void removeDropsTheColony() {
        Colony colony = service.createColony(ORIGIN);

        assertTrue(service.remove(colony.id()));
        assertEquals(0, service.count());
        assertEquals(Optional.empty(), service.find(colony.id()));
    }

    @Test
    void removeMissingIdReportsFalse() {
        assertFalse(service.remove(UUID.randomUUID()));
        assertFalse(service.remove(null));
    }

    @Test
    void clearEmptiesTheRegistry() {
        service.createColony(new ColonyPos(1, 64, 0));
        service.createColony(new ColonyPos(2, 64, 0));

        service.clear();

        assertEquals(0, service.count());
    }

    /** Após remover, o mesmo id pode ser registrado de novo. */
    @Test
    void removeThenRegisterIsAllowed() {
        Colony colony = service.createColony(ORIGIN);
        service.remove(colony.id());

        service.register(colony);

        assertEquals(1, service.count());
    }
}
