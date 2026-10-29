package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.WorkMemoryKeys;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Onde a coleta de superfície já achou o recurso — ADR-038 P7: a busca
 * seguinte começa por essas colunas, da mais perto da vila para a mais longe, e
 * pela mancha em volta de cada uma ({@link #PATCH_RADIUS}), antes da varredura —
 * "lembrar a mancha de areia", regra do autor de 2026-10-11. Coluna sem nada em
 * volta sai da memória. Até {@link #MAX_COLUMNS} colunas por colônia e recurso.
 */
final class SurfaceHits {

    static {
        ServerMemory.register(SurfaceHits.class, SurfaceHits::clearAll);
    }

    static final int MAX_COLUMNS = 32;

    /** Até quantos blocos em volta de uma coluna lembrada a mancha é procurada. */
    static final int PATCH_RADIUS = 3;

    private record Key(UUID colonyId, ResourceType resource) {
    }

    private static final Map<Key, Set<BlockPos>> HITS = new HashMap<>();

    /**
     * Quanto tempo a coluna que o coletor não alcançou fica fora das buscas — Regra 144 (autor
     * 2026-10-28): na taiga simulada três fazendeiros voltaram 79 vezes à mesma terra inalcançável, porque
     * a memória a devolvia a cada tarefa nova. Um dia de jogo.
     */
    static final long UNREACHABLE_FOR = 24_000;

    /** Quantos blocos em volta da coluna inalcançável ficam de fora com ela. */
    static final int UNREACHABLE_PATCH = 2;

    /** As colunas inalcançáveis de cada colônia, e até quando ficam de fora. */
    private static final Map<UUID, Map<BlockPos, Long>> UNREACHABLE = new HashMap<>();

    private SurfaceHits() {
    }

    static void clearAll() {
        HITS.clear();
        UNREACHABLE.clear();
    }

    /** O coletor não alcançou {@code target}: a mancha sai da memória e a coluna fica fora por um dia. */
    static void unreachable(UUID colonyId, BlockPos target, long now) {
        BlockPos column = new BlockPos(target.getX(), 0, target.getZ());

        HITS.forEach((key, columns) -> {
            if (key.colonyId().equals(colonyId)) {
                columns.removeIf(hit -> hit.getManhattanDistance(column) <= 2 * PATCH_RADIUS);
            }
        });
        UNREACHABLE.computeIfAbsent(colonyId, id -> new HashMap<>()).put(column, now + UNREACHABLE_FOR);
    }

    /** Se esta coluna fica a {@link #UNREACHABLE_PATCH} de uma inalcançável há menos de um dia. */
    static boolean isUnreachable(UUID colonyId, BlockPos column, long now) {
        Map<BlockPos, Long> columns = UNREACHABLE.get(colonyId);

        if (columns == null) {
            return false;
        }

        columns.values().removeIf(until -> until <= now);

        // A mancha em volta do alvo também: a terra vizinha fica no mesmo lugar que não se alcança.
        return columns.keySet().stream().anyMatch(marked -> Math.abs(marked.getX() - column.getX()) <= UNREACHABLE_PATCH
                && Math.abs(marked.getZ() - column.getZ()) <= UNREACHABLE_PATCH);
    }

    /** As colunas lembradas, para o save — ADR-039 C. Chave {@code colônia|RECURSO}. */
    static NbtCompound save() {
        NbtCompound nbt = new NbtCompound();

        HITS.forEach((key, columns) -> nbt.putLongArray(key.colonyId() + "|" + key.resource().name(),
                columns.stream().mapToLong(BlockPos::asLong).toArray()));

        return nbt;
    }

    static void load(NbtCompound nbt) {
        for (String key : nbt.getKeys()) {
            int bar = key.indexOf('|');
            UUID colony = bar > 0 ? WorkMemoryKeys.uuid(key.substring(0, bar)) : null;

            if (colony == null) {
                continue;
            }

            try {
                ResourceType resource = ResourceType.valueOf(key.substring(bar + 1));

                for (long at : nbt.getLongArray(key)) {
                    remember(colony, resource, BlockPos.fromLong(at));
                }
            } catch (IllegalArgumentException gone) {
                // Recurso que deixou de existir numa versão nova: a coluna fica de fora.
            }
        }
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
     * O recurso numa coluna lembrada ou na mancha em volta dela, perguntando a
     * {@code probe} (a mesma pergunta da varredura), a partir da coluna mais
     * perto de {@code from}; a coluna sem nada em volta é esquecida.
     */
    static Optional<BlockPos> near(UUID colonyId, ResourceType resource, BlockPos from,
            Predicate<BlockPos> worth, Function<BlockPos, Optional<BlockPos>> probe) {
        Set<BlockPos> columns = HITS.get(new Key(colonyId, resource));

        if (columns == null || columns.isEmpty()) {
            return Optional.empty();
        }

        List<BlockPos> nearestFirst = columns.stream()
                .sorted(Comparator.comparingLong(column -> flatDistance(column, from)))
                .toList();
        Set<BlockPos> asked = new HashSet<>();

        for (BlockPos column : nearestFirst) {
            for (int ring = 0; ring <= PATCH_RADIUS; ring++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        BlockPos at = column.add(dx, 0, dz);

                        if (Math.max(Math.abs(dx), Math.abs(dz)) != ring || !asked.add(at) || !worth.test(at)) {
                            continue;
                        }

                        Optional<BlockPos> found = probe.apply(at);

                        if (found.isPresent()) {
                            return found;
                        }
                    }
                }
            }

            // Nada nela nem em volta: a coluna sai da memória.
            columns.remove(column);
        }

        return Optional.empty();
    }

    private static long flatDistance(BlockPos column, BlockPos from) {
        long dx = (long) column.getX() - from.getX();
        long dz = (long) column.getZ() - from.getZ();

        return dx * dx + dz * dz;
    }
}
