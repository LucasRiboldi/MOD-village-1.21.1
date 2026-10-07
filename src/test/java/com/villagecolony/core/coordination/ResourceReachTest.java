package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A busca vai até 10 blocos além da borda, alternando centro e borda (ADR-036 18). */
class ResourceReachTest {

    /** Uma vila de 61 x 41 com centro em (0, 0). */
    private static final VillageBounds BOX = new VillageBounds(-30, 50, -20, 30, 90, 20);

    private static final ColonyPos CENTRE = new ColonyPos(0, 64, 0);

    @Test
    void theLimitIsTenBlocksPastTheEdge() {
        assertTrue(ResourceReach.within(BOX, 40, 0), "dez além da borda leste vale");
        assertFalse(ResourceReach.within(BOX, 41, 0), "onze além da borda não vale");
        assertTrue(ResourceReach.within(BOX, 0, -30));
        assertFalse(ResourceReach.within(BOX, 0, -31));
    }

    @Test
    void anEvenTurnStartsAtTheCentreAndCoversTheLimit() {
        ResourceReach.Search search = ResourceReach.search(BOX, CENTRE, 0);

        assertEquals(CENTRE, search.origin());
        assertEquals(40, search.radius(), "do centro à borda mais longe do limite");
    }

    @Test
    void oddTurnsStartAtTheEdgesClockwiseFromNorth() {
        assertEquals(new ColonyPos(0, 64, -30), ResourceReach.search(BOX, CENTRE, 1).origin());
        assertEquals(new ColonyPos(40, 64, 0), ResourceReach.search(BOX, CENTRE, 3).origin());
        assertEquals(new ColonyPos(0, 64, 30), ResourceReach.search(BOX, CENTRE, 5).origin());
        assertEquals(new ColonyPos(-40, 64, 0), ResourceReach.search(BOX, CENTRE, 7).origin());
        assertEquals(new ColonyPos(0, 64, -30), ResourceReach.search(BOX, CENTRE, 9).origin(),
                "a quinta borda é o norte de novo");
    }

    @Test
    void anEdgeSearchCoversHalfTheLimit() {
        // Limite de 81 x 61: metade do lado maior.
        assertEquals(40, ResourceReach.search(BOX, CENTRE, 1).radius());
    }

    @Test
    void centreAndEdgeAlternate() {
        assertEquals(CENTRE, ResourceReach.search(BOX, CENTRE, 2).origin());
        assertEquals(CENTRE, ResourceReach.search(BOX, CENTRE, 4).origin());
    }
}
