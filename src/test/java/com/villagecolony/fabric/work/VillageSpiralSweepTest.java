package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.VillageBounds;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageSpiralSweepTest {

    @AfterEach
    void clearCursors() {
        VillageSpiralSweep.clearAll();
    }

    @Test
    void visitsTheVillageBorderInwardThenExpandsOutside() {
        UUID owner = UUID.randomUUID();
        VillageBounds bounds = new VillageBounds(0, 0, 0, 2, 4, 2);
        List<BlockPos> visited = new ArrayList<>();

        VillageSpiralSweep.next(owner, bounds, 7, 1, column -> true, column -> {
            visited.add(column);
            return Optional.empty();
        });

        assertEquals(List.of(
                new BlockPos(0, 7, 0), new BlockPos(1, 7, 0), new BlockPos(2, 7, 0),
                new BlockPos(2, 7, 1), new BlockPos(2, 7, 2), new BlockPos(1, 7, 2),
                new BlockPos(0, 7, 2), new BlockPos(0, 7, 1), new BlockPos(1, 7, 1),
                new BlockPos(-1, 7, -1)),
                visited.subList(0, 10));
    }

    @Test
    void resumesWithoutRepeatingColumnsAndCanFindTheFirstOuterRing() {
        UUID owner = UUID.randomUUID();
        VillageBounds bounds = new VillageBounds(0, 0, 0, 34, 4, 34);
        BlockPos target = new BlockPos(-1, 7, -1);
        Set<BlockPos> unique = new HashSet<>();

        Optional<BlockPos> first = VillageSpiralSweep.next(
                owner, bounds, 7, 1, column -> true, column -> {
                    assertTrue(unique.add(column), "a coluna foi visitada mais de uma vez: " + column);
                    return Optional.empty();
                });

        assertTrue(first.isEmpty());
        assertEquals(VillageSpiralSweep.MAX_COLUMNS, unique.size());
        assertTrue(VillageSpiralSweep.pausedAt(owner).isPresent());

        Optional<BlockPos> found = VillageSpiralSweep.next(
                owner, bounds, 7, 1, column -> true, column -> {
                    assertTrue(unique.add(column), "a retomada repetiu a coluna: " + column);
                    return column.equals(target) ? Optional.of(column) : Optional.empty();
                });

        assertEquals(Optional.of(target), found);
        assertEquals(35 * 35 + 1, unique.size());
        assertTrue(VillageSpiralSweep.pausedAt(owner).isEmpty());
    }

    @Test
    void changedBoundsRestartTheCursorAtTheNewBorder() {
        UUID owner = UUID.randomUUID();
        VillageBounds oldBounds = new VillageBounds(0, 0, 0, 34, 4, 34);
        VillageBounds grown = new VillageBounds(-5, 0, -6, 34, 4, 34);

        VillageSpiralSweep.next(
                owner, oldBounds, 7, 0, column -> true, column -> Optional.empty());

        BlockPos newCorner = new BlockPos(-5, 7, -6);
        Optional<BlockPos> found = VillageSpiralSweep.next(
                owner, grown, 7, 0, column -> true,
                column -> column.equals(newCorner) ? Optional.of(column) : Optional.empty());

        assertEquals(Optional.of(newCorner), found);
    }
}
