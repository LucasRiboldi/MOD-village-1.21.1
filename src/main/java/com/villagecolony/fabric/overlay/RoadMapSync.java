package com.villagecolony.fabric.overlay;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.RoadSettling;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.work.RoadGraphRefresh;
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
 * <p>Cores, as do desenho do autor: linha amarela, nó rosa, entrada branca; nó central vermelho, ponta azul, e cinza
 * só para o índice de ruas de quem ainda não tem grafo. O trecho da linha já travado pela Regra 225 sai verde, coluna a
 * coluna (Regra 234). Ligado por {@code /vc mae mostrar ligar}; não persiste.
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

    static final int LINE = 0xFFFF00;

    /** O trecho da linha que já travou (Regras 225 e 234). */
    static final int FIXED = 0x30FF60;
    static final int INDEX = 0x888888;
    static final int HUB = 0xFF3030;
    static final int ENTRANCE = 0xFFFFFF;
    static final int JUNCTION = 0xFF30FF;
    static final int OTHER = 0x80C0FF;

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
        return mapAround(world, center, RoadGraphRefresh.dayOf(world));
    }

    /** O desenho em volta deste ponto, com a trava contada até {@code today}. */
    static RoadMapPayload mapAround(ServerWorld world, BlockPos center, long today) {
        List<RoadMapPayload.Line> lines = new ArrayList<>();
        List<RoadMapPayload.Marker> markers = new ArrayList<>();
        int[] budget = {MAX_POINTS};

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            Set<Long> inGraph = new HashSet<>();
            var store = ColonySavedData.get(world.getServer()).roadGraphs();
            var graph = store.graph(colony.id());

            if (graph.isPresent()) {
                Set<Long> fixed = RoadSettling.fixed(store.settled(colony.id()), today);

                addGraph(graph.get(), fixed, center, lines, markers, inGraph, budget);
            } else {
                RoadIndex.roadsOf(colony.id())
                        .ifPresent(roads -> addIndex(world, roads, center, markers, inGraph, budget));
            }
        }

        return new RoadMapPayload(lines, markers);
    }

    private static void addGraph(RoadGraph graph, Set<Long> fixed, BlockPos center, List<RoadMapPayload.Line> lines,
                                 List<RoadMapPayload.Marker> markers, Set<Long> inGraph, int[] budget) {
        for (RoadGraph.Edge edge : graph.edges()) {
            List<Long> points = new ArrayList<>();
            List<Boolean> held = new ArrayList<>();

            for (ColonyPos at : edge.columns()) {
                long column = ColonyRoads.column(at.x(), at.z());

                inGraph.add(column);

                if (near(center, at.x(), at.z()) && budget[0] > 0) {
                    points.add(BlockPos.asLong(at.x(), at.y(), at.z()));
                    held.add(fixed.contains(column));
                    budget[0]--;
                }
            }

            addRuns(points, held, lines);
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

    /**
     * Parte a aresta em trechos de uma cor: o passo entre duas colunas travadas é verde, os outros são amarelos. Trechos
     * vizinhos dividem a coluna da troca, e a linha segue inteira na tela (Regra 234).
     */
    static void addRuns(List<Long> points, List<Boolean> held, List<RoadMapPayload.Line> lines) {
        if (points.size() < 2) {
            return;
        }

        List<Long> run = new ArrayList<>(List.of(points.getFirst()));
        int color = colorOfStep(held, 1);

        for (int i = 1; i < points.size(); i++) {
            int step = colorOfStep(held, i);

            if (step != color) {
                lines.add(new RoadMapPayload.Line(color, run));
                run = new ArrayList<>(List.of(points.get(i - 1)));
                color = step;
            }

            run.add(points.get(i));
        }

        lines.add(new RoadMapPayload.Line(color, run));
    }

    private static int colorOfStep(List<Boolean> held, int to) {
        return held.get(to - 1) && held.get(to) ? FIXED : LINE;
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

    static int colorOf(RoadGraph.Node node) {
        if (node.roles().contains(RoadGraph.Role.HUB)) {
            return HUB;
        }

        if (node.roles().contains(RoadGraph.Role.ENTRANCE)) {
            return ENTRANCE;
        }

        // Azul é só a ponta da vila (Regra 206).
        return node.roles().contains(RoadGraph.Role.ENDPOINT) ? OTHER : JUNCTION;
    }

    private static boolean near(BlockPos center, int x, int z) {
        return Math.abs(x - center.getX()) <= RANGE && Math.abs(z - center.getZ()) <= RANGE;
    }

    private static void clearAll() {
        WATCHERS.clear();
    }
}
