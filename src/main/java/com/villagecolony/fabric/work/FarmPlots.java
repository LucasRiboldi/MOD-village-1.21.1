package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.CropPatch;
import com.villagecolony.fabric.integration.VillageRoad;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O canteiro novo do fazendeiro é um retângulo — autor, 2026-10-28, depois do jogo da 0.3.104 (a roça crescia em
 * manchas coladas na fazenda): {@link #SIDE}×{@link #SIDE} em chão plano, ao lado da fazenda existente com 1 bloco de
 * folga, com a água na linha do meio — a mesma forma da fazenda do Minecraft, e toda casa fica a até 4 da água.
 *
 * <p>Um canteiro aberto por colônia: a água entra de uma vez quando ele abre, e cada chamada devolve a próxima casa
 * a arar. Só depois de acabado se procura outro lugar. As manchas que o mod arou antes ficam como estão.
 */
final class FarmPlots {

    static {
        ServerMemory.register(FarmPlots.class, FarmPlots::clearAll);
    }

    /** O lado do canteiro. */
    static final int SIDE = 9;

    /** A linha da água, no meio. */
    static final int MIDDLE = SIDE / 2;

    /** Até onde, a partir do fazendeiro, se procura a fazenda ao lado da qual o canteiro nasce. */
    static final int REACH = 12;

    /** Lugares conferidos por chamada: cada um lê as 81 casas e a borda. */
    static final int SITES_PER_CALL = 24;

    /** Um canteiro: o canto de menor x e z, no nível do chão, e se a água corre ao longo de x. */
    record Plot(BlockPos origin, boolean waterAlongX) {

        BlockPos cell(int a, int b) {
            return origin.add(a, 0, b);
        }

        boolean isWater(int a, int b) {
            return waterAlongX ? b == MIDDLE : a == MIDDLE;
        }
    }

    private static final Map<UUID, Plot> OPEN = new HashMap<>();

    private FarmPlots() {
    }

    /** A próxima casa a arar desta colônia: a do canteiro aberto, ou a primeira de um canteiro novo. */
    static Optional<BlockPos> next(ServerWorld world, UUID colonyId, BlockPos from) {
        Plot open = OPEN.get(colonyId);

        if (open != null) {
            Optional<BlockPos> cell = nextCell(world, open);

            if (cell.isPresent()) {
                return cell;
            }

            OPEN.remove(colonyId);
        }

        Optional<Plot> site = site(world, from);

        if (site.isEmpty()) {
            return Optional.empty();
        }

        for (int i = 0; i < SIDE; i++) {
            BlockPos water = site.get().waterAlongX() ? site.get().cell(i, MIDDLE) : site.get().cell(MIDDLE, i);

            world.setBlockState(water, Blocks.WATER.getDefaultState());
        }

        OPEN.put(colonyId, site.get());

        return nextCell(world, site.get());
    }

    /** A primeira casa do canteiro que ainda é chão de arar. */
    static Optional<BlockPos> nextCell(ServerWorld world, Plot plot) {
        for (int a = 0; a < SIDE; a++) {
            for (int b = 0; b < SIDE; b++) {
                BlockPos cell = plot.cell(a, b);

                if (!plot.isWater(a, b) && isSoil(world, cell)) {
                    return Optional.of(cell);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * O primeiro lugar que serve, ao lado de uma terra arada perto de {@code from}: o canteiro fica além dela com 1
     * de folga, alinhado a ela (centrado, ou começando ou acabando nela), no nível dela ou um abaixo (a roça do
     * deserto tem borda de arenito e areia um abaixo).
     */
    static Optional<Plot> site(ServerWorld world, BlockPos from) {
        int tried = 0;

        for (BlockPos farm : BlockPos.iterateOutwards(from, REACH, 3, REACH)) {
            if (!CropPatch.isFarmland(world.getBlockState(farm))) {
                continue;
            }

            for (Direction side : Direction.Type.HORIZONTAL) {
                for (int dy = 0; dy >= -1; dy--) {
                    for (int shift : new int[] {-MIDDLE, 0, -(SIDE - 1)}) {
                        Plot plot = beside(farm.toImmutable().up(dy), side, shift);

                        if (fits(world, plot)) {
                            return Optional.of(plot);
                        }

                        if (++tried >= SITES_PER_CALL) {
                            return Optional.empty();
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    /** O canteiro além de {@code farm}, para {@code side}, com 1 de folga e deslocado {@code shift} ao longo dela. */
    static Plot beside(BlockPos farm, Direction side, int shift) {
        int x = switch (side) {
            case EAST -> farm.getX() + 2;
            case WEST -> farm.getX() - 1 - SIDE;
            default -> farm.getX() + shift;
        };
        int z = switch (side) {
            case SOUTH -> farm.getZ() + 2;
            case NORTH -> farm.getZ() - 1 - SIDE;
            default -> farm.getZ() + shift;
        };

        // A água corre paralela à borda da fazenda.
        return new Plot(new BlockPos(x, farm.getY(), z), side.getAxis() == Direction.Axis.Z);
    }

    /**
     * Todas as casas são chão de arar no mesmo nível — nunca por cima de uma fazenda que existe —, e a água fica
     * presa: chão sólido embaixo e nas pontas.
     */
    static boolean fits(ServerWorld world, Plot plot) {
        for (int a = 0; a < SIDE; a++) {
            for (int b = 0; b < SIDE; b++) {
                BlockPos cell = plot.cell(a, b);

                if (!world.isChunkLoaded(cell) || !isSoil(world, cell)) {
                    return false;
                }

                if (plot.isWater(a, b) && world.getBlockState(cell.down()).isReplaceable()) {
                    return false;
                }
            }
        }

        BlockPos before = plot.waterAlongX() ? plot.cell(-1, MIDDLE) : plot.cell(MIDDLE, -1);
        BlockPos after = plot.waterAlongX() ? plot.cell(SIDE, MIDDLE) : plot.cell(MIDDLE, SIDE);

        return !world.getBlockState(before).isReplaceable() && !world.getBlockState(after).isReplaceable();
    }

    /** Terra, grama ou areia de arar: ar em cima, nada da vila, da colônia ou da rua. */
    static boolean isSoil(ServerWorld world, BlockPos ground) {
        BlockState state = world.getBlockState(ground);
        boolean soil = state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT);
        // A areia é o chão do deserto, e a da vila gerada também serve (BlockProtection.mayBuildOver).
        boolean sand = state.isIn(BlockTags.SAND) && BlockProtection.mayBuildOver(world, ground, state);

        return ((soil && BlockProtection.mayBreak(world, ground, state)) || sand)
                && world.getBlockState(ground.up()).isAir()
                && !BlockProtection.isColonyBuilt(ground)
                && !pathNear(world, ground);
    }

    /** Caminho encostado — de terra, ou a rua de arenito do deserto: a rua não vira roça. */
    private static boolean pathNear(ServerWorld world, BlockPos ground) {
        for (BlockPos at : BlockPos.iterate(ground.add(-1, -1, -1), ground.add(1, 1, 1))) {
            if (VillageRoad.isPathBlock(world.getBlockState(at)) || VillageRoad.isPaving(world, at.toImmutable())) {
                return true;
            }
        }

        return false;
    }

    static void clearAll() {
        OPEN.clear();
    }
}
