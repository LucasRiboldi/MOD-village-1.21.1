package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineNetwork;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.MineFlooding;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * <p>O enchimento sai dos baús mais perto da mina (pedregulho primeiro) — a colônia não
 * inventa matéria. Sem bloco nenhum, a colônia espera {@link #NO_FILL_WAIT} tiques antes
 * de procurar de novo: varrer os 39 baús a cada tique foi 4% do servidor. Enquanto
 * isso, o mineiro tira o enchimento da própria mina ({@link #quarryFor}).
 */
final class NetworkSafety {

    static {
        ServerMemory.register(NetworkSafety.class, NetworkSafety::clearAll);
    }

    /** Quantos blocos uma passagem põe, no máximo: o trabalho se espalha pelos tiques. */
    static final int PER_PASS = 8;

    /** Quantos baús, dos mais perto da mina, fornecem o enchimento. */
    static final int CHESTS = 6;

    /** Sem enchimento, quanto tempo até procurar de novo. */
    static final long NO_FILL_WAIT = 200;

    /** Os blocos de enchimento, na ordem de preferência. */
    private static final List<Item> FILL = List.of(
            Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.STONE, Items.ANDESITE, Items.DIORITE,
            Items.GRANITE, Items.TUFF, Items.DIRT, Items.NETHERRACK);

    /** Abaixo da entrada do nível, quantos blocos ficam sem fechar: a ligação com o nível de cima. */
    static final int ENTRY_OPEN = 3;

    /** Colônia → até quando ela não procura enchimento de novo. */
    private static final Map<UUID, Long> NO_FILL_UNTIL = new HashMap<>();

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

        return secure(world, colonyId, open, mineCells, shaft.entry().y() - ENTRY_OPEN, network.hub());
    }

    /**
     * Garante as células abertas de um túnel.
     *
     * @return se nada mais falta
     */
    static boolean secureCorridor(
            ServerWorld world, UUID colonyId, Collection<ColonyPos> corridor, Set<ColonyPos> mineCells,
            ColonyPos near) {
        return secure(world, colonyId, corridor, mineCells, Integer.MAX_VALUE, near);
    }

    /**
     * Piso embaixo, fluido encostado e beirada de queda ao lado do piso.
     *
     * @param guardBelowY só fecha beirada ao lado das células abaixo desta altura (a boca
     *     do nível fica aberta para o de cima)
     */
    private static boolean secure(
            ServerWorld world, UUID colonyId, Collection<ColonyPos> cells, Set<ColonyPos> mineCells,
            int guardBelowY, ColonyPos near) {

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

        long now = world.getTime();

        if (now < NO_FILL_UNTIL.getOrDefault(colonyId, 0L)) {
            return false;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, near).stream().limit(CHESTS).toList();
        int placed = 0;
        int left = 0;
        boolean noFill = false;

        for (BlockPos at : todo) {
            if (placed >= PER_PASS || noFill || !world.isChunkLoaded(at) || someoneIsIn(world, at)) {
                left++;
                continue;
            }

            Block fill = takeFill(world, chests);

            if (fill == null) {
                noFill = true;
                left++;
                continue;
            }

            world.setBlockState(at, fill.getDefaultState(), Block.NOTIFY_ALL);
            placed++;
        }

        if (noFill) {
            NO_FILL_UNTIL.put(colonyId, now + NO_FILL_WAIT);
        }

        if (placed > 0) {
            VillageColonyMod.LOGGER.info("Mine {} made the way safe — {} blocks placed, {} left{}",
                    colonyId.toString().substring(0, 8), placed, left, noFill ? " (no fill block in the chests)" : "");
        }

        return left == 0;
    }

    /** Até quantos blocos para fora do túnel a parede é cavada atrás de enchimento. */
    static final int QUARRY_DEPTH = 3;

    /**
     * Uma pedra da parede do túnel para virar enchimento: sem pedregulho nos baús, a mina
     * se abastece sozinha em vez de esperar (playtest 0.3.36: 37 mil esperas, 0 pedregulho).
     * A pedra encosta em espaço já aberto, tem chão firme embaixo (o nicho não vira queda)
     * e não segura fluido. A pedra que o mineiro já recusou fica de fora: no playtest da
     * 0.3.39 a mesma foi escolhida 53 vezes.
     */
    static Optional<BlockPos> quarryFor(
            ServerWorld world, Mine mine, Collection<ColonyPos> corridor, Set<ColonyPos> mineCells) {

        for (int depth = 1; depth <= QUARRY_DEPTH; depth++) {
            for (ColonyPos cell : corridor) {
                BlockPos from = MinecraftTypeAdapter.toBlockPos(cell);

                for (Direction side : Direction.Type.HORIZONTAL) {
                    BlockPos at = from.offset(side, depth);

                    if (mineCells.contains(MinecraftTypeAdapter.toColonyPos(at))
                            || !world.isChunkLoaded(at)
                            || !isOpen(world.getBlockState(at.offset(side.getOpposite())))
                            || isOpen(world.getBlockState(at))
                            || !isFirmFloor(world, at.down())
                            || !MineRock.isDiggableRock(world, mine, at)
                            || MineMarks.isOutOfReach(world, at)
                            || MineFlooding.holdsBackFluid(world, at)) {
                        continue;
                    }

                    return Optional.of(at);
                }
            }
        }

        return Optional.empty();
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

    /** Um bloco de enchimento dos baús; nulo sem nenhum. */
    private static Block takeFill(ServerWorld world, List<ColonyPos> chests) {
        for (Item item : FILL) {
            if (ColonyChests.withdraw(world, chests, item, 1) == 1) {
                return Block.getBlockFromItem(item);
            }
        }

        return null;
    }

    static void clearAll() {
        NO_FILL_UNTIL.clear();
    }
}
