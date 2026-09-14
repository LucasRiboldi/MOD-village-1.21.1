package com.villagecolony.fabric.integration;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Escolhe o setor de superfície com maior afastamento das estruturas da vila. */
public final class FarthestVillageSector {

    public static final int PROTECTED_RADIUS = 64;
    private static final List<Direction> CARDINALS =
            List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    public record Footprint(int minX, int minZ, int maxX, int maxZ) {
    }

    private FarthestVillageSector() {
    }

    public static Direction choose(int centerX, int centerZ, List<Footprint> structures, UUID colonyId) {
        if (structures.isEmpty()) {
            return CARDINALS.get(Math.floorMod(colonyId.hashCode(), CARDINALS.size()));
        }

        return CARDINALS.stream()
                .max(Comparator.comparingLong(direction -> clearance(centerX, centerZ, direction, structures)))
                .orElse(Direction.NORTH);
    }

    public static Direction farthestLoadedSector(ServerWorld world, BlockPos center, UUID colonyId) {
        List<Footprint> structures = new ArrayList<>();
        int minChunkX = (center.getX() - PROTECTED_RADIUS) >> 4;
        int maxChunkX = (center.getX() + PROTECTED_RADIUS) >> 4;
        int minChunkZ = (center.getZ() - PROTECTED_RADIUS) >> 4;
        int maxChunkZ = (center.getZ() + PROTECTED_RADIUS) >> 4;
        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);

                if (chunk == null) {
                    continue;
                }

                for (StructureStart start : chunk.getStructureStarts().values()) {
                    if (!start.hasChildren()
                            || !registry.getEntry(start.getStructure()).isIn(StructureTags.VILLAGE)) {
                        continue;
                    }

                    start.getChildren().stream()
                            .map(piece -> piece.getBoundingBox())
                            .map(FarthestVillageSector::footprint)
                            .forEach(structures::add);
                }
            }
        }

        return choose(center.getX(), center.getZ(), structures, colonyId);
    }

    public static boolean isInSector(BlockPos center, BlockPos candidate, Direction direction) {
        int dx = candidate.getX() - center.getX();
        int dz = candidate.getZ() - center.getZ();
        int forward = dx * direction.getOffsetX() + dz * direction.getOffsetZ();
        int lateral = dx * direction.getOffsetZ() - dz * direction.getOffsetX();
        long distanceSquared = (long) dx * dx + (long) dz * dz;

        return forward > 0
                && Math.abs(lateral) <= forward
                && distanceSquared > (long) PROTECTED_RADIUS * PROTECTED_RADIUS;
    }

    private static long clearance(int centerX, int centerZ, Direction direction, List<Footprint> structures) {
        int x = centerX + direction.getOffsetX() * PROTECTED_RADIUS;
        int z = centerZ + direction.getOffsetZ() * PROTECTED_RADIUS;
        long nearest = Long.MAX_VALUE;

        for (Footprint box : structures) {
            int dx = x < box.minX() ? box.minX() - x : Math.max(0, x - box.maxX());
            int dz = z < box.minZ() ? box.minZ() - z : Math.max(0, z - box.maxZ());
            nearest = Math.min(nearest, (long) dx * dx + (long) dz * dz);
        }

        return nearest;
    }

    private static Footprint footprint(BlockBox box) {
        return new Footprint(box.getMinX(), box.getMinZ(), box.getMaxX(), box.getMaxZ());
    }
}
