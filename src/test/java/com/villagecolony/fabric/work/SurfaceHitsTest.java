package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceType;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A coleta começa onde já achou o recurso (ADR-038 P7). */
class SurfaceHitsTest {

    @AfterEach
    void forget() {
        SurfaceHits.clearAll();
    }

    @Test
    void aNeighbourOfARememberedColumnIsTriedFirst() {
        UUID colony = UUID.randomUUID();
        BlockPos sand = new BlockPos(11, 63, 20);

        SurfaceHits.remember(colony, ResourceType.SAND, new BlockPos(10, 64, 20));

        Optional<BlockPos> found = SurfaceHits.near(colony, ResourceType.SAND, column -> true,
                column -> column.getX() == 11 && column.getZ() == 20 ? Optional.of(sand) : Optional.empty());

        assertEquals(Optional.of(sand), found);
    }

    @Test
    void aColumnThatGivesNothingIsForgotten() {
        UUID colony = UUID.randomUUID();
        int[] probes = {0};

        SurfaceHits.remember(colony, ResourceType.CLAY_BALL, new BlockPos(0, 60, 0));

        assertTrue(SurfaceHits.near(colony, ResourceType.CLAY_BALL, column -> true,
                column -> { probes[0]++; return Optional.empty(); }).isEmpty());
        assertTrue(SurfaceHits.near(colony, ResourceType.CLAY_BALL, column -> true,
                column -> { probes[0]++; return Optional.empty(); }).isEmpty());
        assertEquals(9, probes[0], "a coluna vazia foi perguntada de novo");
    }
}
