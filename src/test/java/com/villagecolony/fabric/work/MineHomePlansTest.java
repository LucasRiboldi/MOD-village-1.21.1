package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MineHomePlansTest {
    @Test
    void recognizesOnlyAHouseNearTheMineMouth() {
        Building near = new Building(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), new ResourceId("villagecolony", "houses/cabin"),
                new ColonyPos(18, 64, 0), new ColonyPos(24, 70, 6), true);
        Building far = new Building(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), new ResourceId("villagecolony", "houses/cabin"),
                new ColonyPos(40, 64, 0), new ColonyPos(46, 70, 6), true);

        assertTrue(MineHomePlans.near(new BlockPos(0, 64, 0), near));
        assertFalse(MineHomePlans.near(new BlockPos(0, 64, 0), far));
    }
}
