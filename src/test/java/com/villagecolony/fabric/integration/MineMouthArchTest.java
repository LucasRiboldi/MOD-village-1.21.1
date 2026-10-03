package com.villagecolony.fabric.integration;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O arco da mina: 5 de largura, 4 de altura, vão de 3 × 3, um lampião de cada lado — autor, 2026-10-03. */
class MineMouthArchTest {

    private static final BlockPos MOUTH = new BlockPos(0, 64, 0);

    @Test
    void theArchIsFiveWideFourHighWithAThreeByThreePassage() {
        for (Direction descent : Direction.Type.HORIZONTAL) {
            List<BlockPos> stones = MineMouth.archStones(MOUTH, descent);
            Direction lanes = descent.rotateYCounterclockwise();

            assertEquals(3 + 3 + 5, stones.size(), "dois pilares de três e a verga de cinco");

            for (int lane = 0; lane < MineMouth.PASSAGE; lane++) {
                for (int up = 1; up <= 3; up++) {
                    BlockPos inside = MOUTH.offset(lanes, lane).up(up);

                    assertFalse(stones.contains(inside), "o vão de 3 × 3 está livre em " + inside + " (" + descent + ")");
                }
            }

            int lowest = stones.stream().mapToInt(BlockPos::getY).min().orElseThrow();
            int highest = stones.stream().mapToInt(BlockPos::getY).max().orElseThrow();

            assertEquals(4, highest - lowest + 1, "quatro de altura");
        }
    }

    @Test
    void aLanternStandsOnTopOfEachSide() {
        List<BlockPos> stones = MineMouth.archStones(MOUTH, Direction.NORTH);
        List<BlockPos> lamps = MineMouth.lampSpots(MOUTH, Direction.NORTH);

        assertEquals(2, lamps.size());

        for (BlockPos lamp : lamps) {
            assertTrue(stones.contains(lamp.down()), "o lampião fica sobre pedra do arco: " + lamp);
            assertTrue(MineMouth.isPortalBlock(MOUTH, Direction.NORTH, lamp), "o lampião é peça do portal");
        }
    }
}
