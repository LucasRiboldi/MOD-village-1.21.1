package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.TerrainDefects;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StreetSurface;
import com.villagecolony.fabric.integration.VillageRoad;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O terreno da vila inteira arrumado pelos aldeões — Regra 323 (autor, 2026-11-18, escolha "a vila inteira"): a caixa da
 * colônia vira uma fila de quadrados de {@value #SQUARE}×{@value #SQUARE}. Cada passada lê {@value #SURVEYS} quadrados e
 * trabalha no de maior prioridade ({@link TerrainDefects}): primeiro buraco e bloco isolados, depois degrau maior que 1 no
 * caminho, depois degrau maior que 1 no resto. Tira ou põe solo, até {@value TerrainDefects#DAILY_COLUMNS} colunas por dia
 * por colônia; construção, peça da vila, obra, água e a boca da mina ficam como estão ({@link GroundSmoothing#isGround}).
 */
final class TerrainQueue {

    static {
        ServerMemory.register(TerrainQueue.class, TerrainQueue::clearAll);
    }

    /** O lado do quadrado da fila. */
    static final int SQUARE = 16;

    /** Quadrados lidos por passada. */
    static final int SURVEYS = 8;

    /** Quantos quadrados com defeito a passada tenta, quando o melhor não deixa mexer em nada. */
    private static final int TRIES = 3;

    /** A janela alisada em volta do quadrado: o anel dela fica fixo, então o quadrado inteiro pode mudar. */
    private static final int MARGIN = (GroundSmoothing.WINDOW - SQUARE) / 2;

    private static final class State {

        private int cursor;
        private final Map<Integer, TerrainDefects.Survey> surveys = new HashMap<>();
        private final TerrainDefects.Daily daily = new TerrainDefects.Daily();
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private TerrainQueue() {
    }

    /** Uma passada: lê os próximos quadrados e arruma o pior; devolve quantas colunas mudaram. */
    static int pass(ServerWorld world, Colony colony, VillagerEntity worker) {
        Optional<VillageBounds> bounds = colony.bounds();

        if (bounds.isEmpty()) {
            return 0;
        }

        VillageBounds box = bounds.get();
        State state = STATES.computeIfAbsent(colony.id(), id -> new State());
        long today = world.getTimeOfDay() / 24_000;
        int allowance = state.daily.allowance(today, GroundSmoothing.EDITS);

        if (allowance == 0) {
            return 0;
        }

        int across = (box.maxX() - box.minX()) / SQUARE + 1;
        int count = across * ((box.maxZ() - box.minZ()) / SQUARE + 1);

        for (int read = 0; read < Math.min(SURVEYS, count); read++) {
            int index = state.cursor % count;

            state.cursor = (state.cursor + 1) % count;
            state.surveys.put(index, survey(world, colony.id(), box, across, index));
        }

        List<Integer> worst = new ArrayList<>(state.surveys.keySet());

        worst.removeIf(index -> index >= count || state.surveys.get(index).score() == 0);
        worst.sort(Comparator.comparingLong((Integer index) -> -state.surveys.get(index).score()));

        for (int index : worst.subList(0, Math.min(TRIES, worst.size()))) {
            int done = work(world, colony.id(), box, across, index, state.surveys.get(index), allowance);

            state.surveys.put(index, survey(world, colony.id(), box, across, index));

            if (done > 0) {
                state.daily.spend(today, done);
                ActionJournal.action(worker.getUuid(), "LEVELLED", "minecraft:dirt", done,
                        new BlockPos(minX(box, across, index) + SQUARE / 2, worker.getBlockY(),
                                minZ(box, across, index) + SQUARE / 2), "village terrain");

                return done;
            }

            // Nada pôde ser mexido: o quadrado espera a próxima leitura dele, e a passada tenta o seguinte.
            state.surveys.remove(index);
        }

        return 0;
    }

    private static int work(ServerWorld world, UUID colonyId, VillageBounds box, int across, int index,
                            TerrainDefects.Survey survey, int allowance) {
        int minX = minX(box, across, index);
        int minZ = minZ(box, across, index);

        if (survey.isolated().isEmpty()) {
            return GroundSmoothing.smoothWindow(world, colonyId, minX - MARGIN, minZ - MARGIN, SQUARE + 2 * MARGIN,
                    allowance);
        }

        int done = 0;

        for (int[] column : survey.isolated()) {
            if (done >= allowance) {
                break;
            }

            int x = minX + column[0];
            int z = minZ + column[1];
            int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

            if (GroundSmoothing.column(world, x, z, top, column[2])) {
                done++;
            }
        }

        return done;
    }

    /** Lê o quadrado e a borda de 1 em volta; dentro do quadrado, só a caixa da colônia é chão mexível. */
    static TerrainDefects.Survey survey(ServerWorld world, UUID colonyId, VillageBounds box, int across, int index) {
        int minX = minX(box, across, index);
        int minZ = minZ(box, across, index);
        int side = SQUARE + 2;
        int[][] top = new int[side][side];
        boolean[][] ground = new boolean[side][side];
        boolean[][] path = new boolean[side][side];
        Optional<BlockPos> mine = VillageColonyMod.MINES.of(colonyId)
                .map(found -> MinecraftTypeAdapter.toBlockPos(found.shaft().entry()));
        List<int[]> footprints = GroundSmoothing.footprintsOf(colonyId);

        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                int x = minX + dx - 1;
                int z = minZ + dz - 1;
                boolean border = dx == 0 || dz == 0 || dx == side - 1 || dz == side - 1;
                boolean inBox = x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ();

                if (!world.isChunkLoaded(x >> 4, z >> 4) || (!border && !inBox)) {
                    continue;
                }

                top[dx][dz] = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos at = new BlockPos(x, top[dx][dz], z);
                BlockState state = world.getBlockState(at);
                boolean nearMine = mine.isPresent() && Math.abs(mine.get().getX() - x) <= GroundSmoothing.MINE_GUARD
                        && Math.abs(mine.get().getZ() - z) <= GroundSmoothing.MINE_GUARD;

                ground[dx][dz] = state.getFluidState().isEmpty() && !nearMine
                        && !GroundSmoothing.insideAny(footprints, x, z) && GroundSmoothing.isGround(world, at, state);
                path[dx][dz] = VillageRoad.isPathBlock(state) || StreetSurface.isStreetMaterial(state);
            }
        }

        return TerrainDefects.survey(top, ground, path);
    }

    private static int minX(VillageBounds box, int across, int index) {
        return box.minX() + (index % across) * SQUARE;
    }

    private static int minZ(VillageBounds box, int across, int index) {
        return box.minZ() + (index / across) * SQUARE;
    }

    /** As colunas que os aldeões mudaram hoje na colônia. */
    static int levelledToday(UUID colonyId, long today) {
        State state = STATES.get(colonyId);

        return state == null ? 0 : state.daily.used(today);
    }

    static void clearAll() {
        STATES.clear();
    }
}
