package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Colunas de água ou lava conhecidas dentro da caixa atual da vila.
 *
 * <p>O índice existe somente em memória e é reconstruído em parcelas. Uma
 * coluna não carregada nunca entra nele; ausência no índice significa
 * "pergunte ao mundo", não "há solo". A caixa exata faz parte da identidade:
 * quando a vila cresce, nenhum resultado da medida anterior é reutilizado.
 */
public final class VillageFluidIndex {

    static {
        ServerMemory.register(VillageFluidIndex.class, VillageFluidIndex::clearAll);
    }

    /** Quantas colunas da caixa podem ser classificadas por passagem. */
    public static final int MAX_COLUMNS_PER_PASS = 1024;

    private static final Map<UUID, Index> INDEXES = new HashMap<>();

    private static final class Index {
        private final VillageBounds bounds;
        private final Set<Long> fluids = new HashSet<>();
        private long nextColumn;

        private Index(VillageBounds bounds) {
            this.bounds = bounds;
        }
    }

    private VillageFluidIndex() {
    }

    /** Avança a reconstrução usando apenas chunks já carregados. */
    public static void refresh(ServerWorld world, UUID colonyId, VillageBounds bounds) {
        Objects.requireNonNull(world, "world");

        refresh(
                colonyId,
                bounds,
                column -> world.getChunkManager().isChunkLoaded(
                        column.getX() >> 4, column.getZ() >> 4),
                column -> {
                    BlockPos.Mutable at = new BlockPos.Mutable(
                            column.getX(), bounds.maxY(), column.getZ());

                    for (int y = bounds.maxY(); y >= bounds.minY(); y--) {
                        at.setY(y);
                        if (world.getFluidState(at).isIn(FluidTags.WATER)
                                || world.getFluidState(at).isIn(FluidTags.LAVA)) {
                            return true;
                        }
                    }

                    return false;
                });
    }

    /** Porta sem Minecraft para provar orçamento, expansão e chunks ausentes. */
    static void refresh(
            UUID colonyId,
            VillageBounds bounds,
            Predicate<BlockPos> loaded,
            Predicate<BlockPos> fluid) {

        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(loaded, "loaded");
        Objects.requireNonNull(fluid, "fluid");

        Index index = INDEXES.get(colonyId);
        if (index == null || !index.bounds.equals(bounds)) {
            index = new Index(bounds);
            INDEXES.put(colonyId, index);
        }

        long total = (long) bounds.sizeX() * bounds.sizeZ();
        int scanned = 0;

        while (index.nextColumn < total && scanned < MAX_COLUMNS_PER_PASS) {
            long ordinal = index.nextColumn++;
            int x = bounds.minX() + (int) (ordinal % bounds.sizeX());
            int z = bounds.minZ() + (int) (ordinal / bounds.sizeX());
            BlockPos column = new BlockPos(x, bounds.minY(), z);
            scanned++;

            if (loaded.test(column) && fluid.test(column)) {
                index.fluids.add(key(x, z));
            }
        }
    }

    /** Se esta coluna pertence ao índice válido e foi classificada como fluido. */
    public static boolean skip(UUID colonyId, VillageBounds bounds, BlockPos column) {
        Index index = INDEXES.get(colonyId);

        return index != null
                && index.bounds.equals(bounds)
                && bounds.containsColumn(column.getX(), column.getZ())
                && index.fluids.contains(key(column.getX(), column.getZ()));
    }

    /** Descarta a medida anterior quando a vila cresce. */
    public static void invalidate(UUID colonyId) {
        INDEXES.remove(colonyId);
    }

    /** Esquece todos os índices ao parar o servidor. */
    public static void clearAll() {
        INDEXES.clear();
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
