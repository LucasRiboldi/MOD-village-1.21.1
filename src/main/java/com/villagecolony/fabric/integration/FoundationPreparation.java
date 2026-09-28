package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Prepara depressões rasas de uma obra já alinhada à rua. */
public final class FoundationPreparation {
    private static final int MIN_SUPPORTED_PERCENT = 50;

    private FoundationPreparation() {
    }

    public static boolean prepareIfQualified(ServerWorld world, ConstructionProject project) {
        Blueprint blueprint = project.blueprint();
        if (!blueprint.hasStreetLayer()) return true;
        int roadY = project.origin().y() + blueprint.streetLayer();
        int width = blueprint.size().x();
        int depth = blueprint.size().z();
        int supported = 0;
        List<BlockPos> gaps = new ArrayList<>();
        for (int dx = 0; dx < width; dx++) for (int dz = 0; dz < depth; dz++) {
            BlockPos base = new BlockPos(project.origin().x() + dx, roadY, project.origin().z() + dz);
            if (!world.getBlockState(base).getCollisionShape(world, base).isEmpty()) supported++;
            else if (isFillableBaseGap(world, base)) gaps.add(base);
            else return false;
        }
        if (supported * 100 < width * depth * MIN_SUPPORTED_PERCENT) return false;
        for (BlockPos gap : gaps) world.setBlockState(gap, VillageBiomes.foundationGroundAt(world, gap).getDefaultState());
        return true;
    }

    static boolean isFillableBaseGap(ServerWorld world, BlockPos base) {
        if (!world.isInBuildLimit(base)) return false;
        BlockState state = world.getBlockState(base);
        return LotGround.isNothing(state) && state.getFluidState().isEmpty()
                && world.isInBuildLimit(base.down()) && LotGround.isLotGround(world, base.down());
    }
}
