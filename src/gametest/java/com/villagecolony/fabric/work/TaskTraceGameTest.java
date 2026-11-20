package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Regra 344, telemetria T1: o diário leva o número curto da tarefa em cada passo dela e em cada ação feita enquanto ela
 * está com o aldeão — o {@code task_report.py} segue a tarefa do pedido ao fim.
 */
public class TaskTraceGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "task_trace", tickLimit = 20)
    public void everyStepAndActionOfATaskCarriesItsId(TestContext context) {
        Path before = ActionJournal.file();
        UUID colony = UUID.randomUUID();
        UUID villager = UUID.randomUUID();

        try {
            Path journal = Files.createTempFile("villagecolony-actions-trace", ".jsonl");
            ActionJournal.flush();
            ActionJournal.writeTo(journal);

            VillageColonyMod.WORKERS.register(villager, colony).assign(ProfessionType.MINER);

            Task task = Task.create(colony, TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                    ResourceType.COBBLESTONE, 4);
            task.reserveFor(villager);
            task.start();
            ActionJournal.action(villager, "MINED", "COBBLESTONE", 3, new BlockPos(1, 2, 3), "");
            task.complete();
            ActionJournal.action(villager, "PICKED_UP", "COBBLESTONE", 1, null, "");
            ActionJournal.flush();

            String id = "\"id\":\"" + task.id().toString().substring(0, 8) + "\"";
            List<String> lines = Files.readAllLines(journal, StandardCharsets.UTF_8).stream()
                    .filter(line -> line.contains(villager.toString().substring(0, 8)))
                    .toList();

            context.assertTrue(lines.size() == 5, "o diário devia ter 5 linhas deste aldeão: " + lines);

            for (String line : lines.subList(0, 4)) {
                context.assertTrue(line.contains(id), "Regra 344: a linha da tarefa saiu sem o número dela: " + line);
            }

            context.assertFalse(lines.get(4).contains("\"id\":"),
                    "Regra 344: a ação depois do fim da tarefa ainda levou o número dela: " + lines.get(4));
            Files.deleteIfExists(journal);
        } catch (IOException failure) {
            throw new AssertionError("o teste não conseguiu usar o disco", failure);
        } finally {
            ActionJournal.writeTo(before);
            VillageColonyMod.WORKERS.remove(villager);
        }

        context.complete();
    }
}
