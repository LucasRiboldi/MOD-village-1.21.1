package com.villagecolony.fabric.integration;

import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarthestVillageSectorTest {

    private static final UUID COLONY = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void choosesTheCardinalSectorFarthestFromVillageStructures() {
        var village = List.of(new FarthestVillageSector.Footprint(-8, 40, 8, 58));

        assertEquals(Direction.NORTH, FarthestVillageSector.choose(0, 0, village, COLONY));
    }

    @Test
    void choosesTheSectorWithTheLargestClearanceWhenStructuresAreUneven() {
        var structures = List.of(
                new FarthestVillageSector.Footprint(-8, -58, 8, -42));

        assertEquals(Direction.SOUTH, FarthestVillageSector.choose(0, 0, structures, COLONY));
    }

    @Test
    void usesStableColonyFallbackWhenNoVillagePiecesAreKnown() {
        assertEquals(Direction.EAST, FarthestVillageSector.choose(0, 0, List.of(), COLONY));
    }

    @Test
    void grassSectorExcludesTheProtectedRadiusAndOtherDirections() {
        var center = new net.minecraft.util.math.BlockPos(0, 64, 0);

        assertFalse(FarthestVillageSector.isInSector(
                center, new net.minecraft.util.math.BlockPos(64, 64, 0), Direction.EAST));
        assertTrue(FarthestVillageSector.isInSector(
                center, new net.minecraft.util.math.BlockPos(65, 64, 0), Direction.EAST));
        assertFalse(FarthestVillageSector.isInSector(
                center, new net.minecraft.util.math.BlockPos(-65, 64, 0), Direction.EAST));
    }
}
