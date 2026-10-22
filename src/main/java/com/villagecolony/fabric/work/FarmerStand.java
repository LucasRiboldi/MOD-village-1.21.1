package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Onde o fazendeiro fica de pé para cuidar de uma lavoura — deserto jogado, 2026-10-20.
 *
 * <p>O destino de caminhada era a própria planta, e o jogo procura caminho até a célula exata dela.
 * Na roça do deserto a fileira da ponta fica fechada entre a borda de arenito e o canal de água: não
 * há caminho até a planta, e o fazendeiro vagava em volta e desistia ("could not reach the crop"),
 * mesmo podendo colher de fora. Agora ele vai ao lugar de pé ao alcance do braço
 * ({@code FarmerWork.REACH}) mais perto dele, como o lenhador vai ao pé da árvore.
 */
final class FarmerStand {

    static {
        ServerMemory.register(FarmerStand.class, FarmerStand::clearAll);
    }

    /** Até onde, no plano, o lugar de pé pode ficar da planta. */
    private static final int AROUND = 2;

    /** O quadrado da distância máxima: dentro do alcance 3 do braço, borda incluída ({@link FarmerWork#reaches}). */
    private static final int WITHIN_REACH_SQUARED = 9;

    private record Known(BlockPos crop, BlockPos stand) {
    }

    private static final Map<UUID, Known> KNOWN = new HashMap<>();

    private FarmerStand() {
    }

    /** O lugar de pé para esta planta: o mesmo enquanto a planta for a mesma. */
    static BlockPos of(ServerWorld world, UUID workerId, BlockPos crop, BlockPos worker) {
        Known known = KNOWN.get(workerId);

        if (known != null && known.crop().equals(crop)) {
            return known.stand();
        }

        BlockPos stand = nearest(world, crop, worker);
        KNOWN.put(workerId, new Known(crop.toImmutable(), stand));

        return stand;
    }

    /** O lugar de pé ao alcance da planta mais perto do fazendeiro; a planta, se não houver nenhum. */
    static BlockPos nearest(ServerWorld world, BlockPos crop, BlockPos worker) {
        BlockPos best = crop.toImmutable();
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -AROUND; dx <= AROUND; dx++) {
            for (int dz = -AROUND; dz <= AROUND; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx * dx + dz * dz + dy * dy > WITHIN_REACH_SQUARED) {
                        continue;
                    }

                    BlockPos spot = crop.add(dx, dy, dz);
                    double distance = spot.getSquaredDistance(worker);

                    if (distance < bestDistance && BuilderApproach.standable(world, spot)) {
                        best = spot;
                        bestDistance = distance;
                    }
                }
            }
        }

        return best;
    }

    static void forget(UUID workerId) {
        KNOWN.remove(workerId);
    }

    static void clearAll() {
        KNOWN.clear();
    }
}
