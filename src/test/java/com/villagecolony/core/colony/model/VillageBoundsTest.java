package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A caixa da vila — decisão do autor, 2026-09-30.
 *
 * <p>Os exemplos são os do autor: uma vila de 100 × 100 com uma construção no
 * bloco 95 passa a 100 × 108; um bloco de rua 1 fora da área passa a
 * 100 × 101. Nunca encolhe.
 */
class VillageBoundsTest {

    /** Uma vila de 100 × 100: X e Z de 0 a 99. */
    private static final VillageBounds HUNDRED = new VillageBounds(0, 60, 0, 99, 70, 99);

    @Test
    void aConstructionNearTheBorderPushesItTwelveBlocksOut() {
        VillageBounds grown = HUNDRED.union(VillageBounds.aroundPiece(
                new ColonyPos(50, 64, 90), new ColonyPos(54, 68, 95)));

        assertEquals(100, grown.sizeX());
        assertEquals(108, grown.sizeZ(), "construção no bloco 95: a vila vai a 100 x 108");
    }

    @Test
    void aRoadBlockOutsideIsIncludedExactly() {
        VillageBounds grown = HUNDRED.union(VillageBounds.block(new ColonyPos(50, 64, 100)));

        assertEquals(101, grown.sizeZ(), "rua 1 bloco fora: a vila vai a 100 x 101");
        assertEquals(100, grown.sizeX());
    }

    @Test
    void aConstructionDeepInsideChangesNothing() {
        VillageBounds same = HUNDRED.union(VillageBounds.aroundPiece(
                new ColonyPos(40, 64, 40), new ColonyPos(50, 68, 50)));

        assertEquals(HUNDRED, same);
    }

    @Test
    void itNeverShrinks() {
        VillageBounds small = new VillageBounds(40, 64, 40, 50, 66, 50);

        assertEquals(HUNDRED, HUNDRED.union(small));
    }

    @Test
    void theCenterIsTheMiddleOfTheBox() {
        assertEquals(50, HUNDRED.centerX());
        assertEquals(50, HUNDRED.centerZ());

        VillageBounds grown = HUNDRED.union(VillageBounds.block(new ColonyPos(50, 64, 120)));

        assertEquals(60, grown.centerZ(), "o centro anda para o meio da medida nova");
    }

    @Test
    void theTypicalVillageIsOneHundredFortyFourAroundThePoint() {
        VillageBounds typical = VillageBounds.typicalAround(new ColonyPos(1000, 70, -500), 66, 74);

        assertEquals(144, typical.sizeX());
        assertEquals(144, typical.sizeZ());
        assertEquals(1000, typical.centerX());
        assertEquals(-500, typical.centerZ());
        assertEquals(66, typical.minY());
        assertEquals(74, typical.maxY());
    }

    @Test
    void theBedWindowGoesEightBelowAndTwentyFourAbove() {
        assertTrue(HUNDRED.holdsBedAt(52));
        assertTrue(HUNDRED.holdsBedAt(94));
        assertFalse(HUNDRED.holdsBedAt(51), "abaixo do solo da vila não há cama dela");
        assertFalse(HUNDRED.holdsBedAt(95), "no céu acima da vila também não");
        assertFalse(HUNDRED.holdsBedAt(-20), "a Trial Chamber embaixo da vila não é a vila");
    }

    @Test
    void containsAColumnWithAndWithoutMargin() {
        assertTrue(HUNDRED.containsColumn(0, 99));
        assertFalse(HUNDRED.containsColumn(100, 50));
        assertTrue(HUNDRED.containsColumn(115, 50, 16));
        assertFalse(HUNDRED.containsColumn(116, 50, 16));
    }

    @Test
    void twoVillagesTouchOnlyWithinTheMargin() {
        VillageBounds sixteenAway = new VillageBounds(115, 60, 0, 200, 70, 99);
        VillageBounds seventeenAway = new VillageBounds(116, 60, 0, 200, 70, 99);

        assertTrue(HUNDRED.touches(sixteenAway, VillageBounds.IDENTITY_MARGIN));
        assertFalse(HUNDRED.touches(seventeenAway, VillageBounds.IDENTITY_MARGIN));
    }

    @Test
    void aUnionWithItselfIsTheSameObject() {
        assertSame(HUNDRED, HUNDRED.union(HUNDRED));
    }

    @Test
    void anInvertedBoxIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new VillageBounds(10, 0, 0, 9, 0, 0));
    }
}
