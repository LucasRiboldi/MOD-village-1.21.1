package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public final class FoundationPreparationGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void halfSupportedStreetBaseIsFilledWithBiomeGround(TestContext context) {
        BlockPos origin = new BlockPos(5, 1, 5);
        context.setBlockState(origin, Blocks.GRASS_BLOCK);
        context.setBlockState(origin.east(), Blocks.GRASS_BLOCK);
        context.setBlockState(origin.south(), Blocks.AIR);
        context.setBlockState(origin.south().east(), Blocks.AIR);
        context.setBlockState(origin.south().down(), Blocks.DIRT);
        context.setBlockState(origin.south().east().down(), Blocks.DIRT);

        context.assertTrue(FoundationPreparation.prepareIfQualified(context.getWorld(), project(context, origin)),
                "metade da base na altura da rua deve permitir a preparação");
        context.assertTrue(context.getWorld().getBlockState(context.getAbsolutePos(origin.south())).isOf(Blocks.GRASS_BLOCK),
                "a lacuna deve receber o solo do bioma");
        context.assertTrue(context.getWorld().getBlockState(context.getAbsolutePos(origin.south().east())).isOf(Blocks.GRASS_BLOCK),
                "a lacuna deve receber o solo do bioma");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void lessThanHalfSupportDoesNotCreateAPlatform(TestContext context) {
        BlockPos origin = new BlockPos(5, 1, 5);
        context.setBlockState(origin, Blocks.GRASS_BLOCK);
        context.setBlockState(origin.east(), Blocks.AIR);
        context.setBlockState(origin.south(), Blocks.AIR);
        context.setBlockState(origin.south().east(), Blocks.AIR);
        context.setBlockState(origin.east().down(), Blocks.DIRT);
        context.setBlockState(origin.south().down(), Blocks.DIRT);
        context.setBlockState(origin.south().east().down(), Blocks.DIRT);

        context.assertTrue(!FoundationPreparation.prepareIfQualified(context.getWorld(), project(context, origin)),
                "menos de metade de apoio não pode criar uma plataforma");
        context.assertTrue(context.getWorld().getBlockState(context.getAbsolutePos(origin.east())).isAir(),
                "a lacuna deve permanecer vazia abaixo do limiar");
        context.complete();
    }

    private static ConstructionProject project(TestContext context, BlockPos relativeOrigin) {
        Blueprint blueprint = Blueprint.of(ResourceId.vanilla("foundation_preparation"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("cobblestone")),
                new BlueprintBlock(new ColonyPos(1, 0, 0), ResourceId.vanilla("cobblestone")),
                new BlueprintBlock(new ColonyPos(0, 0, 1), ResourceId.vanilla("cobblestone")),
                new BlueprintBlock(new ColonyPos(1, 0, 1), ResourceId.vanilla("cobblestone"))
        )).withStreetLayer(0);
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(relativeOrigin));
        return ConstructionProject.plan(UUID.randomUUID(), blueprint, origin);
    }
}
