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
        NbtCompound encoded = RoadGraphSave.encode(graph);
        NbtCompound old = payloads.get(colonyId);

        // A trava de trecho e as ligações planejadas sobrevivem à troca do grafo (Regras 225 e 388).
        if (old != null) {
            RoadGraphSave.putSettled(encoded, RoadGraphSave.settled(old));
            RoadRoutesSave.carry(old, encoded);
        }

        payloads.put(colonyId, encoded);
        changed.run();
    }

    /** O dia em que cada coluna entrou na linha — Regra 225, ADR-050. */
    public Map<Long, Long> settled(UUID colonyId) {
        NbtCompound payload = payloads.get(colonyId);

        return payload == null ? Map.of() : RoadGraphSave.settled(payload);
    }

    /** Grava os dias da trava, só onde já há grafo da versão conhecida. */
    public void settle(UUID colonyId, Map<Long, Long> since) {
        NbtCompound payload = payloads.get(colonyId);

        if (payload == null || !RoadGraphSave.supported(payload) || RoadGraphSave.settled(payload).equals(since)) {
            return;
        }

        NbtCompound copy = payload.copy();

        RoadGraphSave.putSettled(copy, since);
        payloads.put(colonyId, copy);
        changed.run();
    }

    /** As ligações do grafo planejado — Regra 388, ADR-065 §8. Sem grafo guardado, nenhuma. */
    public com.villagecolony.core.construction.model.NeighborhoodRoutes routes(UUID colonyId) {
        NbtCompound payload = payloads.get(colonyId);

        return payload == null ? new com.villagecolony.core.construction.model.NeighborhoodRoutes()
                : RoadRoutesSave.read(payload);
    }

    /** Grava as ligações, só onde já há grafo da versão conhecida e só quando mudaram. */
    public void keepRoutes(UUID colonyId, com.villagecolony.core.construction.model.NeighborhoodRoutes routes) {
        NbtCompound payload = payloads.get(colonyId);

        if (payload == null || !RoadGraphSave.supported(payload) || routes.sameAs(RoadRoutesSave.read(payload))) {
            return;
        }

        NbtCompound copy = payload.copy();

        RoadRoutesSave.put(copy, routes);
        payloads.put(colonyId, copy);
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
