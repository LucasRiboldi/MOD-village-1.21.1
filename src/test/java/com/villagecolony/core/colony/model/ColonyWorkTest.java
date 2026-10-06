package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Vila abandonada para de trabalhar — ADR-036 item 10. */
class ColonyWorkTest {

    private final Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

    @Test
    void aLiveColonyInALoadedChunkWorks() {
        assertTrue(colony.canWork());
    }

    @Test
    void anAbandonedColonyDoesNotWorkAndWorksAgainWhenTheVillageComesBack() {
        colony.setState(ColonyState.ABANDONED);
        assertFalse(colony.canWork(), "a vila abandonada continuou trabalhando");

        colony.setState(ColonyState.STABLE);
        assertTrue(colony.canWork(), "a vila que voltou não voltou a trabalhar");
    }

    @Test
    void aDormantColonyDoesNotWork() {
        colony.setLifecycle(ColonyLifecycle.DORMANT);

        assertFalse(colony.canWork());
    }
}
