package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Regra 285: o aldeão preso na água a céu aberto, com a margem dois blocos acima da água (alta demais para o pulo),
 * sai: primeiro a ida à margem, depois o gelo sob o pulo.
 */
public class WaterRescueGameTest implements FabricGameTest {

    /**
     * Poço de 3 × 3 (x e z de 3 a 5) com água em y 1 e 2; em volta, pedra até y 3, então o chão seco fica dois blocos
     * acima da superfície.
     */
    private static void pool(TestContext context) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                boolean water = x >= 3 && x <= 5 && z >= 3 && z <= 5;
                context.setBlockState(new BlockPos(x, 0, z), Blocks.STONE.getDefaultState());
                for (int y = 1; y <= 3; y++) {
                    context.setBlockState(new BlockPos(x, y, z),
                            water ? (y <= 2 ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState())
                                    : Blocks.STONE.getDefaultState());
                }
                for (int y = 4; y <= 6; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "water_rescue_ice", tickLimit = 1200)
    public void aVillagerStuckInWaterGetsOut(TestContext context) {
        pool(context);
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 4, 1));
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(4, 2, 4));
        boolean[] registered = {false};
        boolean[] done = {false};

        context.runAtEveryTick(() -> {
            if (done[0]) {
                return;
            }
            if (!WorkClock.isWorkTime(context.getWorld().getTimeOfDay())) {
                context.getWorld().setTimeOfDay(1_000);
            }
            if (!registered[0]) {
                registered[0] = true;
                fixture.owning(villager.getUuid());
                VillageColonyMod.WORKERS.register(villager.getUuid(), fixture.colony().id()).assign(ProfessionType.MASON);
            }
            BlockPos at = context.getRelativePos(villager.getBlockPos());
            // Saiu: de pé em chão firme, fora d'água (o pulo acima da superfície não conta).
            boolean out = !villager.isTouchingWater() && villager.isOnGround()
                    && context.getWorld().getFluidState(villager.getBlockPos().down()).isEmpty() && at.getY() >= 3;
            if (out) {
                done[0] = true;
                fixture.cleanUp();
                villager.discard();
                context.complete();
            }
        });

        context.runAtTick(1150, () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            BlockPos at = context.getRelativePos(villager.getBlockPos());
            int ticks = WaterRescue.ticksInWater(villager.getUuid());
            fixture.cleanUp();
            villager.discard();
            context.assertTrue(false, "Regra 285: o aldeão continua na água: em " + at.toShortString()
                    + ", " + ticks + " tiques na água");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "water_rescue_shore", tickLimit = 20)
    public void theShoreIsTheNearestDryFloor(TestContext context) {
        pool(context);
        context.waitAndRun(1, () -> {
            var shore = WaterRescue.shoreNear(context.getWorld(), context.getAbsolutePos(new BlockPos(4, 2, 4)));
            context.assertTrue(shore.isPresent() && context.getRelativePos(shore.get()).getY() == 4,
                    "Regra 285: a margem achada não é o chão seco em y 4: " + shore.map(context::getRelativePos));
            context.complete();
        });
    }
}
