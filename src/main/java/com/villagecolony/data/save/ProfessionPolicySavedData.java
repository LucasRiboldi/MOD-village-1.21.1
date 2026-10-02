package com.villagecolony.data.save;

import com.villagecolony.core.worker.model.ProfessionPolicy;
import com.villagecolony.core.worker.model.ProfessionPolicySet;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.EnumMap;
import java.util.List;

/** Políticas de profissão pertencentes ao Overworld, separadas das colônias. */
public final class ProfessionPolicySavedData extends PersistentState {

    public static final String KEY = "villagecolony_profession_policies";

    private static final String POLICIES = "policies";
    private static final String ORDER = "order";
    private static final String TYPE = "type";
    private static final String ENABLED = "enabled";
    private static final String MAXIMUM_WORKERS = "maximumWorkers";
    private static final String SEARCH_RADIUS = "searchRadius";

    public static final PersistentState.Type<ProfessionPolicySavedData> TYPE_DATA =
            new PersistentState.Type<>(ProfessionPolicySavedData::new,
                    ProfessionPolicySavedData::readNbt, null);

    private ProfessionPolicySet policies = ProfessionPolicySet.defaults();

    private ProfessionPolicySavedData() {
    }

    public static ProfessionPolicySavedData get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE_DATA, KEY);
    }

    public ProfessionPolicySet policies() {
        return policies;
    }

    public void replace(ProfessionPolicySet next) {
        policies = java.util.Objects.requireNonNull(next, "next");
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList entries = new NbtList();
        for (ProfessionType type : ProfessionType.values()) {
            ProfessionPolicy policy = policies.policyOf(type);
            NbtCompound entry = new NbtCompound();
            entry.putString(TYPE, type.name());
            entry.putBoolean(ENABLED, policy.enabled());
            entry.putInt(MAXIMUM_WORKERS, policy.maximumWorkers());
            entry.putInt(SEARCH_RADIUS, policy.searchRadius());
            entries.add(entry);
        }
        nbt.put(POLICIES, entries);

        NbtList order = new NbtList();
        for (ProfessionType type : policies.hiringOrder()) {
            order.add(net.minecraft.nbt.NbtString.of(type.name()));
        }
        nbt.put(ORDER, order);
        return nbt;
    }

    private static ProfessionPolicySavedData readNbt(
            NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ProfessionPolicySavedData data = new ProfessionPolicySavedData();
        EnumMap<ProfessionType, ProfessionPolicy> read = new EnumMap<>(ProfessionType.class);
        for (ProfessionType type : ProfessionType.values()) {
            read.put(type, ProfessionPolicy.defaults());
        }
        for (NbtElement value : nbt.getList(POLICIES, NbtElement.COMPOUND_TYPE)) {
            NbtCompound entry = (NbtCompound) value;
            try {
                ProfessionType type = ProfessionType.valueOf(entry.getString(TYPE));
                read.put(type, new ProfessionPolicy(entry.getBoolean(ENABLED),
                        entry.getInt(MAXIMUM_WORKERS), entry.getInt(SEARCH_RADIUS)));
            } catch (IllegalArgumentException ignored) {
                // Save editado ou de versão futura: mantém o padrão seguro.
            }
        }
        List<ProfessionType> order = nbt.getList(ORDER, NbtElement.STRING_TYPE).stream()
                .map(element -> parseType(element.asString())).flatMap(java.util.Optional::stream).toList();
        try {
            data.policies = new ProfessionPolicySet(read,
                    order.isEmpty() ? ProfessionPolicySet.defaults().hiringOrder() : order);
        } catch (IllegalArgumentException ignored) {
            data.policies = ProfessionPolicySet.defaults();
        }
        return data;
    }

    private static java.util.Optional<ProfessionType> parseType(String value) {
        try {
            return java.util.Optional.of(ProfessionType.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }
}
