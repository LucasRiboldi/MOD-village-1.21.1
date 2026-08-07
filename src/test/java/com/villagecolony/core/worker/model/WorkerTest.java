package com.villagecolony.core.worker.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerTest {

    private static final UUID VILLAGER = UUID.randomUUID();
    private static final UUID COLONY = UUID.randomUUID();

    @Test
    void registeredWorkerKeepsItsIds() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        assertEquals(VILLAGER, worker.villagerId());
        assertEquals(COLONY, worker.colonyId());
    }

    /** Registrar e dar função são momentos diferentes. */
    @Test
    void registeredWorkerHasNoProfessionYet() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        assertEquals(Optional.empty(), worker.profession());
        assertFalse(worker.hasProfession());
    }

    @Test
    void registerRejectsNulls() {
        assertThrows(NullPointerException.class, () -> Worker.register(null, COLONY));
        assertThrows(NullPointerException.class, () -> Worker.register(VILLAGER, null));
    }

    @Test
    void assignGivesTheWorkerAProfession() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        worker.assign(ProfessionType.LUMBERJACK);

        assertEquals(Optional.of(ProfessionType.LUMBERJACK), worker.profession());
        assertTrue(worker.hasProfession());
    }

    /** A colônia realoca conforme a necessidade muda. */
    @Test
    void assignReplacesThePreviousProfession() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        worker.assign(ProfessionType.FARMER);
        worker.assign(ProfessionType.BUILDER);

        assertEquals(Optional.of(ProfessionType.BUILDER), worker.profession());
    }

    @Test
    void assignRejectsNull() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        assertThrows(NullPointerException.class, () -> worker.assign(null));
    }

    @Test
    void unassignRemovesTheProfession() {
        Worker worker = Worker.register(VILLAGER, COLONY);
        worker.assign(ProfessionType.MANUFACTURER);

        worker.unassign();

        assertFalse(worker.hasProfession());
        assertEquals(Optional.empty(), worker.profession());
    }

    @Test
    void restoreKeepsTheSavedProfession() {
        Worker worker = Worker.restore(VILLAGER, COLONY, ProfessionType.BUILDER);

        assertEquals(Optional.of(ProfessionType.BUILDER), worker.profession());
    }

    /** Quem não tinha função quando o mundo fechou volta sem função. */
    @Test
    void restoreAcceptsAbsentProfession() {
        Worker worker = Worker.restore(VILLAGER, COLONY, null);

        assertFalse(worker.hasProfession());
    }

    @Test
    void belongsToAnswersAboutTheOwningColony() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        assertTrue(worker.belongsTo(COLONY));
        assertFalse(worker.belongsTo(UUID.randomUUID()));
    }

    /** A identidade é o aldeão, não a função nem a colônia. */
    @Test
    void identityIsTheVillager() {
        Worker one = Worker.register(VILLAGER, COLONY);
        Worker other = Worker.restore(VILLAGER, UUID.randomUUID(), ProfessionType.FARMER);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void differentVillagersAreDifferentWorkers() {
        Worker one = Worker.register(UUID.randomUUID(), COLONY);
        Worker other = Worker.register(UUID.randomUUID(), COLONY);

        assertNotEquals(one, other);
    }

    @Test
    void professionsAreTheFourFromTheDataModel() {
        assertEquals(4, ProfessionType.values().length);

        assertEquals(ProfessionType.LUMBERJACK, ProfessionType.valueOf("LUMBERJACK"));
        assertEquals(ProfessionType.MANUFACTURER, ProfessionType.valueOf("MANUFACTURER"));
        assertEquals(ProfessionType.FARMER, ProfessionType.valueOf("FARMER"));
        assertEquals(ProfessionType.BUILDER, ProfessionType.valueOf("BUILDER"));
    }
}
