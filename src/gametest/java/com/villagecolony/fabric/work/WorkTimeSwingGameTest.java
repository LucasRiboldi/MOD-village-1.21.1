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
}
