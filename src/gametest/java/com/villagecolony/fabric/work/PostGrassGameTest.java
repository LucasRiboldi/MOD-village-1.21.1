package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * A grama ao pé do poste da ovelha amarrada — regra do autor, 2026-10-20: nasce no chão ao lado do
 * poste e volta depois que a ovelha a come.
 */
public class PostGrassGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "post_grass")
    public void grassGrowsBesideThePostAndComesBackAfterItIsEaten(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos post = sandAround(context, 0);

        try {
            Optional<BlockPos> grass = PostGrass.ensure(world, post);

            context.assertTrue(grass.isPresent() && world.getBlockState(grass.get()).isOf(Blocks.GRASS_BLOCK),
                    "não nasceu grama ao pé do poste");
            context.assertTrue(grass.get().getY() == post.getY() - 1
                            && grass.get().getManhattanDistance(post.down()) <= 4,
                    "a grama nasceu fora do chão ao lado do poste: " + grass.get().subtract(post));

            // A ovelha come: o bloco vira terra, e a próxima tosquia faz a grama voltar no mesmo lugar.
            world.setBlockState(grass.get(), Blocks.DIRT.getDefaultState());
            Optional<BlockPos> again = PostGrass.ensure(world, post);

            context.assertTrue(again.equals(grass) && world.getBlockState(grass.get()).isOf(Blocks.GRASS_BLOCK),
                    "a grama comida não voltou no mesmo bloco: " + again);

            // Controle: com grama no lugar, nada muda e não nasce outra.
            context.assertTrue(PostGrass.ensure(world, post).equals(grass), "nasceu uma segunda grama");
        } finally {
            clear(context, post);
        }

        context.complete();
    }

    /** Controle: chão com coisa em cima (sem ar para a ovelha) não recebe grama. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "post_grass")
    public void groundCoveredAllAroundGetsNoGrass(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos post = sandAround(context, 20);

        for (BlockPos pos : BlockPos.iterate(post.add(-2, 0, -2), post.add(2, 0, 2))) {
            if (!pos.equals(post)) {
                world.setBlockState(pos, Blocks.GLASS.getDefaultState());
            }
        }

        try {
            context.assertTrue(PostGrass.ensure(world, post).isEmpty(), "nasceu grama sob o vidro");
        } finally {
            clear(context, post);
        }

        context.complete();
    }

    /** Um poste de cerca sobre areia, longe da arena (para não entrar nos testes do pastor). */
    private static BlockPos sandAround(TestContext context, int offset) {
        ServerWorld world = context.getWorld();
        BlockPos post = context.getAbsolutePos(new BlockPos(1, 4, 1)).add(1_000_000, 0, 1_900_000 + offset);
        world.getChunk(post);

        for (BlockPos pos : BlockPos.iterate(post.add(-3, -2, -3), post.add(3, -1, 3))) {
            world.setBlockState(pos, Blocks.SAND.getDefaultState());
        }

        world.setBlockState(post, Blocks.OAK_FENCE.getDefaultState());

        return post;
    }

    private static void clear(TestContext context, BlockPos post) {
        for (BlockPos pos : BlockPos.iterate(post.add(-3, -2, -3), post.add(3, 2, 3))) {
            context.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }
}
