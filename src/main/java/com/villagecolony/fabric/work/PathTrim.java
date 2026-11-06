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
 * A rua é uma linha de um bloco — Regra 186 (autor, 2026-11-05): o caminho ao lado da linha do grafo volta a ser o solo do
 * bioma, relva na planície. Trabalho do pedreiro, {@value #BUDGET} colunas por passada; porta e construção ficam.
 */
final class PathTrim {

    private static final int BUDGET = 16;

    private static final Map<UUID, Deque<ColonyPos>> SURPLUS = new HashMap<>();

    static {
        ServerMemory.register(PathTrim.class, SURPLUS::clear);
    }

    private PathTrim() {
    }

    /** O que sobrou na última releitura do grafo. */
    static void offer(UUID colonyId, List<ColonyPos> surplus) {
        SURPLUS.put(colonyId, new ArrayDeque<>(surplus));
    }

    /** Uma passada do pedreiro; devolve quantas colunas mudaram. */
    static int pass(ServerWorld world, Colony colony) {
        Deque<ColonyPos> left = SURPLUS.get(colony.id());
        int done = 0;

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
