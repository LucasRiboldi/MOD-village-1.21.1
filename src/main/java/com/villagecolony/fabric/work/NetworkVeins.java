package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.MineNetwork;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineFlooding;
import com.villagecolony.fabric.integration.OreVein;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Veio primeiro — ADR-042 D4: o minério que aparece na parede de um corredor aberto é
 * seguido, bloco a bloco, enquanto estiver ao alcance do corredor, até {@link #CAP}
 * blocos. A MineColonies faz o mesmo ("tenta minerar o veio inteiro").
 *
 * <p>Só o que o corredor alcança: o mineiro fica de pé dentro do corredor, com piso
 * garantido, e bate na parede. Nunca cava embaixo do piso.
 */
final class NetworkVeins {

    /** Até quantos blocos de veio por busca. */
    static final int CAP = 64;

    /** Até onde, da célula aberta do corredor, o veio é seguido. */
    static final int REACH = MinerReach.REACH;

    private NetworkVeins() {
    }

    /** O próximo bloco de minério exposto na parede deste corredor, se houver. */
    static Optional<BlockPos> next(ServerWorld world, MineNetwork network, MineNetwork.Node node) {
        List<ColonyPos> corridor = network.corridorOf(node);
        Set<ColonyPos> cells = new HashSet<>(corridor);
        int floor = network.hub().y();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();

        for (ColonyPos cell : corridor) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(cell);

            if (!NetworkSafety.isOpen(world.getBlockState(at))) {
                continue;
            }

            for (Direction side : Direction.values()) {
                BlockPos next = at.offset(side);

                if (next.getY() >= floor && !cells.contains(MinecraftTypeAdapter.toColonyPos(next))
                        && seen.add(next) && isWorthIt(world, next)) {
                    queue.add(next);
                }
            }
        }

        int looked = 0;

        while (!queue.isEmpty() && looked++ < CAP) {
            BlockPos ore = queue.poll();

            if (exposed(world, ore) && withinReach(ore, corridor)) {
                return Optional.of(ore);
            }

            for (Direction side : Direction.values()) {
                BlockPos next = ore.offset(side);

                if (next.getY() >= floor && seen.add(next) && isWorthIt(world, next)) {
                    queue.add(next);
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isWorthIt(ServerWorld world, BlockPos at) {
        return world.isChunkLoaded(at)
                && OreVein.isOre(world.getBlockState(at))
                && !MineMarks.isOutOfReach(world, at)
                && !MineFlooding.holdsBackFluid(world, at);
    }

    /** Encostado em ar: dá para bater nele. */
    private static boolean exposed(ServerWorld world, BlockPos ore) {
        for (Direction side : Direction.values()) {
            if (NetworkSafety.isOpen(world.getBlockState(ore.offset(side)))) {
                return true;
            }
        }

        return false;
    }

    private static boolean withinReach(BlockPos ore, List<ColonyPos> corridor) {
        for (ColonyPos cell : corridor) {
            if (Math.abs(cell.x() - ore.getX()) <= REACH
                    && Math.abs(cell.y() - ore.getY()) <= REACH
                    && Math.abs(cell.z() - ore.getZ()) <= REACH) {
                return true;
            }
        }

        return false;
    }
}
