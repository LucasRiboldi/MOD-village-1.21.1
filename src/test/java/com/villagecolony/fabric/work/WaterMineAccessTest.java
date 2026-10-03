package com.villagecolony.fabric.work;

import com.villagecolony.core.type.Side;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterMineAccessTest {

    @Test
    void aRouteHasThreeStairsThreeClearLayersAndASealedGlassShell() {
        WaterMineAccess.Route route = new WaterMineAccess.Route(
                new BlockPos(20, 70, 30), Side.NORTH, 4);

        assertEquals(new BlockPos(20, 66, 26), route.entry());
        assertEquals(12, route.stairs().size());
        assertTrue(route.stairs().contains(new BlockPos(20, 69, 29)));
        assertTrue(route.stairs().contains(new BlockPos(18, 69, 29)));
        assertTrue(route.interior().contains(new BlockPos(19, 70, 29)));
        assertTrue(route.interior().contains(new BlockPos(19, 71, 29)));
        assertTrue(route.shell().contains(new BlockPos(17, 69, 29)));
        assertTrue(route.shell().contains(new BlockPos(21, 72, 29)));
        assertTrue(route.contains(new BlockPos(19, 69, 29)));
        assertTrue(route.contains(new BlockPos(17, 69, 29)));
        assertFalse(route.contains(new BlockPos(16, 69, 29)));
    }
}
