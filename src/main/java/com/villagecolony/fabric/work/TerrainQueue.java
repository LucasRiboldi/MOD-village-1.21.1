package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.TerrainDefects;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StreetSurface;
import com.villagecolony.fabric.integration.VillageRoad;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A medida do terreno da vila em quadrados de {@value #SQUARE}×{@value #SQUARE} — Regra 323: buraco e bloco isolados,
 * degrau maior que 1 no caminho e no resto ({@link TerrainDefects}), para a linha diária {@code VC_TERRAIN}. Quem arruma
 * o chão é o plano de alturas ({@link GradeWork}, Regra 357, ADR-063), que substituiu a fila de quadrados.
 */
final class TerrainQueue {

    /** O lado do quadrado da medida. */
    static final int SQUARE = 16;

    private TerrainQueue() {
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
                boolean nearMine = (mine.isPresent() && Math.abs(mine.get().getX() - x) <= GroundSmoothing.MINE_GUARD
                        && Math.abs(mine.get().getZ() - z) <= GroundSmoothing.MINE_GUARD)
                        || com.villagecolony.fabric.integration.MineMouths.isNear(x, z);

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

}
