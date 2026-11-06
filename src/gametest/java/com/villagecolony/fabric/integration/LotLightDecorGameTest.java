package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.content.ModContent;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** Vela e tocha soltas não impedem a escolha do lote, e saem quando a obra abre — Regra 212. */
public class LotLightDecorGameTest implements FabricGameTest {

    private static final ColonyPos SIZE = new ColonyPos(5, 5, 5);

    private static void ground(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin.add(-2, -1, -2), origin.add(7, 30, 7))) {
            world.setBlockState(at, at.getY() <= origin.getY() ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState(), 18);
        }
    }

    private static void clean(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin.add(-2, -1, -2), origin.add(7, 30, 7))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_light_decor", tickLimit = 20)
    public void aCandleOrATorchDoesNotRefuseTheLot(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(2, 60, 2));

        ground(world, origin);

        try {
            UUID colony = UUID.randomUUID();
            List<BlockState> decor = List.of(Blocks.CANDLE.getDefaultState(), Blocks.RED_CANDLE.getDefaultState(),
                    Blocks.TORCH.getDefaultState(), ModContent.COLONY_TORCH.getDefaultState());

            context.assertTrue(LotLevel.flatGroundAt(world, colony, origin.getX(), origin.getZ(), origin.getY(), origin.getY(), SIZE).isPresent(),
                    "o lote limpo devia servir");

            for (BlockState state : decor) {
                world.setBlockState(origin.add(2, 1, 2), state, 18);

                context.assertTrue(LotLevel.flatGroundAt(world, colony, origin.getX(), origin.getZ(), origin.getY(), origin.getY(), SIZE).isPresent(),
                        "Regra 212: " + state + " recusou o lote");
            }

            world.setBlockState(origin.add(2, 1, 2), Blocks.OAK_PLANKS.getDefaultState(), 18);

            context.assertTrue(LotLevel.flatGroundAt(world, colony, origin.getX(), origin.getZ(), origin.getY(), origin.getY(), SIZE).isEmpty(),
                    "um bloco de verdade tem de continuar recusando o lote");
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    /** A tocha das almas cancela a obra (Regra 108): continua contando no lote. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_light_soul", tickLimit = 20)
    public void aSoulTorchKeepsItsMeaning(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(2, 100, 2));

        ground(world, origin);

        try {
            world.setBlockState(origin.add(2, 1, 2), Blocks.SOUL_TORCH.getDefaultState(), 18);

            context.assertTrue(LotLevel.flatGroundAt(world, UUID.randomUUID(), origin.getX(), origin.getZ(), origin.getY(), origin.getY(), SIZE).isEmpty(),
                    "Regra 108: a tocha das almas passou como vela");
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_light_open", tickLimit = 20)
    public void openingTheWorkTakesTheLightsOutAndNothingElse(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(2, 80, 2));

        ground(world, origin);

        try {
            BlockPos candle = origin.add(1, 1, 1);
            BlockPos torch = origin.add(3, 1, 3);
            BlockPos stone = origin.add(2, 1, 2);
            BlockPos outside = origin.add(6, 1, 6);

            world.setBlockState(candle, Blocks.CANDLE.getDefaultState(), 18);
            world.setBlockState(torch, ModContent.COLONY_TORCH.getDefaultState(), 18);
            world.setBlockState(stone, Blocks.STONE.getDefaultState(), 18);
            world.setBlockState(outside, Blocks.CANDLE.getDefaultState(), 18);

            LotClearance.clearLightDecor(world, new ColonyPos(origin.getX(), origin.getY() + 1, origin.getZ()), SIZE);

            context.assertTrue(world.getBlockState(candle).isAir(), "Regra 212: a vela ficou na obra");
            context.assertTrue(world.getBlockState(torch).isAir(), "Regra 212: a tocha ficou na obra");
            context.assertTrue(world.getBlockState(stone).isOf(Blocks.STONE), "Regra 3: a obra tirou um bloco que não é vela");
            context.assertTrue(world.getBlockState(outside).isOf(Blocks.CANDLE), "Regra 212: a obra tirou a vela de fora do lote");
        } finally {
            clean(world, origin);
        }

        context.complete();
    }
}
