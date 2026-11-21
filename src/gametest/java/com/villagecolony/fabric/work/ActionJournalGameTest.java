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
 * O diário de ações grava uma linha JSON por passo de tarefa e por ação
 * concreta, com a profissão de quem fez — pedido do autor, 2026-10-08.
 */
public class ActionJournalGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "action_journal", tickLimit = 20)
    public void theJournalWritesEveryStepWithTheProfession(TestContext context) {
        Path before = ActionJournal.file();
        UUID colony = UUID.randomUUID();
        UUID villager = UUID.randomUUID();

        try {
            Path journal = Files.createTempFile("villagecolony-actions-test", ".jsonl");
            ActionJournal.flush();
            ActionJournal.writeTo(journal);

            VillageColonyMod.WORKERS.register(villager, colony).assign(ProfessionType.MINER);

            Task task = Task.create(colony, TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                    ResourceType.COBBLESTONE, 4);
            task.reserveFor(villager);
            task.start();
            ActionJournal.action(villager, "MINED", "COBBLESTONE", 3, new BlockPos(1, 2, 3), "say \"hi\"");
            task.complete();
            ActionJournal.flush();

            List<String> lines = Files.readAllLines(journal, StandardCharsets.UTF_8).stream()
                    .filter(line -> line.contains(villager.toString().substring(0, 8)))
                    .toList();

            context.assertTrue(lines.size() == 4, "o diário devia ter 4 linhas deste aldeão, teve " + lines);
            context.assertTrue(lines.get(0).contains("\"action\":\"TASK_TAKEN\"")
                            && lines.get(0).contains("\"prof\":\"MINER\""),
                    "a primeira linha devia ser a tarefa pega pelo mineiro: " + lines.get(0));
            context.assertTrue(lines.get(2).contains("\"action\":\"MINED\"")
                            && lines.get(2).contains("\"n\":3") && lines.get(2).contains("\"x\":1")
                            && lines.get(2).contains("say \\\"hi\\\""),
                    "a ação concreta saiu sem quantidade, posição ou aspas escapadas: " + lines.get(2));
            context.assertTrue(lines.get(3).contains("\"action\":\"TASK_DONE\"")
                            && lines.get(3).contains("\"ticks\":"),
                    "o fim da tarefa devia dizer quanto durou: " + lines.get(3));
            Files.deleteIfExists(journal);
        } catch (IOException failure) {
            throw new AssertionError("o teste não conseguiu usar o disco", failure);
        } finally {
            ActionJournal.writeTo(before);
            VillageColonyMod.WORKERS.remove(villager);
        }

        context.complete();
    }

    /** A soltura diz por quê; sem motivo, cai no estado de onde saiu — AUD-25. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "action_journal", tickLimit = 20)
    public void aReleaseWritesItsReason(TestContext context) {
        Path before = ActionJournal.file();
        UUID colony = UUID.randomUUID();
        UUID villager = UUID.randomUUID();

        try {
            Path journal = Files.createTempFile("villagecolony-actions-test", ".jsonl");
            ActionJournal.flush();
            ActionJournal.writeTo(journal);

            VillageColonyMod.WORKERS.register(villager, colony).assign(ProfessionType.SMELTER);

            Task task = Task.create(colony, TaskType.SMELT_MATERIAL, TaskPriority.PRODUCTION,
                    ResourceType.GLASS, 4);
            task.reserveFor(villager);
            task.release("none of 3 colony chests had minecraft:sand to smelt");
            task.reserveFor(villager);
            task.release();
            ActionJournal.flush();

            List<String> released = Files.readAllLines(journal, StandardCharsets.UTF_8).stream()
                    .filter(line -> line.contains(villager.toString().substring(0, 8)))
                    .filter(line -> line.contains("\"action\":\"TASK_RELEASED\""))
                    .toList();

            context.assertTrue(released.size() == 2, "o diário devia ter 2 solturas, teve " + released);
            context.assertTrue(released.get(0).contains("\"detail\":\"none of 3 colony chests had minecraft:sand to smelt\""),
                    "a soltura com motivo saiu sem ele: " + released.get(0));
            context.assertTrue(released.get(1).contains("\"detail\":\"RESERVED\""),
                    "a soltura sem motivo devia dizer o estado de onde saiu: " + released.get(1));
            Files.deleteIfExists(journal);
        } catch (IOException failure) {
            throw new AssertionError("o teste não conseguiu usar o disco", failure);
        } finally {
            ActionJournal.writeTo(before);
            VillageColonyMod.WORKERS.remove(villager);
        }

        context.complete();
    }
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_refusal_journal", tickLimit = 20)
    public void farmerRefusalRecordsCropWorkerAndReason(TestContext context) throws Exception {
        Path before = ActionJournal.file();
        Path journal = Files.createTempFile("farmer-refusal", ".jsonl");
        var farmer = com.villagecolony.gametest.ColonyFixture.spawnAtWorld(context,
                net.minecraft.entity.EntityType.VILLAGER, context.getAbsolutePos(new BlockPos(1, 2, 1)));
        BlockPos crop = context.getAbsolutePos(new BlockPos(8, 2, 8));

        try {
            ActionJournal.flush();
            ActionJournal.writeTo(journal);
            Task task = Task.create(UUID.randomUUID(), TaskType.COLLECT_FOOD, TaskPriority.PRODUCTION, ResourceType.WHEAT, 1);
            var constructor = FarmerWork.Job.class.getDeclaredConstructor(Task.class, BlockPos.class);
            constructor.setAccessible(true);
            var job = constructor.newInstance(task, crop);
            job.target = crop;
            var giveUp = FarmerWork.class.getDeclaredMethod("giveUp", UUID.class, FarmerWork.Job.class,
                    net.minecraft.entity.passive.VillagerEntity.class);
            giveUp.setAccessible(true);
            giveUp.invoke(null, farmer.getUuid(), job, farmer);
            ActionJournal.flush();
            String content = Files.readString(journal, StandardCharsets.UTF_8);
            context.assertTrue(content.contains("GAVE_UP") && content.contains("approach timeout")
                            && content.contains(farmer.getBlockPos().toShortString())
                            && content.contains("\"x\":" + crop.getX())
                            && content.contains("\"y\":" + crop.getY())
                            && content.contains("\"z\":" + crop.getZ()),
                    "a recusa deve registrar canteiro, posição do aldeão e motivo: " + content);
            job.target = crop;
            var still = WorkStall.class.getDeclaredField("still");
            still.setAccessible(true);
            still.setInt(job.stall, WorkStall.LIMIT);
            giveUp.invoke(null, farmer.getUuid(), job, farmer);
            ActionJournal.flush();
            context.assertTrue(Files.readString(journal, StandardCharsets.UTF_8).contains("stationary"),
                    "a recusa por imobilidade deve informar o motivo");
        } finally {
            ActionJournal.writeTo(before);
            farmer.discard();
            Files.deleteIfExists(journal);
        }
        context.complete();
    }
}
