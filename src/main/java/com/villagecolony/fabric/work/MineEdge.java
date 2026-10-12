package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.Side;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * A mina nova na borda da vila, longe da água — A-4, pedido do autor de
 * 2026-10-02: <i>"a entrada deve ser criada na borda da vila, o mais distante
 * da água"</i>.
 *
 * <p><b>Quando.</b> Ao abrir a mina da colônia. A descida travada não fecha mais a
 * mina (regra do autor, 2026-10-11): o {@link MineDescent} marca o ponto e o
 * mineiro cava outro caminho.
 *
 * <p><b>Onde.</b> De 1 a {@link #INSIDE} blocos para dentro da borda da caixa
 * medida (ADR-038 P3b), a cada {@link #STEP} blocos dos quatro lados, longe da
 * descida travada; vence a soma de distância da água e altura de morro. Vence a boca mais
 * distante de qualquer água, até {@link #WATER_LOOK} blocos. A boca passa pelas
 * mesmas regras de sempre ({@code MineSite}: chão firme, água a mais de quatro
 * blocos). Sem caixa medida, ou sem ponto que sirva, a escolha de sempre
 * decide.
 */
final class MineEdge {

    /** De quantos em quantos blocos da borda se testa uma boca. */
    static final int STEP = 6;

    /** Até onde se procura água em volta de cada boca. */
    static final int WATER_LOOK = 16;

    /** A distância mínima da descida travada. */
    static final int AVOID = MineDescent.RADIUS + 8;

    record Choice(BlockPos mouth, Side side) {
    }

    private MineEdge() {
    }

    /** Até quantos blocos para dentro da caixa da vila a boca nasce — ADR-038 P3b. */
    static final int INSIDE = 5;

    /** Até quantos blocos acima do centro o morro conta a favor. */
    static final int HILL_CAP = 12;

    /**
     * A faixa da borda onde uma boca pode nascer: até este tanto para dentro ou para
     * fora da linha da caixa — Regra 52 (autor, 2026-10-11: <i>"não abrir entrada de
     * mina em lugares aleatórios, somente nas entradas próximas das bordas"</i>).
     */
    static final int EDGE_BAND = INSIDE + 3;

    /** A caixa medida desta colônia, se já houver. */
    static Optional<VillageBounds> boxOf(UUID colonyId) {
        return VillageColonyMod.COLONIES.find(colonyId).flatMap(Colony::bounds);
    }

    /**
     * Se este ponto está na faixa da borda da caixa (Regra 52): a até
     * {@link #EDGE_BAND} blocos da linha de algum lado, dentro ou fora dela.
     */
    static boolean nearTheEdge(VillageBounds box, BlockPos at) {
        if (!box.containsColumn(at.getX(), at.getZ(), EDGE_BAND)) {
            return false;
        }

        int toEdge = Math.min(
                Math.min(Math.abs(at.getX() - box.minX()), Math.abs(at.getX() - box.maxX())),
                Math.min(Math.abs(at.getZ() - box.minZ()), Math.abs(at.getZ() - box.maxZ())));

        return toEdge <= EDGE_BAND;
    }

    /** Longe da água pesa 4 por bloco, morro pesa 3 por bloco de altura (até {@link #HILL_CAP}). */
    static int score(int waterDistance, int hill) {
        return waterDistance * 4 + hill * 3;
    }

    /**
     * A boca da mina dentro da vila — ADR-038 P3b: de 1 a {@link #INSIDE} blocos
     * para dentro da borda da caixa, preferindo o morro e a distância da água.
     * Vazio sem caixa medida ou sem ponto que sirva; aí decide a escolha de sempre.
     *
     * @param avoid um ponto de que a boca fica longe (a descida travada, a mina velha), ou nulo
     */
    static Optional<Choice> inside(ServerWorld world, UUID colonyId, BlockPos center, BlockPos avoid) {
        Optional<VillageBounds> bounds = VillageColonyMod.COLONIES.find(colonyId).flatMap(Colony::bounds);

        if (bounds.isEmpty()) {
            return Optional.empty();
        }

        VillageBounds box = bounds.get();
        Choice best = null;
        int bestScore = Integer.MIN_VALUE;
        int bestWater = 0;

        for (int inset : new int[] {1, INSIDE}) {
            for (Side side : Side.values()) {
                boolean alongX = side == Side.NORTH || side == Side.SOUTH;
                int from = (alongX ? box.minX() : box.minZ()) + inset;
                int to = (alongX ? box.maxX() : box.maxZ()) - inset;

                for (int along = from; along <= to; along += STEP) {
                    int x = alongX ? along : side == Side.WEST ? box.minX() + inset : box.maxX() - inset;
                    int z = alongX ? (side == Side.NORTH ? box.minZ() + inset : box.maxZ() - inset) : along;

                    if (avoid != null && Math.abs(x - avoid.getX()) < AVOID && Math.abs(z - avoid.getZ()) < AVOID) {
                        continue;
                    }

                    Optional<BlockPos> mouth = MineSite.edgeMouth(
                            world, center, x - center.getX(), z - center.getZ(), side);

                    if (mouth.isEmpty()) {
                        continue;
                    }

                    int water = waterDistance(world, mouth.get());
                    int hill = Math.max(0, Math.min(HILL_CAP, mouth.get().getY() - center.getY()));
                    int score = score(water, hill);

                    if (score > bestScore) {
                        bestScore = score;
                        bestWater = water;
                        best = new Choice(mouth.get(), side);
                    }
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
}
