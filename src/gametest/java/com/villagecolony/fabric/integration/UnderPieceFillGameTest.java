package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
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

/**
 * Regra 304: casa da vila não fica sem solo — na geração, a coluna sob a peça é aterrada até o piso também em chão seco;
 * no mundo já gerado, o pedreiro ocioso preenche o vão sob a camada mais baixa da peça com o chão do bioma.
 */
public class UnderPieceFillGameTest implements FabricGameTest {

    private static final String HOUSE = "minecraft:village/plains/houses/plains_small_house_1";

    /** Na geração, o chão seco 3 abaixo do piso da peça sobe até o piso. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "under_piece_generation", tickLimit = 5)
    public void theGenerationFillsTheDryGroundUnderAPiece(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos at = context.getAbsolutePos(BlockPos.ORIGIN).add(2_400_000, 20, 2_400_000);
        int floor = at.getY();
        var chunk = world.getChunk(at);

        for (int y = floor - 8; y <= floor + 4; y++) {
            world.setBlockState(at.withY(y), y <= floor - 3 ? Blocks.DIRT.getDefaultState() : Blocks.AIR.getDefaultState());
        }

        VillageGround.found(chunk, at.getX(), at.getZ(), floor - 3, floor, VillageSoil.of(chunk));

        for (int y = floor - 2; y <= floor; y++) {
            context.assertFalse(world.getBlockState(at.withY(y)).isAir(),
                    "Regra 304: a coluna sob a peça ficou com ar em y " + (y - floor) + " do piso");
        }

        context.complete();
    }

    /** No mundo, o vão cavado sob a casa posta é preenchido; a coluna fora da casa não muda. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "under_piece_mason", tickLimit = 5)
    public void theGapUnderAStandingHouseIsFilled(TestContext context) {
        ServerWorld world = context.getWorld();
        StructureTemplateManager templates = world.getStructureTemplateManager();
        BlockPos pos = context.getAbsolutePos(BlockPos.ORIGIN).add(2_500_000, 20, 2_500_000);
        StructurePoolElement element = StructurePoolElement.ofSingle(HOUSE).apply(StructurePool.Projection.RIGID);
        BlockBox box = element.getBoundingBox(templates, pos, BlockRotation.NONE);
        PoolStructurePiece piece = new PoolStructurePiece(templates, element, pos, 1, BlockRotation.NONE, box,
                StructureLiquidSettings.APPLY_WATERLOGGING);
        int floor = box.getMinY() + piece.getGroundLevelDelta() - 1;

        world.getChunk(pos);

        for (BlockPos at : BlockPos.iterate(box.getMinX() - 2, floor - 6, box.getMinZ() - 2,
                box.getMaxX() + 2, floor - 4, box.getMaxZ() + 2)) {
            world.setBlockState(at.toImmutable(), Blocks.DIRT.getDefaultState());
        }

        piece.generate(world, world.getStructureAccessor(), world.getChunkManager().getChunkGenerator(),
                world.getRandom(), box.expand(2), new net.minecraft.util.math.ChunkPos(pos), pos);

        BlockPos under = new BlockPos(box.getMinX() + 1, floor - 1, box.getMinZ() + 1);
        BlockPos outside = new BlockPos(box.getMaxX() + 2, floor - 1, box.getMaxZ() + 2);

        try {
            context.assertTrue(world.getBlockState(under).isAir() && !world.getBlockState(under.up()).isAir(),
                    "a casa posta não ficou sobre um vão: o teste não mede nada");

            int filled = UnderPieceFill.fill(world, piece, 512);

            context.assertTrue(filled > 0 && !world.getBlockState(under).isAir() && !world.getBlockState(under.down()).isAir(),
                    "Regra 304: o vão sob a casa não foi preenchido (" + filled + " blocos)");
            context.assertTrue(world.getBlockState(outside).isAir(), "o vão fora da casa foi preenchido");
        } finally {
            for (BlockPos at : BlockPos.iterate(box.getMinX() - 2, floor - 6, box.getMinZ() - 2,
                    box.getMaxX() + 2, box.getMaxY() + 2, box.getMaxZ() + 2)) {
                world.setBlockState(at.toImmutable(), Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
            }
        }

        context.complete();
    }
}
