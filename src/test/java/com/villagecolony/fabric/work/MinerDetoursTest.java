package com.villagecolony.fabric.work;

import com.villagecolony.fabric.brain.WorkTargets;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinerDetoursTest {

    @AfterEach
    void clearTargets() {
        WorkTargets.clearAll();
    }

    @Test
    void aLocalDetourAimsAtTheCurrentRouteLegInsteadOfTheDistantStone() {
        UUID worker = UUID.randomUUID();
        BlockPos routeLeg = new BlockPos(8, 64, 0);
        BlockPos distantStone = new BlockPos(96, 64, 0);
        WorkTargets.set(worker, routeLeg, MinerReach.ARRIVAL);

        assertEquals(routeLeg, MinerDetours.aimFor(worker, distantStone));
    }
}
