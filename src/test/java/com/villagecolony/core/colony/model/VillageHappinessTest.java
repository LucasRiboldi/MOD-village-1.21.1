package com.villagecolony.core.colony.model;

import org.junit.jupiter.api.Test;

import static com.villagecolony.core.colony.model.VillageHappiness.Mood.CONTENT;
import static com.villagecolony.core.colony.model.VillageHappiness.Mood.HAPPY;
import static com.villagecolony.core.colony.model.VillageHappiness.Mood.UNHAPPY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Comida por pessoa, camas sobrando e obras concluídas (ADR-036 20). */
class VillageHappinessTest {

    @Test
    void fedRoomyAndBuiltIsHappy() {
        VillageHappiness happiness = VillageHappiness.measure(10 * 12, 10, 12, 1);

        assertEquals(HAPPY, happiness.mood());
        assertEquals(12, happiness.foodPerAdult());
        assertEquals(2, happiness.spareBeds());
        assertEquals(3, happiness.score());
    }

    @Test
    void hungryAndCrowdedIsUnhappy() {
        assertEquals(UNHAPPY, VillageHappiness.measure(3 * 10, 10, 10, 5).mood(),
                "três pontos por pessoa e nenhuma cama sobrando: uma obra não compensa");
    }

    @Test
    void inBetweenIsContent() {
        assertEquals(CONTENT, VillageHappiness.measure(8 * 4, 4, 5, 0).mood());
        assertEquals(CONTENT, VillageHappiness.measure(12 * 4, 4, 4, 1).mood(),
                "bem alimentada e com obra, mas sem cama sobrando");
    }

    @Test
    void aVillageWithNoAdultsDoesNotDivideByZero() {
        assertEquals(30, VillageHappiness.measure(30, 0, 2, 0).foodPerAdult());
    }

    @Test
    void aSupperThatFedEveryoneMakesTheVillageHappier() {
        assertEquals(CONTENT, VillageHappiness.measure(12 * 4, 4, 4, 1, false).mood());
        assertEquals(HAPPY, VillageHappiness.measure(12 * 4, 4, 4, 1, true).mood(),
                "o convívio da reunião soma");
    }

    @Test
    void threeWheatAreWorthABread() {
        assertEquals(4, VillageHappiness.wheatPoints(3));
        assertEquals(8, VillageHappiness.wheatPoints(6));
    }
}
