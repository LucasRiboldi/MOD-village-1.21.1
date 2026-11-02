package com.villagecolony.fabric.work;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MineSupplyTest {

    @AfterEach
    void cleanUp() {
        MineSupply.clearAll();
    }

    @Test
    void autonomousMiningStopsAfterOneStackForTheDay() {
        UUID colony = UUID.randomUUID();

        assertEquals(64, MineSupply.room(colony, 0, 0, 128));
        assertEquals(32, MineSupply.room(colony, 100, 32, 96));
        assertEquals(0, MineSupply.room(colony, 200, 64, 64));
    }

    @Test
    void removingStoneDoesNotOpenAnotherDailyStack() {
        UUID colony = UUID.randomUUID();

        MineSupply.room(colony, 0, 0, 128);
        MineSupply.room(colony, 100, 64, 64);

        assertEquals(0, MineSupply.room(colony, 200, 0, 128));
    }

    @Test
    void nextDayStartsOneNewStack() {
        UUID colony = UUID.randomUUID();

        MineSupply.room(colony, 0, 0, 128);
        MineSupply.room(colony, 100, 64, 64);

        assertEquals(64, MineSupply.room(colony, 24_000, 64, 128));
    }
}
