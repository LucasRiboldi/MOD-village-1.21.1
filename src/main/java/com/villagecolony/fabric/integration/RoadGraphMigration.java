package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Inspeção sem escrita para reconstruir o índice derivado — ADR-043. */
public final class RoadGraphMigration {
    private static final int SEARCH_DEPTH = 12;

    private RoadGraphMigration() {
    }

    /** Resultado incompleto nunca publica grafo; colunas desconhecidas permanecem pendentes. */
    public record Inspection(Optional<RoadGraph> graph, Set<Long> pending, Set<Long> missing) {
        public Inspection {
            pending = Set.copyOf(pending);
            missing = Set.copyOf(missing);
        }
    }

    /** Revalida ruas legadas sem carregar chunks ou alterar o índice original — ADR-043. */
    public static Inspection inspect(ServerWorld world, ColonyRoads legacy, Set<ColonyPos> entrances) {
        Set<Long> pending = new HashSet<>();
        Set<Long> missing = new HashSet<>();
        Set<ColonyPos> confirmed = new HashSet<>();

        // Índice antigo maior que o limite novo não pode ser truncado nem publicado — ADR-043.
        if (legacy.columns().size() > RoadNetwork.MAX_COLUMNS) {
            return new Inspection(Optional.empty(), Set.copyOf(legacy.columns()), Set.of());
        }

        for (long column : legacy.columns()) {
            int x = ColonyRoads.xOf(column);
            int z = ColonyRoads.zOf(column);
            if (!world.isChunkLoaded(new BlockPos(x, 0, z))) {
                pending.add(column);
                continue;
            }
            int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            int bottom = Math.max(world.getBottomY(), top - SEARCH_DEPTH);
            BlockPos ground = null;
            boolean ambiguous = false;
            for (int y = top; y >= bottom; y--) {
                BlockPos candidate = new BlockPos(x, y, z);
                if (!usable(world, candidate)) {
                    continue;
                }
                if (ground != null) {
                    ambiguous = true;
                    break;
                }
                ground = candidate;
            }
            // O legado não guarda Y: pisos concorrentes e cobertura além da janela não provam ausência — ADR-043.
            if (ambiguous || bottom > legacy.from().y()) {
                pending.add(column);
            } else if (ground == null) {
                missing.add(column);
            } else {
                confirmed.add(MinecraftTypeAdapter.toColonyPos(ground));
            }
        }

        if (!pending.isEmpty()) {
            return new Inspection(Optional.empty(), pending, missing);
        }

        ColonyPos hub = confirmed.stream().filter(at -> distance(at, legacy.from()) <= RoadNetwork.SEED_RADIUS)
                .min(Comparator.comparingInt((ColonyPos at) -> distance(at, legacy.from()))
                        .thenComparingInt(ColonyPos::x).thenComparingInt(ColonyPos::y).thenComparingInt(ColonyPos::z))
                .orElse(legacy.from());
        return new Inspection(Optional.of(RoadGraph.from(confirmed, hub, entrances)), pending, missing);
    }

    private static boolean usable(ServerWorld world, BlockPos ground) {
        BlockPos support = ground.down();
        var state = world.getBlockState(ground);
        // A coluna legada fornece o contexto do material de bioma; consultar estruturas pode carregar chunks — ADR-043.
        return (VillageRoad.isPaving(world, state) || VillageRoad.isBiomePath(state))
                && world.getBlockState(support).isSideSolidFullSquare(world, support, Direction.UP)
                && world.getBlockState(ground.up()).getCollisionShape(world, ground.up()).isEmpty()
                && world.getBlockState(ground.up(2)).getCollisionShape(world, ground.up(2)).isEmpty()
                && world.getFluidState(ground.up()).isEmpty()
                && world.getFluidState(ground.up(2)).isEmpty();
    }

    private static int distance(ColonyPos from, ColonyPos to) {
        return Math.max(Math.abs(from.x() - to.x()), Math.abs(from.z() - to.z()));
    }
}
