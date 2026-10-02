package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.Side;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A mina nova na borda da vila, longe da água — A-4, pedido do autor de
 * 2026-10-02: <i>"a entrada deve ser criada na borda da vila, o mais distante
 * da água"</i>.
 *
 * <p><b>Quando.</b> O {@link MineDescent} marca o ponto onde a descida trava (a
 * caverna com lava de 02-10). Na segunda parada no mesmo ponto, a mina da
 * colônia sai do registro. O mineiro seguinte abre outra, e a boca dela é
 * escolhida aqui.
 *
 * <p><b>Onde.</b> Na borda da caixa medida da vila, a cada {@link #STEP}
 * blocos dos quatro lados, longe da descida travada. Vence a boca mais
 * distante de qualquer água, até {@link #WATER_LOOK} blocos. A boca passa pelas
 * mesmas regras de sempre ({@code MineSite}: chão firme, água a mais de quatro
 * blocos). Sem caixa medida, ou sem ponto que sirva, a escolha de sempre
 * decide.
 */
final class MineEdge {

    static {
        ServerMemory.register(MineEdge.class, MineEdge::clearAll);
    }

    /** De quantos em quantos blocos da borda se testa uma boca. */
    static final int STEP = 6;

    /** Até onde se procura água em volta de cada boca. */
    static final int WATER_LOOK = 16;

    /** A distância mínima da descida travada. */
    static final int AVOID = MineDescent.RADIUS + 8;

    /** Quanto fora da caixa fica a boca: a borda, e não dentro de um quintal. */
    private static final int OUTSIDE = 2;

    record Choice(BlockPos mouth, Side side) {
    }

    /** Colônia → o ponto onde a descida travou, à espera da mina nova. */
    private static final Map<UUID, BlockPos> RELOCATE = new HashMap<>();

    private MineEdge() {
    }

    /** A descida travou: a mina sai do registro, e a próxima nasce na borda. */
    static void relocate(UUID colonyId, BlockPos blocked) {
        if (VillageColonyMod.MINES.of(colonyId).isEmpty() || RELOCATE.containsKey(colonyId)) {
            return;
        }

        RELOCATE.put(colonyId, blocked.toImmutable());
        VillageColonyMod.MINES.removeOfColony(colonyId);

        VillageColonyMod.LOGGER.info(
                "Colony {} closes its mine — the way down stops at {}; the next one opens at the edge of the"
                        + " village, as far from water as it can",
                colonyId, blocked.toShortString());
    }

    /** A boca da mina nova, se esta colônia está trocando de mina. */
    static Optional<Choice> relocation(ServerWorld world, UUID colonyId, BlockPos center) {
        BlockPos avoid = RELOCATE.remove(colonyId);

        if (avoid == null) {
            return Optional.empty();
        }

        Optional<VillageBounds> bounds = VillageColonyMod.COLONIES.find(colonyId).flatMap(Colony::bounds);

        if (bounds.isEmpty()) {
            return Optional.empty();
        }

        VillageBounds box = bounds.get();
        Choice best = null;
        int bestWater = -1;

        for (Side side : Side.values()) {
            boolean alongX = side == Side.NORTH || side == Side.SOUTH;
            int from = alongX ? box.minX() : box.minZ();
            int to = alongX ? box.maxX() : box.maxZ();

            for (int along = from; along <= to; along += STEP) {
                int x = alongX ? along : side == Side.WEST ? box.minX() - OUTSIDE : box.maxX() + OUTSIDE;
                int z = alongX ? (side == Side.NORTH ? box.minZ() - OUTSIDE : box.maxZ() + OUTSIDE) : along;

                if (Math.abs(x - avoid.getX()) < AVOID && Math.abs(z - avoid.getZ()) < AVOID) {
                    continue;
                }

                Optional<BlockPos> mouth = MineSite.edgeMouth(world, center, x - center.getX(), z - center.getZ(), side);

                if (mouth.isEmpty()) {
                    continue;
                }

                int water = waterDistance(world, mouth.get());

                if (water > bestWater) {
                    bestWater = water;
                    best = new Choice(mouth.get(), side);
                }
            }
        }

        if (best != null) {
            VillageColonyMod.LOGGER.info("Colony {} picks the new mine mouth at {} on the {} edge — water {}",
                    colonyId, best.mouth().toShortString(), best.side(),
                    bestWater >= WATER_LOOK ? "farther than " + WATER_LOOK + " blocks" : bestWater + " blocks away");
        }

        return Optional.ofNullable(best);
    }

    /** A distância (em anéis) da água mais perto, até {@link #WATER_LOOK}. */
    static int waterDistance(ServerWorld world, BlockPos at) {
        for (int ring = 1; ring < WATER_LOOK; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }

                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos check = at.add(dx, dy, dz);

                        if (world.getChunkManager().getWorldChunk(check.getX() >> 4, check.getZ() >> 4) != null
                                && !world.getFluidState(check).isEmpty()) {
                            return ring;
                        }
                    }
                }
            }
        }

        return WATER_LOOK;
    }

    static void clearAll() {
        RELOCATE.clear();
    }
}
