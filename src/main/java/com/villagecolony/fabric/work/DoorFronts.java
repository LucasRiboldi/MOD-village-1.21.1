package com.villagecolony.fabric.work;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * O chão em frente a cada porta térrea da construção — Regra 207 (autor, 2026-11-05): sai pelo lado de fora da porta, passa
 * por degrau e escada, e é o primeiro chão firme fora da caixa. Porta que não dá para fora, a de dentro da casa, não conta;
 * porta dupla conta uma vez. A caixa da peça pode incluir o quintal: porta cuja saída para em barreira dentro dela só conta
 * se nenhuma outra da casa sair, no primeiro chão depois da porta, e assoalho não é quintal — Regra 209.
 */
final class DoorFronts {

    private static final int MAX_WALK = 16;

    /** Até quantos blocos abaixo da porta procurar o chão. */
    private static final int MAX_DROP = 4;

    /** Quantas alturas da caixa, a partir do chão, contam como térreo. */
    private static final int GROUND_FLOOR = 3;

    /** O chão fora da caixa, se a saída chegou lá, e o primeiro chão de quintal do caminho. */
    private record Walk(Optional<BlockPos> front, Optional<BlockPos> yard) {
    }

    private DoorFronts() {
    }

    static List<BlockPos> of(ServerWorld world, BlockPos min, BlockPos max) {
        List<BlockPos> fronts = new ArrayList<>();
        List<BlockPos> yards = new ArrayList<>();

        for (BlockPos at : BlockPos.iterate(min, new BlockPos(max.getX(), Math.min(max.getY(), min.getY() + GROUND_FLOOR), max.getZ()))) {
            if (world.isChunkLoaded(at)) {
                BlockState state = world.getBlockState(at);

                if (state.isIn(BlockTags.DOORS) && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                    // O facing da porta aponta para dentro: é para onde olhava quem a pôs, do lado de fora.
                    Direction inward = state.get(DoorBlock.FACING);

                    Walk out = walkOut(world, at, inward.getOpposite(), min, max);
                    Optional<BlockPos> front = out.front().or(() -> walkOut(world, at, inward, min, max).front());

                    if (front.isPresent()) {
                        addOnce(fronts, front.get());
                    } else {
                        out.yard().ifPresent(yard -> addOnce(yards, yard));
                    }
                }
            }
        }

        return fronts.isEmpty() ? yards : fronts;
    }

    private static void addOnce(List<BlockPos> list, BlockPos front) {
        if (list.stream().noneMatch(other -> other.getManhattanDistance(front) <= 1)) {
            list.add(front);
        }
    }

    private static Walk walkOut(ServerWorld world, BlockPos door, Direction out, BlockPos min, BlockPos max) {
        Optional<BlockPos> yard = Optional.empty();

        for (int step = 1; step <= MAX_WALK; step++) {
            BlockPos column = door.offset(out, step);

            if (!world.isChunkLoaded(column)) {
                return new Walk(Optional.empty(), yard);
            }

            Optional<BlockPos> floor = floorAt(world, column.up());

            if (floor.isEmpty() || (floor.get().getY() >= door.getY() && !isStep(world.getBlockState(floor.get())))) {
                return new Walk(Optional.empty(), yard);
            }

            boolean outside = column.getX() < min.getX() || column.getX() > max.getX()
                    || column.getZ() < min.getZ() || column.getZ() > max.getZ();

            BlockState ground = world.getBlockState(floor.get());

            if (outside && !isStep(ground)) {
                return new Walk(floor, yard);
            }

            if (yard.isEmpty() && !isStep(ground) && !ground.isIn(BlockTags.PLANKS)) {
                yard = floor;
            }
        }

        return new Walk(Optional.empty(), yard);
    }

    /** O primeiro bloco com corpo, de cima para baixo; tapete não conta. */
    private static Optional<BlockPos> floorAt(ServerWorld world, BlockPos from) {
        for (int down = 0; down <= MAX_DROP + 1; down++) {
            BlockPos at = from.down(down);
            BlockState state = world.getBlockState(at);

            if (!state.getCollisionShape(world, at).isEmpty() && !state.isIn(BlockTags.WOOL_CARPETS)) {
                return Optional.of(at.toImmutable());
            }
        }

        return Optional.empty();
    }

    private static boolean isStep(BlockState state) {
        return state.isIn(BlockTags.STAIRS) || state.isIn(BlockTags.SLABS);
    }
}
