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
 * A entrada de cada porta térrea da construção — Regras 207, 223 e 305 (autor, 2026-11-17): o nó é o primeiro chão à frente
 * da porta fora dos blocos da estrutura (degrau, laje e assoalho não contam), com passagem livre acima, mesmo dentro da
 * caixa, e a saída da caixa é só o caminho até ele. Porta que não dá para fora, a de dentro da casa,
 * não conta; porta dupla conta uma vez. A caixa da peça pode incluir o quintal: porta cuja saída para em barreira dentro dela
 * só conta se nenhuma outra da casa sair, e então a entrada é o chão logo na frente dela, no quintal ou tapado pelo chão
 * natural, que o Zelador acerta (Regra 360). Assoalho não é quintal — Regra 209.
 */
final class DoorFronts {

    private static final int MAX_WALK = 16;

    /** Até quantos blocos abaixo da porta procurar o chão. */
    private static final int MAX_DROP = 4;

    /** Quantas alturas da caixa, a partir do chão, contam como térreo. */
    private static final int GROUND_FLOOR = 3;

    /** Quantos blocos livres acima do chão fazem uma passagem. */
    private static final int HEADROOM = 2;

    /**
     * A entrada de uma porta.
     *
     * @param front o bloco em frente à porta (Regra 360)
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

    /** Uma porta térrea e o lado de fora dela: o oposto de para onde olhava quem a pôs. */
    record GroundDoor(BlockPos at, Direction out) {
    }

    /** As portas térreas da caixa: cada metade de baixo conta, também na porta dupla (Regra 356). */
    static List<GroundDoor> groundDoors(ServerWorld world, BlockPos min, BlockPos max) {
        List<GroundDoor> doors = new ArrayList<>();

        for (BlockPos at : BlockPos.iterate(min, new BlockPos(max.getX(), Math.min(max.getY(), min.getY() + GROUND_FLOOR), max.getZ()))) {
            if (world.isChunkLoaded(at)) {
                BlockState state = world.getBlockState(at);

                if (state.isIn(BlockTags.DOORS) && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                        && GroundFloorDoors.isGroundFloor(world, at, state.get(DoorBlock.FACING).getOpposite())) {
                    doors.add(new GroundDoor(at.toImmutable(), state.get(DoorBlock.FACING).getOpposite()));
                }
            }
        }

        return doors;
    }

    static List<Entrance> entrances(ServerWorld world, BlockPos min, BlockPos max) {
        List<Entrance> fronts = new ArrayList<>();
        List<Entrance> blocked = new ArrayList<>();

        for (BlockPos at : BlockPos.iterate(min, new BlockPos(max.getX(), Math.min(max.getY(), min.getY() + GROUND_FLOOR), max.getZ()))) {
            if (world.isChunkLoaded(at)) {
                BlockState state = world.getBlockState(at);

                if (state.isIn(BlockTags.DOORS) && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                        // O grafo nunca busca porta do andar de cima (Regra 335).
                        && (GroundFloorDoors.isGroundFloor(world, at, state.get(DoorBlock.FACING).getOpposite())
                        || GroundFloorDoors.isGroundFloor(world, at, state.get(DoorBlock.FACING)))) {
                    // O facing da porta aponta para dentro: é para onde olhava quem a pôs, do lado de fora.
                    Direction inward = state.get(DoorBlock.FACING);
                    Walk out = walkOut(world, at, inward.getOpposite(), min, max);
                    Walk chosen = out.out().isPresent() ? out : walkOut(world, at, inward, min, max);

                    if (chosen.out().isPresent()) {
                        int first = firstGround(world, chosen.way());
                        List<BlockPos> way = chosen.way().subList(first, chosen.way().size());

                        addOnce(fronts, new Entrance(way.getFirst(), chosen.out().get(), List.copyOf(way)));
                    } else if (out.yard().isPresent() || isBuried(world, at, inward.getOpposite())) {
                        // Sem saída reta: a entrada fica na frente da porta, e não onde o quintal abre (Regra 360).
                        BlockPos front = frontOf(world, at, inward.getOpposite());

                        addOnce(blocked, new Entrance(front, front, List.of()));
                    }
                }
            }
        }

        return fronts.isEmpty() ? blocked : fronts;
    }

    /** O chão logo na frente da porta: o de baixo dela, quando o chão natural tapa a passagem (Regra 360). */
    private static BlockPos frontOf(ServerWorld world, BlockPos door, Direction out) {
        BlockPos column = door.offset(out);
        Optional<BlockPos> floor = floorAt(world, column.up());

        return floor.filter(at -> at.getY() < door.getY()).orElse(column.down());
    }

    /** Se o chão natural tapa a frente da porta, na altura dela. */
    private static boolean isBuried(ServerWorld world, BlockPos door, Direction out) {
        BlockPos front = door.offset(out);

        return world.isChunkLoaded(front)
                && com.villagecolony.fabric.integration.SiteSurroundings.isNaturalGround(world.getBlockState(front));
    }

    private static void addOnce(List<Entrance> list, Entrance entrance) {
        if (list.stream().noneMatch(other -> other.front().getManhattanDistance(entrance.front()) <= 1)) {
            list.add(entrance);
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

    /**
     * O nó de entrada: o primeiro chão do trajeto fora dos blocos da estrutura — terra, pedra do mundo ou caminho — com
     * passagem livre acima (Regra 305). Sem nenhum, o último, que é o chão fora da caixa.
     */
    private static int firstGround(ServerWorld world, List<BlockPos> way) {
        for (int i = 0; i < way.size(); i++) {
            BlockState state = world.getBlockState(way.get(i));

            if ((com.villagecolony.fabric.integration.SiteSurroundings.isNaturalGround(state)
                    || com.villagecolony.fabric.integration.VillageRoad.isPathBlock(state)) && hasRoom(world, way.get(i))) {
                return i;
            }
        }

        return way.size() - 1;
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
