package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Regra 293: o pátio da boca da mina — disco de pedra na base do arco, nivelado, sem árvore e sem nada em cima, com o
 * buraco da escada intacto. Raio 3 no teste, para caber na área (em jogo, {@link MineYard#RADIUS}).
 */
public class MineYardGameTest implements FabricGameTest {

    private static final int TEST_RADIUS = 3;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_yard", tickLimit = 5)
    public void theYardIsAFlatStoneDiscWithTheStairsOpen(TestContext context) {
        // Chão de terra em y 0 e 1; a boca em (3, 2, 4), descendo para o sul; o buraco da escada na coluna da boca.
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        context.setBlockState(new BlockPos(3, 1, 4), Blocks.AIR);
        context.setBlockState(new BlockPos(3, 0, 4), Blocks.AIR);
        context.setBlockState(new BlockPos(4, 2, 5), Blocks.DIRT);
        context.setBlockState(new BlockPos(4, 2, 3), Blocks.OAK_LOG);
        context.setBlockState(new BlockPos(4, 3, 3), Blocks.OAK_LEAVES);
        context.setBlockState(new BlockPos(4, 7, 4), Blocks.OAK_LEAVES);

        BlockPos mouth = context.getAbsolutePos(new BlockPos(3, 2, 4));
        BlockPos center = mouth.offset(Direction.SOUTH.rotateYCounterclockwise());

        try {
            context.assertTrue(MineYard.lay(context.getWorld(), mouth, Direction.SOUTH, TEST_RADIUS) > 0,
                    "o pátio da boca não foi posto");
            context.assertTrue(context.getWorld().getBlockState(center.down()).isOf(Blocks.DIRT_PATH),
                    "Regra 302: o chão sob o arco não é caminho");
            context.assertTrue(context.getWorld().getBlockState(center.down().east()).isOf(Blocks.STONE)
                    || context.getWorld().getBlockState(center.down().west()).isOf(Blocks.STONE),
                    "o pátio em volta do caminho não é pedra");
            context.assertTrue(context.getBlockState(new BlockPos(3, 1, 4)).isAir()
                    && context.getBlockState(new BlockPos(3, 0, 4)).isAir(), "o pátio tampou o buraco da escada");
            context.assertTrue(context.getBlockState(new BlockPos(4, 2, 5)).isAir(), "o calombo ficou no pátio");
            context.assertFalse(context.getBlockState(new BlockPos(4, 2, 3)).isIn(BlockTags.LOGS),
                    "a árvore ficou no pátio");
            context.assertTrue(context.getBlockState(new BlockPos(4, 7, 4)).isAir(), "ficou bloco em cima do arco");
            context.assertTrue(MineYard.isYardAt(center.down(), context.getWorld().getBlockState(center.down()),
                    mouth, Direction.SOUTH), "o caminho sob o arco não é reconhecido como pátio");
            context.assertFalse(MineYard.isYardAt(context.getAbsolutePos(new BlockPos(4, 3, 4)),
                    Blocks.STONE.getDefaultState(), mouth, Direction.SOUTH), "pedra fora da altura do disco virou pátio");
        } finally {
            for (BlockPos at : BlockPos.iterate(0, 0, 0, 7, 7, 7)) {
                context.setBlockState(at.toImmutable(), at.getY() == 0 ? Blocks.STONE : Blocks.AIR);
            }
        }

        context.complete();
    }
}
