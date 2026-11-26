package com.villagecolony.fabric.work;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regra 374 (notas do jogo da 0.4.145, J2 🅲: "as duas primeiras construções de qualquer vila é um curral grande e uma
 * fazenda grande do mod" e "acrescentar a regra de construir fazenda a cada 10 construções"): o curral grande vem
 * primeiro, a plantação grande depois, e a cada 10 construções prontas a vila deve mais uma plantação grande.
 */
class FirstBuildsTest {

    @Test
    @DisplayName("Regra 374: curral grande primeiro, plantação grande depois, e uma plantação a cada 10 obras")
    void thePenComesFirstThenTheFarmThenOneFarmEveryTenBuildings() {
        assertEquals(UrgentPlans.First.PEN, UrgentPlans.firstBuild(false, 0, 0));
        assertEquals(UrgentPlans.First.FARM, UrgentPlans.firstBuild(true, 0, 1));
        assertEquals(UrgentPlans.First.NONE, UrgentPlans.firstBuild(true, 1, 9));
        assertEquals(UrgentPlans.First.FARM, UrgentPlans.firstBuild(true, 1, 10));
        assertEquals(UrgentPlans.First.NONE, UrgentPlans.firstBuild(true, 2, 19));
        assertEquals(UrgentPlans.First.FARM, UrgentPlans.firstBuild(true, 2, 20));
    }
}
