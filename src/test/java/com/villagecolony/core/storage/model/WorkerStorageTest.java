package com.villagecolony.core.storage.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerStorageTest {

    private static final ColonyPos CHEST = new ColonyPos(10, 64, 20);

    @Test
    void keepsWhatItWasGiven() {
        UUID worker = UUID.randomUUID();

        WorkerStorage storage = WorkerStorage.of(worker, CHEST);

        assertEquals(worker, storage.workerId());
        assertEquals(CHEST, storage.chestPosition());
    }

    @Test
    void answersWhereItIs() {
        WorkerStorage storage = WorkerStorage.of(UUID.randomUUID(), CHEST);

        assertTrue(storage.isAt(new ColonyPos(10, 64, 20)));
        assertFalse(storage.isAt(new ColonyPos(10, 65, 20)));
    }

    /** Um trabalhador tem um baú: a identidade é a do dono. */
    @Test
    void twoStoragesOfTheSameWorkerAreTheSame() {
        UUID worker = UUID.randomUUID();

        assertEquals(
                WorkerStorage.of(worker, CHEST),
                WorkerStorage.of(worker, new ColonyPos(99, 64, 99)));

        assertNotEquals(
                WorkerStorage.of(worker, CHEST),
                WorkerStorage.of(UUID.randomUUID(), CHEST));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class,
                () -> WorkerStorage.of(null, CHEST));

        assertThrows(NullPointerException.class,
                () -> WorkerStorage.of(UUID.randomUUID(), null));
    }
}
