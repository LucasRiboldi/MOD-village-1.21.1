package com.villagecolony.data.save;

import com.villagecolony.core.coordination.DemandKey;
import com.villagecolony.core.coordination.DemandRetries;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Falhas diárias de demandas, separadas do estado estrutural da colônia. */
public final class DemandRetrySave extends PersistentState {

    public static final String KEY = "villagecolony_demand_retries";
    private static final String RETRIES = "retries";
    private final List<DemandRetries.Retry> retries = new ArrayList<>();

    public static final PersistentState.Type<DemandRetrySave> TYPE = new PersistentState.Type<>(
            DemandRetrySave::new, DemandRetrySave::readNbt, null);

    public static DemandRetrySave get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, KEY);
    }

    public void sync(Collection<DemandRetries.Retry> current) {
        retries.clear();
        retries.addAll(current);
        markDirty();
    }

    public List<DemandRetries.Retry> retries() {
        return List.copyOf(retries);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList values = new NbtList();
        for (DemandRetries.Retry retry : retries) {
            NbtCompound value = new NbtCompound();
            value.putUuid("colony", retry.demand().colonyId());
            value.putString("task", retry.demand().type().name());
            value.putString("resource", retry.demand().resource().name());
            value.putLong("day", retry.day());
            value.putInt("count", retry.count());
            values.add(value);
        }
        nbt.put(RETRIES, values);
        return nbt;
    }

    static DemandRetrySave readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        DemandRetrySave save = new DemandRetrySave();
        NbtList values = nbt.getList(RETRIES, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < values.size(); i++) {
            NbtCompound value = values.getCompound(i);
            try {
                if (value.containsUuid("colony") && value.getInt("count") > 0) {
                    save.retries.add(new DemandRetries.Retry(new DemandKey(value.getUuid("colony"),
                            TaskType.valueOf(value.getString("task")),
                            ResourceType.valueOf(value.getString("resource"))),
                            value.getLong("day"), value.getInt("count")));
                }
            } catch (IllegalArgumentException ignored) {
                // Entrada de versão antiga ou corrompida não bloqueia o carregamento da vila.
            }
        }
        return save;
    }
}
