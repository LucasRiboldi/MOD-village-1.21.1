package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A boca da mina na superfície — Regra 356: nenhum caminho e nenhum nivelamento aterra o buraco da escada. A folga em volta
 * da entrada do nível atual fica com quem já a media; esta guarda cada boca da superfície vista pelo arco de pé, porque
 * depois que a mina desce por um ramal a entrada do nível atual pode ficar longe da boca.
 */
public final class MineMouths {

    /** Colônia → boca da superfície → rumo da descida. Memória do servidor: o arco e a escada a mostram de novo. */
    private static final Map<UUID, Map<BlockPos, Direction>> KNOWN = new ConcurrentHashMap<>();

    static {
        ServerMemory.register(MineMouths.class, KNOWN::clear);
    }

    private MineMouths() {
    }

    /** Esta colônia tem uma boca de mina na superfície em {@code mouth}, descendo para {@code descent}. */
    public static void remember(UUID colonyId, BlockPos mouth, Direction descent) {
        KNOWN.computeIfAbsent(colonyId, ignored -> new ConcurrentHashMap<>()).put(mouth.toImmutable(), descent);
    }

    public static void forget(UUID colonyId) {
        KNOWN.remove(colonyId);
    }

    /** As bocas da superfície já vistas desta colônia, com o rumo da descida. */
    public static Map<BlockPos, Direction> of(UUID colonyId) {
        return Map.copyOf(KNOWN.getOrDefault(colonyId, Map.of()));
    }

    /**
     * Se a coluna fica na folga de alguma boca da superfície ({@link WorldPathTerrain#MINE_CLEARANCE}). Só vale a boca de
     * colônia que ainda tem mina: a de mina desfeita não protege mais nada.
     */
    public static boolean isNear(int x, int z) {
        for (Map.Entry<UUID, Map<BlockPos, Direction>> colony : KNOWN.entrySet()) {
            if (nearAny(colony.getKey(), colony.getValue(), x, z)) {
                return true;
            }
        }

        return false;
    }

    /** O mesmo, só com as bocas desta colônia. */
    public static boolean isNear(UUID colonyId, int x, int z) {
        return nearAny(colonyId, KNOWN.getOrDefault(colonyId, Map.of()), x, z);
    }

    private static boolean nearAny(UUID colonyId, Map<BlockPos, Direction> mouths, int x, int z) {
        if (mouths.isEmpty() || VillageColonyMod.MINES.of(colonyId).isEmpty()) {
            return false;
        }

        for (BlockPos mouth : mouths.keySet()) {
            if (near(x, z, mouth.getX(), mouth.getZ())) {
                return true;
            }
        }

        return false;
    }

    /** As células do caracol do primeiro nível, da boca para baixo: o buraco que fica aberto (Regra 356). */
    public static List<BlockPos> stairCells(BlockPos mouth, Direction descent) {
        MineShaft shaft = MineShaft.from(MinecraftTypeAdapter.toColonyPos(mouth), MinecraftTypeAdapter.toSide(descent));
        List<BlockPos> cells = new ArrayList<>(MineShaft.CARVED);

        for (int index = 0; index < MineShaft.CARVED; index++) {
            cells.add(MinecraftTypeAdapter.toBlockPos(shaft.positionAt(index)));
        }

        return cells;
    }

    private static boolean near(int x, int z, int mouthX, int mouthZ) {
        return Math.abs(x - mouthX) <= WorldPathTerrain.MINE_CLEARANCE
                && Math.abs(z - mouthZ) <= WorldPathTerrain.MINE_CLEARANCE;
    }
}
