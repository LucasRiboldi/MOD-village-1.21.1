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

    /** As colunas que cada colônia calçou — Regra 390: só elas podem voltar ao solo pela limpeza. */
    private final Map<UUID, java.util.Set<Long>> laid = new HashMap<>();

    /** Teto de colunas guardadas por colônia; a mais antiga sai. */
    static final int MAX_LAID = 32_768;

    private static final String LAID = "roadLaid";
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

    /** A colônia calçou esta coluna (Regra 390). */
    public void markLaid(UUID colonyId, int x, int z) {
        java.util.Set<Long> columns = laid.computeIfAbsent(colonyId, ignored -> new java.util.LinkedHashSet<>());

        if (columns.add(com.villagecolony.core.construction.model.ColonyRoads.column(x, z))) {
            if (columns.size() > MAX_LAID) {
                columns.remove(columns.iterator().next());
            }

            changed.run();
        }
    }

    /** Se alguma colônia calçou esta coluna (Regra 390). */
    public boolean laidByAColony(int x, int z) {
        long column = com.villagecolony.core.construction.model.ColonyRoads.column(x, z);

        return laid.values().stream().anyMatch(columns -> columns.contains(column));
    }

    void read(NbtCompound root, Set<UUID> colonies) {
        payloads.clear();
        payloads.putAll(RoadGraphSave.read(root, colonies));
        laid.clear();

        NbtCompound all = root.getCompound(LAID);

        for (String key : all.getKeys()) {
            try {
                UUID id = UUID.fromString(key);
                long[] columns = all.getLongArray(key);

                if (colonies.contains(id)) {
                    java.util.Set<Long> set = new java.util.LinkedHashSet<>();

                    for (int i = Math.max(0, columns.length - MAX_LAID); i < columns.length; i++) {
                        set.add(columns[i]);
                    }

                    laid.put(id, set);
                }
            } catch (IllegalArgumentException broken) {
                // Chave estragada: a colônia segue sem a lista, e a limpeza só não toca no caminho dela.
            }
        }
    }

    void write(NbtCompound root) {
        RoadGraphSave.write(root, payloads);

        NbtCompound all = new NbtCompound();

        laid.forEach((id, columns) -> all.putLongArray(id.toString(), columns.stream().mapToLong(Long::longValue).toArray()));
        root.put(LAID, all);
    }
}
