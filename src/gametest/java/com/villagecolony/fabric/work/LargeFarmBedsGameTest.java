package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A plantação grande arada e semeada inteira — Regra 325: os canteiros são as colunas de terra arada do molde da peça, e
 * não só a terra arada que está no mundo; cada canteiro recebe a lavoura que a vila tem menos.
 */
public class LargeFarmBedsGameTest implements FabricGameTest {

    private static final int FLOOR = 2;

    private static final int SIDE = 16;

    /** A água do molde {@code colony/large_farm}. */
    private static final int[][] WATER = {{4, 4}, {11, 4}, {4, 11}, {11, 11}};

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "large_farm_beds_whole", tickLimit = 20)
    public void theFinishedLargeFarmIsTilledAndSownWhole(TestContext context) {
        BlockBox box = farm(context);
        ServerWorld world = context.getWorld();

        // Um pedaço de grama no meio, com mato em cima, longe de qualquer canteiro arado.
        grassPatch(context, 5, 9);

        try {
            LargeFarmSeeds.sowAtCompletion(world, LargeFarmPlans.MODEL, box);

            Map<Block, Integer> crops = new HashMap<>();
            int missing = 0;

            for (int x = 1; x < SIDE - 1; x++) {
                for (int z = 1; z < SIDE - 1; z++) {
                    if (isWater(x, z)) {
                        continue;
                    }

                    BlockPos bed = context.getAbsolutePos(new BlockPos(x, FLOOR, z));
                    BlockState crop = world.getBlockState(bed.up());

                    if (!world.getBlockState(bed).isOf(Blocks.FARMLAND) || crop.isAir()) {
                        missing++;
                    } else {
                        crops.merge(crop.getBlock(), 1, Integer::sum);
                    }
                }
            }

            context.assertTrue(missing == 0, missing + " canteiros do molde ficaram sem arar ou semear");
            context.assertTrue(crops.size() == 4, "a plantação não saiu variada: " + crops);
        } finally {
            LargeFarmBeds.clearAll();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "large_farm_beds_regrown", tickLimit = 20)
    public void theFarmerTillsABedThatTurnedToGrass(TestContext context) {
        BlockBox box = farm(context);
        ServerWorld world = context.getWorld();

        try {
            LargeFarmSeeds.sowAtCompletion(world, LargeFarmPlans.MODEL, box);

            // O meio voltou a ser grama com mato: nenhum canteiro arado com ar em cima encosta nele.
            grassPatch(context, 6, 8);

            Optional<OwnFarm.Found> found = OwnFarm.work(world, box, true);

            context.assertTrue(found.filter(work -> work.chore() == FarmerWork.Chore.SOW).isPresent(),
                    "o fazendeiro não achou o canteiro do molde que virou grama: " + found);

            BlockPos bed = found.get().at();

            OwnFarm.tillIfDirt(world, bed);
            context.assertTrue(world.getBlockState(bed).isOf(Blocks.FARMLAND) && world.getBlockState(bed.up()).isAir(),
                    "o canteiro " + bed.toShortString() + " não foi arado");
        } finally {
            LargeFarmBeds.clearAll();
        }

        context.complete();
    }

    /** A plantação grande do molde, com a terra arada vazia, a borda de tora e a água; devolve a caixa da obra. */
    private static BlockBox farm(TestContext context) {
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                boolean border = x == 0 || z == 0 || x == SIDE - 1 || z == SIDE - 1;

                context.setBlockState(new BlockPos(x, 0, z), Blocks.STONE);
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, FLOOR, z),
                        border ? Blocks.OAK_LOG : isWater(x, z) ? Blocks.WATER : Blocks.FARMLAND);

                for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }

        BlockPos min = context.getAbsolutePos(new BlockPos(0, FLOOR, 0));
        BlockPos max = context.getAbsolutePos(new BlockPos(SIDE - 1, FLOOR + 1, SIDE - 1));

        return BlockBox.create(min, max);
    }

    /** Grama com mato em cima, de {@code from} a {@code to} nos dois eixos. */
    private static void grassPatch(TestContext context, int from, int to) {
        for (int x = from; x <= to; x++) {
            for (int z = from; z <= to; z++) {
                context.setBlockState(new BlockPos(x, FLOOR, z), Blocks.GRASS_BLOCK);
                context.setBlockState(new BlockPos(x, FLOOR + 1, z), Blocks.SHORT_GRASS);
            }
        }
    }

    private static boolean isWater(int x, int z) {
        for (int[] water : WATER) {
            if (water[0] == x && water[1] == z) {
                return true;
            }
        }

        return false;
    }
}
