package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Regra 363 (G1 🅲 do jogo da 0.4.136, notas do jogo da 0.4.141): o aterro sob areia é arenito, e o Zelador troca a terra
 * que ficou no chão de areia.
 */
public class BiomeSoilGameTest implements FabricGameTest {

    private static final int SIDE = 9;

    /** Sobe a coluna de areia em 2: o que entra embaixo do topo é arenito, nunca terra. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_soil_fill", tickLimit = 20)
    public void theFillUnderSandIsSandstone(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = sandFloor(context);
        BlockPos column = base.add(4, 0, 4);

        try {
            context.assertTrue(GroundSmoothing.column(world, column.getX(), column.getZ(), column.getY() + 2,
                    column.getY() + 4), "o nivelamento recusou a coluna: o teste não mede nada");

            for (int y = column.getY(); y <= column.getY() + 3; y++) {
                context.assertFalse(world.getBlockState(column.withY(y)).isOf(Blocks.DIRT),
                        "Regra 363: terra no aterro da coluna de areia, em y " + y);
            }
            context.assertTrue(world.getBlockState(column.withY(column.getY() + 4)).isOf(Blocks.SAND),
                    "o topo da coluna deixou de ser areia");
        } finally {
            clean(context);
        }

        context.complete();
    }

    /** A terra no topo do chão de areia volta a ser areia, e a terra debaixo da areia vira arenito. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_soil_restore", tickLimit = 20)
    public void theKeeperTurnsTheDirtOfTheSandBack(TestContext context) {
        ServerWorld world = context.getWorld();
        // Longe da arena: a barreira que a cerca seria o topo das colunas.
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_600_000, 20, 2_600_000);

        force(world, base, true);
        sandFloor(world, base);
        BlockPos topDirt = base.add(3, 2, 3);
        BlockPos underSand = base.add(5, 1, 5);
        BlockPos farm = base.add(1, 2, 7);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(base.add(4, 3, 4)));

        colony.restoreBounds(new VillageBounds(base.getX(), base.getY(), base.getZ(), base.getX() + SIDE - 1,
                base.getY() + 4, base.getZ() + SIDE - 1));
        world.setBlockState(topDirt, Blocks.DIRT.getDefaultState());
        world.setBlockState(underSand, Blocks.DIRT.getDefaultState());
        // A terra ao lado da plantação é da roça: fica.
        world.setBlockState(farm, Blocks.DIRT.getDefaultState());
        world.setBlockState(farm.east(), Blocks.FARMLAND.getDefaultState());

        try {
            int changed = 0;

            for (int round = 0; round < 8; round++) {
                changed += SoilRestore.pass(world, colony);
            }

            context.assertTrue(world.getBlockState(topDirt).isOf(Blocks.SAND),
                    "Regra 363: a terra no chão de areia ficou (" + changed + " trocas)");
            context.assertTrue(world.getBlockState(underSand).isOf(Blocks.SANDSTONE),
                    "Regra 363: a terra debaixo da areia ficou (" + changed + " trocas)");
            context.assertTrue(world.getBlockState(farm).isOf(Blocks.DIRT), "a terra da roça foi trocada");
        } finally {
            SoilRestore.forget(colony.id());
            clean(world, base);
            force(world, base, false);
        }

        context.complete();
    }

    private static BlockPos sandFloor(TestContext context) {
        BlockPos base = context.getAbsolutePos(new BlockPos(0, 1, 0));

        sandFloor(context.getWorld(), base);

        return base;
    }

    /** Chão de 9 × 9: pedra, arenito e duas camadas de areia, com ar em cima. */
    private static void sandFloor(ServerWorld world, BlockPos base) {
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                world.setBlockState(base.add(x, -1, z), Blocks.STONE.getDefaultState());
                world.setBlockState(base.add(x, 0, z), Blocks.SANDSTONE.getDefaultState());
                world.setBlockState(base.add(x, 1, z), Blocks.SAND.getDefaultState());
                world.setBlockState(base.add(x, 2, z), Blocks.SAND.getDefaultState());
                for (int y = 3; y <= 8; y++) {
                    world.setBlockState(base.add(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static void clean(TestContext context) {
        clean(context.getWorld(), context.getAbsolutePos(new BlockPos(0, 1, 0)));
    }

    private static void clean(ServerWorld world, BlockPos base) {
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                for (int y = -1; y <= 8; y++) {
                    world.setBlockState(base.add(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
        VillageColonyMod.LOGGER.debug("BiomeSoilGameTest limpo");
    }

    private static void force(ServerWorld world, BlockPos base, boolean forced) {
        for (int cx = (base.getX() >> 4) - 1; cx <= ((base.getX() + SIDE) >> 4) + 1; cx++) {
            for (int cz = (base.getZ() >> 4) - 1; cz <= ((base.getZ() + SIDE) >> 4) + 1; cz++) {
                world.setChunkForced(cx, cz, forced);
            }
        }
    }
}
