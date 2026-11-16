package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Map;

/**
 * Regra 303: a frente de cada porta da vila gerada fica no piso da porta, com um bloco de caminho — sem degrau de 2. A
 * porta é lida do molde da peça, na posição e no giro dela.
 */
public class DoorFrontsGameTest implements FabricGameTest {

    private static final String HOUSE = "minecraft:village/plains/houses/plains_small_house_1";

    /** As frentes lidas do molde encostam numa porta de verdade quando a peça é posta, em qualquer giro. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_read", tickLimit = 5)
    public void theDoorFrontsComeFromTheTemplate(TestContext context) {
        ServerWorld world = context.getWorld();
        StructureTemplateManager templates = world.getStructureTemplateManager();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_200_000, 10, 2_200_000);

        for (BlockRotation rotation : BlockRotation.values()) {
            BlockPos pos = base.add(rotation.ordinal() * 20, 0, 0);
            StructurePoolElement element = StructurePoolElement.ofSingle(HOUSE).apply(StructurePool.Projection.RIGID);
            BlockBox box = element.getBoundingBox(templates, pos, rotation);
            PoolStructurePiece piece = new PoolStructurePiece(templates, element, pos, 1, rotation, box,
                    StructureLiquidSettings.APPLY_WATERLOGGING);

            Map<Long, Integer> fronts = DoorFronts.of(piece, templates, world.getRegistryManager());

            context.assertFalse(fronts.isEmpty(), "Regra 303: nenhuma porta lida do molde no giro " + rotation);
            world.getChunk(pos);
            piece.generate(world, world.getStructureAccessor(), world.getChunkManager().getChunkGenerator(),
                    world.getRandom(), box.expand(2), new net.minecraft.util.math.ChunkPos(pos), pos);

            fronts.forEach((column, floor) -> {
                int x = com.villagecolony.core.construction.model.ColonyRoads.xOf(column);
                int z = com.villagecolony.core.construction.model.ColonyRoads.zOf(column);
                boolean beside = false;

                for (Direction side : Direction.Type.HORIZONTAL) {
                    var state = world.getBlockState(new BlockPos(x, floor + 1, z).offset(side));
                    beside |= state.getBlock() instanceof DoorBlock && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                }

                context.assertTrue(beside, "Regra 303: a frente " + x + "," + z + " (giro " + rotation
                        + ") não encosta em porta no piso " + floor);
            });

            for (BlockPos at : BlockPos.iterate(box.getMinX() - 2, box.getMinY() - 2, box.getMinZ() - 2,
                    box.getMaxX() + 2, box.getMaxY() + 2, box.getMaxZ() + 2)) {
                world.setBlockState(at.toImmutable(), Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
            }
        }

        context.complete();
    }

    /** O degrau de 2 e o morro de 3 na frente da porta viram o piso dela, com caminho no topo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_shape", tickLimit = 5)
    public void theDoorFrontIsAtTheFloorWithAPath(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_300_000, 20, 2_300_000);
        int floor = base.getY();

        for (int[] column : new int[][] {{0, -2}, {3, 3}}) {
            BlockPos at = base.add(column[0], 0, 0);
            var chunk = world.getChunk(at);

            for (int y = floor - 6; y <= floor + 6; y++) {
                world.setBlockState(new BlockPos(at.getX(), y, at.getZ()), y < floor + column[1]
                        ? Blocks.DIRT.getDefaultState() : y == floor + column[1]
                        ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState());
            }

            DoorFronts.shape(world, chunk, at.getX(), at.getZ(), floor + column[1], floor, VillageSoil.of(chunk));

            context.assertTrue(world.getBlockState(at.withY(floor)).isOf(Blocks.DIRT_PATH)
                            && world.getBlockState(at.withY(floor + 1)).isAir(),
                    "Regra 303: a frente da porta com topo " + column[1] + " não ficou no piso com caminho: "
                            + world.getBlockState(at.withY(floor)) + " / " + world.getBlockState(at.withY(floor + 1)));
        }

        context.complete();
    }
}
