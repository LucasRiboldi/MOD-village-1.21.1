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
        assertEquals(UrgentPlans.First.PEN, UrgentPlans.firstBuild(false, 0, 0, false));
        assertEquals(UrgentPlans.First.FARM, UrgentPlans.firstBuild(true, 0, 1, false));
        assertEquals(UrgentPlans.First.NONE, UrgentPlans.firstBuild(true, 1, 9, false));
        assertEquals(UrgentPlans.First.FARM, UrgentPlans.firstBuild(true, 1, 10, false));
        assertEquals(UrgentPlans.First.NONE, UrgentPlans.firstBuild(true, 2, 19, false));
        assertEquals(UrgentPlans.First.FARM, UrgentPlans.firstBuild(true, 2, 20, false));
    }

    /**
     * Emenda de 26-11 (K2 🅱️ do jogo da 0.4.148: a plantação devida passava pela frente do adiamento da Regra 373 e as obras
     * pararam de novo): com a plantação adiada, a vila não a deve agora; e a plantação grande nasce em qualquer lote, sem
     * o alcance do fazendeiro.
     */
    @Test
    @DisplayName("Regra 374: a plantação devida respeita o adiamento, e a grande não pede o alcance do fazendeiro")
    void theOwedFarmWaitsWhilePostponedAndTheLargeFarmGoesAnywhere() {
        assertEquals(UrgentPlans.First.NONE, UrgentPlans.firstBuild(true, 0, 1, true));
        assertEquals(UrgentPlans.First.PEN, UrgentPlans.firstBuild(false, 0, 1, true));
        assertEquals(false, LotSeeking.needsTheFarmersReach(LargeFarmPlans.MODEL));
        assertEquals(true, LotSeeking.needsTheFarmersReach(
                com.villagecolony.core.type.ResourceId.vanilla("village/plains/houses/plains_small_farm_1")));
    }
}
