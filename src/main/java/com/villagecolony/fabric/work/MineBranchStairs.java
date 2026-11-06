package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/** Como um nível se liga ao de cima quando sai de um ramal qualquer, não só do ramal zero — Regra 182. */
final class MineBranchStairs {

    private MineBranchStairs() {
    }

    /** O nível de {@code above} que leva à entrada {@code below}: pelo ramal zero de sempre, ou por outro (Regra 182). */
    static Optional<MineShaft> between(ColonyPos above, ColonyPos below) {
        for (Side descent : Side.values()) {
            MineShaft plain = MineShaft.from(above, descent);

            if (plain.deepened().entry().equals(below)) {
                return Optional.of(plain);
            }
        }

        for (Side descent : Side.values()) {
            for (Side gallery : Side.values()) {
                MineShaft shaft = new MineShaft(above, descent, gallery);

                if (shaft.deepened().entry().equals(below)) {
                    return Optional.of(shaft);
                }
            }
        }

        return Optional.empty();
    }

    /** O nível de cima cujo caracol de ramal, que não é o zero, chega cavado até {@code at}. */
    static Optional<MineShaft> above(ServerWorld world, ColonyPos at, double minShare, boolean[] cut) {
        Optional<MineShaft> best = Optional.empty();
        double bestShare = minShare;

        for (Side descent : Side.values()) {
            for (Side gallery : Side.values()) {
                if (gallery == descent.clockwise().clockwise()) {
                    continue;
                }

                ColonyPos step = new MineShaft(new ColonyPos(0, 0, 0), descent, gallery).deepened().entry();
                MineShaft level = new MineShaft(new ColonyPos(at.x() - step.x(), at.y() - step.y(), at.z() - step.z()),
                        descent, gallery);
                double share = armStairShare(world, level, cut);

                if (share >= bestShare) {
                    best = Optional.of(level);
                    bestShare = share;
                }
            }
        }

        return best;
    }

    private static double armStairShare(ServerWorld world, MineShaft level, boolean[] cut) {
        int open = 0;
        int cells = MineShaft.ARM_STAIRS * MineShaft.STAIR_STEP_BLOCKS;

        for (int index = 0; index < cells; index++) {
            BlockPos cell = MinecraftTypeAdapter.toBlockPos(level.positionAt(MineShaft.SHARED_BLOCKS + index));

            if (world.getChunkManager().getWorldChunk(cell.getX() >> 4, cell.getZ() >> 4) == null) {
                cut[0] = true;
            } else if (MineRock.isOpenSpace(world, cell, world.getBlockState(cell))) {
                open++;
            }
        }

        return (double) open / cells;
    }
}
