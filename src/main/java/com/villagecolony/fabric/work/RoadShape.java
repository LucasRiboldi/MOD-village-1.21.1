package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.RoadSides;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageRoad;
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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * O desenho que a vila pede ao grafo — Regras 183 a 185, 208 e 360 (autor): anel de caminho em volta do chafariz, o meio
 * do lado da plantação como entrada, sem anel em volta dela, e as pontas mais distantes crescendo com os dias.
 *
 * <p>Tudo aqui só propõe colunas ao grafo ({@link RoadGraphRefresh}): quem calça é o construtor, andando até lá.
 */
final class RoadShape {

    /** Quantas releituras até a entrada da plantação (o meio do lado, Regra 360) valer. */
    static final int CORNER_AFTER_READS = 3;

    /** Até onde, além da vila gerada, as pontas levam a vila (Regra 229). */
    static final int MAX_REACH = 128;

    /** A ponta calçada que sai da caixa por esta distância leva a caixa até ela (Regra 230). */
    static final int CLAIM_AFTER = 7;

    private static final long DAY = 24_000;

    private static final Map<UUID, Long> LAST_DAY = new HashMap<>();

    /** O que cresceu e ainda não foi calçado. */
    private static final Map<UUID, Set<ColonyPos>> GROWTH = new HashMap<>();

    static {
        ServerMemory.register(RoadShape.class, RoadShape::clearAll);
    }

    private RoadShape() {
    }

    static boolean isFarm(Building building) {
        return building.blueprint().path().contains("farm");
    }

    /** As caixas das plantações: da vila gerada e as obras prontas. */
    private static List<BlockBox> farms(ServerWorld world, Colony colony) {
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());
        List<BlockBox> boxes = new ArrayList<>(GeneratedVillages.piecesAt(world, hub.getX(), hub.getZ(), "_farm_"));

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (building.finished() && isFarm(building)) {
                BlockPos min = MinecraftTypeAdapter.toBlockPos(building.min());
                BlockPos max = MinecraftTypeAdapter.toBlockPos(building.max());

                boxes.add(new BlockBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()));
            }
        }

        return boxes;
    }

    /** O chão em volta do chafariz (Regra 183). */
    static Set<ColonyPos> rings(ServerWorld world, Colony colony, WorldPathTerrain terrain) {
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());
        Set<ColonyPos> ring = new HashSet<>();

        for (BlockBox box : GeneratedVillages.piecesAt(world, hub.getX(), hub.getZ(), "/town_centers/")) {
            ring.addAll(ringOf(world, terrain, box));
        }

        return ring;
    }

    /** O chão colado nas plantações: não é pedido como caminho, e o que já for caminho nele sai se puder (Regra 208). */
    static Set<ColonyPos> farmRings(ServerWorld world, Colony colony, WorldPathTerrain terrain) {
        Set<ColonyPos> ring = new HashSet<>();

        for (BlockBox box : farms(world, colony)) {
            ring.addAll(ringOf(world, terrain, box));
        }

        return ring;
    }

    /** A volta da caixa, um bloco fora dela. */
    static Set<ColonyPos> ringOf(ServerWorld world, WorldPathTerrain terrain, BlockBox box) {
        Set<ColonyPos> ring = new HashSet<>();

        for (int x = box.getMinX() - 1; x <= box.getMaxX() + 1; x++) {
            groundAt(world, terrain, x, box.getMinZ() - 1, ring);
            groundAt(world, terrain, x, box.getMaxZ() + 1, ring);
        }

        for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
            groundAt(world, terrain, box.getMinX() - 1, z, ring);
            groundAt(world, terrain, box.getMaxX() + 1, z, ring);
        }

        return ring;
    }

    /**
     * A entrada de cada plantação: o meio do lado mais perto da rua, na aresta e não na quina; sem rua, o mais perto do
     * centro (Regra 360).
     */
    static Set<ColonyPos> farmEntrances(ServerWorld world, Colony colony, WorldPathTerrain terrain, Set<ColonyPos> roads) {
        Set<ColonyPos> entrances = new HashSet<>();
        ColonyPos center = colony.center();

        for (BlockBox farm : farms(world, colony)) {
            sidesOf(world, terrain, farm).stream()
                    .min(Comparator.comparingInt((ColonyPos at) -> roads.isEmpty()
                                    ? Math.abs(at.x() - center.x()) + Math.abs(at.z() - center.z())
                                    : nearest(at, roads, farm))
                            .thenComparingInt(ColonyPos::x).thenComparingInt(ColonyPos::z))
                    .ifPresent(entrances::add);
        }

        return entrances;
    }

    /** A distância, em passos, da rua mais perto; o anel colado na plantação não conta como rua. */
    private static int nearest(ColonyPos at, Set<ColonyPos> roads, BlockBox farm) {
        int best = Integer.MAX_VALUE;

        for (ColonyPos road : roads) {
            if (road.x() >= farm.getMinX() - 1 && road.x() <= farm.getMaxX() + 1 && road.z() >= farm.getMinZ() - 1
                    && road.z() <= farm.getMaxZ() + 1) {
                continue;
            }

            best = Math.min(best, Math.abs(at.x() - road.x()) + Math.abs(at.z() - road.z()));
        }

        return best;
    }

    /** O meio de cada lado da plantação, um bloco para fora dela. */
    private static Set<ColonyPos> sidesOf(ServerWorld world, WorldPathTerrain terrain, BlockBox box) {
        Set<ColonyPos> sides = new HashSet<>();
        int midX = Math.floorDiv(box.getMinX() + box.getMaxX(), 2);
        int midZ = Math.floorDiv(box.getMinZ() + box.getMaxZ(), 2);

        for (int[] side : new int[][] {{midX, box.getMinZ() - 1}, {midX, box.getMaxZ() + 1}, {box.getMinX() - 1, midZ},
                {box.getMaxX() + 1, midZ}}) {
            cornerGround(world, terrain, side[0], side[1], box.getMinY()).ifPresent(sides::add);
        }

        return sides;
    }

    /**
     * O chão da entrada da plantação, procurado em volta da altura do canteiro {@code farmY}, e não do topo da coluna —
     * Regra 291: a entrada fica no nível da plantação, e um beiral, telhado ou copa em cima não a leva para o alto.
     */
    static java.util.Optional<ColonyPos> cornerGround(ServerWorld world, WorldPathTerrain terrain, int x, int z, int farmY) {
        if (!world.isChunkLoaded(x >> 4, z >> 4) || terrain.blocked(x, z)) {
            return java.util.Optional.empty();
        }

        OptionalInt ground = terrain.groundY(x, z, farmY);

        return ground.isPresent() ? java.util.Optional.of(new ColonyPos(x, ground.getAsInt(), z)) : java.util.Optional.empty();
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

    /**
     * Uma vez por dia, cada ponta azul, uma por lado da vila, avança um bloco para fora por aquele lado, até
     * {@value #MAX_REACH} blocos além da vila gerada — Regras 185 e 229. O construtor calça.
     */
    static void growTips(ServerWorld world, Colony colony, RoadGraph graph, WorldPathTerrain terrain) {
        long day = world.getTimeOfDay() / DAY;
        Long last = LAST_DAY.put(colony.id(), day);

        if (last == null || last == day) {
            return;
        }

        ColonyPos center = colony.center();
        List<RoadGraph.Node> tips = graph.nodes().stream().filter(RoadShape::isTip).toList();

        Optional<VillageBounds> reference = GeneratedVillages.boundsAt(world, center.x(), center.z()).or(colony::bounds);

        for (RoadGraph.Node tip : tips) {
            grow(world, colony.id(), tip.position(), center, reference, terrain);
        }
    }

    /** A ponta calçada a {@value #CLAIM_AFTER} blocos ou mais fora da caixa leva a caixa da vila até ela (Regra 230). */
    static void claimTips(ServerWorld world, Colony colony, RoadGraph graph) {
        Optional<VillageBounds> box = colony.bounds();

        if (box.isEmpty()) {
            return;
        }

        for (RoadGraph.Node tip : graph.nodes()) {
            ColonyPos at = tip.position();

            if (isTip(tip) && outsideBy(box.get(), at.x(), at.z()) >= CLAIM_AFTER
                    && world.isChunkLoaded(at.x() >> 4, at.z() >> 4)
                    && VillageRoad.isPathBlock(world.getBlockState(MinecraftTypeAdapter.toBlockPos(at)))
                    && colony.grow(VillageBounds.block(at))) {
                VillageColonyMod.LOGGER.info("VC_ROAD_TIP colony={} tip={} result=claimed box={}",
                        colony.id().toString().substring(0, 8), at, colony.bounds().orElseThrow());
            }
        }
    }

    private static boolean isTip(RoadGraph.Node node) {
        return node.roles().contains(RoadGraph.Role.ENDPOINT)
                && !node.roles().contains(RoadGraph.Role.ENTRANCE) && !node.roles().contains(RoadGraph.Role.HUB);
    }

    /** Quanto a coluna fica fora da caixa, pelo eixo em que mais sai; zero dentro. */
    static int outsideBy(VillageBounds box, int x, int z) {
        int dx = Math.max(Math.max(box.minX() - x, x - box.maxX()), 0);
        int dz = Math.max(Math.max(box.minZ() - z, z - box.maxZ()), 0);

        return Math.max(dx, dz);
    }

    private static void grow(ServerWorld world, UUID colonyId, ColonyPos tip, ColonyPos center,
                             Optional<VillageBounds> reference, WorldPathTerrain terrain) {
        Side outward = RoadSides.of(tip.x(), tip.z(), center);
        int x = tip.x() + outward.offsetX();
        int z = tip.z() + outward.offsetZ();

        if (reference.filter(box -> outsideBy(box, x, z) > MAX_REACH).isPresent()
                || !world.isChunkLoaded(x >> 4, z >> 4) || terrain.blocked(x, z)) {
            return;
        }

        OptionalInt ground = terrain.groundY(x, z, tip.y());

        if (ground.isEmpty() || Math.abs(ground.getAsInt() - tip.y()) > 1) {
            return;
        }

        GROWTH.computeIfAbsent(colonyId, ignored -> new HashSet<>()).add(new ColonyPos(x, ground.getAsInt(), z));
    }

    private static void clearAll() {
        LAST_DAY.clear();
        GROWTH.clear();
    }
}
