package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/** Um golpe vale como trabalho numa amostra só: o braço do aldeão desce depois de lido. */
public class WorkTimeSwingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours", tickLimit = 40)
    public void aSwingCountsOnceNotForever(TestContext context) {
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 1, 1));

        villager.swingHand(Hand.MAIN_HAND);

        // Vinte tiques depois, o braço do aldeão continua erguido no servidor.
        context.waitAndRun(20, () -> {
            context.assertTrue(WorkTime.swungSinceLastSample(villager), "o golpe não foi visto na amostra");
            context.assertTrue(!WorkTime.swungSinceLastSample(villager),
                    "sem golpe novo, a amostra seguinte ainda contou trabalho");

            villager.swingHand(Hand.MAIN_HAND);
            context.assertTrue(WorkTime.swungSinceLastSample(villager), "o segundo golpe não foi visto");
            context.complete();
        });
    }

    /** Regra 270: o aldeão sem ofício do mod fica fora da conta do expediente; o pastor entra. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_time_unemployed", tickLimit = 20)
    public void aVillagerWithoutTradeStaysOutOfTheWorkTimeAccount(TestContext context) {
        net.minecraft.server.world.ServerWorld world = context.getWorld();
        com.villagecolony.gametest.ColonyFixture fixture =
                com.villagecolony.gametest.ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            com.villagecolony.core.worker.model.Worker shepherd = fixture.equippedWorker(context,
                    com.villagecolony.core.worker.model.ProfessionType.SHEPHERD, new BlockPos(3, 1, 3));
            VillagerEntity idle = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, 1, 5));

            fixture.owning(idle.getUuid());
            com.villagecolony.VillageColonyMod.WORKERS.register(idle.getUuid(), fixture.colony().id());

            WorkTime.sample(world);

            java.util.Map<String, java.util.Map<WorkTime.State, Integer>> window = WorkTime.lastWindowOf(fixture.colony().id());

            context.assertTrue(window.containsKey("SHEPHERD"), "o pastor ficou fora da conta: " + window + " " + shepherd);
            context.assertFalse(window.containsKey("NONE"), "Regra 270: o sem ofício entrou na conta do expediente: " + window);
        } finally {
            world.setTimeOfDay(before);
            fixture.cleanUp();
        }

        context.complete();
    }
}
