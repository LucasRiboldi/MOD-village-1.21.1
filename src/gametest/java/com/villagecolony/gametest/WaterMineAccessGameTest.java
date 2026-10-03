package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.work.MineRock;
import com.villagecolony.fabric.work.MineTrouble;
import com.villagecolony.fabric.work.WaterMineAccess;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** A rota aquática existe no mundo e não deixa água dentro do corredor. */
public class WaterMineAccessGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "water_mine", tickLimit = 20)
    public void anExhaustedWaterMineReopensThroughTheSealedStaircase(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(7, 10, 7));
        UUID colonyId = UUID.randomUUID();
        BlockPos oldEntry = center.north(30);
        WaterMineAccess.Route route = new WaterMineAccess.Route(center.south(12), Side.SOUTH, 12);

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.setBlockState(center.add(x, 0, z), Blocks.WATER.getDefaultState());
            }
        }
        for (BlockPos at : route.shell()) {
            world.setBlockState(at, Blocks.WATER.getDefaultState());
        }
        for (BlockPos at : route.interior()) {
            world.setBlockState(at, Blocks.WATER.getDefaultState());
        }
        for (BlockPos at : route.stairs()) {
            world.setBlockState(at, Blocks.WATER.getDefaultState());
        }
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                world.setBlockState(route.entry().add(x, 0, z), Blocks.STONE.getDefaultState());
            }
        }
        world.setBlockState(oldEntry, Blocks.COBBLESTONE.getDefaultState());
        loadRouteChunks(world, route);
        assertRouteVolumeCanBePlaced(context, world, route);

        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                BlockPos exit = route.entry().add(x, 0, z);
                context.assertTrue(
                        MineRock.isDiggableRock(world, exit),
                        "a saída de rocha não é escavável em " + exit.toShortString());
            }
        }

        context.assertTrue(
                WaterMineAccess.find(world, center, Side.SOUTH).isPresent(),
                "a rota selada deveria ser elegível antes de esgotar a mina");

        Mine exhausted = Mine.open(
                colonyId,
                MineShaft.from(MinecraftTypeAdapter.toColonyPos(oldEntry), Side.NORTH));
        VillageColonyMod.MINES.restore(exhausted);

        try {
            MineTrouble.abandonAtBottom(world, colonyId, exhausted, center);

            Mine replacement = VillageColonyMod.MINES.of(colonyId).orElseThrow();
            context.assertTrue(
                    MinecraftTypeAdapter.toBlockPos(replacement.entry()).equals(route.entry()),
                    "a mina esgotada na água não foi transferida para a entrada da escada de vidro");
            context.assertTrue(
                    world.getBlockState(route.stairs().iterator().next()).isOf(Blocks.STONE_BRICK_STAIRS),
                    "a escada selada não foi construída");
            context.assertTrue(
                    world.getBlockState(oldEntry).isOf(Blocks.COBBLESTONE),
                    "a mina normal anterior foi alterada ao abrir o acesso aquático");
        } finally {
            VillageColonyMod.MINES.removeOfColony(colonyId);
        }

        context.complete();
    }

    private static void loadRouteChunks(ServerWorld world, WaterMineAccess.Route route) {
        for (BlockPos at : route.stairs()) {
            world.getChunk(at);
        }
        for (BlockPos at : route.interior()) {
            world.getChunk(at);
        }
        for (BlockPos at : route.shell()) {
            world.getChunk(at);
        }
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                world.getChunk(route.entry().add(x, 0, z));
            }
        }
    }

    private static void assertRouteVolumeCanBePlaced(
            TestContext context, ServerWorld world, WaterMineAccess.Route route) {
        for (BlockPos at : route.stairs()) {
            context.assertTrue(
                    world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4) != null,
                    "chunk da escada não está carregado em " + at.toShortString());
            context.assertFalse(
                    BlockProtection.isVillageOriginal(world, at)
                            || BlockProtection.isColonyBuilt(at)
                            || BlockProtection.isPlayerPlaced(world.getBlockState(at)),
                    "a escada toca bloco protegido em " + at.toShortString());
            context.assertTrue(
                    (at.getY() == route.entry().getY() && MineRock.isDiggableRock(world, at))
                            || world.getBlockState(at).isReplaceable()
                            || !world.getFluidState(at).isEmpty(),
                    "a escada não pode ocupar " + at.toShortString());
        }
        for (BlockPos at : route.interior()) {
            context.assertTrue(
                    world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4) != null,
                    "chunk do interior não está carregado em " + at.toShortString());
            context.assertTrue(
                    (at.getY() == route.entry().getY() && MineRock.isDiggableRock(world, at))
                            || world.getBlockState(at).isReplaceable()
                            || !world.getFluidState(at).isEmpty(),
                    "o interior não pode ocupar " + at.toShortString());
        }
        for (BlockPos at : route.shell()) {
            context.assertTrue(
                    world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4) != null,
                    "chunk da concha não está carregado em " + at.toShortString());
            context.assertTrue(
                    (at.getY() == route.entry().getY() && MineRock.isDiggableRock(world, at))
                            || world.getBlockState(at).isReplaceable()
                            || !world.getFluidState(at).isEmpty(),
                    "a concha não pode ocupar " + at.toShortString());
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "water_mine", tickLimit = 20)
    public void theSealedRoutePlacesThreeWideStairsWithoutWaterInside(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos surface = context.getAbsolutePos(new BlockPos(7, 10, 7));
        WaterMineAccess.Route route = new WaterMineAccess.Route(surface, Side.NORTH, 4);

        for (BlockPos at : route.shell()) {
            world.setBlockState(at, Blocks.WATER.getDefaultState());
        }
        for (BlockPos at : route.interior()) {
            world.setBlockState(at, Blocks.WATER.getDefaultState());
        }
        for (BlockPos at : route.stairs()) {
            world.setBlockState(at, Blocks.WATER.getDefaultState());
        }
        for (BlockPos stair : route.stairs()) {
            if (stair.getY() == route.entry().getY()) {
                world.setBlockState(stair, Blocks.STONE.getDefaultState());
            }
        }

        context.assertTrue(route.place(world), "a rota selada recusou um volume de água válido");

        for (BlockPos stair : route.stairs()) {
            context.assertTrue(
                    world.getBlockState(stair).isOf(Blocks.STONE_BRICK_STAIRS),
                    "faltou degrau na rota em " + stair.toShortString());
        }
        for (BlockPos interior : route.interior()) {
            context.assertTrue(
                    world.getFluidState(interior).isEmpty(),
                    "água entrou no corredor em " + interior.toShortString());
        }
        for (BlockPos shell : route.shell()) {
            context.assertTrue(
                    world.getBlockState(shell).isOf(Blocks.GLASS),
                    "faltou vidro de proteção em " + shell.toShortString());
        }

        context.complete();
    }
}
