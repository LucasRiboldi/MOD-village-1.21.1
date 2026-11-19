package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.RoadRegions;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Set;
import java.util.UUID;

/** Regra 319 (ADR-057, modo 2): o bloco mudado que importa ao grafo marca a região; a planta que cresce não marca. */
public class RoadRegionEventsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_region_events", tickLimit = 5)
    public void aPathPlacedInTheVillageMarksItsRegionAndAGrowingCropDoesNot(TestContext context) {
        BlockPos path = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos crop = context.getAbsolutePos(new BlockPos(4, 2, 4));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(path));
        colony.restoreBounds(new VillageBounds(path.getX() - 8, path.getY() - 8, path.getZ() - 8,
                path.getX() + 8, path.getY() + 8, path.getZ() + 8));
        VillageColonyMod.COLONIES.register(colony);

        try {
            context.setBlockState(new BlockPos(4, 1, 4), Blocks.FARMLAND.getDefaultState());
            context.setBlockState(new BlockPos(4, 2, 4), Blocks.WHEAT.getDefaultState());
            RoadRegionEvents.drain(colony.id());

            context.getWorld().setBlockState(crop, Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, 3));
            context.assertTrue(RoadRegionEvents.drain(colony.id()).isEmpty(), "ADR-057: o trigo que cresce marcou a região");

            context.getWorld().setBlockState(path, Blocks.DIRT_PATH.getDefaultState());
            context.assertTrue(RoadRegionEvents.drain(colony.id()).equals(Set.of(RoadRegions.regionOf(path.getX(), path.getZ()))),
                    "ADR-057: o caminho posto na vila não marcou a região dele");
        } finally {
            VillageColonyMod.COLONIES.remove(colony.id());
            RoadRegionEvents.drain(colony.id());
        }

        context.complete();
    }
}
