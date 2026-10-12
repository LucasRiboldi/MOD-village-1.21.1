package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineNetwork;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Piso garantido e aberturas vedadas — ADR-042 D1 e D6, o {@code secureBlock} da
 * MineColonies.
 *
 * <p>Toda célula aberta da mina (escadaria, salão, corredor) tem chão firme embaixo: o
 * que falta recebe um bloco de enchimento. Toda célula vizinha de fora da mina que é ar
 * de caverna ou fluido, ao lado ou embaixo, recebe um bloco também: o aldeão não sai da
 * mina para dentro de uma caverna, não cai num buraco e a água não entra. O playtest
 * 0.3.32 tinha quedas de três blocos no caminho principal e cavernas abertas, e os
 * mineiros desceram por elas e não voltaram.
 *
 * <p>O enchimento sai dos baús da vila (pedregulho primeiro) — a colônia não inventa
 * matéria. Sem bloco nenhum, o que falta fica para a próxima passagem.
 */
final class NetworkSafety {

    /** Quantos blocos uma passagem põe, no máximo: o trabalho se espalha pelos tiques. */
    static final int PER_PASS = 24;

    /** Os blocos de enchimento, na ordem de preferência. */
    private static final List<Item> FILL = List.of(
            Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.STONE, Items.ANDESITE, Items.DIORITE,
            Items.GRANITE, Items.TUFF, Items.DIRT, Items.NETHERRACK);

    /** Abaixo da entrada do nível, quantos blocos ficam sem vedar: a ligação com o nível de cima. */
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
     * Garante as células abertas de um corredor.
     *
     * @return se nada mais falta
     */
    static boolean secureCorridor(
            ServerWorld world, UUID colonyId, Collection<ColonyPos> corridor, Set<ColonyPos> mineCells) {
        return secure(world, colonyId, corridor, mineCells, Integer.MAX_VALUE);
    }

    /**
     * Piso embaixo, fluido e caverna em volta.
     *
     * @param sealBelowY só veda ao lado das células abaixo desta altura (a boca do
     *     nível fica aberta para o de cima)
     */
    private static boolean secure(
            ServerWorld world, UUID colonyId, Collection<ColonyPos> cells, Set<ColonyPos> mineCells, int sealBelowY) {

        Set<BlockPos> floors = new LinkedHashSet<>();
        Set<BlockPos> seals = new LinkedHashSet<>();

        for (ColonyPos cell : cells) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(cell);
            BlockPos below = at.down();

            if (!mineCells.contains(MinecraftTypeAdapter.toColonyPos(below)) && !isFirmFloor(world, below)) {
                floors.add(below);
            }

            if (cell.y() >= sealBelowY) {
                continue;
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

                // Em cima só o fluido: ar acima do teto não derruba ninguém.
                boolean fluid = !state.getFluidState().isEmpty();
                boolean cave = side != Direction.UP && isOpen(state);

                if (fluid || cave) {
                    seals.add(next);
                }
            }
        }

        if (floors.isEmpty() && seals.isEmpty()) {
            return true;
        }

        List<BlockPos> todo = new ArrayList<>(floors);
        todo.addAll(seals);

        List<ColonyPos> chests = VillageColonyMod.COLONIES.find(colonyId)
                .map(colony -> ColonyChests.nearestFirst(world, colonyId, colony.center()))
                .orElse(List.of());
        int placed = 0;
        int left = 0;

        for (BlockPos at : todo) {
            if (placed >= PER_PASS) {
                left++;
                continue;
            }

            if (!world.isChunkLoaded(at) || someoneIsIn(world, at)) {
                left++;
                continue;
            }

            Block fill = takeFill(world, chests);

            if (fill == null) {
                left++;
                continue;
            }

            world.setBlockState(at, fill.getDefaultState(), Block.NOTIFY_ALL);
            placed++;
        }

        if (placed > 0) {
            VillageColonyMod.LOGGER.info(
                    "Mine {} made the way safe — {} blocks placed ({} floors, {} openings), {} left",
                    colonyId.toString().substring(0, 8), placed, floors.size(), seals.size(), left);
        }

        return left == 0;
    }

    /** Ar ou o que se atravessa (tocha, grama): aberto. */
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
}
