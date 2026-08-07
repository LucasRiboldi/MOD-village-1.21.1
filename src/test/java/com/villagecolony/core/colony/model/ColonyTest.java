package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColonyTest {

    private static final ColonyPos CENTER = new ColonyPos(100, 64, -200);

    @Test
    void createStartsStableAndActive() {
        Colony colony = Colony.create(UUID.randomUUID(), CENTER);

        assertEquals(ColonyState.STABLE, colony.state());
        assertEquals(ColonyLifecycle.ACTIVE, colony.lifecycle());
    }

    @Test
    void createKeepsIdAndCenter() {
        UUID id = UUID.randomUUID();

        Colony colony = Colony.create(id, CENTER);

        assertEquals(id, colony.id());
        assertEquals(CENTER, colony.center());
    }

    @Test
    void createRejectsNulls() {
        assertThrows(NullPointerException.class, () -> Colony.create(null, CENTER));
        assertThrows(NullPointerException.class, () -> Colony.create(UUID.randomUUID(), null));
    }

    @Test
    void restoreKeepsSavedStates() {
        Colony colony = Colony.restore(
                UUID.randomUUID(), CENTER, ColonyState.EXPANSION, ColonyLifecycle.DORMANT);

        assertEquals(ColonyState.EXPANSION, colony.state());
        assertEquals(ColonyLifecycle.DORMANT, colony.lifecycle());
    }

    /** ADR-002: hibernar não pode apagar o que a colônia estava fazendo. */
    @Test
    void goingDormantPreservesState() {
        Colony colony = Colony.create(UUID.randomUUID(), CENTER);
        colony.setState(ColonyState.PRODUCTION);

        colony.setLifecycle(ColonyLifecycle.DORMANT);

        assertEquals(ColonyState.PRODUCTION, colony.state());
        assertFalse(colony.isActive());
    }

    @Test
    void isActiveFollowsLifecycle() {
        Colony colony = Colony.create(UUID.randomUUID(), CENTER);
        assertTrue(colony.isActive());

        colony.setLifecycle(ColonyLifecycle.DORMANT);
        assertFalse(colony.isActive());

        colony.setLifecycle(ColonyLifecycle.ACTIVE);
        assertTrue(colony.isActive());
    }

    @Test
    void settersRejectNulls() {
        Colony colony = Colony.create(UUID.randomUUID(), CENTER);

        assertThrows(NullPointerException.class, () -> colony.setState(null));
        assertThrows(NullPointerException.class, () -> colony.setLifecycle(null));
    }

    @Test
    void identityIsTheId() {
        UUID id = UUID.randomUUID();

        Colony one = Colony.create(id, CENTER);
        Colony other = Colony.restore(
                id, new ColonyPos(0, 0, 0), ColonyState.EXPANSION, ColonyLifecycle.DORMANT);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void differentIdsAreDifferentColonies() {
        Colony one = Colony.create(UUID.randomUUID(), CENTER);
        Colony other = Colony.create(UUID.randomUUID(), CENTER);

        assertNotEquals(one, other);
    }
}
