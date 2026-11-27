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

    /**
     * Regra 371 (notas do jogo da 0.4.145, J3 🅲: "nó não se juntou ao grafo" na entrada da mina): depois que a mina
     * desce um nível, a entrada guardada dela fica no fundo, mas o pátio da boca da superfície continua pátio — a
     * entrada da mina e as portas em volta não viram folga da boca.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_yard_deeper", tickLimit = 5)
    public void theSurfaceYardStaysAYardAfterTheMineGoesDeeper(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, 1, z), Blocks.SAND);
            }
        }

        context.setBlockState(new BlockPos(3, 1, 4), Blocks.AIR);
        context.setBlockState(new BlockPos(3, 0, 4), Blocks.AIR);

        BlockPos mouth = context.getAbsolutePos(new BlockPos(3, 2, 4));
        BlockPos underTheArch = MineYard.underTheArch(mouth, Direction.SOUTH);
        java.util.UUID colonyId = java.util.UUID.randomUUID();
        // A entrada guardada da mina, um nível abaixo da boca: como fica depois de "went one level deeper".
        com.villagecolony.core.construction.model.MineShaft deeper = com.villagecolony.core.construction.model.MineShaft
                .from(new com.villagecolony.core.type.ColonyPos(mouth.getX(), mouth.getY() - 6, mouth.getZ()),
                        com.villagecolony.core.type.Side.SOUTH);

        try {
            context.assertTrue(MineYard.lay(context.getWorld(), mouth, Direction.SOUTH, TEST_RADIUS) > 0,
                    "o pátio da boca não foi posto");
            com.villagecolony.VillageColonyMod.MINES.restore(
                    com.villagecolony.core.construction.model.Mine.restore(colonyId, deeper, 1));
            MineMouths.remember(colonyId, mouth, Direction.SOUTH);

            context.assertTrue(MineYard.isYardColumn(context.getWorld(), underTheArch.getX(), underTheArch.getZ()),
                    "Regra 371: o caminho sob o arco deixou de ser pátio depois que a mina desceu");
        } finally {
            com.villagecolony.VillageColonyMod.MINES.removeOfColony(colonyId);
            MineMouths.forget(colonyId);
            for (BlockPos at : BlockPos.iterate(0, 0, 0, 7, 7, 7)) {
                context.setBlockState(at.toImmutable(), at.getY() == 0 ? Blocks.STONE : Blocks.AIR);
            }
        }

        context.complete();
    }

    /**
     * Regra 371: a frente de uma porta perto da boca (a casa do mineiro) e as colunas em volta dela não são folga da boca
     * para o grafo; o caracol da escada continua fechado para o caminho.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_yard_door", tickLimit = 5)
    public void aDoorNearTheMouthIsNotMineClearance(TestContext context) {
        BlockPos mouth = context.getAbsolutePos(new BlockPos(3, 2, 4));
        java.util.UUID colonyId = java.util.UUID.randomUUID();
        BlockPos front = mouth.add(4, 0, 4);
        BlockPos stair = MineMouths.stairCells(mouth, Direction.SOUTH).get(2);

        try {
            com.villagecolony.VillageColonyMod.MINES.restore(com.villagecolony.core.construction.model.Mine.restore(
                    colonyId, com.villagecolony.core.construction.model.MineShaft.from(
                            com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(mouth),
                            com.villagecolony.core.type.Side.SOUTH), 1));
            MineMouths.remember(colonyId, mouth, Direction.SOUTH);

            context.assertTrue(new WorldPathTerrain(context.getWorld(), colonyId).blocked(front.getX(), front.getZ()),
                    "o cenário não pôs a porta na folga da boca");

            WorldPathTerrain terrain = new WorldPathTerrain(context.getWorld(), colonyId);

            terrain.floorAnywhereAt(java.util.List.of(
                    com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(front),
                    com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(stair)));

            context.assertFalse(terrain.blocked(front.getX(), front.getZ()),
                    "Regra 371: a frente da porta perto da boca ficou bloqueada para o grafo");
            context.assertFalse(terrain.blocked(front.getX() + 1, front.getZ()),
                    "Regra 371: a coluna ao lado da frente da porta ficou bloqueada para o grafo");
            context.assertTrue(terrain.blocked(stair.getX(), stair.getZ()), "o caracol da escada abriu para o caminho");
        } finally {
            com.villagecolony.VillageColonyMod.MINES.removeOfColony(colonyId);
            MineMouths.forget(colonyId);
        }

        context.complete();
    }
}
