package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.VillageBounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** As faixas da vila, do centro para fora — pedido do autor de 2026-10-08. */
class VillageZoneTest {

    /** 121 de largura em x e 61 em z, centro em (0, 0). */
    private static final VillageBounds BOX = new VillageBounds(-60, 60, -30, 60, 80, 30);

    @Test
    void theThirdsOfTheBoxAreCentreMiddleAndEdge() {
        assertEquals(VillageZone.CENTER, VillageZone.of(BOX, 10, 5, 10));
        assertEquals(VillageZone.MIDDLE, VillageZone.of(BOX, 30, 0, 10));
        assertEquals(VillageZone.EDGE, VillageZone.of(BOX, 55, 0, 10));
    }

    @Test
    void theNarrowAxisCountsByItsOwnHalfWidth() {
        // 25 em z é quase a borda de uma caixa de 61; em x seria o centro.
        assertEquals(VillageZone.EDGE, VillageZone.of(BOX, 0, 25, 10));
        assertEquals(VillageZone.CENTER, VillageZone.of(BOX, 18, 0, 10));
    }

    @Test
    void aLittleOutsideIsAllowedAndFurtherIsNot() {
        assertEquals(VillageZone.OUTSIDE, VillageZone.of(BOX, 68, 0, 10));
        assertEquals(VillageZone.BEYOND, VillageZone.of(BOX, 75, 0, 10));
    }
}
