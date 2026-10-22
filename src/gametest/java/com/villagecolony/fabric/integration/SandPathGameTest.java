package com.villagecolony.fabric.integration;

import com.villagecolony.fabric.content.ModContent;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O caminho de areia — autor, 2026-10-22: a cópia do caminho de terra com a cor do arenito, o caminho
 * exclusivo da vila do deserto. No deserto a colônia só assenta ele; fora, o caminho natural do chão.
 * Como o de terra, coberto por bloco sólido ele volta a ser o chão — areia.
 */
public class SandPathGameTest implements FabricGameTest {

    private static final int Y = 40;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sand_path", tickLimit = 40)
    public void theDesertPavesWithSandPathAndNothingElseDoes(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos desert = context.getAbsolutePos(new BlockPos(2, Y, 2));
        BlockPos plains = context.getAbsolutePos(new BlockPos(2, Y, 2)).add(32, 0, 0);

        setBiome(context, desert, "minecraft:desert");

        // No alto, a areia sem apoio cairia: pedra embaixo de tudo.
        for (BlockPos at : new BlockPos[] {desert, desert.east(), plains, plains.east()}) {
            world.setBlockState(at.down(), Blocks.STONE.getDefaultState());
        }

        world.setBlockState(desert, Blocks.SAND.getDefaultState());
        world.setBlockState(desert.east(), Blocks.GRASS_BLOCK.getDefaultState());
        world.setBlockState(plains, Blocks.SAND.getDefaultState());
        world.setBlockState(plains.east(), Blocks.GRASS_BLOCK.getDefaultState());

        context.assertTrue(VillageRoad.pavingOn(world, desert, world.getBlockState(desert)) == ModContent.SAND_PATH,
                "no deserto a areia devia virar caminho de areia");
        context.assertTrue(VillageRoad.pavingOn(world, desert.east(), world.getBlockState(desert.east())) == ModContent.SAND_PATH,
                "no deserto até a grama vira caminho de areia — é o caminho exclusivo da vila");
        context.assertTrue(VillageRoad.pavingOn(world, plains, world.getBlockState(plains)) == Blocks.SMOOTH_SANDSTONE,
                "fora do deserto a areia segue o caminho natural dela");
        context.assertTrue(VillageRoad.pavingOn(world, plains.east(), world.getBlockState(plains.east())) == Blocks.DIRT_PATH,
                "fora do deserto a grama segue com o caminho de terra");

        world.setBlockState(desert, ModContent.SAND_PATH.getDefaultState());

        context.assertTrue(VillageRoad.isPaving(world, world.getBlockState(desert)), "o caminho de areia não conta como rua");
        context.assertTrue(VillageRoad.isPathBlock(world.getBlockState(desert)), "o caminho de areia não é bloco de caminho");

        world.setBlockState(desert.up(), Blocks.STONE.getDefaultState());

        context.waitAndRun(3, () -> {
            context.assertTrue(world.getBlockState(desert).isOf(Blocks.SAND),
                    "coberto por pedra, o caminho de areia devia voltar a ser areia: " + world.getBlockState(desert));

            for (BlockPos at : new BlockPos[] {desert, desert.up(), desert.east(), plains, plains.east()}) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
                world.setBlockState(at.down(), Blocks.AIR.getDefaultState());
            }

            setBiome(context, desert, "minecraft:plains");
            context.complete();
        });
    }

    private static void setBiome(TestContext context, BlockPos at, String biome) {
        BlockPos from = at.add(-4, -4, -4);
        BlockPos to = at.add(4, 4, 4);

        context.getWorld().getChunk(from);
        context.getWorld().getChunk(to);
        context.getWorld().getServer().getCommandManager().executeWithPrefix(
                context.getWorld().getServer().getCommandSource().withSilent(),
                "fillbiome " + from.getX() + " " + from.getY() + " " + from.getZ() + " "
                        + to.getX() + " " + to.getY() + " " + to.getZ() + " " + biome);
    }
}
