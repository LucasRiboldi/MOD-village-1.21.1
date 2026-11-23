package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O excedente de abate usa a meta de criação vigente (Regras 68 e 238). */
class FlockHarvestTest {

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
}
