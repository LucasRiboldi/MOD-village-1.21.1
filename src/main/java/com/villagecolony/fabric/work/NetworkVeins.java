package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.MineNetwork;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineFlooding;
import com.villagecolony.fabric.integration.OreVein;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Minério primeiro — ADR-042 D4, revista no playtest 0.3.35.
 *
 * <p>Todo minério ao alcance do braço ({@link MinerReach#REACH}) de uma célula aberta do
 * túnel do nó é tirado antes de o túnel avançar, exposto ou não, na ordem de utilidade
 * da vila ({@link OreVein#priorityOf}). Antes só o minério que aparecia na parede contava,
 * e a mina de 0.3.35 cavou 1.411 blocos para tirar 2 de ferro.
 *
 * <p>Nunca mais fundo que {@link #BELOW_FLOOR} abaixo do piso: o buraco do minério logo
 * embaixo do túnel recebe piso de novo ({@link NetworkSafety}).
 *
 * <p>A busca custa umas mil leituras de bloco, então a lista fica guardada por mineiro e
 * nó e é refeita a cada {@link #RESCAN_TICKS}.
 */
final class NetworkVeins {

    static {
        ServerMemory.register(NetworkVeins.class, NetworkVeins::clearAll);
    }

    /** Até quantos minérios a lista guarda. */
    static final int CAP = 64;

    /** Até onde, da célula aberta do túnel, o minério é tirado. */
    static final int REACH = MinerReach.REACH;

    /** Até quantos blocos abaixo do piso o minério ainda é tirado. */
    static final int BELOW_FLOOR = 2;

    /** De quanto em quanto tempo a lista é refeita. */
    static final long RESCAN_TICKS = 100;

    private record Found(int gx, int gz, List<BlockPos> ores, long at) {
    }

    private static final Map<UUID, Found> FOUND = new HashMap<>();

    private NetworkVeins() {
    }

    /** O próximo minério ao alcance do túnel deste nó, se houver. */
    static Optional<BlockPos> next(ServerWorld world, UUID workerId, MineNetwork network, MineNetwork.Node node) {
        long now = world.getTime();
        Found known = FOUND.get(workerId);

        if (known == null || known.gx() != node.gx() || known.gz() != node.gz() || now - known.at() >= RESCAN_TICKS) {
            known = new Found(node.gx(), node.gz(), scan(world, network, node), now);
            FOUND.put(workerId, known);
        }

        while (!known.ores().isEmpty()) {
            BlockPos ore = known.ores().getFirst();

            if (isWorthIt(world, ore)) {
                return Optional.of(ore);
            }

            known.ores().removeFirst();
        }

        return Optional.empty();
    }

    /** Os minérios ao alcance das células abertas do túnel, do mais útil e mais perto primeiro. */
    static List<BlockPos> scan(ServerWorld world, MineNetwork network, MineNetwork.Node node) {
        List<ColonyPos> tunnel = network.corridorOf(node);
        Set<ColonyPos> cells = new HashSet<>(tunnel);
        int lowest = network.hub().y() - BELOW_FLOOR;
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> ores = new ArrayList<>();
        List<BlockPos> open = new ArrayList<>();

        for (ColonyPos cell : tunnel) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(cell);

            if (world.isChunkLoaded(at) && NetworkSafety.isOpen(world.getBlockState(at))) {
                open.add(at);
            }
        }

        for (BlockPos at : open) {
            for (BlockPos near : BlockPos.iterate(at.add(-REACH, -REACH, -REACH), at.add(REACH, REACH, REACH))) {
                if (ores.size() >= CAP) {
                    break;
                }

                BlockPos candidate = near.toImmutable();

                if (candidate.getY() < lowest || cells.contains(MinecraftTypeAdapter.toColonyPos(candidate))
                        || !seen.add(candidate)) {
                    continue;
                }

                if (isWorthIt(world, candidate)) {
                    ores.add(candidate);
                }
            }
        }

        ores.sort(Comparator.comparingInt((BlockPos ore) -> OreVein.priorityOf(world.getBlockState(ore)))
                .thenComparingInt(ore -> nearest(ore, open)));

        return ores;
    }

    private static int nearest(BlockPos ore, List<BlockPos> open) {
        int best = Integer.MAX_VALUE;

        for (BlockPos at : open) {
            best = Math.min(best, at.getManhattanDistance(ore));
        }

        return best;
    }

    private static boolean isWorthIt(ServerWorld world, BlockPos at) {
        return world.isChunkLoaded(at)
                && OreVein.isOre(world.getBlockState(at))
                && !MineMarks.isOutOfReach(world, at)
                && !MineFlooding.holdsBackFluid(world, at);
    }

    static void forget(UUID workerId) {
        FOUND.remove(workerId);
    }

    static void clearAll() {
        FOUND.clear();
    }
}
