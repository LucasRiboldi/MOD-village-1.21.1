package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A cama atribuída e os limites da moradia conhecida pela colônia. */
public final class Homes {
    private Homes() {
    }

    public static Optional<BlockPos> assignedBed(ServerWorld world, VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .filter(home -> home.dimension().equals(world.getRegistryKey()))
                .map(home -> home.pos());
    }

    public static Optional<BlockBox> containing(ServerWorld world, UUID colony, BlockPos bed) {
        if (!world.isChunkLoaded(bed) || !(world.getBlockState(bed).getBlock() instanceof BedBlock)) {
            return Optional.empty();
        }

        return VillageColonyMod.BUILDINGS.ofColony(colony).stream()
                .filter(building -> building.finished() && building.contains(MinecraftTypeAdapter.toColonyPos(bed)))
                .findFirst()
                .map(building -> new BlockBox(building.min().x(), building.min().y(), building.min().z(),
                        building.max().x(), building.max().y(), building.max().z()))
                .or(() -> VanillaBedChests.originalVillagePiece(world, bed))
                .filter(house -> isLoaded(world, house));
    }

    static List<Direction> centerFaces(BlockBox house, BlockPos from) {
        double dx = (house.getMinX() + house.getMaxX()) / 2.0 - from.getX();
        double dz = (house.getMinZ() + house.getMaxZ()) / 2.0 - from.getZ();
        var faces = new ArrayList<Direction>(2);
        for (Direction side : Direction.Type.HORIZONTAL) {
            if (projection(side, dx, dz) > 0 && house.contains(from.offset(side))) {
                faces.add(side);
            }
        }
        faces.sort(Comparator.comparingDouble(side -> -projection(side, dx, dz)));
        return faces;
    }

    private static double projection(Direction side, double dx, double dz) {
        return dx * side.getOffsetX() + dz * side.getOffsetZ();
    }

    static boolean isLoaded(ServerWorld world, BlockBox box) {
        for (int x = (box.getMinX() - 1) >> 4; x <= (box.getMaxX() + 1) >> 4; x++) {
            for (int z = (box.getMinZ() - 1) >> 4; z <= (box.getMaxZ() + 1) >> 4; z++) {
                if (!world.getChunkManager().isChunkLoaded(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }
}
