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

        Optional<BlockPos> found = SurfaceHits.near(colony, ResourceType.SAND, BlockPos.ORIGIN, column -> true,
                column -> column.getX() == 11 && column.getZ() == 20 ? Optional.of(sand) : Optional.empty());

        assertEquals(Optional.of(sand), found);
    }

    @Test
    void aColumnThatGivesNothingIsForgotten() {
        UUID colony = UUID.randomUUID();
        int[] probes = {0};

        SurfaceHits.remember(colony, ResourceType.CLAY_BALL, new BlockPos(0, 60, 0));

        assertTrue(SurfaceHits.near(colony, ResourceType.CLAY_BALL, BlockPos.ORIGIN, column -> true,
                column -> { probes[0]++; return Optional.empty(); }).isEmpty());
        assertTrue(SurfaceHits.near(colony, ResourceType.CLAY_BALL, BlockPos.ORIGIN, column -> true,
                column -> { probes[0]++; return Optional.empty(); }).isEmpty());
        int patch = (2 * SurfaceHits.PATCH_RADIUS + 1) * (2 * SurfaceHits.PATCH_RADIUS + 1);
        assertEquals(patch, probes[0], "a coluna vazia foi perguntada de novo");
    }

    /** A mancha em volta da coluna lembrada, não só a vizinha — regra do autor, 2026-10-11. */
    @Test
    void theSandThreeBlocksFromARememberedColumnIsFound() {
        UUID colony = UUID.randomUUID();
        BlockPos sand = new BlockPos(13, 62, 23);

        SurfaceHits.remember(colony, ResourceType.SAND, new BlockPos(10, 64, 20));

        Optional<BlockPos> found = SurfaceHits.near(colony, ResourceType.SAND, BlockPos.ORIGIN, column -> true,
                column -> column.getX() == 13 && column.getZ() == 23 ? Optional.of(sand) : Optional.empty());

        assertEquals(Optional.of(sand), found);
    }

    /** A mancha mais perto da vila vem antes da lembrada por último. */
    @Test
    void theRememberedColumnNearestTheVillageIsTriedFirst() {
        UUID colony = UUID.randomUUID();
        BlockPos near = new BlockPos(5, 62, 0);
        BlockPos far = new BlockPos(90, 62, 0);

        SurfaceHits.remember(colony, ResourceType.SAND, near);
        SurfaceHits.remember(colony, ResourceType.SAND, far);

        Optional<BlockPos> found = SurfaceHits.near(colony, ResourceType.SAND, BlockPos.ORIGIN, column -> true,
                column -> column.getX() == 5 || column.getX() == 90
                        ? Optional.of(new BlockPos(column.getX(), 62, column.getZ())) : Optional.empty());

        assertEquals(Optional.of(near), found);
    }
}
