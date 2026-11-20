package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 344, telemetria T2: a linha do tempo de cada aldeão grava no diário só a mudança de estado que se firma por
 * {@link VillagerTimeline#SETTLE} amostras, com o estado de antes, quanto durou e onde ele estava.
 */
class VillagerTimelineTest {

    private static final UUID MINER = UUID.randomUUID();

    private Path before;

    private Path journal;

    @BeforeEach
    void open() throws IOException {
        before = ActionJournal.file();
        ActionJournal.flush();
        journal = Files.createTempFile("villagecolony-timeline", ".jsonl");
        ActionJournal.writeTo(journal);
        VillageColonyMod.WORKERS.register(MINER, UUID.randomUUID()).assign(ProfessionType.MINER);
        VillagerTimeline.forget(MINER);
    }

    @AfterEach
    void close() throws IOException {
        ActionJournal.writeTo(before);
        VillageColonyMod.WORKERS.remove(MINER);
        VillagerTimeline.forget(MINER);
        Files.deleteIfExists(journal);
    }

    private List<String> lines() throws IOException {
        ActionJournal.flush();
        return Files.readAllLines(journal, StandardCharsets.UTF_8).stream()
                .filter(line -> line.contains("\"action\":\"STATE\"")).toList();
    }

    @Test
    void aChangeThatSettlesIsWrittenWithTheStateBeforeAndHowLongItLasted() throws IOException {
        BlockPos at = new BlockPos(5, 64, 9);

        for (long second = 0; second < 30; second++) {
            VillagerTimeline.sample(MINER, "WORK", second * 20, at, "");
        }
        for (long second = 30; second < 40; second++) {
            VillagerTimeline.sample(MINER, "WAIT", second * 20, at, "SMELT_MATERIAL:IRON_INGOT");
        }

        List<String> lines = lines();

        assertEquals(1, lines.size(), "Regra 344: uma mudança firmada, uma linha: " + lines);
        assertTrue(lines.get(0).contains("\"what\":\"wait\"") && lines.get(0).contains("\"ticks\":600")
                        && lines.get(0).contains("from WORK") && lines.get(0).contains("SMELT_MATERIAL:IRON_INGOT")
                        && lines.get(0).contains("\"x\":5"),
                "Regra 344: a linha não diz o estado, a duração, o anterior, o motivo ou o lugar: " + lines.get(0));
    }

    @Test
    void aBlinkShorterThanTheSettleIsNotWritten() throws IOException {
        BlockPos at = new BlockPos(0, 64, 0);

        for (long second = 0; second < 20; second++) {
            String state = second % 3 == 0 ? "WALK" : "WORK";
            VillagerTimeline.sample(MINER, state, second * 20, at, "");
        }

        assertTrue(lines().isEmpty(), "Regra 344: o pisca entre trabalho e caminhada encheu o diário");
    }
}
