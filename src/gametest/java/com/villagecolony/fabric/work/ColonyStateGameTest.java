package com.villagecolony.fabric.work;

import com.google.gson.JsonObject;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** O retrato da vila (VC_STATE) traz cada assunto e fica gravado no arquivo de análise do mundo. */
public class ColonyStateGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_state", tickLimit = 20)
    public void theStateHasEverySubjectAndIsWrittenToTheWorldFolder(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2))));
        UUID mason = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(mason, colony.id()).assign(ProfessionType.MASON);

        try {
            JsonObject state = ColonyState.of(context.getWorld(), colony);

            for (String subject : new String[] {"people", "time", "stock", "build", "tasks", "mine", "village"}) {
                context.assertTrue(state.has(subject), "o retrato não tem " + subject + ": " + state);
            }
            context.assertTrue(state.getAsJsonObject("people").get("villagers").getAsInt() == 1,
                    "o retrato devia contar o pedreiro: " + state.get("people"));
            context.assertTrue(state.getAsJsonObject("people").getAsJsonObject("trades").get("MASON").getAsInt() == 1,
                    "o retrato devia trazer o ofício: " + state.get("people"));

            Path file = AnalyticsLog.record(context.getWorld(), colony, "test").orElseThrow();

            context.assertTrue(Files.readString(file).contains(colony.id().toString().substring(0, 8)),
                    "o arquivo de análise não recebeu o retrato: " + file);
        } catch (java.io.IOException failed) {
            throw new AssertionError(failed);
        } finally {
            VillageColonyMod.WORKERS.remove(mason);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
