package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
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

class WorkerServiceTest {

    private static final UUID COLONY = UUID.randomUUID();
    private static final UUID OTHER_COLONY = UUID.randomUUID();

    private WorkerService service;

    @BeforeEach
    void setUp() {
        service = new WorkerService();
    }

    @Test
    void startsEmpty() {
        assertEquals(0, service.count());
        assertTrue(service.all().isEmpty());
    }

    @Test
    void registerAddsTheWorker() {
        UUID villager = UUID.randomUUID();

        Worker worker = service.register(villager, COLONY);

        assertEquals(1, service.count());
        assertEquals(Optional.of(worker), service.find(villager));
        assertTrue(service.isRegistered(villager));
    }

    /**
     * A varredura roda a cada ciclo e reencontra os mesmos aldeões.
     * Reencontrar alguém não pode criar um segundo registro.
     */
    @Test
    void registeringTheSameVillagerTwiceIsIdempotent() {
        UUID villager = UUID.randomUUID();

        Worker first = service.register(villager, COLONY);
        Worker second = service.register(villager, COLONY);

        assertEquals(1, service.count());
        assertSame(first, second);
    }

    /** E, sobretudo, não pode apagar a profissão já atribuída. */
    @Test
    void reregisteringKeepsTheAssignedProfession() {
        UUID villager = UUID.randomUUID();

        service.register(villager, COLONY).assign(ProfessionType.LUMBERJACK);

        Worker again = service.register(villager, COLONY);

        assertEquals(Optional.of(ProfessionType.LUMBERJACK), again.profession());
    }

    @Test
    void registerRejectsNulls() {
        assertThrows(NullPointerException.class, () -> service.register(null, COLONY));
        assertThrows(NullPointerException.class, () -> service.register(UUID.randomUUID(), null));
    }

    @Test
    void findMissingVillagerIsEmpty() {
        assertEquals(Optional.empty(), service.find(UUID.randomUUID()));
        assertEquals(Optional.empty(), service.find(null));
        assertFalse(service.isRegistered(null));
    }

    @Test
    void ofColonyReturnsOnlyThatColonysWorkers() {
        Worker mine = service.register(UUID.randomUUID(), COLONY);
        service.register(UUID.randomUUID(), OTHER_COLONY);
        Worker alsoMine = service.register(UUID.randomUUID(), COLONY);

        assertEquals(List.of(mine, alsoMine), service.ofColony(COLONY));
        assertEquals(2, service.countOfColony(COLONY));
        assertEquals(1, service.countOfColony(OTHER_COLONY));
    }

    @Test
    void ofColonyWithNoWorkersIsEmpty() {
        assertTrue(service.ofColony(UUID.randomUUID()).isEmpty());
        assertThrows(NullPointerException.class, () -> service.ofColony(null));
    }

    @Test
    void allKeepsInsertionOrder() {
        Worker first = service.register(UUID.randomUUID(), COLONY);
        Worker second = service.register(UUID.randomUUID(), COLONY);

        assertEquals(List.of(first, second), List.copyOf(service.all()));
    }

    @Test
    void allIsUnmodifiable() {
        Worker worker = service.register(UUID.randomUUID(), COLONY);

        assertThrows(
                UnsupportedOperationException.class, () -> service.all().remove(worker));
    }

    @Test
    void removeDropsTheWorker() {
        UUID villager = UUID.randomUUID();
        service.register(villager, COLONY);

        assertTrue(service.remove(villager));
        assertEquals(0, service.count());
        assertFalse(service.remove(villager));
        assertFalse(service.remove(null));
    }

    @Test
    void restorePutsBackAWorkerWithItsProfession() {
        UUID villager = UUID.randomUUID();

        service.restore(Worker.restore(villager, COLONY, ProfessionType.FARMER));

        assertEquals(
                ProfessionType.FARMER,
                service.find(villager).orElseThrow().profession().orElseThrow());
    }

    /**
     * Ao contrário de register, que a varredura repete de propósito:
     * villagerId repetido no save esconderia qual profissão venceu.
     */
    @Test
    void restoreRejectsDuplicate() {
        UUID villager = UUID.randomUUID();

        service.restore(Worker.restore(villager, COLONY, ProfessionType.FARMER));

        assertThrows(IllegalStateException.class, () ->
                service.restore(Worker.restore(villager, OTHER_COLONY, ProfessionType.BUILDER)));
    }

    /** A varredura roda depois do carregamento e não pode apagar função. */
    @Test
    void registerAfterRestoreKeepsProfession() {
        UUID villager = UUID.randomUUID();

        service.restore(Worker.restore(villager, COLONY, ProfessionType.LUMBERJACK));
        service.register(villager, COLONY);

        assertEquals(
                ProfessionType.LUMBERJACK,
                service.find(villager).orElseThrow().profession().orElseThrow());
    }

    @Test
    void restoreRejectsNull() {
        assertThrows(NullPointerException.class, () -> service.restore(null));
    }

    @Test
    void clearEmptiesTheRegistry() {
        service.register(UUID.randomUUID(), COLONY);
        service.register(UUID.randomUUID(), OTHER_COLONY);

        service.clear();

        assertEquals(0, service.count());
    }
}
