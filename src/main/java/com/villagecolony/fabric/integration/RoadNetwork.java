package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * As ruas da vila medidas como rede de caminhos conectados — item 4 do autor, 2026-10-13:
 * <i>"criar um método de medir as ruas considerando o ciclo de caminhos conectados"</i>.
 *
 * <p>Parte dos blocos de calçamento perto do centro e segue de bloco em bloco pelos quatro lados,
 * subindo ou descendo um bloco, até {@link #MARGIN} além da caixa e no máximo {@link #MAX_COLUMNS}
 * colunas. O resultado é o índice de ruas da busca de lote: ela anda direto pelas ruas, sem a
 * varredura em anéis que levava ~20 minutos por volta (reanálise de 13-10, C4). Caminho solto,
 * sem ligação com a vila, fica de fora.
 */
public final class RoadNetwork {

    static {
        ServerMemory.register(RoadNetwork.class, RoadNetwork::clearAll);
    }

    /** Até quantas colunas a rede tem. */
    static final int MAX_COLUMNS = 6_000;

    /** Até quantos blocos além da caixa a rede segue. */
    static final int MARGIN = 32;

    /** Até quantos blocos do centro se procura o primeiro calçamento. */
    static final int SEED_RADIUS = 24;

    /** De quanto em quanto tempo uma colônia pode traçar a rede de novo. */
    static final long RETRACE_EVERY = 600;

    private static final Map<UUID, Long> TRACED_AT = new HashMap<>();

    private RoadNetwork() {
    }

    /**
     * O índice desta colônia pela rede de caminhos, se ela ainda não tem um.
     *
     * @return o índice novo, quando foi traçado agora
     */
    public static Optional<ColonyRoads> indexIfMissing(ServerWorld world, UUID colonyId) {
        if (SweepState.ROADS.containsKey(colonyId)
                || world.getTime() - TRACED_AT.getOrDefault(colonyId, Long.MIN_VALUE / 2) < RETRACE_EVERY) {
            return Optional.empty();
        }

        TRACED_AT.put(colonyId, world.getTime());

        Optional<ColonyRoads> roads = VillageColonyMod.COLONIES.find(colonyId).flatMap(colony -> trace(world, colony));

        roads.ifPresent(found -> {
            SweepState.ROADS.put(colonyId, found);
            SweepState.ROAD_CURSOR.remove(colonyId);
            VillageColonyMod.LOGGER.info("Colony {} traced its road network — {} connected path columns",
                    colonyId.toString().substring(0, 8), found.columns().size());
        });

        return roads;
    }

    /** A rede de caminhos conectados desta colônia; vazia sem calçamento perto do centro. */
    static Optional<ColonyRoads> trace(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        VillageBounds box = colony.bounds().orElse(null);
        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<Long> seen = new LinkedHashSet<>();

        for (BlockPos column : BlockPos.iterateOutwards(center.withY(0), SEED_RADIUS, 0, SEED_RADIUS)) {
            surfacePaving(world, column.getX(), column.getZ()).ifPresent(seed -> {
                if (seen.add(ColonyRoads.column(seed.getX(), seed.getZ()))) {
                    frontier.add(seed);
                }
            });
        }

        while (!frontier.isEmpty() && seen.size() < MAX_COLUMNS) {
            BlockPos at = frontier.poll();

            for (Direction side : Direction.Type.HORIZONTAL) {
                BlockPos next = at.offset(side);
                long column = ColonyRoads.column(next.getX(), next.getZ());

                if (seen.contains(column) || (box != null && !box.containsColumn(next.getX(), next.getZ(), MARGIN))
                        || !world.isChunkLoaded(next)) {
                    continue;
                }

                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos step = next.up(dy);

                    if (isPaving(world, step)) {
                        seen.add(column);
                        frontier.add(step);
                        break;
                    }
                }
            }
        }

        if (seen.isEmpty()) {
            return Optional.empty();
        }

        List<Long> columns = new ArrayList<>(seen);

        return Optional.of(new ColonyRoads(colony.id(), colony.center(), columns));
    }

    private static Optional<BlockPos> surfacePaving(ServerWorld world, int x, int z) {
        if (!world.isChunkLoaded(new BlockPos(x, 0, z))) {
            return Optional.empty();
        }

        BlockPos ground = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);

        return isPaving(world, ground) ? Optional.of(ground) : Optional.empty();
    }

    private static boolean isPaving(ServerWorld world, BlockPos at) {
        return VillageRoad.isPaving(world, world.getBlockState(at)) && world.getBlockState(at.up()).isReplaceable();
    }

    static void clearAll() {
        TRACED_AT.clear();
    }
}
