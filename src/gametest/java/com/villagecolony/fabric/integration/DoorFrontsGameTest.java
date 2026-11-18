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

import java.util.List;
import java.util.Map;

/**
 * Regra 303: a frente de cada porta da vila gerada fica no piso da porta, com um bloco de caminho — sem degrau de 2. A
 * porta é lida do molde da peça, na posição e no giro dela.
 */
public class DoorFrontsGameTest implements FabricGameTest {

    private static final String HOUSE = "minecraft:village/plains/houses/plains_small_house_1";

    /**
     * As entradas lidas do molde (Regra 305), com a peça posta, em qualquer giro: nenhum bloco da estrutura do piso para
     * cima, e uma porta de verdade em linha reta.
     */
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
                boolean inLine = false;

                // A porta fica um acima da entrada, ou dois depois do degrau (Regra 313).
                for (int up = 1; up <= 2 && !inLine; up++) {
                    for (Direction side : Direction.Type.HORIZONTAL) {
                        for (int step = 1; step <= DoorFronts.MAX_WALK && !inLine; step++) {
                            var state = world.getBlockState(new BlockPos(x, floor + up, z).offset(side, step));
                            inLine = state.getBlock() instanceof DoorBlock && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                        }
                    }
                }

                context.assertTrue(inLine && world.getBlockState(new BlockPos(x, floor + 1, z)).isAir()
                                && world.getBlockState(new BlockPos(x, floor + 2, z)).isAir(),
                        "Regra 305: a entrada " + x + "," + z + " (giro " + rotation + ") tem bloco acima do piso " + floor
                                + " ou não vê porta em linha reta");
            });

            for (BlockPos at : BlockPos.iterate(box.getMinX() - 2, box.getMinY() - 2, box.getMinZ() - 2,
                    box.getMaxX() + 2, box.getMaxY() + 2, box.getMaxZ() + 2)) {
                world.setBlockState(at.toImmutable(), Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
            }
        }

        context.complete();
    }

    /** O degrau de 2 e o morro de 3 na frente da porta viram a altura da entrada, com o solo no topo (Regra 313). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_shape", tickLimit = 5)
    public void theDoorFrontIsAtItsLevelWithSoil(TestContext context) {
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

            context.assertTrue(!world.getBlockState(at.withY(floor)).isOf(Blocks.DIRT_PATH)
                            && SiteSurroundings.isNaturalGround(world.getBlockState(at.withY(floor)))
                            && world.getBlockState(at.withY(floor + 1)).isAir(),
                    "Regra 313: a frente da porta com topo " + column[1] + " não ficou na altura com o solo: "
                            + world.getBlockState(at.withY(floor)) + " / " + world.getBlockState(at.withY(floor + 1)));
        }

        context.complete();
    }

    /** Casas da planície que a vila gera, para medir o degrau da porta. */
    private static final List<String> HOUSES = List.of(
            "plains_small_house_1", "plains_small_house_2", "plains_small_house_3", "plains_small_house_4",
            "plains_small_house_5", "plains_small_house_6", "plains_small_house_7", "plains_small_house_8",
            "plains_medium_house_1", "plains_medium_house_2", "plains_butcher_shop_1", "plains_library_1",
            "plains_tannery_1", "plains_fisher_cottage_1");

    /**
     * Regra 313: depois do degrau do molde (escada ou laje no piso da porta), a entrada fica um abaixo do piso, no chão; sem
     * degrau, no piso. No jogo da 0.4.91 a entrada ficava no piso depois da escada, um bloco acima do chão em volta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_fronts_step", tickLimit = 5)
    public void afterTheStepTheEntryIsOneBelowTheFloor(TestContext context) {
        ServerWorld world = context.getWorld();
        StructureTemplateManager templates = world.getStructureTemplateManager();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_400_000, 10, 2_400_000);
        int steps = 0;

        for (int i = 0; i < HOUSES.size(); i++) {
            BlockPos pos = base.add(i * 24, 0, 0);
            StructurePoolElement element = StructurePoolElement.ofSingle("minecraft:village/plains/houses/" + HOUSES.get(i))
                    .apply(StructurePool.Projection.RIGID);
            BlockBox box = element.getBoundingBox(templates, pos, BlockRotation.NONE);
            PoolStructurePiece piece = new PoolStructurePiece(templates, element, pos, 1, BlockRotation.NONE, box,
                    StructureLiquidSettings.APPLY_WATERLOGGING);
            Map<Long, Integer> fronts = DoorFronts.of(piece, templates, world.getRegistryManager());

            world.getChunk(pos);
            piece.generate(world, world.getStructureAccessor(), world.getChunkManager().getChunkGenerator(),
                    world.getRandom(), box.expand(2), new net.minecraft.util.math.ChunkPos(pos), pos);

            for (var front : fronts.entrySet()) {
                BlockPos entry = new BlockPos(com.villagecolony.core.construction.model.ColonyRoads.xOf(front.getKey()),
                        front.getValue(), com.villagecolony.core.construction.model.ColonyRoads.zOf(front.getKey()));
                BlockPos door = null;
                Direction toDoor = null;

                for (int up = 1; up <= 2 && door == null; up++) {
                    for (Direction side : Direction.Type.HORIZONTAL) {
                        for (int step = 1; step <= DoorFronts.MAX_WALK && door == null; step++) {
                            BlockPos at = entry.up(up).offset(side, step);
                            var state = world.getBlockState(at);

                            if (state.getBlock() instanceof DoorBlock && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                                door = at;
                                toDoor = side;
                            }
                        }
                    }
                }

                context.assertTrue(door != null, "Regra 313: a entrada " + entry.toShortString() + " de " + HOUSES.get(i)
                        + " não vê porta em linha reta");
                int floor = door.getY() - 1;
                var before = world.getBlockState(entry.offset(toDoor).withY(floor));
                boolean step = before.getBlock() instanceof net.minecraft.block.StairsBlock
                        || before.getBlock() instanceof net.minecraft.block.SlabBlock;

                steps += step ? 1 : 0;
                context.assertTrue(front.getValue() == (step ? floor - 1 : floor),
                        "Regra 313: em " + HOUSES.get(i) + ", a entrada " + entry.toShortString() + " ficou em "
                                + front.getValue() + " com piso " + floor + (step ? " depois do degrau " : " sem degrau ")
                                + before);
            }

            for (BlockPos at : BlockPos.iterate(box.getMinX() - 2, box.getMinY() - 2, box.getMinZ() - 2,
                    box.getMaxX() + 2, box.getMaxY() + 2, box.getMaxZ() + 2)) {
                world.setBlockState(at.toImmutable(), Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
            }
        }

        context.assertTrue(steps > 0, "Regra 313: nenhuma das casas medidas tinha degrau na porta");
        context.complete();
    }
}
