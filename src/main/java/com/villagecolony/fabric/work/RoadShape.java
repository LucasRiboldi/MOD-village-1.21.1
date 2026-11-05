package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * O desenho que a vila pede ao grafo — Regras 183 a 185 (autor, 2026-11-05): anel de caminho em volta do chafariz e das
 * plantações, a quina da plantação como entrada e as pontas mais distantes crescendo com os dias.
 *
 * <p>Tudo aqui só propõe colunas ao grafo ({@link RoadGraphRefresh}): quem calça é o construtor, andando até lá.
 */
final class RoadShape {

    /** Quantas releituras até a quina da plantação virar entrada. */
    static final int CORNER_AFTER_READS = 3;

    /** Quantas pontas crescem por dia, das mais distantes do centro. */
    private static final int TIPS_PER_DAY = 2;

    /** A ponta precisa estar a pelo menos esta fração da mais distante, em porcentagem. */
    private static final int FAR_ENOUGH_PERCENT = 75;

    private static final long DAY = 24_000;

    private static final Map<UUID, Long> LAST_DAY = new HashMap<>();

    /** O que cresceu e ainda não foi calçado. */
    private static final Map<UUID, Set<ColonyPos>> GROWTH = new HashMap<>();

    static {
        ServerMemory.register(RoadShape.class, RoadShape::clearAll);
    }

    private RoadShape() {
    }

    /** As caixas das plantações: da vila gerada e as obras prontas. */
    private static List<BlockBox> farms(ServerWorld world, Colony colony) {
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());
        List<BlockBox> boxes = new ArrayList<>(GeneratedVillages.piecesAt(world, hub.getX(), hub.getZ(), "_farm_"));

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (building.finished() && building.blueprint().path().contains("farm")) {
                BlockPos min = MinecraftTypeAdapter.toBlockPos(building.min());
                BlockPos max = MinecraftTypeAdapter.toBlockPos(building.max());

                boxes.add(new BlockBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()));
            }
        }

        return boxes;
    }

    /** O chão em volta do chafariz e de cada plantação (Regra 183): um bloco fora da caixa. */
    static Set<ColonyPos> rings(ServerWorld world, Colony colony, WorldPathTerrain terrain) {
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());
        List<BlockBox> boxes = farms(world, colony);

        boxes.addAll(GeneratedVillages.piecesAt(world, hub.getX(), hub.getZ(), "/town_centers/"));

        Set<ColonyPos> ring = new HashSet<>();

        for (BlockBox box : boxes) {
            for (int x = box.getMinX() - 1; x <= box.getMaxX() + 1; x++) {
                groundAt(world, terrain, x, box.getMinZ() - 1, ring);
                groundAt(world, terrain, x, box.getMaxZ() + 1, ring);
            }

            for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                groundAt(world, terrain, box.getMinX() - 1, z, ring);
                groundAt(world, terrain, box.getMaxX() + 1, z, ring);
            }
        }

        return ring;
    }

    /** As quatro quinas de cada anel: fixas no grafo, para ele ficar retângulo. */
    static Set<ColonyPos> corners(ServerWorld world, Colony colony, WorldPathTerrain terrain) {
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());
        List<BlockBox> boxes = farms(world, colony);

        boxes.addAll(GeneratedVillages.piecesAt(world, hub.getX(), hub.getZ(), "/town_centers/"));

        Set<ColonyPos> corners = new HashSet<>();

        for (BlockBox box : boxes) {
            corners.addAll(cornersOf(world, terrain, box));
        }

        return corners;
    }

    /** A quina de cada plantação mais perto do centro: a entrada dela (Regra 184). */
    static Set<ColonyPos> farmEntrances(ServerWorld world, Colony colony, WorldPathTerrain terrain) {
        Set<ColonyPos> entrances = new HashSet<>();
        ColonyPos center = colony.center();

        for (BlockBox farm : farms(world, colony)) {
            cornersOf(world, terrain, farm).stream()
                    .min(Comparator.comparingInt((ColonyPos at) -> Math.abs(at.x() - center.x()) + Math.abs(at.z() - center.z()))
                            .thenComparingInt(ColonyPos::x).thenComparingInt(ColonyPos::z))
                    .ifPresent(entrances::add);
        }

        return entrances;
    }

    private static Set<ColonyPos> cornersOf(ServerWorld world, WorldPathTerrain terrain, BlockBox box) {
        Set<ColonyPos> corners = new HashSet<>();

        for (int x : new int[] {box.getMinX() - 1, box.getMaxX() + 1}) {
            for (int z : new int[] {box.getMinZ() - 1, box.getMaxZ() + 1}) {
                groundAt(world, terrain, x, z, corners);
            }
        }

        return corners;
    }

    private static void groundAt(ServerWorld world, WorldPathTerrain terrain, int x, int z, Set<ColonyPos> into) {
        if (!world.isChunkLoaded(x >> 4, z >> 4) || terrain.blocked(x, z)) {
            return;
        }

        OptionalInt ground = terrain.groundY(x, z, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1);

        if (ground.isPresent()) {
            into.add(new ColonyPos(x, ground.getAsInt(), z));
        }
    }

    /** O que cresceu e falta calçar; tira o que já é caminho e o que a coluna recusou. */
    static Set<ColonyPos> pendingGrowth(UUID colonyId, Set<ColonyPos> paved, Set<Long> refused) {
        Set<ColonyPos> growth = GROWTH.getOrDefault(colonyId, Set.of());

        if (growth.isEmpty()) {
            return Set.of();
        }

        Set<ColonyPos> left = new HashSet<>(growth);

        left.removeAll(paved);
        left.removeIf(at -> refused.contains(com.villagecolony.core.construction.model.ColonyRoads.column(at.x(), at.z())));
        GROWTH.put(colonyId, left);

        return left;
    }

    /** Uma vez por dia, as pontas mais distantes do centro avançam um bloco em linha reta (Regra 185). */
    static void growTips(ServerWorld world, Colony colony, RoadGraph graph, WorldPathTerrain terrain) {
        long day = world.getTimeOfDay() / DAY;
        Long last = LAST_DAY.put(colony.id(), day);

        if (last == null || last == day) {
            return;
        }

        ColonyPos center = colony.center();
        List<RoadGraph.Node> tips = new ArrayList<>(graph.nodes().stream()
                .filter(node -> node.roles().contains(RoadGraph.Role.ENDPOINT)
                        && !node.roles().contains(RoadGraph.Role.ENTRANCE) && !node.roles().contains(RoadGraph.Role.HUB))
                .toList());

        tips.sort(Comparator.comparingInt((RoadGraph.Node node) -> -distance(node.position(), center)));

        if (tips.isEmpty()) {
            return;
        }

        int farthest = distance(tips.getFirst().position(), center);
        int grown = 0;

        for (RoadGraph.Node tip : tips) {
            if (grown >= TIPS_PER_DAY || distance(tip.position(), center) * 100 < farthest * FAR_ENOUGH_PERCENT) {
                break;
            }

            if (grow(world, colony.id(), graph, tip.position(), terrain)) {
                grown++;
            }
        }
    }

    private static int distance(ColonyPos a, ColonyPos b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.z() - b.z());
    }

    private static boolean grow(ServerWorld world, UUID colonyId, RoadGraph graph, ColonyPos tip, WorldPathTerrain terrain) {
        for (RoadGraph.Edge edge : graph.edges()) {
            List<ColonyPos> columns = edge.columns();
            int at = columns.indexOf(tip);

            if (at < 0 || columns.size() < 2 || (at != 0 && at != columns.size() - 1)) {
                continue;
            }

            ColonyPos before = columns.get(at == 0 ? 1 : at - 1);
            int dx = tip.x() - before.x();
            int dz = tip.z() - before.z();

            if (Math.abs(dx) + Math.abs(dz) != 1) {
                return false;
            }

            int x = tip.x() + dx;
            int z = tip.z() + dz;

            if (!world.isChunkLoaded(x >> 4, z >> 4) || terrain.blocked(x, z)) {
                return false;
            }

            OptionalInt ground = terrain.groundY(x, z, tip.y());

            if (ground.isEmpty() || Math.abs(ground.getAsInt() - tip.y()) > 1) {
                return false;
            }

            GROWTH.computeIfAbsent(colonyId, ignored -> new HashSet<>()).add(new ColonyPos(x, ground.getAsInt(), z));

            return true;
        }

        return false;
    }

    private static void clearAll() {
        LAST_DAY.clear();
        GROWTH.clear();
    }
}
