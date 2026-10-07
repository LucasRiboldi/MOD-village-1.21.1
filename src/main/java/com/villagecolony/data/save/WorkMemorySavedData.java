package com.villagecolony.data.save;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/**
 * As memórias de trabalho que sobrevivem ao fechar o mundo — ADR-039 C. Um
 * arquivo à parte, como o {@link WorkMarksSavedData}: memória que pode ser
 * perdida sem estragar colônia nenhuma. Quem monta e lê o conteúdo é
 * {@code fabric.work.WorkMemory}; aqui só se guarda.
 */
public final class WorkMemorySavedData extends PersistentState {

    public static final String KEY = "villagecolony_memory";

    static final String MEMORY = "memory";

    public static final PersistentState.Type<WorkMemorySavedData> TYPE = new PersistentState.Type<>(
            WorkMemorySavedData::new,
            WorkMemorySavedData::readNbt,
            null);

    private NbtCompound memory = new NbtCompound();

    public static WorkMemorySavedData get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, KEY);
    }

    /** Troca o conteúdo guardado e marca para gravação. */
    public void sync(NbtCompound current) {
        memory = current.copy();
        markDirty();
    }

    public NbtCompound memory() {
        return memory.copy();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.put(MEMORY, memory.copy());
        return nbt;
    }

    public static WorkMemorySavedData readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        WorkMemorySavedData data = new WorkMemorySavedData();
        data.memory = nbt.getCompound(MEMORY).copy();
        return data;
    }
}
