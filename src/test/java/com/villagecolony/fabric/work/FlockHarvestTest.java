package com.villagecolony.fabric.work;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O excedente de abate usa a meta de criação vigente (Regras 68 e 238). */
class FlockHarvestTest {

    /** O abate lê as espécies do registro do jogo: ele precisa estar de pé, seja qual for a ordem dos testes. */
    @BeforeAll
    static void bootMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void nothingToHarvestAtOrBelowTheBaseTarget() {
        int target = ShepherdFlock.FLOCK_TARGET;
        assertEquals(0, FlockHarvest.surplus(target, target));
        assertEquals(0, FlockHarvest.surplus(target - 1, target));
        assertEquals(0, FlockHarvest.surplus(0, target));
    }

    @Test
    void oneAboveTheTargetIsSurplus() {
        int target = ShepherdFlock.FLOCK_TARGET;
        assertEquals(1, FlockHarvest.surplus(target + 1, target));
    }

    @Test
    void harvestUsesTheHigherTargetWhenFoodIsPlentiful() {
        int target = ShepherdFlock.targetFor(ShepherdFlock.SURPLUS_FOOD);
        assertEquals(ShepherdFlock.SURPLUS_TARGET, target);
        assertEquals(0, FlockHarvest.surplus(target, target));
        assertEquals(1, FlockHarvest.surplus(target + 1, target));
    }

    /**
     * Regra 380 (nota do jogo da 0.4.145, J6 🅲: "só depois de ter uma quantidade mínima, que qualquer abate daquela raça
     * pode acontecer"): nenhum abate com menos de 6 adultos da espécie, mesmo que a meta de criação seja menor.
     */
    @Test
    void noHarvestBelowSixAdultsWhateverTheTarget() {
        assertEquals(0, FlockHarvest.surplus(6, 2));
        assertEquals(1, FlockHarvest.surplus(7, 2));
        assertEquals(0, FlockHarvest.surplus(5, 0));
    }
}
