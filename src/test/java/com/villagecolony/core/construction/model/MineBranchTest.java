package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** E2: o estado de cada ramal, para o relatório não contar quatro quando há um. */
class MineBranchTest {

    private static Mine opened() {
        return Mine.open(UUID.randomUUID(), MineShaft.from(new ColonyPos(100, 64, 200), Side.NORTH));
    }

    @Test
    void whileTheShaftIsSharedOnlyTheFirstBranchIsOpen() {
        Mine mine = opened();

        assertEquals(MineBranch.RESERVED, MineBranch.of(mine, 0, true));
        assertEquals(MineBranch.NOT_OPEN_YET, MineBranch.of(mine, 1, false));
        assertEquals(MineBranch.NOT_OPEN_YET, MineBranch.of(mine, 3, false));
    }

    @Test
    void theOtherBranchesOpenWhenTheGalleryStarts() {
        Mine mine = opened();

        for (int index = 0; index < MineShaft.SHARED_BLOCKS; index++) {
            mine.arm(0).nextPosition();
        }

        assertEquals(MineBranch.OPEN, MineBranch.of(mine, 1, false));
        assertEquals(MineBranch.RESERVED, MineBranch.of(mine, 2, true));
    }

    @Test
    void aFinishedBranchIsExhaustedWhoeverHeldIt() {
        Mine mine = opened();

        mine.arm(0).finish();

        assertEquals(MineBranch.EXHAUSTED, MineBranch.of(mine, 0, true));
    }
}
