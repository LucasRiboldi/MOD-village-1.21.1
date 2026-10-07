package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/** Fora do expediente o mod só solta quem anda para um alvo dele (ADR-037 V1). */
public class OffHoursReleaseGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "off_hours_release", tickLimit = 20)
    public void theUnemployedKeepsItsVanillaWalkAtNight(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();
        VillagerEntity idle = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        VillagerEntity miner = context.spawnEntity(EntityType.VILLAGER, new BlockPos(4, 1, 2));

        try {
            fixture.owning(idle.getUuid()).owning(miner.getUuid());
            VillageColonyMod.WORKERS.register(idle.getUuid(), colony.id());
            VillageColonyMod.WORKERS.register(miner.getUuid(), colony.id()).assign(ProfessionType.MINER);
            context.getWorld().setTimeOfDay(WorkClock.REST + 1_000);

            BlockPos bed = context.getAbsolutePos(new BlockPos(6, 1, 6));
            idle.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(bed, 0.5f, 1));
            miner.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(bed, 0.5f, 1));
            WorkTargets.set(miner.getUuid(), bed);

            ColonyCycleRunner.runOngoingWork(context.getWorld(), colony);

            context.assertTrue(idle.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET).isPresent(),
                    "o desempregado perdeu a caminhada do Vanilla à noite");
            context.assertTrue(miner.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET).isEmpty()
                            && WorkTargets.of(miner.getUuid()).isEmpty(),
                    "o mineiro continuou andando para o alvo do mod fora do expediente");
        } finally {
            WorkTargets.clear(miner.getUuid());
            fixture.cleanUp();
        }

        context.complete();
    }
}
