package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

/**
 * Caminho natural de cada chão, e a limpeza do lote nos biomas de árvores juntas — pedido do autor,
 * 2026-10-20.
 *
 * <p>Fora da arena (céu aberto: o lote exige 25 blocos livres, e o teto da arena fica a 9). O bioma
 * do trecho é trocado por {@code /fillbiome}; a planície do mundo de teste é o controle.
 */
public class BiomeSiteGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_site")
    public void everyGroundGetsItsNaturalPath(TestContext context) {
        Map<Block, Block> expected = Map.of(
                Blocks.GRASS_BLOCK, Blocks.DIRT_PATH,
                Blocks.MYCELIUM, Blocks.DIRT_PATH,
                Blocks.SAND, Blocks.SMOOTH_SANDSTONE,
                Blocks.RED_SAND, Blocks.SMOOTH_RED_SANDSTONE,
                Blocks.MUD, Blocks.PACKED_MUD,
                Blocks.SNOW_BLOCK, Blocks.PACKED_ICE,
                Blocks.STONE, Blocks.GRAVEL,
                Blocks.TERRACOTTA, Blocks.TERRACOTTA);

        expected.forEach((ground, path) -> context.assertTrue(
                VillageRoad.pavingOn(ground.getDefaultState()).equals(path),
                ground + " virou " + VillageRoad.pavingOn(ground.getDefaultState()) + ", e não " + path));

        // O arenito liso é a parede das casas do deserto: fora do índice de ruas não é rua — nem
        // pelo bloco, nem pela posição.
        BlockPos wall = context.getAbsolutePos(new BlockPos(2, 1, 2));
        context.getWorld().setBlockState(wall, Blocks.SMOOTH_SANDSTONE.getDefaultState());

        try {
            context.assertFalse(VillageRoad.isPaving(context.getWorld(), Blocks.SMOOTH_SANDSTONE.getDefaultState()),
                    "o arenito liso virou rua em qualquer lugar");
            context.assertFalse(VillageRoad.isPaving(context.getWorld(), wall),
                    "a parede de arenito liso, fora do índice de ruas, foi tomada por rua");
        } finally {
            context.getWorld().setBlockState(wall, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    /** Na floresta a árvore natural sai do lote; na planície continua ocupando (controle). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_site")
    public void aForestTreeDoesNotOccupyTheLotButAPlainsTreeDoes(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos forest = groundAt(context, 0);
        BlockPos plains = groundAt(context, 32);

        setBiome(context, forest, "minecraft:forest");
        treeOn(world, forest, Blocks.GRASS_BLOCK);
        treeOn(world, plains, Blocks.GRASS_BLOCK);

        try {
            context.assertTrue(LotClearance.isClearAbove(world, forest.getX(), forest.getZ(), forest.getY() + 1, 25),
                    "na floresta, a árvore natural ainda ocupa o lote (bioma "
                            + world.getBiome(forest.up()).getKey().map(key -> key.getValue().toString()).orElse("?")
                            + ", mata " + LotGrowth.clearsTreesAt(world, forest.up()) + ")");
            context.assertFalse(LotClearance.isClearAbove(world, plains.getX(), plains.getZ(), plains.getY() + 1, 25),
                    "controle: na planície a árvore deixou de ocupar o lote");

            int ground = LotGround.groundInColumn(world, forest.getX(), forest.getZ(), forest.getY())
                    .map(BlockPos::getY).orElse(Integer.MIN_VALUE);
            context.assertTrue(ground == forest.getY(), "o chão sob a árvore ficou em " + ground
                    + ", e não em " + forest.getY());

            int cleared = LotGrowth.clearColumn(world, forest.getX(), forest.getY() + 1, forest.getY() + 25, forest.getZ());
            context.assertTrue(cleared >= 5 && world.getBlockState(forest.up()).isAir(),
                    "a árvore da floresta não saiu: " + cleared + " blocos");
        } finally {
            clear(context, forest);
            clear(context, plains);
        }

        context.complete();
    }

    /**
     * A árvore que nasce do chão de uma muda da colônia (viveiro, bosque) nunca sai; a de terra
     * enraizada que a colônia não plantou — a cerejeira do Vanilla — sai (controle).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_site")
    public void aNurseryTreeIsNeverCleared(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos nursery = groundAt(context, 64);

        BlockPos wild = nursery.add(4, 0, 0);

        setBiome(context, nursery, "minecraft:forest");
        treeOn(world, nursery, Blocks.ROOTED_DIRT);
        treeOn(world, wild, Blocks.ROOTED_DIRT);
        com.villagecolony.fabric.world.ColonyTrees.planted(nursery);

        try {
            context.assertTrue(LotClearance.isClearAbove(world, wild.getX(), wild.getZ(), wild.getY() + 1, 25),
                    "controle: a árvore de terra enraizada que a colônia não plantou (cerejeira) ficou protegida");

            context.assertFalse(LotClearance.isClearAbove(world, nursery.getX(), nursery.getZ(), nursery.getY() + 1, 25),
                    "a árvore do viveiro foi aceita como mato a limpar");
            context.assertTrue(LotGrowth.clearColumn(world, nursery.getX(), nursery.getY() + 1, nursery.getY() + 25,
                    nursery.getZ()) == 0 || world.getBlockState(nursery.up()).isOf(Blocks.OAK_LOG),
                    "o tronco da árvore do viveiro foi cortado");
        } finally {
            com.villagecolony.fabric.world.ColonyTrees.clearAll();
            clear(context, nursery);
            clear(context, wild);
        }

        context.complete();
    }

    private static BlockPos groundAt(TestContext context, int offset) {
        BlockPos at = context.getAbsolutePos(new BlockPos(1, 0, 1)).add(1_000_000, 0, 1_500_000 + offset);
        context.getWorld().getChunk(at);

        return at;
    }

    private static void setBiome(TestContext context, BlockPos at, String biome) {
        BlockPos from = at.add(-8, -4, -8);
        BlockPos to = at.add(8, 30, 8);

        // O /fillbiome recusa área com chunk descarregado.
        for (BlockPos corner : new BlockPos[] {from, to, new BlockPos(from.getX(), 0, to.getZ()),
                new BlockPos(to.getX(), 0, from.getZ())}) {
            context.getWorld().getChunk(corner);
        }

        context.getWorld().getServer().getCommandManager().executeWithPrefix(
                context.getWorld().getServer().getCommandSource().withSilent(),
                "fillbiome " + from.getX() + " " + from.getY() + " " + from.getZ() + " "
                        + to.getX() + " " + to.getY() + " " + to.getZ() + " " + biome);
    }

    /** Um carvalho natural de 4 de tronco com copa, sobre o chão pedido. */
    private static void treeOn(ServerWorld world, BlockPos ground, Block soil) {
        world.setBlockState(ground.down(), Blocks.STONE.getDefaultState());
        world.setBlockState(ground, soil.getDefaultState());

        for (int dy = 1; dy <= 4; dy++) {
            world.setBlockState(ground.up(dy), Blocks.OAK_LOG.getDefaultState());
        }

        for (BlockPos leaf : BlockPos.iterate(ground.add(-1, 5, -1), ground.add(1, 6, 1))) {
            BlockState state = Blocks.OAK_LEAVES.getDefaultState();
            world.setBlockState(leaf, state);
        }
    }

    private static void clear(TestContext context, BlockPos ground) {
        for (BlockPos pos : BlockPos.iterate(ground.add(-2, -1, -2), ground.add(2, 8, 2))) {
            context.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }
}
