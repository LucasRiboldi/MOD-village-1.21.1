package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.type.Side;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.TreeConfiguredFeatures;

import java.util.ArrayList;
import java.util.List;

/**
 * Árvore no traçado do caminho sai inteira, sem deixar tronco para trás (Regras 280 e 326), com as árvores que o Vanilla
 * gera de verdade, e não com uma árvore desenhada à mão.
 */
public class PathTreeClearingGameTest implements FabricGameTest {

    /** Meia largura da caixa conferida em volta da árvore. */
    private static final int HALF = 8;

    /** Altura da caixa conferida acima do chão. */
    private static final int TALL = 32;

    private static final List<RegistryKey<ConfiguredFeature<?, ?>>> TREES = List.of(
            TreeConfiguredFeatures.OAK, TreeConfiguredFeatures.FANCY_OAK, TreeConfiguredFeatures.BIRCH);

    private static void forceChunks(ServerWorld world, BlockPos center, boolean force) {
        for (int x = (center.getX() - HALF) >> 4; x <= (center.getX() + HALF) >> 4; x++) {
            for (int z = (center.getZ() - HALF) >> 4; z <= (center.getZ() + HALF) >> 4; z++) {
                world.setChunkForced(x, z, force);
            }
        }
    }

    private static void ground(ServerWorld world, BlockPos center) {
        for (BlockPos at : BlockPos.iterate(center.add(-HALF, -1, -HALF), center.add(HALF, TALL, HALF))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }

        for (BlockPos at : BlockPos.iterate(center.add(-HALF, -1, -HALF), center.add(HALF, 0, HALF))) {
            world.setBlockState(at, at.getY() == center.getY() ? Blocks.GRASS_BLOCK.getDefaultState()
                    : Blocks.STONE.getDefaultState(), 18);
        }
    }

    private static List<BlockPos> logsLeft(ServerWorld world, BlockPos center) {
        List<BlockPos> left = new ArrayList<>();

        for (BlockPos at : BlockPos.iterate(center.add(-HALF, 1, -HALF), center.add(HALF, TALL, HALF))) {
            if (world.getBlockState(at).isIn(BlockTags.LOGS)) {
                left.add(at.toImmutable());
            }
        }

        return left;
    }

    /**
     * Cada árvore da planície, com várias sementes, cortada pela coluna que passa pelo tronco e pelas que passam só
     * pela copa, a um, dois e três blocos dele. Copa acima da altura de passagem não é tocada; a que é tocada sai
     * inteira.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_tree_clearing", tickLimit = 20)
    public void aPlainsTreeOnThePathLeavesNoTrunkBehind(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 140, 1));
        var features = world.getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE);
        List<String> failures = new ArrayList<>();
        int grown = 0;

        forceChunks(world, center, true);

        try {
            for (RegistryKey<ConfiguredFeature<?, ?>> tree : TREES) {
                for (long seed = 0; seed < 12; seed++) {
                    for (int offset = 0; offset <= 3; offset++) {
                        ground(world, center);

                        boolean made = features.getOrThrow(tree).generate(world,
                                world.getChunkManager().getChunkGenerator(), Random.create(seed), center.up());

                        if (!made) {
                            continue;
                        }

                        PathStep step = new PathStep(center.getX() + offset, center.getY(), center.getZ(), Side.EAST, 0, 0);

                        if (PathPaving.clearTreeOver(world, step, List.of()) == 0) {
                            continue;
                        }

                        grown++;

                        List<BlockPos> left = logsLeft(world, center);

                        if (!left.isEmpty()) {
                            failures.add(tree.getValue().getPath() + " semente " + seed + " coluna +" + offset
                                    + ": " + left.size() + " troncos, ex. " + left.get(0).subtract(center));
                        }
                    }
                }
            }

            context.assertTrue(grown > 0, "nenhuma árvore nasceu e foi cortada na arena");
            context.assertTrue(failures.isEmpty(), "Regra 326: a árvore do caminho deixou tronco — " + failures.size()
                    + " de " + grown + ": " + String.join("; ", failures.subList(0, Math.min(6, failures.size()))));
        } finally {
            for (BlockPos at : BlockPos.iterate(center.add(-HALF, -1, -HALF), center.add(HALF, TALL, HALF))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }

            forceChunks(world, center, false);
        }

        context.complete();
    }
}
