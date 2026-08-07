package com.villagecolony.core.storage.service;

import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageRegistryTest {

    private static final ColonyPos CHEST = new ColonyPos(10, 64, 20);
    private static final ColonyPos OTHER_CHEST = new ColonyPos(11, 64, 20);

    private StorageRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new StorageRegistry();
    }

    @Test
    void registersAndFindsByWorker() {
        UUID worker = UUID.randomUUID();

        assertTrue(registry.register(WorkerStorage.of(worker, CHEST)).isEmpty());

        assertEquals(CHEST, registry.of(worker).orElseThrow().chestPosition());
        assertTrue(registry.hasStorage(worker));
        assertEquals(1, registry.count());
    }

    /**
     * Storage-System.md §"Falhas": o baú quebrado deve poder ser
     * trocado, senão o trabalhador fica preso a um que não existe mais.
     */
    @Test
    void registeringAgainReplacesAndReturnsThePrevious() {
        UUID worker = UUID.randomUUID();

        registry.register(WorkerStorage.of(worker, CHEST));

        WorkerStorage previous =
                registry.register(WorkerStorage.of(worker, OTHER_CHEST)).orElseThrow();

        assertEquals(CHEST, previous.chestPosition());
        assertEquals(OTHER_CHEST, registry.of(worker).orElseThrow().chestPosition());
        assertEquals(1, registry.count());
    }

    /**
     * Sem isto, dois aldeões do mesmo cômodo partilhariam um baú e cada
     * um contaria o estoque do outro como seu.
     */
    @Test
    void aClaimedChestIsReportedAsTaken() {
        registry.register(WorkerStorage.of(UUID.randomUUID(), CHEST));

        assertTrue(registry.isTaken(CHEST));
        assertFalse(registry.isTaken(OTHER_CHEST));
        assertFalse(registry.isTaken(null));
    }

    /** Trocado o baú, o antigo volta a estar livre. */
    @Test
    void theOldChestIsFreeAgainAfterAMove() {
        UUID worker = UUID.randomUUID();

        registry.register(WorkerStorage.of(worker, CHEST));
        registry.register(WorkerStorage.of(worker, OTHER_CHEST));

        assertFalse(registry.isTaken(CHEST));
    }

    @Test
    void removeForgetsTheStorage() {
        UUID worker = UUID.randomUUID();
        registry.register(WorkerStorage.of(worker, CHEST));

        assertTrue(registry.remove(worker));
        assertFalse(registry.hasStorage(worker));
        assertFalse(registry.remove(worker));
        assertFalse(registry.remove(null));
    }

    @Test
    void unknownWorkerHasNoStorage() {
        assertTrue(registry.of(UUID.randomUUID()).isEmpty());
        assertTrue(registry.of(null).isEmpty());
        assertFalse(registry.hasStorage(null));
    }

    @Test
    void clearEmptiesTheRegistry() {
        registry.register(WorkerStorage.of(UUID.randomUUID(), CHEST));
        registry.register(WorkerStorage.of(UUID.randomUUID(), OTHER_CHEST));

        registry.clear();

        assertEquals(0, registry.count());
    }

    @Test
    void allIsReadOnly() {
        registry.register(WorkerStorage.of(UUID.randomUUID(), CHEST));

        var all = registry.all();

        assertThrows(UnsupportedOperationException.class, () -> all.clear());
    }

    @Test
    void registerRejectsNull() {
        assertThrows(NullPointerException.class, () -> registry.register(null));
    }
}
