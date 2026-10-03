package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * As colunas de água e lava da vila, lidas uma vez — pedido do autor,
 * 2026-10-03.
 *
 * <p><i>"Quando a vila fizer sua varredura de área ela deve marcar os blocos
 * de água e lava, para que a próxima varredura não leia esses blocos;
 * sempre que a vila expande é realizado um escaneamento completo e novamente
 * estes blocos são marcados."</i>
 *
 * <p>A busca que lê uma coluna e não acha nada pergunta também se o topo dela
 * é fluido; sendo, a coluna fica marcada, e a próxima busca a pula pelo
 * filtro barato do {@link RingSweep} — sem ler o mundo. A marca vale para a
 * caixa em que foi feita: a vila cresceu, a caixa mudou, e as marcas são
 * apagadas para a volta seguinte marcar tudo de novo.
 *
 * <p><b>A argila fica de fora</b>: ela mora no fundo do lago, e pular a água
 * seria nunca mais achá-la. Quem busca argila não usa esta porta.
 */
public final class FluidColumns {

    static {
        ServerMemory.register(FluidColumns.class, FluidColumns::clearAll);
    }

    private record Marks(VillageBounds box, Set<Long> fluid) {
    }

    private static final Map<UUID, Marks> MARKS = new HashMap<>();

    private FluidColumns() {
    }

    /** Se esta coluna já foi lida como água ou lava, na caixa atual da vila. */
    public static boolean isFluid(UUID colonyId, int x, int z) {
        Marks marks = MARKS.get(colonyId);

        return marks != null && marks.fluid().contains(key(x, z));
    }

    /** Quantas colunas desta vila estão marcadas. */
    public static int marked(UUID colonyId) {
        Marks marks = MARKS.get(colonyId);

        return marks == null ? 0 : marks.fluid().size();
    }

    /** Lê o topo da coluna e a marca, se for fluido e estiver na vila. */
    public static void observe(ServerWorld world, UUID colonyId, BlockPos column) {
        Optional<Marks> marks = current(colonyId);

        if (marks.isEmpty() || !marks.get().box().containsColumn(column.getX(), column.getZ())
                || !world.isChunkLoaded(column.getX() >> 4, column.getZ() >> 4)) {
            return;
        }

        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, column.getX(), column.getZ()) - 1;

        if (!world.getFluidState(new BlockPos(column.getX(), top, column.getZ())).isEmpty()) {
            marks.get().fluid().add(key(column.getX(), column.getZ()));
        }
    }

    /** O filtro da busca, pulando a coluna marcada. */
    public static Predicate<BlockPos> skipping(UUID colonyId, Predicate<BlockPos> worth) {
        current(colonyId);

        return column -> !isFluid(colonyId, column.getX(), column.getZ()) && worth.test(column);
    }

    /** A pergunta da busca, marcando a coluna em que ela não achou nada. */
    public static <T> Function<BlockPos, Optional<T>> marking(
            ServerWorld world, UUID colonyId, Function<BlockPos, Optional<T>> test) {

        return column -> {
            Optional<T> found = test.apply(column);

            if (found.isEmpty()) {
                observe(world, colonyId, column);
            }

            return found;
        };
    }

    /** As marcas da caixa atual; caixa nova apaga as velhas. */
    private static Optional<Marks> current(UUID colonyId) {
        Optional<VillageBounds> box = VillageColonyMod.COLONIES.find(colonyId).flatMap(Colony::bounds);

        if (box.isEmpty()) {
            MARKS.remove(colonyId);

            return Optional.empty();
        }

        Marks marks = MARKS.get(colonyId);

        if (marks == null || !marks.box().equals(box.get())) {
            if (marks != null) {
                VillageColonyMod.LOGGER.info(
                        "Colony {} grew — its {} water and lava columns are read again on the next full sweep",
                        colonyId.toString().substring(0, 8), marks.fluid().size());
            }

            marks = new Marks(box.get(), new HashSet<>());
            MARKS.put(colonyId, marks);
        }

        return Optional.of(marks);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    static void clearAll() {
        MARKS.clear();
    }
}
