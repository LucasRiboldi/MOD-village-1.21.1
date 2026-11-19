package com.villagecolony.fabric.integration;

import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Regra 314: a casa do mod posta inteira (a BigHouseMOD e a casa do mineiro) ganha caminho rente ao chão na frente de cada
 * porta. No jogo da 0.4.91 a porta da BigHouseMOD tinha grama dos dois lados.
 */
public class DoorPathsGameTest implements FabricGameTest {

    /** Grama no piso ao lado da porta vira caminho; o chão de tábua da casa e o chão mais baixo ficam como estão. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_paths_flat", tickLimit = 5)
    public void theGroundBesideTheDoorBecomesAPath(TestContext context) {
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 5; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
                context.setBlockState(new BlockPos(x, 2, z), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        door(context, new BlockPos(3, 3, 3));
        context.setBlockState(new BlockPos(3, 2, 4), Blocks.OAK_PLANKS.getDefaultState());
        door(context, new BlockPos(6, 3, 3));
        context.setBlockState(new BlockPos(6, 2, 2), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(6, 1, 2), Blocks.GRASS_BLOCK.getDefaultState());

        DoorPaths.pave(context.getWorld(), BlockBox.create(context.getAbsolutePos(new BlockPos(1, 1, 1)),
                context.getAbsolutePos(new BlockPos(7, 5, 5))));

        context.expectBlock(Blocks.DIRT_PATH, new BlockPos(3, 2, 2));
        context.expectBlock(Blocks.OAK_PLANKS, new BlockPos(3, 2, 4));
        context.expectBlock(Blocks.GRASS_BLOCK, new BlockPos(6, 1, 2));
        context.expectBlock(Blocks.AIR, new BlockPos(6, 2, 2));
        context.complete();
    }

    /** A BigHouseMOD de verdade, posta sobre grama: cada porta tem caminho num dos lados, no piso dela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_paths_big_house", tickLimit = 5)
    public void everyDoorOfTheBigHouseGetsAPath(TestContext context) {
        ServerWorld world = context.getWorld();
        StructureTemplate template = world.getStructureTemplateManager()
                .getTemplate(MinecraftTypeAdapter.toIdentifier(StructureBlueprintReader.BIG_HOUSE_MOD)).orElseThrow();
        BlockPos origin = context.getAbsolutePos(BlockPos.ORIGIN).add(2_500_000, 20, 2_500_000);
        StructurePlacementData placement = new StructurePlacementData().setIgnoreEntities(true);
        BlockBox box = template.calculateBoundingBox(placement, origin);
        BlockBox around = box.expand(3);

        for (BlockPos at : BlockPos.iterate(around.getMinX(), origin.getY() - 2, around.getMinZ(),
                around.getMaxX(), origin.getY() - 1, around.getMaxZ())) {
            world.getChunk(at);
            world.setBlockState(at.toImmutable(), at.getY() == origin.getY() - 1
                    ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
        }

        template.place(world, origin, origin, placement, Random.create(0), Block.NOTIFY_LISTENERS);
        DoorPaths.pave(world, box);
        int doors = 0;
        int paved = 0;

        for (BlockPos at : BlockPos.iterate(box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ())) {
            BlockState state = world.getBlockState(at);

            if (state.getBlock() instanceof DoorBlock && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                doors++;
                Direction facing = state.get(DoorBlock.FACING);

                if (world.getBlockState(at.offset(facing).down()).isOf(Blocks.DIRT_PATH)
                        || world.getBlockState(at.offset(facing.getOpposite()).down()).isOf(Blocks.DIRT_PATH)) {
                    paved++;
                }
            }
        }

        try {
            context.assertTrue(doors > 0 && paved == doors,
                    "Regra 314: " + paved + " de " + doors + " portas da BigHouseMOD com caminho no piso");
        } finally {
            for (BlockPos at : BlockPos.iterate(around.getMinX(), origin.getY() - 2, around.getMinZ(),
                    around.getMaxX(), box.getMaxY() + 1, around.getMaxZ())) {
                world.setBlockState(at.toImmutable(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }

        context.complete();
    }

    private static void door(TestContext context, BlockPos at) {
        BlockState lower = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH);
        context.setBlockState(at, lower);
        context.setBlockState(at.up(), lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }
}
