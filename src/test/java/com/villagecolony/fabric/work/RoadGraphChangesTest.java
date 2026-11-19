package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regra 317: o VC_ROAD_GRAPH conta quantas vezes o grafo mudou no dia de jogo, por colônia. */
class RoadGraphChangesTest {

    @Test
    void countsChangesWithinADayAndStartsOverTheNextDay() {
        UUID colony = UUID.randomUUID();

        assertEquals(1, RoadGraphRefresh.changedOn(colony, 4));
        assertEquals(2, RoadGraphRefresh.changedOn(colony, 4));
        assertEquals(3, RoadGraphRefresh.changedOn(colony, 4));
        assertEquals(1, RoadGraphRefresh.changedOn(colony, 5), "o dia novo recomeça a conta");
    }

    @Test
    void eachColonyHasItsOwnCount() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        RoadGraphRefresh.changedOn(first, 7);
        RoadGraphRefresh.changedOn(first, 7);

        assertEquals(1, RoadGraphRefresh.changedOn(second, 7));
    }
}
