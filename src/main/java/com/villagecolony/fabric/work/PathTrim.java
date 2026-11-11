package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageBiomes;
import com.villagecolony.fabric.integration.VillageRoad;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A rua é uma linha de um bloco — Regras 186 e 242 (autor, 2026-11-05 e 2026-11-11): o caminho de dentro da caixa sem
 * nenhuma marca do grafo (linha, nó, entrada, o que falta calçar e o que está planejado) volta a ser o solo do bioma,
 * relva na planície; o caminho coberto, sem 2 livres em cima, vira terra (Regra 180). Nenhum bloco de caminho muda
 * sozinho: é trabalho de quem arruma o chão (pedreiro, carpinteiro e fundidor sem tarefa), {@value #BUDGET} colunas por
 * passada; porta e construção ficam.
 */
final class PathTrim {

    private static final int BUDGET = 16;

    private static final Map<UUID, Deque<ColonyPos>> SURPLUS = new HashMap<>();

    /** O caminho coberto achado na releitura, à espera de quem o devolva ao solo. */
    private static final Map<UUID, java.util.LinkedHashSet<BlockPos>> COVERED = new HashMap<>();

    /** Teto da fila do coberto: o resto volta na releitura seguinte. */
    static final int MAX_COVERED = 1_024;

    static {
        ServerMemory.register(PathTrim.class, () -> {
            SURPLUS.clear();
            COVERED.clear();
        });
    }

    /** Guarda o caminho coberto desta colônia para a próxima passada (Regras 180 e 242). */
    static void cover(UUID colonyId, BlockPos path) {
        java.util.LinkedHashSet<BlockPos> covered = COVERED.computeIfAbsent(colonyId, ignored -> new java.util.LinkedHashSet<>());

        if (covered.size() < MAX_COVERED) {
            covered.add(path.toImmutable());
        }
    }

    /** O caminho coberto à espera, para o teste. */
    static java.util.Set<BlockPos> coveredOf(UUID colonyId) {
        return java.util.Collections.unmodifiableSet(COVERED.getOrDefault(colonyId, new java.util.LinkedHashSet<>()));
    }

    /**
     * O caminho de dentro da caixa sem nenhuma marca do grafo: fora da linha e dos nós, fora da frente das portas, do que
     * falta calçar e do que está planejado (Regra 242).
     */
    static List<ColonyPos> unmarked(java.util.Set<ColonyPos> inside, com.villagecolony.core.construction.model.RoadGraph graph,
                                    java.util.Set<Long> atTheDoor, java.util.Collection<com.villagecolony.core.construction.model.PathStep> toPave,
                                    java.util.Collection<ColonyPos> planned) {
        java.util.Set<Long> marked = new java.util.HashSet<>(atTheDoor);

        graph.nodes().forEach(node -> marked.add(com.villagecolony.core.construction.model.ColonyRoads.column(
                node.position().x(), node.position().z())));
        graph.edges().forEach(edge -> edge.columns().forEach(at -> marked.add(
                com.villagecolony.core.construction.model.ColonyRoads.column(at.x(), at.z()))));
        toPave.forEach(step -> marked.add(com.villagecolony.core.construction.model.ColonyRoads.column(step.x(), step.z())));
        planned.forEach(at -> marked.add(com.villagecolony.core.construction.model.ColonyRoads.column(at.x(), at.z())));

        return inside.stream()
                .filter(at -> !marked.contains(com.villagecolony.core.construction.model.ColonyRoads.column(at.x(), at.z())))
                .toList();
    }

    private PathTrim() {
    }

    /** O que sobrou na última releitura do grafo. */
    static void offer(UUID colonyId, List<ColonyPos> surplus) {
        SURPLUS.put(colonyId, new ArrayDeque<>(surplus));
    }

    /** Uma passada de quem arruma o chão; devolve quantas colunas mudaram. O coberto vem primeiro. */
    static int pass(ServerWorld world, Colony colony) {
        Deque<ColonyPos> left = SURPLUS.get(colony.id());
        java.util.Iterator<BlockPos> covered = COVERED.getOrDefault(colony.id(), new java.util.LinkedHashSet<>()).iterator();
        int done = 0;

        while (covered.hasNext() && done < BUDGET) {
            BlockPos at = covered.next();

            covered.remove();

            if (world.isChunkLoaded(at.getX() >> 4, at.getZ() >> 4) && VillageRoad.isPathBlock(world.getBlockState(at))) {
                RoadGraphRefresh.coverPath(world, at.up(), world.getBlockState(at.up()));
                done += VillageRoad.isPathBlock(world.getBlockState(at)) ? 0 : 1;
            }
        }

        while (left != null && !left.isEmpty() && done < BUDGET) {
            ColonyPos cell = left.poll();
            BlockPos at = MinecraftTypeAdapter.toBlockPos(cell);

            if (world.isChunkLoaded(at.getX() >> 4, at.getZ() >> 4) && VillageRoad.isPathBlock(world.getBlockState(at))
                    && !BuildingGaps.insideABuilding(world, colony, at.getX(), at.getZ())) {
                world.setBlockState(at, VillageBiomes.foundationGroundAt(world, at).getDefaultState());
                done++;
            }
        }

        return done;
    }
}
