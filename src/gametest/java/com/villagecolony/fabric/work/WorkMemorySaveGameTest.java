package com.villagecolony.fabric.work;

import com.villagecolony.data.save.WorkMemorySavedData;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/** O arquivo `villagecolony_memory` grava e relê o que recebeu, pelo NBT do save (ADR-039 C). */
public class WorkMemorySaveGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours", tickLimit = 20)
    public void theMemoryFileWritesAndReadsBack(TestContext context) {
        NbtCompound memory = new NbtCompound();
        NbtCompound trees = new NbtCompound();
        trees.putLongArray("trees", new long[] {42L, 7L});
        memory.put("trees", trees);

        WorkMemorySavedData written = new WorkMemorySavedData();
        written.sync(memory);

        NbtCompound file = written.writeNbt(new NbtCompound(), context.getWorld().getRegistryManager());
        WorkMemorySavedData read = WorkMemorySavedData.readNbt(file, context.getWorld().getRegistryManager());

        context.assertTrue(read.memory().equals(memory),
                "o arquivo não devolveu o que gravou: " + read.memory() + " em vez de " + memory);
        context.assertTrue(written.isDirty(), "a memória nova não foi marcada para gravar");
        context.complete();
    }
}
