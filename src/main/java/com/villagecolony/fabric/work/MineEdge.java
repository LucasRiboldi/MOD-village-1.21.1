package com.villagecolony.fabric.work;

import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.core.type.ServerMemory;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
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

    /** De quantos em quantos blocos da borda se testa uma boca (Regra 359). */
    static final int STEP = 3;

    /** Quanto pesa cada bloco natural que o Zelador terá de tirar de cima da boca (Regra 359). */
    static final int OVERHANG_COST = 8;

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
     * fora da linha da caixa — Regra 53 (autor, 2026-10-11: <i>"não abrir entrada de
     * mina em lugares aleatórios, somente nas entradas próximas das bordas"</i>).
     */
    static final int EDGE_BAND = INSIDE + 3;

    /** A caixa medida desta colônia, se já houver. */
    static Optional<VillageBounds> boxOf(UUID colonyId) {
        return VillageColonyMod.COLONIES.find(colonyId).flatMap(Colony::bounds);
    }

    /**
     * Se este ponto está na faixa da borda da caixa (Regra 53): a até
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
        // A boca vale onde a vila chega a pé (Regra 359); sem nenhuma assim, a escolha de antes, perto do nível do centro.
        Set<Long> reach = MineSky.reach(world, center, box, EDGE_BAND);
        Scored best = new Scored();
        Scored fallback = new Scored();
        // Nenhuma boca nasce perto de outra, nem de uma já abandonada (Regra 182).
        Set<BlockPos> past = PAST.computeIfAbsent(colonyId, ignored -> new HashSet<>());

        if (avoid != null) {
            past.add(avoid);
        }

        MineEntrance.rememberedMouth(colonyId).map(MinecraftTypeAdapter::toBlockPos).ifPresent(past::add);

        for (int inset : new int[] {1, 3, INSIDE}) {
            for (Side side : Side.values()) {
                boolean alongX = side == Side.NORTH || side == Side.SOUTH;
                int from = (alongX ? box.minX() : box.minZ()) + inset;
                int to = (alongX ? box.maxX() : box.maxZ()) - inset;

                for (int along = from; along <= to; along += STEP) {
                    int x = alongX ? along : side == Side.WEST ? box.minX() + inset : box.maxX() - inset;
                    int z = alongX ? (side == Side.NORTH ? box.minZ() + inset : box.maxZ() - inset) : along;

                    considerMouth(world, center, past, side, x, z, reach, best, fallback);
                }
            }
        }

        Scored chosen = best.choice != null ? best : fallback;

        if (chosen.choice != null) {
            past.add(chosen.choice.mouth());
            VillageColonyMod.LOGGER.info("Colony {} picks the new mine mouth at {} on the {} edge — water {}",
                    colonyId, chosen.choice.mouth().toShortString(), chosen.choice.side(),
                    chosen.water >= WATER_LOOK ? "farther than " + WATER_LOOK + " blocks" : chosen.water + " blocks away");
            VillageColonyMod.LOGGER.info("VC_MINE_MOUTH version=1 colony={} at={} side={} rise={} overhang={} reached={}"
                    + " walkable={} rule=359", colonyId.toString().substring(0, 8), chosen.choice.mouth().toShortString(),
                    chosen.choice.side(), chosen.choice.mouth().getY() - center.getY(), chosen.overhang,
                    chosen == best, reach.size());
        }

        return Optional.ofNullable(chosen.choice);
    }

    /** As bocas que esta colônia já usou ou escolheu nesta sessão. */
    private static final Map<UUID, Set<BlockPos>> PAST = new HashMap<>();

    static {
        ServerMemory.register(MineEdge.class, PAST::clear);
    }

    /** O melhor candidato achado até agora: a escolha, a pontuação e a distância de água dela. */
    private static final class Scored {
        Choice choice;
        int score = Integer.MIN_VALUE;
        int water;
        int overhang;
    }

    /**
     * Pontua a boca na coluna (x, z) deste lado: alcançada a pé, disputa {@code best}; senão, perto do nível do centro,
     * disputa {@code fallback}. Cada bloco a tirar de cima dela pesa {@value #OVERHANG_COST} (Regra 359).
     */
    private static void considerMouth(
            ServerWorld world, BlockPos center, Set<BlockPos> avoid, Side side, int x, int z, Set<Long> reach,
            Scored best, Scored fallback) {

        for (BlockPos other : avoid) {
            if (Math.abs(x - other.getX()) < AVOID && Math.abs(z - other.getZ()) < AVOID) {
                return;
            }
        }

        Optional<BlockPos> mouth = MineSite.openEdgeMouth(world, center, x - center.getX(), z - center.getZ(), side);

        if (mouth.isEmpty()) {
            return;
        }

        boolean reached = reach.contains(mouth.get().up().asLong());

        // Sem chegar a pé, só o que a busca de antes acharia: perto do nível do centro, e não o morro de fora da vila.
        if (!reached && mouth.get().getY() - center.getY() > MineSite.LOOK_UP) {
            return;
        }

        int water = waterDistance(world, mouth.get());
        int hill = Math.max(0, Math.min(HILL_CAP, mouth.get().getY() - center.getY()));
        int overhang = MineSky.overhang(world, mouth.get());
        int score = score(water, hill) - overhang * OVERHANG_COST;
        Scored target = reached ? best : fallback;

        if (score > target.score) {
            target.score = score;
            target.water = water;
            target.overhang = overhang;
            target.choice = new Choice(mouth.get(), side);
        }
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
