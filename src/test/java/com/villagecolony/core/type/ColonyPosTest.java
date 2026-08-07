package com.villagecolony.core.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColonyPosTest {

    @Test
    void distanceIgnoresHeight() {
        ColonyPos ground = new ColonyPos(0, 64, 0);
        ColonyPos hilltop = new ColonyPos(3, 200, 4);

        assertEquals(25L, ground.horizontalDistanceSquared(hilltop));
    }

    @Test
    void distanceIsSymmetric() {
        ColonyPos one = new ColonyPos(10, 64, 20);
        ColonyPos other = new ColonyPos(-5, 70, 8);

        assertEquals(
                one.horizontalDistanceSquared(other),
                other.horizontalDistanceSquared(one));
    }

    @Test
    void distanceToItselfIsZero() {
        ColonyPos pos = new ColonyPos(7, 64, -3);

        assertEquals(0L, pos.horizontalDistanceSquared(pos));
    }

    /**
     * Coordenadas extremas do Minecraft (±30.000.000) estouram int quando
     * elevadas ao quadrado. O cálculo precisa ser feito em long.
     */
    @Test
    void distanceSurvivesWorldBorderCoordinates() {
        ColonyPos west = new ColonyPos(-30_000_000, 64, 0);
        ColonyPos east = new ColonyPos(30_000_000, 64, 0);

        assertEquals(3_600_000_000_000_000L, west.horizontalDistanceSquared(east));
    }

    @Test
    void equalityIsByValue() {
        assertEquals(new ColonyPos(1, 2, 3), new ColonyPos(1, 2, 3));
    }
}
