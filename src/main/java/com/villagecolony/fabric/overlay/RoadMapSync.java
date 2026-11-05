package com.villagecolony.fabric.overlay;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.integration.RoadIndex;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O sinalizador dos caminhos, só para o desenvolvimento do mod — autor, 2026-11-04: mostra ao operador que pediu o que
 * a vila guarda como caminho, a {@value #RANGE} blocos dele: arestas do grafo das ruas ({@code RoadGraph}), os nós
 * com o papel de cada um, e as colunas do índice de ruas fora do grafo.
 *
 * <p>Cores: rua principal ciano, secundária amarela, índice cinza; nó central vermelho, entrada verde, junção
 * magenta, os demais brancos. Ligado por {@code /vc mae mostrar ligar}; não persiste.
 */
public final class RoadMapSync {

    static {
        ServerMemory.register(RoadMapSync.class, RoadMapSync::clearAll);
    }

    private static final int EVERY_TICKS = 40;

    /** Até onde em volta do jogador o desenho vai. */
    static final int RANGE = 192;

    /** O teto de pontos e marcadores de um pacote. */
    static final int MAX_POINTS = 8_000;

    static final int PRIMARY = 0x00FFFF;
    static final int SECONDARY = 0xFFFF00;
    static final int INDEX = 0x888888;
    static final int HUB = 0xFF3030;
    static final int ENTRANCE = 0x30FF30;
    static final int JUNCTION = 0xFF30FF;
    static final int OTHER = 0xFFFFFF;

    private static final Map<UUID, RoadMapPayload> WATCHERS = new HashMap<>();

    private static final RoadMapPayload NOTHING = new RoadMapPayload(List.of(), List.of());

    private RoadMapSync() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!WATCHERS.isEmpty() && server.getTicks() % EVERY_TICKS == 0) {
                tick(server);
            }
        });
    }

    /** Liga ou desliga o desenho para este jogador; desligar apaga o que está na tela. */
    public static void set(ServerPlayerEntity player, boolean on) {
        if (on) {
            WATCHERS.putIfAbsent(player.getUuid(), NOTHING);
        } else {
            WATCHERS.remove(player.getUuid());

            if (ServerPlayNetworking.canSend(player, RoadMapPayload.ID)) {
                ServerPlayNetworking.send(player, NOTHING);
            }
        }
    }

    private static void tick(MinecraftServer server) {
        WATCHERS.keySet().removeIf(id -> server.getPlayerManager().getPlayer(id) == null);

        for (Map.Entry<UUID, RoadMapPayload> watcher : WATCHERS.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(watcher.getKey());

            if (player == null || !ServerPlayNetworking.canSend(player, RoadMapPayload.ID)) {
                continue;
            }

            RoadMapPayload map = mapAround(player.getServerWorld(), player.getBlockPos());

            if (!map.equals(watcher.getValue())) {
                watcher.setValue(map);
                ServerPlayNetworking.send(player, map);
            }
        }
    }

    /** O desenho em volta deste ponto. */
    static RoadMapPayload mapAround(ServerWorld world, BlockPos center) {
        List<RoadMapPayload.Line> lines = new ArrayList<>();
        List<RoadMapPayload.Marker> markers = new ArrayList<>();
        int[] budget = {MAX_POINTS};

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            Set<Long> inGraph = new HashSet<>();

            ColonySavedData.get(world.getServer()).roadGraphs().graph(colony.id())
                    .ifPresent(graph -> addGraph(graph, center, lines, markers, inGraph, budget));
            RoadIndex.roadsOf(colony.id())
                    .ifPresent(roads -> addIndex(world, roads, center, markers, inGraph, budget));
        }

        return new RoadMapPayload(lines, markers);
    }

    private static void addGraph(RoadGraph graph, BlockPos center, List<RoadMapPayload.Line> lines,
                                 List<RoadMapPayload.Marker> markers, Set<Long> inGraph, int[] budget) {
        for (RoadGraph.Edge edge : graph.edges()) {
            List<Long> points = new ArrayList<>();

            for (ColonyPos at : edge.columns()) {
                inGraph.add(ColonyRoads.column(at.x(), at.z()));

                if (near(center, at.x(), at.z()) && budget[0] > 0) {
                    points.add(BlockPos.asLong(at.x(), at.y(), at.z()));
                    budget[0]--;
                }
            }

            if (points.size() > 1) {
                lines.add(new RoadMapPayload.Line(edge.roadClass() == RoadGraph.RoadClass.PRIMARY ? PRIMARY : SECONDARY,
                        points));
            }
        }

        for (RoadGraph.Node node : graph.nodes()) {
            ColonyPos at = node.position();

            inGraph.add(ColonyRoads.column(at.x(), at.z()));

            if (near(center, at.x(), at.z()) && budget[0] > 0) {
                markers.add(new RoadMapPayload.Marker(colorOf(node), BlockPos.asLong(at.x(), at.y(), at.z())));
                budget[0]--;
            }
        }
    }

    private static void addIndex(ServerWorld world, ColonyRoads roads, BlockPos center,
                                 List<RoadMapPayload.Marker> markers, Set<Long> inGraph, int[] budget) {
        for (long column : roads.columns()) {
            int x = ColonyRoads.xOf(column);
            int z = ColonyRoads.zOf(column);

            if (!inGraph.contains(column) && near(center, x, z) && budget[0] > 0
                    && world.isChunkLoaded(x >> 4, z >> 4)) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

                markers.add(new RoadMapPayload.Marker(INDEX, BlockPos.asLong(x, y, z)));
                budget[0]--;
            }
        }
    }

    private static int colorOf(RoadGraph.Node node) {
        if (node.roles().contains(RoadGraph.Role.HUB)) {
            return HUB;
        }

        if (node.roles().contains(RoadGraph.Role.ENTRANCE)) {
            return ENTRANCE;
        }

        return node.roles().contains(RoadGraph.Role.JUNCTION) ? JUNCTION : OTHER;
    }

    private static boolean near(BlockPos center, int x, int z) {
        return Math.abs(x - center.getX()) <= RANGE && Math.abs(z - center.getZ()) <= RANGE;
    }

    private static void clearAll() {
        WATCHERS.clear();
    }
}
