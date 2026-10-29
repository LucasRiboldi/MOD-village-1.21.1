package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O fazendeiro abre canteiro retangular — autor, 2026-10-28, depois do jogo da 0.3.104 (a roça crescia em manchas):
 * um canteiro 9×9 em chão plano, ao lado da fazenda com 1 de folga, com a água na linha do meio ({@link FarmPlots}).
 */
public class FarmerTillGameTest implements FabricGameTest {

    private static final int Y = 50;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_sowing", tickLimit = 20)
    public void theFarmerOpensANineByNinePlotBesideTheFarm(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        BlockPos farm = context.getAbsolutePos(new BlockPos(0, Y, 4));
        Set<BlockPos> field = new HashSet<>();

        try {
            ground(context);

            for (int z = 0; z <= 8; z++) {
                BlockPos at = context.getAbsolutePos(new BlockPos(0, Y, z));
                world.setBlockState(at, Blocks.FARMLAND.getDefaultState());
                field.add(at);
            }

            Set<BlockPos> tilled = tillWholePlot(context, colonyId, farm.up());

            assertNineByNine(context, tilled);

            for (BlockPos cell : tilled) {
                for (BlockPos old : field) {
                    context.assertTrue(Math.abs(cell.getX() - old.getX()) + Math.abs(cell.getZ() - old.getZ()) > 1,
                            "o canteiro encostou na fazenda sem folga em " + cell.toShortString());
                }
            }
        } finally {
            clear(context);
            FarmPlots.clearAll();
        }

        context.complete();
    }

    /** Uma rua atravessando o chão plano: não cabe 9×9 sem cortar a rua, e nenhum canteiro é aberto. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_sowing_road", tickLimit = 20)
    public void noPlotIsOpenedAcrossAStreet(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        BlockPos farm = context.getAbsolutePos(new BlockPos(0, Y, 4));

        try {
            ground(context);

            for (int z = 0; z <= 8; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(0, Y, z)), Blocks.FARMLAND.getDefaultState());
            }

            for (int z = 0; z <= 14; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(6, Y, z)), Blocks.DIRT_PATH.getDefaultState());
            }

            Optional<BlockPos> plot = FarmPlots.next(world, colonyId, farm.up());

            context.assertTrue(plot.isEmpty(), "não cabe 9×9 sem cortar a rua, e mesmo assim saiu " + plot);
        } finally {
            clear(context);
            FarmPlots.clearAll();
        }

        context.complete();
    }

    /** Ara o canteiro inteiro, como o fazendeiro faria: 72 casas, cada uma devolvida e arada. */
    static Set<BlockPos> tillWholePlot(TestContext context, UUID colonyId, BlockPos from) {
        ServerWorld world = context.getWorld();
        Set<BlockPos> tilled = new HashSet<>();

        for (int i = 0; i < 72; i++) {
            Optional<BlockPos> cell = FarmPlots.next(world, colonyId, from);

            context.assertTrue(cell.isPresent(), "o canteiro parou na casa " + i);
            context.assertTrue(tilled.add(cell.get()), "a mesma casa voltou: " + cell.get().toShortString());
            world.setBlockState(cell.get(), Blocks.FARMLAND.getDefaultState());
        }

        return tilled;
    }

    /** 72 casas num quadrado 9×9 plano, e as 9 do meio de uma das linhas são água. */
    static void assertNineByNine(TestContext context, Set<BlockPos> tilled) {
        ServerWorld world = context.getWorld();
        int minX = tilled.stream().mapToInt(BlockPos::getX).min().orElseThrow();
        int maxX = tilled.stream().mapToInt(BlockPos::getX).max().orElseThrow();
        int minZ = tilled.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
        int maxZ = tilled.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
        int y = tilled.iterator().next().getY();

        context.assertTrue(maxX - minX == 8 && maxZ - minZ == 8, "o canteiro não é 9×9: x " + minX + ".." + maxX
                + ", z " + minZ + ".." + maxZ);
        context.assertTrue(tilled.stream().allMatch(cell -> cell.getY() == y), "o canteiro não é plano");

        int water = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos cell = new BlockPos(x, y, z);

                if (world.getBlockState(cell).isOf(Blocks.WATER)) {
                    water++;
                    context.assertTrue(x == minX + 4 || z == minZ + 4, "água fora da linha do meio em "
                            + cell.toShortString());
                } else {
                    context.assertTrue(tilled.contains(cell), "casa do canteiro sem arar em " + cell.toShortString());
                }
            }
        }

        context.assertTrue(water == 9, "a linha do meio devia ter 9 de água, tem " + water);
    }

    /** Grama de x 0..14, z 0..14 em Y, pedra embaixo e ar em cima. */
    private static void ground(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y - 1, z)), Blocks.STONE.getDefaultState());
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.GRASS_BLOCK.getDefaultState());

                for (int y = Y + 1; y <= Y + 3; y++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static void clear(TestContext context) {
        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                for (int y = Y - 1; y <= Y + 3; y++) {
                    context.getWorld().setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)),
                            Blocks.AIR.getDefaultState());
                }
            }
        }
    }
}
