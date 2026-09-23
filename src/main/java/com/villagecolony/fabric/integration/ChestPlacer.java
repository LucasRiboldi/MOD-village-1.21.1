package com.villagecolony.fabric.integration;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Posiciona o baú privado de uma cama de vila vanilla já confirmada.
 *
 * <p>Escrever um baú no mundo só é permitido na adoção inicial da vila e
 * dentro da peça vanilla que contém a cama. A rotina prefere desistir a
 * adivinhar o cômodo, a porta ou a abertura do baú.
 */
public final class ChestPlacer {

    /** Resultado estável de uma única tentativa de adoção. */
    public enum Outcome {
        PLACED,
        ALREADY_PRESENT,
        SKIPPED_NOT_A_COMPLETE_BED,
        SKIPPED_NO_UNAMBIGUOUS_DOOR,
        SKIPPED_NO_SAFE_POSITION
    }

    /** Posição encontrada ou criada, junto do motivo quando não houve baú. */
    public record Result(Optional<BlockPos> chest, Outcome outcome) {
    }

    private ChestPlacer() {
    }

    /**
     * Compatibilidade para chamadores antigos: ciclos de trabalhador não
     * têm a prova de peça vanilla nem de porta exigida para escrever.
     */
    public static Optional<BlockPos> placeBeside(ServerWorld world, BlockPos bed) {
        return Optional.empty();
    }

    /** Não há mais memória de recusas: a tentativa acontece uma vez na adoção. */
    public static void clearAll() {
        // Mantido para a fronteira de ciclo de vida enquanto os chamadores antigos somem.
    }

    /**
     * Encontra ou cria o único baú aceitável para esta cama dentro da peça.
     * A porta só é aceita quando possui exatamente uma célula caminhável do
     * lado interno da caixa; peças ambíguas são ignoradas.
     */
    public static Result placeForOriginalVillageBed(
            ServerWorld world, BlockPos bedPoi, BlockBox piece) {
        Optional<Bed> bed = completeBed(world, bedPoi);
        if (bed.isEmpty()) {
            return new Result(Optional.empty(), Outcome.SKIPPED_NOT_A_COMPLETE_BED);
        }

        Optional<BlockPos> insideDoor = unambiguousInsideDoor(world, piece);
        if (insideDoor.isEmpty()) {
            return new Result(Optional.empty(), Outcome.SKIPPED_NO_UNAMBIGUOUS_DOOR);
        }

        for (BlockPos spot : candidates(bed.get())) {
            if (isDoorApproach(world, piece, spot)) {
                continue;
            }

            Optional<Direction> opening = directionToward(spot, insideDoor.get());
            if (opening.isEmpty() || !hasWallBehind(world, piece, spot, opening.get())) {
                continue;
            }

            if (isCompliantChest(world, spot, opening.get())) {
                return new Result(Optional.of(spot), Outcome.ALREADY_PRESENT);
            }

            if (!isSafeEmptyChestSpot(world, spot)) {
                continue;
            }

            world.setBlockState(spot, Blocks.CHEST.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, opening.get()));
            return new Result(Optional.of(spot), Outcome.PLACED);
        }

        return new Result(Optional.empty(), Outcome.SKIPPED_NO_SAFE_POSITION);
    }

    /** Se este baú é a posição privada e conforme de uma cama vanilla. */
    public static boolean isCompliantVillageBedChest(
            ServerWorld world, BlockPos chest, BlockBox piece) {
        if (!piece.contains(chest)) {
            return false;
        }

        Optional<BlockPos> insideDoor = unambiguousInsideDoor(world, piece);
        if (insideDoor.isEmpty()) {
            return false;
        }

        for (Direction direction : Direction.Type.HORIZONTAL) {
            Optional<Bed> bed = completeBed(world, chest.offset(direction));
            if (bed.isEmpty()) {
                continue;
            }
            if (!candidates(bed.get()).contains(chest) || isDoorApproach(world, piece, chest)) {
                continue;
            }
            Optional<Direction> opening = directionToward(chest, insideDoor.get());
            if (opening.isPresent()
                    && hasWallBehind(world, piece, chest, opening.get())
                    && isCompliantChest(world, chest, opening.get())) {
                return true;
            }
        }

        return false;
    }

    private static Optional<Bed> completeBed(ServerWorld world, BlockPos poi) {
        BlockState state = world.getBlockState(poi);
        if (!(state.getBlock() instanceof BedBlock) || !state.contains(Properties.BED_PART)
                || !state.contains(Properties.HORIZONTAL_FACING)) {
            return Optional.empty();
        }

        Direction facing = state.get(Properties.HORIZONTAL_FACING);
        BlockPos foot = state.get(Properties.BED_PART) == BedPart.FOOT
                ? poi : poi.offset(facing.getOpposite());
        BlockState footState = world.getBlockState(foot);
        BlockState headState = world.getBlockState(foot.offset(facing));
        if (!(footState.getBlock() instanceof BedBlock)
                || !(headState.getBlock() instanceof BedBlock)
                || footState.get(Properties.BED_PART) != BedPart.FOOT
                || headState.get(Properties.BED_PART) != BedPart.HEAD
                || footState.get(Properties.HORIZONTAL_FACING) != facing
                || headState.get(Properties.HORIZONTAL_FACING) != facing) {
            return Optional.empty();
        }
        return Optional.of(new Bed(foot, facing));
    }

    private static Optional<BlockPos> unambiguousInsideDoor(ServerWorld world, BlockBox piece) {
        List<BlockPos> inside = new ArrayList<>();
        int doors = 0;
        for (int x = piece.getMinX(); x <= piece.getMaxX(); x++) {
            for (int y = piece.getMinY(); y <= piece.getMaxY(); y++) {
                for (int z = piece.getMinZ(); z <= piece.getMaxZ(); z++) {
                    BlockPos door = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(door);
                    if (!(state.getBlock() instanceof DoorBlock)
                            || !state.contains(Properties.DOUBLE_BLOCK_HALF)
                            || state.get(Properties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER) {
                        continue;
                    }
                    doors++;
                    for (Direction direction : Direction.Type.HORIZONTAL) {
                        BlockPos candidate = door.offset(direction);
                        if (piece.contains(candidate)
                                && world.getBlockState(candidate).isAir()
                                && world.getBlockState(candidate.down())
                                        .isSolidBlock(world, candidate.down())) {
                            inside.add(candidate);
                        }
                    }
                }
            }
        }
        return doors == 1 && inside.size() == 1 ? Optional.of(inside.getFirst()) : Optional.empty();
    }

    private static List<BlockPos> candidates(Bed bed) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos half : List.of(bed.foot(), bed.foot().offset(bed.facing()))) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                if (direction != bed.facing() && direction != bed.facing().getOpposite()) {
                    found.add(half.offset(direction));
                }
            }
        }
        return found;
    }

    private static boolean isDoorApproach(ServerWorld world, BlockBox piece, BlockPos spot) {
        for (int x = piece.getMinX(); x <= piece.getMaxX(); x++) {
            for (int y = piece.getMinY(); y <= piece.getMaxY(); y++) {
                for (int z = piece.getMinZ(); z <= piece.getMaxZ(); z++) {
                    BlockPos door = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(door);
                    if (!(state.getBlock() instanceof DoorBlock)
                            || !state.contains(Properties.DOUBLE_BLOCK_HALF)
                            || state.get(Properties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER) {
                        continue;
                    }
                    for (Direction direction : Direction.Type.HORIZONTAL) {
                        if (spot.equals(door.offset(direction))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static Optional<Direction> directionToward(BlockPos from, BlockPos target) {
        if (from.getY() != target.getY()) {
            return Optional.empty();
        }
        if (from.getX() == target.getX() && from.getZ() != target.getZ()) {
            return Optional.of(target.getZ() > from.getZ() ? Direction.SOUTH : Direction.NORTH);
        }
        if (from.getZ() == target.getZ() && from.getX() != target.getX()) {
            return Optional.of(target.getX() > from.getX() ? Direction.EAST : Direction.WEST);
        }
        return Optional.empty();
    }

    private static boolean hasWallBehind(
            ServerWorld world, BlockBox piece, BlockPos spot, Direction opening) {
        BlockPos wall = spot.offset(opening.getOpposite());
        return piece.contains(wall) && world.getBlockState(wall).isSolidBlock(world, wall);
    }

    private static boolean isCompliantChest(ServerWorld world, BlockPos spot, Direction opening) {
        BlockState state = world.getBlockState(spot);
        return state.isOf(Blocks.CHEST)
                && state.contains(Properties.HORIZONTAL_FACING)
                && state.get(Properties.HORIZONTAL_FACING) == opening
                && world.getBlockState(spot.up()).isAir()
                && world.getBlockState(spot.down()).isSolidBlock(world, spot.down())
                && noAdjacentChest(world, spot);
    }

    private static boolean isSafeEmptyChestSpot(ServerWorld world, BlockPos spot) {
        BlockState state = world.getBlockState(spot);
        return state.isReplaceable()
                && !(state.getBlock() instanceof BedBlock)
                && world.getBlockState(spot.down()).isSolidBlock(world, spot.down())
                && world.getBlockState(spot.up()).isAir()
                && noAdjacentChest(world, spot);
    }

    private static boolean noAdjacentChest(ServerWorld world, BlockPos spot) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (world.getBlockState(spot.offset(direction)).isOf(Blocks.CHEST)) {
                return false;
            }
        }
        return true;
    }

    private record Bed(BlockPos foot, Direction facing) {
    }
}
