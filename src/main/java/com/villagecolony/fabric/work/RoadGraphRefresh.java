package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.RoadLines;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MaeRoadAccess;
import com.villagecolony.fabric.integration.NewVillage;
import com.villagecolony.fabric.integration.RoadGraphMigration;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.RoadNetwork;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * Relê o caminho da vila de tempos em tempos e refaz o grafo — Regra 177 (autor, 2026-11-05): o jogador pode corrigir
 * o caminho na mão e criar áreas e estruturas novas.
 *
 * <p>Só em vila nova ({@link NewVillage}). A cada {@value #EVERY} tiques varre a caixa da vila aos poucos, junta o que
 * achou ao índice de ruas, lê as entradas das construções e publica o grafo ({@link RoadLines}). O que falta calçar
 * vira um plano para o construtor, {@value #MAX_REPAIR} colunas por vez. Coluna que recusou não volta nesta sessão.
 */
public final class RoadGraphRefresh {

    static {
        ServerMemory.register(RoadGraphRefresh.class, RoadGraphRefresh::clearAll);
    }

    /** A releitura normal: 5 minutos. */
    static final long EVERY = 6_000;

    /** Com ligação ou calçada pendente: 30 segundos. */
    static final long SOON = 600;

    private static final int COLUMNS_PER_TICK = 2_048;

    private static final int MAX_REPAIR = 64;

    /** Cada ligação entre partes é uma busca cara: uma por releitura. */
    private static final int LINKS_PER_READ = 1;

    private static final class Scan {
        final VillageBounds box;
        final Set<ColonyPos> cells = new HashSet<>();
        int x;
        int z;

        Scan(VillageBounds box) {
            this.box = box;
            this.x = box.minX();
            this.z = box.minZ();
        }
    }

    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Map<UUID, Scan> SCANS = new HashMap<>();
    private static final Map<UUID, Set<Long>> REFUSED = new HashMap<>();
    private static final Map<UUID, Set<ColonyPos>> PLANNED = new HashMap<>();
    private static final Map<UUID, Integer> TURNS = new HashMap<>();

    private RoadGraphRefresh() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick(server.getOverworld()));
    }

    /** Pede a releitura na próxima passada. */
    public static void requestNow(UUID colonyId) {
        NEXT.put(colonyId, Long.MIN_VALUE);
        SCANS.remove(colonyId);
    }

    /** Esta coluna não aceitou caminho: fica fora dos próximos planos. */
    static void refused(UUID colonyId, PathStep step) {
        REFUSED.computeIfAbsent(colonyId, ignored -> new HashSet<>()).add(ColonyRoads.column(step.x(), step.z()));
    }

    private static void tick(ServerWorld world) {
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            Scan scan = SCANS.get(colony.id());

            if (scan != null) {
                advance(world, colony, scan, now);
            } else if (now >= NEXT.getOrDefault(colony.id(), Long.MIN_VALUE)) {
                begin(world, colony, now);
            }
        }
    }

    private static void begin(ServerWorld world, Colony colony, long now) {
        NEXT.put(colony.id(), now + SOON);

        if (colony.isActive() && colony.isAttended(now) && colony.bounds().isPresent() && NewVillage.is(world, colony)
                && MaeRoadAccess.metadataAllowed(world, colony.id())) {
            SCANS.put(colony.id(), new Scan(colony.bounds().orElseThrow()));
        }
    }

    private static void advance(ServerWorld world, Colony colony, Scan scan, long now) {
        for (int done = 0; done < COLUMNS_PER_TICK; done++) {
            if (scan.x > scan.box.maxX()) {
                SCANS.remove(colony.id());
                NEXT.put(colony.id(), publish(world, colony, scan.cells) ? now + SOON : now + EVERY);
                return;
            }

            read(world, scan);

            if (++scan.z > scan.box.maxZ()) {
                scan.z = scan.box.minZ();
                scan.x++;
            }
        }
    }

    private static void read(ServerWorld world, Scan scan) {
        if (!world.isChunkLoaded(scan.x >> 4, scan.z >> 4)) {
            return;
        }

        BlockPos at = new BlockPos(scan.x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, scan.x, scan.z) - 1, scan.z);

        if (VillageRoad.isPathBlock(world.getBlockState(at)) && RoadGraphMigration.usable(world, at)) {
            scan.cells.add(MinecraftTypeAdapter.toColonyPos(at));
        }
    }

    /** Publica o grafo; verdadeiro quando ainda há ligação ou calçada por fazer. */
    private static boolean publish(ServerWorld world, Colony colony, Set<ColonyPos> found) {
        UUID id = colony.id();
        Set<Long> known = new HashSet<>(RoadIndex.roadsOf(id).map(ColonyRoads::columns).orElse(List.of()));
        Set<Long> scanned = new HashSet<>();

        for (ColonyPos at : found) {
            long column = ColonyRoads.column(at.x(), at.z());

            scanned.add(column);

            // Caminho novo, do jogador: entra no índice como os outros.
            if (!known.contains(column)) {
                RoadIndex.remember(id, MinecraftTypeAdapter.toBlockPos(at));
            }
        }

        Set<ColonyPos> cells = new HashSet<>(found);

        for (long column : known) {
            if (!scanned.contains(column)) {
                int x = ColonyRoads.xOf(column);
                int z = ColonyRoads.zOf(column);

                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    return true;
                }

                RoadGraphMigration.firstFloor(world, x, z).ifPresent(at -> cells.add(MinecraftTypeAdapter.toColonyPos(at)));
            }
        }

        var store = ColonySavedData.get(world.getServer()).roadGraphs();

        if (cells.size() > RoadNetwork.MAX_COLUMNS || !store.versionSupported(id)
                || !MaeRoadAccess.metadataAllowed(world, id)) {
            return false;
        }

        WorldPathTerrain terrain = new WorldPathTerrain(world, id);
        RoadLines.Result result = RoadLines.build(cells, PLANNED.getOrDefault(id, Set.of()), colony.center(),
                entrances(world, colony, terrain), terrain, LINKS_PER_READ, TURNS.merge(id, 1, Integer::sum));

        result.graph().ifPresent(graph -> update(colony, store, graph, result));

        return offerRepair(world, id, result.toPave()) || result.isolated() > 0;
    }

    private static void update(Colony colony, com.villagecolony.data.save.RoadGraphStore store, RoadGraph graph,
                               RoadLines.Result result) {
        try {
            if (!store.graph(colony.id()).equals(Optional.of(graph))) {
                store.update(colony.id(), graph);
                VillageColonyMod.LOGGER.info("VC_ROAD_GRAPH colony={} nodes={} edges={} toPave={} isolated={}",
                        colony.id().toString().substring(0, 8), graph.nodes().size(), graph.edges().size(),
                        result.toPave().size(), result.isolated());
            }
        } catch (IllegalArgumentException refused) {
            // O grafo anterior continua: formato recusado não derruba o servidor nem o save.
            VillageColonyMod.LOGGER.warn("VC_ROAD_GRAPH colony={} refused by the save format: {}",
                    colony.id().toString().substring(0, 8), refused.getMessage());
        }
    }

    private static boolean offerRepair(ServerWorld world, UUID id, List<PathStep> toPave) {
        Set<Long> refused = REFUSED.getOrDefault(id, Set.of());
        List<PathStep> steps = new ArrayList<>();
        Set<ColonyPos> planned = new HashSet<>();

        for (PathStep step : toPave) {
            if (!refused.contains(ColonyRoads.column(step.x(), step.z())) && !isPaved(world, step)) {
                steps.add(step);
                planned.add(new ColonyPos(step.x(), step.floorY(), step.z()));
            }
        }

        // A ligação traçada continua valendo até ser calçada: a próxima releitura liga outra parte.
        PLANNED.put(id, planned);

        if (steps.isEmpty()) {
            return false;
        }

        PathLinks.offerRepair(id, steps.subList(0, Math.min(MAX_REPAIR, steps.size())));

        return true;
    }

    private static boolean isPaved(ServerWorld world, PathStep step) {
        return VillageRoad.isPathBlock(world.getBlockState(new BlockPos(step.x(), step.floorY(), step.z())));
    }

    /** O chão em frente às entradas: portas das construções e das casas da vila, portão, e o lado de quem não tem. */
    private static Set<ColonyPos> entrances(ServerWorld world, Colony colony, WorldPathTerrain terrain) {
        Set<ColonyPos> fronts = new HashSet<>();
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (building.finished()) {
                BlockPos min = MinecraftTypeAdapter.toBlockPos(building.min());
                BlockPos max = MinecraftTypeAdapter.toBlockPos(building.max());

                front(world, min, max, hub, terrain).ifPresent(fronts::add);
            }
        }

        for (BlockBox house : GeneratedVillages.housesAt(world, hub.getX(), hub.getZ())) {
            PathLinks.entranceOf(world, new BlockPos(house.getMinX(), house.getMinY(), house.getMinZ()),
                            new BlockPos(house.getMaxX(), house.getMaxY(), house.getMaxZ()))
                    .ifPresent(entrance -> fronts.add(floorOf(entrance.front())));
        }

        return fronts;
    }

    private static ColonyPos floorOf(BlockPos front) {
        return MinecraftTypeAdapter.toColonyPos(front.down());
    }

    private static Optional<ColonyPos> front(ServerWorld world, BlockPos min, BlockPos max, BlockPos hub,
                                             WorldPathTerrain terrain) {
        Optional<PathLinks.Entrance> door = PathLinks.entranceOf(world, min, max);

        if (door.isPresent()) {
            return Optional.of(floorOf(door.get().front()));
        }

        Optional<ColonyPos> gate = gateFront(world, min, max, terrain);

        return gate.isPresent() ? gate : sideFacing(min, max, hub, terrain);
    }

    /** Curral: o lado de fora do primeiro portão da borda. */
    private static Optional<ColonyPos> gateFront(ServerWorld world, BlockPos min, BlockPos max, WorldPathTerrain terrain) {
        for (BlockPos at : BlockPos.iterate(min.getX(), min.getY(), min.getZ(), max.getX(), min.getY() + 2, max.getZ())) {
            boolean border = at.getX() == min.getX() || at.getX() == max.getX()
                    || at.getZ() == min.getZ() || at.getZ() == max.getZ();

            if (border && world.getBlockState(at).getBlock() instanceof FenceGateBlock) {
                int x = at.getX() == min.getX() ? at.getX() - 1 : at.getX() == max.getX() ? at.getX() + 1 : at.getX();
                int z = at.getZ() == min.getZ() ? at.getZ() - 1 : at.getZ() == max.getZ() ? at.getZ() + 1 : at.getZ();
                OptionalInt ground = terrain.groundY(x, z, at.getY() - 1);

                if (ground.isPresent()) {
                    return Optional.of(new ColonyPos(x, ground.getAsInt(), z));
                }
            }
        }

        return Optional.empty();
    }

    /** Roça e o que não tem porta nem portão: o meio do lado virado para o centro da vila. */
    private static Optional<ColonyPos> sideFacing(BlockPos min, BlockPos max, BlockPos hub, WorldPathTerrain terrain) {
        int midX = (min.getX() + max.getX()) / 2;
        int midZ = (min.getZ() + max.getZ()) / 2;
        int[][] sides = {{midX, min.getZ() - 1}, {midX, max.getZ() + 1}, {min.getX() - 1, midZ}, {max.getX() + 1, midZ}};
        Optional<ColonyPos> best = Optional.empty();
        int bestDistance = Integer.MAX_VALUE;

        for (int[] side : sides) {
            OptionalInt ground = terrain.groundY(side[0], side[1], min.getY());
            int distance = Math.abs(side[0] - hub.getX()) + Math.abs(side[1] - hub.getZ());

            if (ground.isPresent() && !terrain.blocked(side[0], side[1]) && distance < bestDistance) {
                bestDistance = distance;
                best = Optional.of(new ColonyPos(side[0], ground.getAsInt(), side[1]));
            }
        }

        return best;
    }

    private static void clearAll() {
        NEXT.clear();
        SCANS.clear();
        REFUSED.clear();
        PLANNED.clear();
        TURNS.clear();
    }
}
