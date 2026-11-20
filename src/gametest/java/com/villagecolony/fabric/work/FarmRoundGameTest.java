package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.CropPatch;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/** O pasto do curral não é roça (Regra 332), e o fazendeiro sem tarefa faz a ronda da roça (Regra 333). */
public class FarmRoundGameTest implements FabricGameTest {

    private static final int FLOOR = 28;

    /** Regra 332: o trigo maduro dentro do curral não é colheita do fazendeiro; o de fora é. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_round_pen", tickLimit = 20)
    public void theWheatInsideAPenIsNotTheFarmersCrop(TestContext context) {
        ServerWorld world = context.getWorld();
        int y = 230;

        try {
            for (int x = 0; x <= FLOOR; x++) {
                for (int z = 0; z <= FLOOR; z++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)), Blocks.GRASS_BLOCK.getDefaultState());
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y + 1, z)), Blocks.AIR.getDefaultState());

                    boolean ring = (x == 1 || x == 7 || z == 1 || z == 7) && x >= 1 && x <= 7 && z >= 1 && z <= 7;

                    if (ring) {
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, y + 1, z)), Blocks.OAK_FENCE.getDefaultState());
                    }
                }
            }

            BlockPos gate = context.getAbsolutePos(new BlockPos(4, y + 1, 1));
            world.setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState());

            // O trigo maduro só no meio do curral.
            BlockPos inPen = context.getAbsolutePos(new BlockPos(4, y + 1, 4));
            world.setBlockState(inPen.down(), Blocks.FARMLAND.getDefaultState());
            world.setBlockState(inPen, Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, CropBlock.MAX_AGE));

            BlockPos low = context.getAbsolutePos(new BlockPos(0, y - 2, 0));
            BlockPos high = context.getAbsolutePos(new BlockPos(FLOOR, y + 4, FLOOR));
            Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(gate));

            colony.measure(new VillageBounds(Math.min(low.getX(), high.getX()), low.getY(), Math.min(low.getZ(), high.getZ()),
                    Math.max(low.getX(), high.getX()), high.getY(), Math.max(low.getZ(), high.getZ())));
            context.assertTrue(ShepherdPens.pensOf(world, colony).size() == 1, "o curral não foi lido");

            BlockPos center = context.getAbsolutePos(new BlockPos(14, y + 1, 14));
            UUID survey = UUID.randomUUID();
            Optional<BlockPos> ripe = drain(world, survey, center);

            context.assertTrue(ripe.isEmpty(), "Regra 332: o trigo do curral virou colheita do fazendeiro: " + ripe);

            // O mesmo trigo fora do curral é colheita.
            BlockPos outside = context.getAbsolutePos(new BlockPos(20, y + 1, 20));
            world.setBlockState(outside.down(), Blocks.FARMLAND.getDefaultState());
            world.setBlockState(outside, Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, CropBlock.MAX_AGE));
            CropPatch.forget(survey);

            context.assertTrue(drain(world, survey, center).filter(outside::equals).isPresent(),
                    "o trigo de fora do curral não foi visto");
            CropPatch.forget(survey);
        } finally {
            for (int x = 0; x <= FLOOR; x++) {
                for (int z = 0; z <= FLOOR; z++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y + 1, z)), Blocks.AIR.getDefaultState());
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)), Blocks.AIR.getDefaultState());
                }
            }
        }

        context.complete();
    }

    /** A varredura é dividida em passadas; repete até ela terminar. */
    private static Optional<BlockPos> drain(ServerWorld world, UUID survey, BlockPos center) {
        for (int pass = 0; pass < 64; pass++) {
            CropPatch.Field field = CropPatch.survey(world, survey, center, 20);

            if (field.ripe().isPresent() || !field.incomplete()) {
                return field.ripe();
            }
        }

        return Optional.empty();
    }

    /** Regra 333: sem tarefa de comida na fila, o fazendeiro faz a ronda da roça; sem nada a fazer, descansa. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_round_clock", tickLimit = 20)
    public void aFarmerWithoutATaskWorksTheFarm(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            var farmer = fixture.equippedWorker(context, ProfessionType.FARMER, new BlockPos(2, 1, 2));

            FarmerWork.run(world, colony);

            context.assertTrue(FarmRound.isOnRound(farmer.villagerId()), "Regra 333: sem tarefa, o fazendeiro ficou parado");

            FarmRound.nothingLeft(world, farmer.villagerId());
            context.assertFalse(FarmRound.isOnRound(farmer.villagerId()), "a ronda não acabou com nada a fazer");
            context.assertTrue(FarmRound.offer(world, colony, java.util.Set.of()).isEmpty(),
                    "Regra 333: a ronda recomeçou sem o descanso");
        } finally {
            world.setTimeOfDay(before);
            fixture.cleanUp();
        }

        context.complete();
    }
}
