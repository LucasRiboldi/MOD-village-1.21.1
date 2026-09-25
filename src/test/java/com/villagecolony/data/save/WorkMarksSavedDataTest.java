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
}
