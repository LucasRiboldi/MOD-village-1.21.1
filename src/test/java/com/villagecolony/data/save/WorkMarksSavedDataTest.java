package com.villagecolony.data.save;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As marcas de trabalho vão ao disco e voltam iguais — 2026-09-25, ADR-025. */
class WorkMarksSavedDataTest {

    @Test
    void mineRefusalsRoundTrip() {
        WorkMarksSavedData data = new WorkMarksSavedData();
        List<WorkMarksSavedData.MineRefusal> refusals = List.of(
                new WorkMarksSavedData.MineRefusal(553, 39, 158, 123_456L, 2),
                new WorkMarksSavedData.MineRefusal(-10, -60, 7, 1L, 1));

        data.sync(refusals);

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);
        WorkMarksSavedData loaded = WorkMarksSavedData.readNbt(nbt, null);

        assertEquals(refusals, loaded.mineRefusals());
    }

    @Test
    void anEmptySaveReadsAsNoMarks() {
        assertTrue(WorkMarksSavedData.readNbt(new NbtCompound(), null).mineRefusals().isEmpty());
    }

    /** As tentativas de peças sem profissão voltam iguais. */
    @Test
    void supplyAttemptsRoundTrip() {
        WorkMarksSavedData data = new WorkMarksSavedData();
        data.syncSupplyAttempts(java.util.Map.of(
                "7f0c3a52-0000-0000-0000-000000000001/brewing_stand", 2,
                "7f0c3a52-0000-0000-0000-000000000001/white_terracotta", 1));

        WorkMarksSavedData read = WorkMarksSavedData.readNbt(data.writeNbt(new NbtCompound(), null), null);

        assertEquals(data.supplyAttempts(), read.supplyAttempts());
    }

    /** O baú de cada trabalhador volta igual — 2026-09-26. */
    @Test
    void workerChestsRoundTrip() {
        WorkMarksSavedData data = new WorkMarksSavedData();
        data.syncWorkerChests(List.of(
                new WorkMarksSavedData.WorkerChest(java.util.UUID.fromString("7f0c3a52-0000-0000-0000-000000000001"), -432, 70, 3578),
                new WorkMarksSavedData.WorkerChest(java.util.UUID.fromString("7f0c3a52-0000-0000-0000-000000000002"), -449, 70, 3586)));

        WorkMarksSavedData read = WorkMarksSavedData.readNbt(data.writeNbt(new NbtCompound(), null), null);

        assertEquals(data.workerChests(), read.workerChests());
    }
}
