package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.construction.model.PlannedPaths;
import com.villagecolony.core.construction.model.RetiredSpurs;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.RoadLines;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.PathProposals;
import com.villagecolony.fabric.integration.RoadGraphMigration;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.RoadNetwork;
import com.villagecolony.fabric.integration.RoadPaving;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Relê o caminho da vila de tempos em tempos e refaz o grafo — Regra 177 (autor, 2026-11-05): o jogador pode corrigir
 * o caminho na mão e criar áreas e estruturas novas.
 *
 * <p>A cada {@value #EVERY} tiques varre a caixa da vila aos poucos, junta o que
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

    /** Até onde, fora da caixa da vila, a varredura procura portão de mina: a boca costuma ficar na borda. */
    private static final int GATE_MARGIN = 32;

    private static final class Scan {
        final UUID colony;
        final VillageBounds box;
        final Set<ColonyPos> cells = new HashSet<>();
        int x;
        int z;

        Scan(UUID colony, VillageBounds box) {
            this.colony = colony;
            this.box = box;
            this.x = box.minX() - GATE_MARGIN;
            this.z = box.minZ() - GATE_MARGIN;
        }

        boolean inTheBox() {
            return x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ();
        }
    }

    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Map<UUID, Scan> SCANS = new HashMap<>();
    private static final Map<UUID, Set<Long>> REFUSED = new HashMap<>();
    private static final Map<UUID, PlannedPaths> PLANNED = new HashMap<>();
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
                begin(colony, now);
            }
        }
    }

    private static void begin(Colony colony, long now) {
        NEXT.put(colony.id(), now + SOON);

        if (colony.isActive() && colony.isAttended(now) && colony.bounds().isPresent()) {
            SCANS.put(colony.id(), new Scan(colony.id(), colony.bounds().orElseThrow()));
        }
    }

    private static void advance(ServerWorld world, Colony colony, Scan scan, long now) {
        for (int done = 0; done < COLUMNS_PER_TICK; done++) {
            if (scan.x > scan.box.maxX() + GATE_MARGIN) {
                SCANS.remove(colony.id());
                NEXT.put(colony.id(), publish(world, colony, scan.cells) ? now + SOON : now + EVERY);
                return;
            }

            read(world, scan);

            if (++scan.z > scan.box.maxZ() + GATE_MARGIN) {
                scan.z = scan.box.minZ() - GATE_MARGIN;
                scan.x++;
            }
        }
    }

    private static void read(ServerWorld world, Scan scan) {
        if (!world.isChunkLoaded(scan.x >> 4, scan.z >> 4)) {
            return;
        }

        BlockPos at = new BlockPos(scan.x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, scan.x, scan.z) - 1, scan.z);
        BlockState top = world.getBlockState(at);

        MineGates.look(world, scan.colony, at, top);

        if (!scan.inTheBox()) {
            return;
        }

        if (VillageRoad.isPathBlock(top)) {
            if (RoadGraphMigration.usable(world, at)) {
                scan.cells.add(MinecraftTypeAdapter.toColonyPos(at));
            }

            return;
        }

        coverPath(world, at, top);
    }

    /** Caminho com bloco em cima deixa de ser caminho — Regra 180: vira terra, ou areia no deserto. */
    static void coverPath(ServerWorld world, BlockPos on, BlockState top) {
        BlockPos under = on.down();
        BlockState path = world.getBlockState(under);

        if (VillageRoad.isPathBlock(path) && !top.getCollisionShape(world, on).isEmpty()) {
            world.setBlockState(under, path.isOf(Blocks.DIRT_PATH) ? Blocks.DIRT.getDefaultState() : Blocks.SAND.getDefaultState());
            VillageColonyMod.LOGGER.info("VC_ROAD_COVERED path at {} under {} is ground again", under.toShortString(),
                    net.minecraft.registry.Registries.BLOCK.getId(top.getBlock()));
        }
    }

    /** Publica o grafo; verdadeiro quando ainda há ligação ou calçada por fazer. */
    static boolean publish(ServerWorld world, Colony colony, Set<ColonyPos> found) {
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

        if (cells.size() > RoadNetwork.MAX_COLUMNS || !store.versionSupported(id)) {
            return false;
        }

        WorldPathTerrain terrain = new WorldPathTerrain(world, id);
        WorldPathTerrain bridging = new WorldPathTerrain(world, id, true);
        int turn = TURNS.merge(id, 1, Integer::sum);
        PlannedPaths paths = PLANNED.computeIfAbsent(id, ignored -> new PlannedPaths());

        paths.read(turn, PathProposals.drain(id), at -> false);

        Set<ColonyPos> planned = paths.cells();
        StructureFronts.Reading reading = StructureFronts.read(world, colony, terrain, cells);
        Set<ColonyPos> fronts = reading.fronts();
        Set<ColonyPos> retired = reading.retired();

        planned.addAll(reading.ways());
        planned.addAll(RoadShape.rings(world, colony, terrain));
        planned.addAll(RoadShape.pendingGrowth(id, cells, REFUSED.getOrDefault(id, Set.of())));

        if (turn >= RoadShape.CORNER_AFTER_READS) {
            StructureFronts.farmCorners(RoadShape.farmEntrances(world, colony, terrain), store.graph(id), fronts, retired);
        }

        RoadLines.Result result = RoadLines.build(cells, planned, colony.center(), fronts,
                RoadShape.farmRings(world, colony, terrain), terrain, bridging, BridgeDeck.MAX_SPAN, LINKS_PER_READ, turn);

        result.graph().ifPresent(graph -> {
            update(colony, store, graph, result);
            publishNetwork(colony, graph);
            RoadShape.growTips(world, colony, graph, terrain);
        });

        Set<ColonyPos> surplus = new java.util.LinkedHashSet<>(result.surplus());

        result.graph().ifPresent(graph -> surplus.addAll(RetiredSpurs.of(cells, graph, retired)));
        PathTrim.offer(id, List.copyOf(surplus));

        // Degrau e assoalho em frente à porta ficam como estão: a linha só passa (Regra 223).
        Set<Long> atTheDoor = new HashSet<>();

        fronts.forEach(at -> atTheDoor.add(ColonyRoads.column(at.x(), at.z())));
        reading.ways().forEach(at -> atTheDoor.add(ColonyRoads.column(at.x(), at.z())));

        return offerRepair(world, id, result.toPave(), turn, atTheDoor) || result.isolated() > 0;
    }

    /** As linhas do grafo viram a rede que os outros sistemas consultam (Regra 216); a entrada solta ainda não é rede. */
    private static void publishNetwork(Colony colony, RoadGraph graph) {
        Set<Long> lines = new java.util.TreeSet<>();

        graph.edges().forEach(edge -> edge.columns().forEach(at -> lines.add(ColonyRoads.column(at.x(), at.z()))));
        RoadNetwork.publish(colony.id(), colony.center(), List.copyOf(lines));
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

    private static boolean offerRepair(ServerWorld world, UUID id, List<PathStep> toPave, int read, Set<Long> atTheDoor) {
        Set<Long> refused = REFUSED.getOrDefault(id, Set.of());
        List<PathStep> steps = new ArrayList<>();
        Set<ColonyPos> wanted = new HashSet<>();

        for (PathStep step : toPave) {
            if (!refused.contains(ColonyRoads.column(step.x(), step.z())) && !isPaved(world, step)
                    && (!atTheDoor.contains(ColonyRoads.column(step.x(), step.z()))
                    || RoadPaving.takesPath(world, new BlockPos(step.x(), step.floorY(), step.z())))) {
                steps.add(step);
                wanted.add(new ColonyPos(step.x(), step.floorY(), step.z()));
            }
        }

        // A ligação traçada continua valendo até ser calçada: a próxima releitura liga outra parte (Regra 214).
        PlannedPaths paths = PLANNED.computeIfAbsent(id, ignored -> new PlannedPaths());

        paths.read(read, wanted, at -> refused.contains(ColonyRoads.column(at.x(), at.z()))
                || isPaved(world, new PathStep(at.x(), at.y(), at.z(), com.villagecolony.core.type.Side.NORTH, 0, 0)));
        PathProposals.planned(id, paths.cells());

        if (steps.isEmpty()) {
            return false;
        }

        PathLinks.offerRepair(id, steps.subList(0, Math.min(MAX_REPAIR, steps.size())));

        return true;
    }

    private static boolean isPaved(ServerWorld world, PathStep step) {
        return VillageRoad.isPathBlock(world.getBlockState(new BlockPos(step.x(), step.floorY(), step.z())));
    }

    private static void clearAll() {
        MineGates.clearAll();
        NEXT.clear();
        SCANS.clear();
        REFUSED.clear();
        PLANNED.clear();
        TURNS.clear();
    }
}
