package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.ResourceReach;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O6 (autor, 2026-10-28): o fundidor busca areia e argila mais longe da vila do que a busca comum. */
class SurfaceReachTest {

    @Test
    void sandAndClayAreSoughtFartherThanTheUsualMargin() {
        for (ResourceType raw : new ResourceType[] {ResourceType.SAND, ResourceType.CLAY,
                ResourceType.CLAY_BALL}) {
            assertTrue(SurfaceReach.marginFor(raw) >= 2 * ResourceReach.WIDE_MARGIN,
                    raw + " devia ir a pelo menos " + 2 * ResourceReach.WIDE_MARGIN + " além da borda");
        }
    }

    @Test
    void otherSurfaceGoodsKeepTheUsualMargin() {
        assertEquals(ResourceReach.EDGE_MARGIN, SurfaceReach.marginFor(ResourceType.CACTUS));
    }
}
