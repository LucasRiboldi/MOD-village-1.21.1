package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Regra 370 (autor 2026-11-26, H5 do jogo da 0.4.143: "todos baú estarem encostados em uma parede e ter acesso de
 * trânsito dentro da casa"): o baú posto ao lado da cama nunca tira de quem entra pela porta o acesso a uma cama.
 */
public class ChestPassageGameTest implements FabricGameTest {

    /**
     * Cômodo de 2 × 4 (x 1..2, z 1..4), porta ao norte, cama encostada na parede oeste com a cabeceira ao sul e uma
     * pedra na frente do pé. O primeiro lugar com parede ao lado da cama, (2, 1, 3), deixaria a cama sem lado nenhum
     * alcançável da porta: o baú vai para o canto, (2, 1, 4), e a cama segue alcançável por (2, 1, 3).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_passage", tickLimit = 20)
    public void aBedChestNeverSplitsTheRoom(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 1, 1));
        BlockBox box = new BlockBox(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 3, origin.getY() + 3,
                origin.getZ() + 6);

        try {
            for (BlockPos at : BlockPos.iterate(origin, origin.add(3, 3, 6))) {
                int x = at.getX() - origin.getX();
                int y = at.getY() - origin.getY();
                int z = at.getZ() - origin.getZ();
                boolean shell = y == 0 || y == 3 || x == 0 || x == 3 || z == 0 || z >= 5;

                world.setBlockState(at, shell ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState(), 18);
            }

            BlockPos door = origin.add(1, 1, 0);
            var lower = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH);

            world.setBlockState(door, lower.with(DoorBlock.HALF, DoubleBlockHalf.LOWER), 18);
            world.setBlockState(door.up(), lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 18);

            world.setBlockState(origin.add(1, 1, 2), Blocks.STONE.getDefaultState(), 18);

            BlockPos foot = origin.add(1, 1, 3);
            var bed = Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH);

            world.setBlockState(foot, bed.with(BedBlock.PART, BedPart.FOOT), 18);
            world.setBlockState(foot.south(), bed.with(BedBlock.PART, BedPart.HEAD), 18);

            BlockPos gap = origin.add(2, 1, 3);

            context.assertTrue(ChestPassage.closesAPassage(world, gap, box),
                    "Regra 370: o baú em " + gap.toShortString() + " tiraria o acesso à cama e não foi visto assim");

            ChestPlacer.Result result = ChestPlacer.placeBesideBedInStructure(world, foot.south(), box);

            context.assertFalse(world.getBlockState(gap).isOf(Blocks.CHEST),
                    "Regra 370: o baú tirou da porta o acesso à cama: " + result);
            context.assertTrue(result.outcome() == ChestPlacer.Outcome.PLACED
                    && world.getBlockState(origin.add(2, 1, 4)).isOf(Blocks.CHEST), "o baú não foi para o canto: " + result);
        } finally {
            for (BlockPos at : BlockPos.iterate(origin, origin.add(3, 3, 6))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }
        }

        context.complete();
    }
}
