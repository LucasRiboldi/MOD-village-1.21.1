package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.WorkClock;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Regra 358: o Zelador que está calçando ainda limpa a porta tapada e leva o plano de alturas adiante. Calçar não tira a
 * vez do resto do dia dele.
 */
public class GroundKeeperPavingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "keeper_paving", tickLimit = 20)
    public void theKeeperWhoPavesStillLevels(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        VillagerEntity keeper = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        long before = world.getTimeOfDay();

        PathPaving.giveTurn(colonyId, keeper.getUuid());

        try {
            // O relógio é do mundo todo: volta antes do fim deste tique, e os testes vizinhos não o veem mudado.
            world.setTimeOfDay(Math.floorDiv(before, WorkClock.DAY) * WorkClock.DAY + 1_000);

            context.assertTrue(PathPaving.isPaving(keeper.getUuid()), "o teste não pôs o Zelador a calçar");
            context.assertFalse(IdleWorkers.isFree(world, keeper, null),
                    "calçando, ele já contava como livre: o teste não mede nada");
            context.assertTrue(GroundKeeper.mayWork(world, keeper),
                    "Regra 358: calçando, o Zelador perde a porta tapada e o plano de alturas");
        } finally {
            world.setTimeOfDay(before);
            PathPaving.takeTurn(colonyId);
            keeper.discard();
        }

        context.complete();
    }
}
