package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 275: "saiu do curral" é pisar fora das colunas que ele alcançava de dentro.
 */
class PenEscapeOutTest {

    private static final Set<BlockPos> REACH = Set.of(new BlockPos(961, 64, 212), new BlockPos(961, 64, 214));

    @Test
    void aJumpInPlaceIsStillInside() {
        assertTrue(FencedIn.isOverTheReach(REACH, new BlockPos(961, 65, 212)),
                "um bloco acima da soleira contou como fora do curral");
    }

    @Test
    void theSameBlockIsInside() {
        assertTrue(FencedIn.isOverTheReach(REACH, new BlockPos(961, 64, 214)));
    }

    @Test
    void anotherColumnIsOutside() {
        assertFalse(FencedIn.isOverTheReach(REACH, new BlockPos(963, 63, 212)));
    }
}
