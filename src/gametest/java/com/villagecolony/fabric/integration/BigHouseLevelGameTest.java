package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.Optional;
import java.util.UUID;

/**
 * Regra 338: a BigHouseMOD nasce na altura das casas da vila. O platô plano no alto do morro, ligado à vila por uma
 * rampa, perde para o campo ondulado de 1 na altura da vila — o jogo da 0.4.93 em montanha pôs a casa 60 acima.
 */
public class BigHouseLevelGameTest implements FabricGameTest {

    /** O lado do campo; o platô e a rampa ficam dentro dele. */
    private static final int SIDE = 64;

    /** O chão do campo, acima do mundo plano do teste. */
    private static final int GROUND = 100;

    /** Quanto o platô fica acima do campo. */
    private static final int RISE = 24;

    private static final int PLATEAU = 9;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_level", tickLimit = 20)
    public void theHouseIsBuiltAtTheHeightOfTheVillageNotOnTheFlatPlateau(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos low = context.getAbsolutePos(BlockPos.ORIGIN).add(2_900_000, 0, 2_900_000).withY(GROUND);
        BlockPos center = low.add(SIDE / 2, 1, SIDE / 2);

        try {
            forceChunks(world, low, true);
            field(world, low);
            plateau(world, low.add(SIDE / 2 + 4, 0, SIDE / 2 - PLATEAU / 2));

            Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(center));
            Optional<BlockPos> site = BigHouseFoundation.siteFor(world, colony, new Vec3i(5, 4, 5));

            context.assertTrue(site.isPresent(), "nenhum lote para a casa: o cenário não mede nada");
            context.assertTrue(Math.abs(site.get().getY() - center.getY()) <= FoundationLotSearch.HEIGHT_BAND,
                    "Regra 338: a casa foi para y " + site.get().getY() + " em " + site.get().toShortString()
                            + ", com a vila em y " + center.getY());
        } finally {
            for (BlockPos at : BlockPos.iterate(low, low.add(SIDE - 1, RISE + 2, SIDE - 1))) {
                world.setBlockState(at.toImmutable(), Blocks.AIR.getDefaultState(), 18);
            }

            forceChunks(world, low, false);
        }

        context.complete();
    }

    /** Terra com grama, ondulada de 1 em x: todo lote do campo tem desnível 1. */
    private static void field(ServerWorld world, BlockPos low) {
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                BlockPos at = low.add(x, 0, z);

                world.setBlockState(at.down(), Blocks.DIRT.getDefaultState(), 18);
                world.setBlockState(at, (x % 2 == 0 ? Blocks.GRASS_BLOCK : Blocks.DIRT).getDefaultState(), 18);

                if (x % 2 == 1) {
                    world.setBlockState(at.up(), Blocks.GRASS_BLOCK.getDefaultState(), 18);
                }
            }
        }
    }

    /** O platô plano, {@link #RISE} acima do campo, e a rampa de degrau 1 que o liga a ele, descendo em z. */
    private static void plateau(ServerWorld world, BlockPos corner) {
        for (int x = 0; x < PLATEAU; x++) {
            for (int z = 0; z < PLATEAU; z++) {
                column(world, corner.add(x, 0, z), RISE);
            }
        }

        for (int step = 1; step < RISE; step++) {
            for (int x = 0; x < 3; x++) {
                column(world, corner.add(x, 0, PLATEAU - 1 + step), RISE - step);
            }
        }
    }

    private static void column(ServerWorld world, BlockPos base, int height) {
        for (int y = 1; y <= height; y++) {
            world.setBlockState(base.up(y), (y == height ? Blocks.GRASS_BLOCK : Blocks.DIRT).getDefaultState(), 18);
        }
    }

    private static void forceChunks(ServerWorld world, BlockPos low, boolean force) {
        for (int x = (low.getX() >> 4) - 1; x <= ((low.getX() + SIDE) >> 4) + 1; x++) {
            for (int z = (low.getZ() >> 4) - 1; z <= ((low.getZ() + SIDE) >> 4) + 1; z++) {
                world.setChunkForced(x, z, force);

                if (force) {
                    world.getChunk(x, z);
                }
            }
        }
    }
}
