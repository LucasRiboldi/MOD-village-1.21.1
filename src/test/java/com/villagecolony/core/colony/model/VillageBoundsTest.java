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
    void aConstructionNearTheBorderPushesOnlyThatSideFifteenBlocksOut() {
        VillageBounds village = HUNDRED.centered();
        VillageBounds grown = village.grownBy(VillageBounds.aroundPiece(
                new ColonyPos(50, 64, 90), new ColonyPos(54, 68, 95)));

        assertEquals(village.sizeX(), grown.sizeX(), "o lado que a obra não passa não muda");
        assertEquals(0, grown.minZ(), "o norte não muda");
        assertEquals(110, grown.maxZ(), "construção até o bloco 95: os 15 em volta levam o sul a 110");
    }

    /** Autor, 2026-10-03: a vila nasce com as peças da vila gerada e o centro num bloco só. */
    @Test
    void theGeneratedVillageGetsAnExactCenterBlock() {
        // A vila do autor: peças em X −312..−171 (142) e Z 361..476 (116).
        VillageBounds village = new VillageBounds(-312, 61, 361, -171, 78, 476).centered();

        assertEquals(143, village.sizeX());
        assertEquals(117, village.sizeZ());
        assertEquals(-241, village.centerX(), "o centro é o bloco do meio, com 71 blocos de cada lado");
        assertEquals(-241 - (-312), -170 - (-241), "o mesmo número de blocos dos dois lados do centro em X");
        assertEquals(419 - 361, 477 - 419, "e em Z");
    }

    /** Uma obra em cima e outra do lado: cada lado avança pelo seu tanto. */
    @Test
    void twoBuildsPushTheirOwnSidesByTheirOwnAmounts() {
        VillageBounds village = HUNDRED.centered();

        VillageBounds grown = village
                .grownBy(VillageBounds.aroundPiece(new ColonyPos(40, 64, 8), new ColonyPos(45, 68, 12)))
                .grownBy(VillageBounds.aroundPiece(new ColonyPos(90, 64, 40), new ColonyPos(94, 68, 50)));

        assertEquals(-8, grown.minZ(), "a obra de cima passou 7 do norte; o lado par ganha mais uma linha ao norte");
        assertEquals(110, grown.maxX(), "a obra do lado passou 9 do leste; o lado par ganha mais uma linha a leste");
        assertEquals(0, grown.minX(), "o oeste não muda");
        assertEquals(100, grown.maxZ(), "o sul não muda");
        assertEquals(1, grown.sizeX() % 2, "lado ímpar");
        assertEquals(1, grown.sizeZ() % 2, "lado ímpar");
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

    /** A marcação da área mostra só a borda perto do jogador — 2026-10-03. */
    @Test
    void onlyTheBorderNearThePlayerIsMarked() {
        VillageBounds village = new VillageBounds(0, 60, 0, 100, 70, 100);

        java.util.List<ColonyPos> near = village.borderNear(0, 50, 10, 2);

        assertTrue(!near.isEmpty(), "o jogador encostado no oeste devia ver a borda oeste");
        assertTrue(near.stream().allMatch(column -> column.x() == 0 && Math.abs(column.z() - 50) <= 10),
                "só a borda oeste, até 10 blocos: " + near);
        assertTrue(village.borderNear(50, 50, 10, 2).isEmpty(), "no meio da vila não há borda perto");
        assertEquals(400, village.borderNear(50, 50, 200, 1).size(), "a borda inteira de 101 x 101: 4 x 101 - 4");
    }
}
