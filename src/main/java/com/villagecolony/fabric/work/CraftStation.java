package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Onde o artesão trabalha — ADR-038 P3c: diante da bancada do ofício, se a vila
 * tem uma (carpinteiro: bancada de trabalho perto da casa; pedreiro: cortador de
 * pedra da vila); sem bancada, em volta do sino; sem sino, no baú, como antes.
 * O material sai de todos os baús e o produto vai direto ao baú da profissão
 * ({@link CraftingSteps}): ele não carrega nada.
 */
final class CraftStation {

    static {
        ServerMemory.register(CraftStation.class, CraftStation::clearAll);
    }

    /** Quanto a escolha vale antes de olhar o mundo de novo. */
    static final int REFRESH_TICKS = 6_000;

    /** Até onde da casa (do baú) procurar a bancada do carpinteiro. */
    static final int BENCH_RADIUS = 16;

    /** Até onde procurar cortador de pedra e sino pelo registro de pontos do jogo. */
    static final int POI_RADIUS = 64;

    private record Choice(BlockPos spot, long at) {
    }

    private static final Map<UUID, Choice> CHOSEN = new HashMap<>();

    private CraftStation() {
    }

    static void clearAll() {
        CHOSEN.clear();
    }

    /** Onde este artesão fica para trabalhar; {@code chest} é o baú da profissão dele. */
    static BlockPos spotFor(ServerWorld world, UUID workerId, BlockPos chest) {
        Choice known = CHOSEN.get(workerId);

        if (known != null && world.getTime() - known.at() < REFRESH_TICKS) {
            return known.spot();
        }

        Optional<ProfessionType> profession = VillageColonyMod.WORKERS.find(workerId).flatMap(Worker::profession);
        BlockPos spot = bench(world, profession.orElse(null), chest)
                .or(() -> bell(world, chest))
                .orElse(chest);

        CHOSEN.put(workerId, new Choice(spot.toImmutable(), world.getTime()));

        return spot;
    }

    private static Optional<BlockPos> bench(ServerWorld world, ProfessionType profession, BlockPos chest) {
        if (profession == ProfessionType.MASON) {
            return world.getPointOfInterestStorage().getNearestPosition(
                    entry -> entry.matchesKey(PointOfInterestTypes.MASON), chest, POI_RADIUS,
                    PointOfInterestStorage.OccupationStatus.ANY);
        }

        if (profession != ProfessionType.CARPENTER) {
            return Optional.empty();
        }

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos at : BlockPos.iterate(chest.add(-BENCH_RADIUS, -4, -BENCH_RADIUS),
                chest.add(BENCH_RADIUS, 4, BENCH_RADIUS))) {
            WorldChunk chunk = world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);

            if (chunk != null && chunk.getBlockState(at).isOf(Blocks.CRAFTING_TABLE)) {
                double distance = at.getSquaredDistance(chest);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = at.toImmutable();
                }
            }
        }

        return Optional.ofNullable(best);
    }

    private static Optional<BlockPos> bell(ServerWorld world, BlockPos chest) {
        return world.getPointOfInterestStorage().getNearestPosition(
                entry -> entry.matchesKey(PointOfInterestTypes.MEETING), chest, POI_RADIUS,
                PointOfInterestStorage.OccupationStatus.ANY);
    }
}
