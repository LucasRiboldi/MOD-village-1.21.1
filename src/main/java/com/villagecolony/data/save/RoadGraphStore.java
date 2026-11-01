package com.villagecolony.data.save;

import com.villagecolony.core.construction.model.RoadGraph;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistência derivada das ruas separada do estado geral da colônia — ADR-043. */
public final class RoadGraphStore {
    private final Map<UUID, NbtCompound> payloads = new HashMap<>();
    private final Runnable changed;

    RoadGraphStore(Runnable changed) {
        this.changed = changed;
    }

    /** Grafo do disco ainda requer revalidação física antes do uso — ADR-043. */
    public Optional<RoadGraph> graph(UUID colonyId) {
        NbtCompound payload = payloads.get(colonyId);
        return payload == null ? Optional.empty() : RoadGraphSave.decode(payload);
    }

    /** Versão desconhecida desabilita planejamento, sem truncar seu payload — ADR-043. */
    public boolean versionSupported(UUID colonyId) {
        NbtCompound payload = payloads.get(colonyId);
        return payload == null || RoadGraphSave.supported(payload);
    }

    /** Publica somente grafo confirmado pelo adaptador; não altera ruas nem índice legado — ADR-043. */
    public void update(UUID colonyId, RoadGraph graph) {
        if (!versionSupported(colonyId)) {
            throw new IllegalStateException("unsupported future road graph");
        }
        payloads.put(colonyId, RoadGraphSave.encode(graph));
        changed.run();
    }

    void read(NbtCompound root, Set<UUID> colonies) {
        payloads.clear();
        payloads.putAll(RoadGraphSave.read(root, colonies));
    }

    void write(NbtCompound root) {
        RoadGraphSave.write(root, payloads);
    }
}
