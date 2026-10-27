package com.villagecolony.fabric.integration;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * O registro de roças da colônia visto de fora do fazendeiro — Regras 121 e 127: quem ara põe o canteiro
 * nele, e quem ajuda na roça (o pastor) acha a lavoura madura nele sem mexer na varredura do
 * {@link CropPatch}.
 */
public final class KnownFarmland {

    private KnownFarmland() {
    }

    /** Põe no registro a terra arada que alguém da colônia acabou de fazer — Regra 121. */
    public static void register(UUID colonyId, BlockPos farmland) {
        CropPatch.learn(colonyId, farmland);
    }

    /** A lavoura madura conhecida mais perto de {@code from}, se houver. */
    public static Optional<BlockPos> ripeNear(ServerWorld world, UUID colonyId, BlockPos from) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos cell : CropPatch.knownOf(colonyId)) {
            if (!world.isChunkLoaded(cell) || !CropPatch.isRipe(world.getBlockState(cell.up()))) {
                continue;
            }

            double distance = cell.getSquaredDistance(from);

            if (distance < bestDistance) {
                best = cell.up().toImmutable();
                bestDistance = distance;
            }
        }

        return Optional.ofNullable(best);
    }
}
