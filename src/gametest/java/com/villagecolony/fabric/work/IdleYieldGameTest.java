package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.IdleYield;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** O ofício parado por duas janelas cede uma pessoa sem tarefa (ADR-038 P1, B4). */
public class IdleYieldGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours", tickLimit = 20)
    public void anIdleTradeGivesUpOneWorker(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        UUID colony = fixture.colony().id();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        try {
            fixture.owning(first).owning(second);
            VillageColonyMod.WORKERS.register(first, colony).assign(ProfessionType.LUMBERJACK);
            VillageColonyMod.WORKERS.register(second, colony).assign(ProfessionType.LUMBERJACK);

            EnumMap<WorkTime.State, Integer> idle = new EnumMap<>(WorkTime.State.class);
            idle.put(WorkTime.State.IDLE, 100);

            for (int window = 0; window < IdleYield.WINDOWS; window++) {
                WorkTime.yieldIdleTrades(colony, Map.of("LUMBERJACK", idle));
            }

            List<Worker> stayed = VillageColonyMod.WORKERS.ofColony(colony).stream()
                    .filter(worker -> worker.profession().filter(ProfessionType.LUMBERJACK::equals).isPresent())
                    .toList();

            context.assertTrue(stayed.size() == 1,
                    "duas janelas 100% paradas e o ofício ficou com " + stayed.size() + " lenhadores");
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }
}
