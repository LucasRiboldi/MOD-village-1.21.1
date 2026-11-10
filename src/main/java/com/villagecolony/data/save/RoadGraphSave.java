package com.villagecolony.data.save;

import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Formato paralelo ao índice legado; payload futuro permanece opaco — ADR-043. */
final class RoadGraphSave {
    private static final String ROOT = "roadGraphs";
    private static final String VERSION = "version";
    private static final int CURRENT = 1;
    private static final int MAX_COLUMNS = 6000;
    private static final String SETTLED = "settled";

    private RoadGraphSave() { }

    static Map<UUID, NbtCompound> read(NbtCompound root, Set<UUID> colonies) {
        Map<UUID, NbtCompound> found = new HashMap<>();
        NbtCompound graphs = root.getCompound(ROOT);
        for (UUID id : colonies) {
            if (graphs.contains(id.toString(), NbtElement.COMPOUND_TYPE)) {
                found.put(id, graphs.getCompound(id.toString()).copy());
            }
        }
        return found;
    }

    static void write(NbtCompound root, Map<UUID, NbtCompound> payloads) {
        NbtCompound graphs = new NbtCompound();
        payloads.forEach((id, payload) -> graphs.put(id.toString(), payload.copy()));
        root.put(ROOT, graphs);
    }

    static boolean supported(NbtCompound payload) {
        return payload.contains(VERSION, NbtElement.INT_TYPE) && payload.getInt(VERSION) == CURRENT;
    }

    static NbtCompound encode(RoadGraph graph) {
        NbtCompound payload = new NbtCompound();
        payload.putInt(VERSION, CURRENT);
        NbtList nodes = new NbtList();
        for (RoadGraph.Node node : graph.nodes()) {
            NbtCompound entry = new NbtCompound();
            entry.putIntArray("position", coordinates(List.of(node.position())));
            NbtList roles = new NbtList();
            node.roles().stream().sorted().forEach(role -> roles.add(NbtString.of(role.name())));
            entry.put("roles", roles);
            nodes.add(entry);
        }
        payload.put("nodes", nodes);
        NbtList edges = new NbtList();
        for (RoadGraph.Edge edge : graph.edges()) {
            NbtCompound entry = new NbtCompound();
            entry.putIntArray("from", coordinates(List.of(edge.from())));
            entry.putIntArray("to", coordinates(List.of(edge.to())));
            entry.putIntArray("columns", coordinates(edge.columns()));
            entry.putString("class", edge.roadClass().name());
            edges.add(entry);
        }
        payload.put("edges", edges);
        require(decode(payload).isPresent());
        return payload;
    }

    /** O dia em que cada coluna entrou na linha, para a trava de trecho — Regra 225, ADR-050. Ausente: nenhum. */
    static Map<Long, Long> settled(NbtCompound payload) {
        Map<Long, Long> since = new HashMap<>();
        int[] values = payload.getIntArray(SETTLED);

        if (values.length % 3 != 0 || values.length > MAX_COLUMNS * 3) {
            return since;
        }

        for (int i = 0; i < values.length; i += 3) {
            since.put(com.villagecolony.core.construction.model.ColonyRoads.column(values[i], values[i + 1]), (long) values[i + 2]);
        }

        return since;
    }

    static void putSettled(NbtCompound payload, Map<Long, Long> since) {
        int[] values = new int[Math.min(since.size(), MAX_COLUMNS) * 3];
        int i = 0;

        for (Map.Entry<Long, Long> entry : new java.util.TreeMap<>(since).entrySet()) {
            if (i >= values.length) {
                break;
            }

            values[i] = com.villagecolony.core.construction.model.ColonyRoads.xOf(entry.getKey());
            values[i + 1] = com.villagecolony.core.construction.model.ColonyRoads.zOf(entry.getKey());
            values[i + 2] = (int) Math.min(Integer.MAX_VALUE, Math.max(0, entry.getValue()));
            i += 3;
        }

        payload.putIntArray(SETTLED, values);
    }

    static Optional<RoadGraph> decode(NbtCompound payload) {
        if (!supported(payload)
                || !payload.contains("nodes", NbtElement.LIST_TYPE)
                || !payload.contains("edges", NbtElement.LIST_TYPE)) {
            return Optional.empty();
        }
        try {
            return Optional.of(readGraph(payload));
        } catch (IllegalArgumentException invalid) {
            // Corrupção invalida apenas o grafo; roads.columns continua legível — ADR-043.
            return Optional.empty();
        }
    }

    private static RoadGraph readGraph(NbtCompound payload) {
        List<RoadGraph.Node> nodes = new ArrayList<>();
        Set<ColonyPos> positions = new HashSet<>();
        NbtList encodedNodes = compounds(payload, "nodes");
        require(encodedNodes.size() <= MAX_COLUMNS);
        boolean hasHub = false;
        for (int i = 0; i < encodedNodes.size(); i++) {
            NbtCompound entry = encodedNodes.getCompound(i);
            ColonyPos position = position(entry, "position");
            Set<RoadGraph.Role> roles = new HashSet<>();
            NbtList encodedRoles = entry.getList("roles", NbtElement.STRING_TYPE);
            for (int j = 0; j < encodedRoles.size(); j++) {
                roles.add(RoadGraph.Role.valueOf(encodedRoles.getString(j)));
            }
            require(!roles.isEmpty() && positions.add(position));
            if (roles.contains(RoadGraph.Role.HUB)) {
                require(!hasHub);
                hasHub = true;
            }
            nodes.add(new RoadGraph.Node(position, roles));
        }
        List<RoadGraph.Edge> edges = new ArrayList<>();
        NbtList encodedEdges = compounds(payload, "edges");
        require(encodedEdges.size() <= MAX_COLUMNS * 6);
        Set<ColonyPos> physical = new HashSet<>(positions);
        int count = 0;
        for (int i = 0; i < encodedEdges.size(); i++) {
            NbtCompound entry = encodedEdges.getCompound(i);
            ColonyPos from = position(entry, "from");
            ColonyPos to = position(entry, "to");
            List<ColonyPos> columns = positions(entry.getIntArray("columns"));
            physical.addAll(columns);
            require(physical.size() <= MAX_COLUMNS);
            count += columns.size();
            require(count <= MAX_COLUMNS * 12 && columns.size() >= 2
                    && positions.contains(from) && positions.contains(to)
                    && from.equals(columns.getFirst()) && to.equals(columns.getLast()));
            for (int j = 1; j < columns.size(); j++) {
                ColonyPos a = columns.get(j - 1);
                ColonyPos b = columns.get(j);
                // Um nó interno exige divisão do trecho para preservar as rotas — ADR-043.
                require(j == columns.size() - 1 || !positions.contains(b));
                require(Math.abs((long) a.x() - b.x()) + Math.abs((long) a.z() - b.z()) == 1
                        && Math.abs((long) a.y() - b.y()) <= 1);
            }
            edges.add(new RoadGraph.Edge(from, to, columns,
                    RoadGraph.RoadClass.valueOf(entry.getString("class"))));
        }
        return new RoadGraph(nodes, edges);
    }

    private static ColonyPos position(NbtCompound entry, String key) {
        int[] values = entry.getIntArray(key);
        require(values.length == 3);
        return new ColonyPos(values[0], values[1], values[2]);
    }

    private static List<ColonyPos> positions(int[] values) {
        // Um circuito repete a âncora final sem consumir outra coluna física — ADR-043.
        require(values.length % 3 == 0 && values.length <= (MAX_COLUMNS + 1) * 3);
        List<ColonyPos> result = new ArrayList<>();
        for (int i = 0; i < values.length; i += 3) {
            result.add(new ColonyPos(values[i], values[i + 1], values[i + 2]));
        }
        return result;
    }

    private static NbtList compounds(NbtCompound payload, String key) {
        require(payload.get(key) instanceof NbtList);
        NbtList list = (NbtList) payload.get(key);
        require(list != null && (list.isEmpty() || list.getHeldType() == NbtElement.COMPOUND_TYPE));
        return list;
    }

    private static int[] coordinates(List<ColonyPos> positions) {
        int[] values = new int[positions.size() * 3];
        for (int i = 0; i < positions.size(); i++) {
            ColonyPos position = positions.get(i);
            values[i * 3] = position.x();
            values[i * 3 + 1] = position.y();
            values[i * 3 + 2] = position.z();
        }
        return values;
    }

    private static void require(boolean valid) {
        if (!valid) {
            throw new IllegalArgumentException("invalid road graph");
        }
    }
}
