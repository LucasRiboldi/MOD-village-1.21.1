package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineNetwork;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Piso garantido, fluido vedado e beirada de queda fechada — ADR-042 D1 e D6, o
 * {@code secureBlock} da MineColonies, na medida do playtest 0.3.35.
 *
 * <p>Toda célula aberta da mina (escadaria, salão, túnel) tem chão firme embaixo: o que
 * falta recebe um bloco de enchimento. Fluido encostado é vedado. Ao lado de uma célula
 * de piso, a abertura que dá numa queda recebe um bloco: o aldeão não cai. O resto da
 * caverna fica como está — vedar toda abertura levantou 2.392 blocos de parede na
 * 0.3.35, gastou o pedregulho da vila e fez a mina parecer um labirinto.
 *
 * <p><b>O enchimento surge; a mina não gasta recurso da vila</b> — decisão do autor,
 * 2026-10-13. A construção interna da mina (piso, beirada) é infraestrutura, não obra:
 * competir com a obra e o fundidor pelo pedregulho dos baús travava a mina de vez
 * (playtest 0.3.50, nó parado 41 min esperando enchimento, {@code MINER} 0%; a mesma
 * escolha que {@link com.villagecolony.fabric.integration.MineFlooding} já fazia para
 * vedar fluido). O bloco surge como pedregulho, o que um jogador poria.
 */
final class NetworkSafety {

    /** Quantos blocos uma passagem põe, no máximo: o trabalho se espalha pelos tiques. */
    static final int PER_PASS = 8;

    /** O bloco de enchimento que a mina gera: pedregulho, o que o mineiro tira o dia inteiro. */
    private static final Block FILL = Blocks.COBBLESTONE;

    /** Abaixo da entrada do nível, quantos blocos ficam sem fechar: a ligação com o nível de cima. */
    static final int ENTRY_OPEN = 3;

    private NetworkSafety() {
    }

    /**
     * Garante a escadaria e o salão deste nível.
     *
     * @return se nada mais falta
     */
    static boolean secureShaft(ServerWorld world, UUID colonyId, Mine mine, MineNetwork network) {
        MineShaft shaft = mine.shaft();
        Set<ColonyPos> shared = new LinkedHashSet<>();

        for (int index = 0; index < MineShaft.SHARED_BLOCKS; index++) {
            shared.add(shaft.positionAt(index));
        }

        Set<ColonyPos> mineCells = new HashSet<>(shared);
        mineCells.addAll(network.cells());

        List<ColonyPos> open = new ArrayList<>();

        for (ColonyPos cell : shared) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(cell);

            if (!world.isChunkLoaded(at)) {
                return false;
            }

            if (isOpen(world.getBlockState(at))) {
                open.add(cell);
            }
        }

        return secure(world, colonyId, open, mineCells, shaft.entry().y() - ENTRY_OPEN);
    }

    /**
     * Garante as células abertas de um túnel.
     *
     * @return se nada mais falta
     */
    static boolean secureCorridor(
            ServerWorld world, UUID colonyId, Collection<ColonyPos> corridor, Set<ColonyPos> mineCells) {
        return secure(world, colonyId, corridor, mineCells, Integer.MAX_VALUE);
    }

    /**
     * Piso embaixo, fluido encostado e beirada de queda ao lado do piso. O enchimento surge
     * (a mina não gasta recurso da vila, 2026-10-13), então a passagem só falta terminar o que
     * {@link #PER_PASS} não cobriu ou o que tem alguém em cima.
     *
     * @param guardBelowY só fecha beirada ao lado das células abaixo desta altura (a boca
     *     do nível fica aberta para o de cima)
     */
    private static boolean secure(
            ServerWorld world, UUID colonyId, Collection<ColonyPos> cells, Set<ColonyPos> mineCells,
            int guardBelowY) {

        Set<BlockPos> todo = new LinkedHashSet<>();

        for (ColonyPos cell : cells) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(cell);
            BlockPos below = at.down();
            boolean floorCell = !mineCells.contains(MinecraftTypeAdapter.toColonyPos(below));

            if (floorCell && !isFirmFloor(world, below)) {
                todo.add(below);
            }

            for (Direction side : Direction.values()) {
                if (side == Direction.DOWN) {
                    continue;
                }

                BlockPos next = at.offset(side);

                if (mineCells.contains(MinecraftTypeAdapter.toColonyPos(next))) {
                    continue;
                }

                BlockState state = world.getBlockState(next);

                if (!state.getFluidState().isEmpty()) {
                    todo.add(next);
                } else if (floorCell && side != Direction.UP && cell.y() < guardBelowY
                        && isOpen(state) && !isFirmFloor(world, next.down())) {
                    // A abertura ao lado do piso dá numa queda: um bloco na beirada.
                    todo.add(next);
                }
            }
        }

        if (todo.isEmpty()) {
            return true;
        }

        int placed = 0;
        int left = 0;

        for (BlockPos at : todo) {
            if (placed >= PER_PASS || !world.isChunkLoaded(at) || someoneIsIn(world, at)) {
                left++;
                continue;
            }

            world.setBlockState(at, FILL.getDefaultState(), Block.NOTIFY_ALL);
            placed++;
        }

        if (placed > 0) {
            VillageColonyMod.LOGGER.info("Mine {} made the way safe — {} blocks placed, {} left",
                    colonyId.toString().substring(0, 8), placed, left);
        }

        return left == 0;
    }

    /** Ar ou o que se atravessa (tocha, grama), ou fluido: aberto. */
    static boolean isOpen(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || state.isReplaceable();
    }

    /** Chão em que se pisa sem cair: bloco de colisão cheia em cima, sem fluido. */
    static boolean isFirmFloor(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        return state.getFluidState().isEmpty() && state.isSideSolidFullSquare(world, at, Direction.UP);
    }

    private static boolean someoneIsIn(ServerWorld world, BlockPos at) {
        return !world.getEntitiesByClass(LivingEntity.class, new Box(at), entity -> true).isEmpty();
    }
}
