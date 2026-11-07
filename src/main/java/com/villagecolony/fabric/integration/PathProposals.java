package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O caminho que uma parte do mod quer e ainda não existe — Regra 214: quem decide é o grafo e quem calça é o aldeão, então
 * ninguém mais escreve caminho no mundo. A releitura do grafo recolhe os pedidos; a coluna pedida ou planejada já conta como rua
 * para a escolha de lote, porque o lote novo nasce ao lado dela.
 */
public final class PathProposals {

    private static final Map<UUID, Set<ColonyPos>> WAITING = new HashMap<>();
    private static final Map<UUID, Set<Long>> PLANNED = new HashMap<>();

    static {
        ServerMemory.register(PathProposals.class, PathProposals::clearAll);
    }

    private PathProposals() {
    }

    /** Pede caminho nesta coluna, com o piso em {@code floor}. */
    public static void add(UUID colonyId, BlockPos floor) {
        WAITING.computeIfAbsent(colonyId, ignored -> new HashSet<>()).add(MinecraftTypeAdapter.toColonyPos(floor));
    }

    /** O que foi pedido desde a última releitura. */
    public static List<ColonyPos> drain(UUID colonyId) {
        Set<ColonyPos> waiting = WAITING.remove(colonyId);

        return waiting == null ? List.of() : List.copyOf(waiting);
    }

    /** O que o grafo tem por calçar agora. */
    public static void planned(UUID colonyId, Set<ColonyPos> cells) {
        Set<Long> columns = new HashSet<>();

        cells.forEach(at -> columns.add(ColonyRoads.column(at.x(), at.z())));
        PLANNED.put(colonyId, columns);
    }

    /** Se esta coluna já foi pedida ou está planejada. */
    public static boolean isWanted(UUID colonyId, int x, int z) {
        return PLANNED.getOrDefault(colonyId, Set.of()).contains(ColonyRoads.column(x, z))
                || WAITING.getOrDefault(colonyId, Set.of()).stream().anyMatch(at -> at.x() == x && at.z() == z);
    }

    static void clearAll() {
        WAITING.clear();
        PLANNED.clear();
    }
}
