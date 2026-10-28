package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** Regra 131: o anel em volta da obra fica na altura da rua ±1, e a rampa abre a faixa um por passo. */
public class SiteSurroundingsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "site_surroundings", tickLimit = 20)
    public void theRingFollowsTheRoadAndTheRampOpensOneAStep(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(BlockPos.ORIGIN).add(4_400_000, 60, 4_400_000);
        int roadY = origin.getY();
        BlockPos hill = origin.add(-1, 0, 0);
        BlockPos pit = origin.add(0, 0, -2);
        BlockPos rampHill = origin.add(-4, 0, 1);
        BlockPos path = origin.add(1, 0, 3);

        try {
            column(world, hill, roadY + 4, Blocks.DIRT);
            column(world, pit, roadY - 3, Blocks.DIRT);
            column(world, rampHill, roadY + 6, Blocks.STONE);
            column(world, path, roadY + 3, Blocks.DIRT_PATH);

            Blueprint house = Blueprint.of(ResourceId.vanilla("surroundings_house"), List.of(
                    new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("cobblestone")),
                    new BlueprintBlock(new ColonyPos(1, 0, 1), ResourceId.vanilla("cobblestone")))).withStreetLayer(0);
            ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(), house,
                    new ColonyPos(origin.getX(), origin.getY(), origin.getZ()));

            SiteSurroundings.level(world, project);

            context.assertTrue(topOf(world, hill, roadY) == roadY + 1, "o morro do anel devia descer a rua+1, ficou "
                    + (topOf(world, hill, roadY) - roadY));
            context.assertTrue(topOf(world, pit, roadY) == roadY - 1, "o buraco do anel devia subir a rua-1, ficou "
                    + (topOf(world, pit, roadY) - roadY));
            context.assertTrue(topOf(world, rampHill, roadY) == roadY + SiteSurroundings.bandAt(4),
                    "a rampa a 4 blocos devia deixar rua+" + SiteSurroundings.bandAt(4) + ", ficou "
                            + (topOf(world, rampHill, roadY) - roadY));
            context.assertTrue(world.getBlockState(path.up(3)).isOf(Blocks.DIRT_PATH), "o caminho não pode ser cortado");
        } finally {
            for (BlockPos at : BlockPos.iterate(origin.add(-6, -9, -6), origin.add(8, 9, 8))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }

    /** Uma coluna de chão até {@code top}, com ar em cima. */
    private static void column(ServerWorld world, BlockPos at, int top, Block ground) {
        world.getChunk(at);
        for (int y = at.getY() - 8; y <= at.getY() + 8; y++) {
            BlockPos here = new BlockPos(at.getX(), y, at.getZ());
            world.setBlockState(here, y < top ? Blocks.STONE.getDefaultState()
                    : y == top ? ground.getDefaultState() : Blocks.AIR.getDefaultState());
        }
    }

    private static int topOf(ServerWorld world, BlockPos at, int roadY) {
        for (int y = roadY + 8; y >= roadY - 8; y--) {
            if (!world.getBlockState(new BlockPos(at.getX(), y, at.getZ())).isAir()) {
                return y;
            }
        }

        return Integer.MIN_VALUE;
    }
}
