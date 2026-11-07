package com.villagecolony.fabric.work;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A entrada de cada porta térrea da construção — Regras 207 e 223 (autor, 2026-11-07): o marcador é o primeiro bloco em frente
 * à porta, mesmo dentro da caixa, e a saída da caixa é só o caminho até ele. Porta que não dá para fora, a de dentro da casa,
 * não conta; porta dupla conta uma vez. A caixa da peça pode incluir o quintal: porta cuja saída para em barreira dentro dela
 * só conta se nenhuma outra da casa sair, e então a entrada é onde o quintal abre para fora, ou o primeiro chão depois da
 * porta se ele for fechado. Assoalho não é quintal — Regra 209.
 */
final class DoorFronts {

    private static final int MAX_WALK = 16;

    /** Até quantos blocos abaixo da porta procurar o chão. */
    private static final int MAX_DROP = 4;

    /** Quantas alturas da caixa, a partir do chão, contam como térreo. */
    private static final int GROUND_FLOOR = 3;

    /** Quantos blocos livres acima do chão fazem uma passagem. */
    private static final int HEADROOM = 2;

    /** Quantas colunas do quintal se olham à procura da abertura. */
    private static final int MAX_YARD = 256;

    /**
     * A entrada de uma porta.
     *
     * @param front o bloco em frente à porta, ou a abertura do quintal
     * @param outside o primeiro chão fora da caixa, em linha reta da porta
     * @param way as colunas entre {@code front} e {@code outside}, as duas inclusive; vazio quando já está fora
     */
    record Entrance(BlockPos front, BlockPos outside, List<BlockPos> way) {
    }

    /** A saída em linha reta: o chão fora da caixa, se ela chegou lá, e o primeiro chão de quintal do caminho. */
    private record Walk(Optional<BlockPos> out, List<BlockPos> way, Optional<BlockPos> yard) {
    }

    private DoorFronts() {
    }

    /** O marcador de cada porta. */
    static List<BlockPos> of(ServerWorld world, BlockPos min, BlockPos max) {
        return entrances(world, min, max).stream().map(Entrance::front).toList();
    }

    /** O chão fora da caixa em linha reta de cada porta: a altura de quem chega à casa (Regra 178). */
    static List<BlockPos> outside(ServerWorld world, BlockPos min, BlockPos max) {
        return entrances(world, min, max).stream().map(Entrance::outside).toList();
    }

    static List<Entrance> entrances(ServerWorld world, BlockPos min, BlockPos max) {
        List<Entrance> fronts = new ArrayList<>();
        List<BlockPos> yards = new ArrayList<>();

        for (BlockPos at : BlockPos.iterate(min, new BlockPos(max.getX(), Math.min(max.getY(), min.getY() + GROUND_FLOOR), max.getZ()))) {
            if (world.isChunkLoaded(at)) {
                BlockState state = world.getBlockState(at);

                if (state.isIn(BlockTags.DOORS) && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                    // O facing da porta aponta para dentro: é para onde olhava quem a pôs, do lado de fora.
                    Direction inward = state.get(DoorBlock.FACING);
                    Walk out = walkOut(world, at, inward.getOpposite(), min, max);
                    Walk chosen = out.out().isPresent() ? out : walkOut(world, at, inward, min, max);

                    if (chosen.out().isPresent()) {
                        addOnce(fronts, new Entrance(chosen.way().getFirst(), chosen.out().get(), chosen.way()));
                    } else {
                        out.yard().ifPresent(yard -> addYardOnce(yards, yard));
                    }
                }
            }
        }

        if (!fronts.isEmpty()) {
            return fronts;
        }

        List<Entrance> open = new ArrayList<>();

        for (BlockPos yard : yards) {
            BlockPos front = openingOf(world, yard, min, max).orElse(yard);

            addOnce(open, new Entrance(front, front, List.of()));
        }

        return open;
    }

    private static void addOnce(List<Entrance> list, Entrance entrance) {
        if (list.stream().noneMatch(other -> other.front().getManhattanDistance(entrance.front()) <= 1)) {
            list.add(entrance);
        }
    }

    private static void addYardOnce(List<BlockPos> list, BlockPos yard) {
        if (list.stream().noneMatch(other -> other.getManhattanDistance(yard) <= 1)) {
            list.add(yard);
        }
    }

    private static boolean isOutside(BlockPos column, BlockPos min, BlockPos max) {
        return column.getX() < min.getX() || column.getX() > max.getX()
                || column.getZ() < min.getZ() || column.getZ() > max.getZ();
    }

    private static Walk walkOut(ServerWorld world, BlockPos door, Direction out, BlockPos min, BlockPos max) {
        Optional<BlockPos> yard = Optional.empty();
        List<BlockPos> way = new ArrayList<>();

        for (int step = 1; step <= MAX_WALK; step++) {
            BlockPos column = door.offset(out, step);

            if (!world.isChunkLoaded(column)) {
                return new Walk(Optional.empty(), way, yard);
            }

            Optional<BlockPos> floor = floorAt(world, column.up());

            if (floor.isEmpty() || (floor.get().getY() >= door.getY() && !isStep(world.getBlockState(floor.get())))) {
                return new Walk(Optional.empty(), way, yard);
            }

            way.add(floor.get());

            BlockState ground = world.getBlockState(floor.get());

            if (isOutside(column, min, max) && !isStep(ground)) {
                return new Walk(floor, way, yard);
            }

            if (yard.isEmpty() && !isStep(ground) && !ground.isIn(BlockTags.PLANKS)) {
                yard = floor;
            }
        }

        return new Walk(Optional.empty(), way, yard);
    }

    /** Onde o quintal abre para fora da caixa: o primeiro chão de fora que se alcança andando desde a porta. */
    private static Optional<BlockPos> openingOf(ServerWorld world, BlockPos yard, BlockPos min, BlockPos max) {
        Set<BlockPos> seen = new HashSet<>(Set.of(yard));
        Deque<BlockPos> open = new ArrayDeque<>(List.of(yard));

        while (!open.isEmpty() && seen.size() <= MAX_YARD) {
            BlockPos at = open.poll();

            for (Direction way : Direction.Type.HORIZONTAL) {
                BlockPos column = at.offset(way);

                if (!world.isChunkLoaded(column)) {
                    continue;
                }

                Optional<BlockPos> floor = floorAt(world, column.up(2));

                if (floor.isEmpty() || Math.abs(floor.get().getY() - at.getY()) > 1 || !hasRoom(world, floor.get())
                        || world.getBlockState(floor.get()).isIn(BlockTags.PLANKS)) {
                    continue;
                }

                if (isOutside(column, min, max)) {
                    return floor;
                }

                if (seen.add(floor.get())) {
                    open.add(floor.get());
                }
            }
        }

        return Optional.empty();
    }

    private static boolean hasRoom(ServerWorld world, BlockPos floor) {
        for (int up = 1; up <= HEADROOM; up++) {
            BlockPos above = floor.up(up);

            if (!world.getBlockState(above).getCollisionShape(world, above).isEmpty()) {
                return false;
            }
        }

        return true;
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
