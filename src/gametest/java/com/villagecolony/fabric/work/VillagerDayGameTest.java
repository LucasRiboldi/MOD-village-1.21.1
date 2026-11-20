package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Regra 344, telemetria T5: o dia de cada aldeão de profissão — expediente, sono, acordado fora do expediente, sem cama
 * e a menor vida vista — na linha {@code VC_VILLAGER_DAY}.
 */
public class VillagerDayGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "villager_day", tickLimit = 20)
    public void theDayLineSaysWorkSleepAwakeNoBedAndTheLowestHealth(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 2, 1));
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 3));

        try {
            villager.setAiDisabled(true);
            VillageColonyMod.WORKERS.register(villager.getUuid(), fixture.colony().id()).assign(ProfessionType.MASON);
            fixture.owning(villager.getUuid());
            VillagerDay.forget(villager.getUuid());

            for (int second = 0; second < 30; second++) {
                VillagerDay.sample(villager, true);
            }

            villager.setHealth(12);

            for (int second = 0; second < 10; second++) {
                VillagerDay.sample(villager, false);
            }

            String line = VillagerDay.line(fixture.colony().id(), 3);
            String id = villager.getUuid().toString().substring(0, 8);

            context.assertTrue(line.startsWith("VC_VILLAGER_DAY version=1 colony="), "Regra 344: o começo: " + line);
            context.assertTrue(line.contains(" " + id + "=MASON,30,0,10,40,12"),
                    "Regra 344: 30 s no expediente, 10 acordado fora, 40 sem cama, vida mínima 12: " + line);
        } finally {
            VillagerDay.forget(villager.getUuid());
            villager.discard();
            fixture.cleanUp();
        }

        context.complete();
    }
}
