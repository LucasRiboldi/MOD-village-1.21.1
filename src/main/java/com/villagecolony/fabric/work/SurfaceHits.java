package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Onde a coleta de superfície já achou o recurso — ADR-038 P7: a busca
 * seguinte começa por essas colunas e pelas oito em volta de cada uma, antes
 * da varredura. Coluna que não dá mais nada sai da memória. Até
 * {@link #MAX_COLUMNS} colunas por colônia e recurso, em memória.
 */
final class SurfaceHits {

    static {
        ServerMemory.register(SurfaceHits.class, SurfaceHits::clearAll);
    }

    static final int MAX_COLUMNS = 32;

    private record Key(UUID colonyId, ResourceType resource) {
    }

    private static final Map<Key, Set<BlockPos>> HITS = new HashMap<>();

    private SurfaceHits() {
    }

    static void clearAll() {
        HITS.clear();
    }

    /** Achou o recurso nesta posição: a coluna dela entra na memória. */
    static void remember(UUID colonyId, ResourceType resource, BlockPos found) {
        Set<BlockPos> columns = HITS.computeIfAbsent(new Key(colonyId, resource), ignored -> new LinkedHashSet<>());
        BlockPos column = new BlockPos(found.getX(), 0, found.getZ());

        columns.remove(column);
        columns.add(column);

        if (columns.size() > MAX_COLUMNS) {
            Iterator<BlockPos> oldest = columns.iterator();
            oldest.next();
            oldest.remove();
        }
    }

    /**
     * O recurso numa coluna lembrada ou vizinha dela, perguntando a {@code probe}
     * (a mesma pergunta da varredura); a coluna lembrada que não dá nada é esquecida.
     */
    static Optional<BlockPos> near(UUID colonyId, ResourceType resource,
            Predicate<BlockPos> worth, Function<BlockPos, Optional<BlockPos>> probe) {
        Set<BlockPos> columns = HITS.get(new Key(colonyId, resource));

        if (columns == null || columns.isEmpty()) {
            return Optional.empty();
        }

        for (Iterator<BlockPos> it = columns.iterator(); it.hasNext(); ) {
            BlockPos column = it.next();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos at = column.add(dx, 0, dz);

                    if (!worth.test(at)) {
                        continue;
                    }

                    Optional<BlockPos> found = probe.apply(at);

                    if (found.isPresent()) {
                        return found;
                    }
                }
            }

            // Nada nela nem em volta: a coluna sai da memória.
            it.remove();
        }

        return Optional.empty();
    }
}
