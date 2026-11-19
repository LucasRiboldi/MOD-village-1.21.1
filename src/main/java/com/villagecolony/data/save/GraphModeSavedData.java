package com.villagecolony.data.save;

import com.villagecolony.core.construction.model.RoadGraphMode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/** O modo do grafo de ruas deste mundo — ADR-057: um ligado por vez, gravado no Overworld. */
public final class GraphModeSavedData extends PersistentState {

    public static final String KEY = "villagecolony_graph_mode";

    private static final String MODE = "mode";

    public static final PersistentState.Type<GraphModeSavedData> TYPE_DATA =
            new PersistentState.Type<>(GraphModeSavedData::new, GraphModeSavedData::readNbt, null);

    private RoadGraphMode mode = RoadGraphMode.ATUAL;

    private GraphModeSavedData() {
    }

    public static GraphModeSavedData get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE_DATA, KEY);
    }

    public RoadGraphMode mode() {
        return mode;
    }

    /** Liga este modo; os outros ficam desligados. */
    public void set(RoadGraphMode next) {
        mode = java.util.Objects.requireNonNull(next, "next");
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putString(MODE, mode.id());

        return nbt;
    }

    private static GraphModeSavedData readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        GraphModeSavedData data = new GraphModeSavedData();

        // Nome desconhecido (versão futura ou arquivo mexido) volta ao modo de sempre, sem derrubar o mundo.
        data.mode = RoadGraphMode.parse(nbt.getString(MODE)).orElse(RoadGraphMode.ATUAL);

        return data;
    }
}
