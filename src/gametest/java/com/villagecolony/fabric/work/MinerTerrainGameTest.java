package com.villagecolony.fabric.work;

import com.villagecolony.core.movement.Cell;
import com.villagecolony.core.movement.DetourMoves;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O mineiro não constrói caminho para fora da montanha — regra do autor de 2026-10-22: no deserto o
 * desvio dele pôs piso no ar ("0 dug, 8 placed") a caminho de uma boca no alto da duna. Cenários quarenta
 * blocos acima do piso, longe das outras arenas.
 */
public class MinerTerrainGameTest implements FabricGameTest {

    private static final int Y = 40;

    /** Duas plataformas no céu com um vão: o desvio de sempre faz a ponte, o do mineiro não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_terrain")
    public void theMinerDoesNotBridgeAGapInTheOpen(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 6; x++) {
            if (x != 3) {
                context.setBlockState(new BlockPos(x, Y, 2), Blocks.DIRT.getDefaultState());
            }
        }

        BlockPos from = context.getAbsolutePos(new BlockPos(0, Y + 1, 2));
        BlockPos to = context.getAbsolutePos(new BlockPos(6, Y + 1, 2));

        Optional<DetourWalker> usual = DetourWalker.plan(
                world, UUID.randomUUID(), from, to, to::equals, Set.of(), false);
        Optional<DetourWalker> miner = DetourWalker.plan(
                world, UUID.randomUUID(), from, to, to::equals, Set.of(), false,
                MinerTerrain::of, false, DetourMoves.Costs.MINER);

        context.assertTrue(usual.isPresent(), "a montagem falhou: o desvio de sempre não fez a ponte sobre o vão");
        context.assertTrue(miner.isEmpty(), "o mineiro planejou pôr piso no ar para atravessar o vão");

        context.complete();
    }

    /** Dentro da montanha o vazio ainda recebe bloco; ao céu aberto, não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_terrain")
    public void onlyTheOpenSkyRefusesABlock(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int y = Y; y <= Y + 4; y++) {
            context.setBlockState(new BlockPos(2, y, 2), Blocks.STONE.getDefaultState());
        }

        context.setBlockState(new BlockPos(2, Y + 1, 2), Blocks.AIR.getDefaultState());

        MinerTerrain terrain = MinerTerrain.of(world);
        BlockPos cave = context.getAbsolutePos(new BlockPos(2, Y + 1, 2));
        BlockPos sky = context.getAbsolutePos(new BlockPos(2, Y + 6, 2));

        context.assertTrue(terrain.at(MinecraftTypeAdapter.toColonyPos(cave)) == Cell.OPEN,
                "o vazio coberto pela rocha devia receber bloco: " + terrain.at(MinecraftTypeAdapter.toColonyPos(cave)));
        context.assertTrue(terrain.at(MinecraftTypeAdapter.toColonyPos(sky)) == Cell.PASSAGE,
                "o céu aberto devia ser só passagem: " + terrain.at(MinecraftTypeAdapter.toColonyPos(sky)));

        context.complete();
    }
}
