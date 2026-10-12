package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Relva alta e flores da planta surgem sozinhas, sem o construtor (Regra 55). */
public class GrowingPiecesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "growing_pieces", tickLimit = 20)
    public void tallGrassAndFlowersGrowWithoutTheBuilder(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos ground = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos origin = ground.up();
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(origin);
        Blueprint plan = Blueprint.of(new ResourceId("villagecolony", "growing_test"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("tall_grass"), false, Optional.empty(),
                        Map.of("half", "lower")),
                new BlueprintBlock(new ColonyPos(0, 1, 0), ResourceId.vanilla("tall_grass"), false, Optional.empty(),
                        Map.of("half", "upper")),
                new BlueprintBlock(new ColonyPos(2, 0, 0), ResourceId.vanilla("dandelion"))));
        ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(), plan, at);

        world.setBlockState(ground, Blocks.DIRT.getDefaultState());
        world.setBlockState(ground.east(2), Blocks.GRASS_BLOCK.getDefaultState());

        try {
            GrowingPieces.appear(world, project);
            GrowingPieces.appear(world, project);

            context.assertTrue(world.getBlockState(origin).isOf(Blocks.TALL_GRASS)
                    && world.getBlockState(origin.up()).isOf(Blocks.TALL_GRASS), "a relva alta não surgiu inteira");
            context.assertTrue(world.getBlockState(origin.east(2)).isOf(Blocks.DANDELION), "a flor não surgiu");
            context.assertTrue(project.isFinished(), "sobrou peça de grama na obra: " + project.remaining());
        } finally {
            for (BlockPos pos : BlockPos.iterate(ground, origin.add(2, 1, 0))) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }
}
