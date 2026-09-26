package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.BedPart;
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
 * dentro da peça vanilla que contém a cama. Desde 2026-09-26 (regra (b) do
 * autor) o baú vai ao lado da cama, encostado numa parede e nunca diante de
 * uma porta.
 */
public final class ChestPlacer {

    static {
        ServerMemory.register(ChestPlacer.class, ChestPlacer::clearAll);
    }

    /** Resultado estável de uma única tentativa de adoção. */
    public enum Outcome {
        PLACED,
        ALREADY_PRESENT,
        SKIPPED_NOT_A_COMPLETE_BED,
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
     * Encontra ou cria o baú desta cama dentro da peça — regra (b), decisão do
     * autor em 2026-09-26: <i>"ao lado da cama e encostado em uma parede,
     * nunca na frente da porta"</i>.
     *
     * <p><b>Por que mudou.</b> A regra de 23-09 só punha baú quando a porta da
     * peça era inequívoca, e a caixa da casa vanilla inclui o degrau de fora:
     * a porta tinha dois lados "dentro" e a regra desistia. Na vila de 26-09
     * foram 3 camas de 3; nas oito vilas da sessão, 18 recusas contra 15 baús.
     * Agora a porta não decide se há baú — ela só diz onde ele não pode ficar.
     *
     * <p><b>O que continua valendo:</b> dentro da peça vanilla, nunca a outra
     * metade da cama, nunca sobre o vazio, nunca debaixo de teto (não abriria),
     * nunca colado noutro baú (viraria baú duplo), nunca trocando bloco que
     * não seja substituível.
     */
    public static Result placeForOriginalVillageBed(
            ServerWorld world, BlockPos bedPoi, BlockBox piece) {
        return placeBeside(world, bedPoi, piece::contains);
    }

    /**
     * A mesma regra (b) para a cama de qualquer trabalhador — 2026-09-26,
     * decisão do autor: "todos aldeões de profissão devem ter um baú nascido
     * destinado a cada um deles". Sem a peça de vila para conter o baú: a
     * cama pode estar numa casa da colônia ou na BigHouseMOD.
     */
    public static Result placeBesideBed(ServerWorld world, BlockPos bedPoi) {
        return placeBeside(world, bedPoi, spot -> true);
    }

    private static Result placeBeside(
            ServerWorld world, BlockPos bedPoi, java.util.function.Predicate<BlockPos> inside) {
        Optional<Bed> bed = completeBed(world, bedPoi);
        if (bed.isEmpty()) {
            return new Result(Optional.empty(), Outcome.SKIPPED_NOT_A_COMPLETE_BED);
        }

        for (BlockPos spot : candidates(bed.get())) {
            if (!inside.test(spot) || isDoorApproach(world, spot)) {
                continue;
            }

            Optional<Direction> wall = wallBeside(world, spot);
            if (wall.isEmpty()) {
                continue;
            }

            Direction opening = wall.get().getOpposite();

            if (isCompliantChest(world, spot, opening)) {
                return new Result(Optional.of(spot), Outcome.ALREADY_PRESENT);
            }

            if (!isSafeEmptyChestSpot(world, spot)) {
                continue;
            }

            world.setBlockState(spot, Blocks.CHEST.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, opening));
            return new Result(Optional.of(spot), Outcome.PLACED);
        }

        return new Result(Optional.empty(), Outcome.SKIPPED_NO_SAFE_POSITION);
    }

    /** Se este baú é a posição privada e conforme de uma cama vanilla — a mesma regra (b). */
    public static boolean isCompliantVillageBedChest(
            ServerWorld world, BlockPos chest, BlockBox piece) {
        if (!piece.contains(chest) || isDoorApproach(world, chest)) {
            return false;
        }

        Optional<Direction> wall = wallBeside(world, chest);
        if (wall.isEmpty() || !isCompliantChest(world, chest, wall.get().getOpposite())) {
            return false;
        }

        for (Direction direction : Direction.Type.HORIZONTAL) {
            Optional<Bed> bed = completeBed(world, chest.offset(direction));
            if (bed.isPresent() && candidates(bed.get()).contains(chest)) {
                return true;
            }
        }

        return false;
    }

    /**
     * A parede em que o baú encosta: o primeiro vizinho horizontal que é bloco
     * sólido inteiro — nem cama, nem baú, nem porta. Sem parede, sem baú: ele
     * não fica solto no meio do quarto.
     */
    private static Optional<Direction> wallBeside(ServerWorld world, BlockPos spot) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos side = spot.offset(direction);
            BlockState state = world.getBlockState(side);

            if (state.getBlock() instanceof BedBlock
                    || state.getBlock() instanceof DoorBlock
                    || state.isOf(Blocks.CHEST)) {
                continue;
            }

            if (state.isSolidBlock(world, side)) {
                return Optional.of(direction);
            }
        }

        return Optional.empty();
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

    /**
     * Se a posição fica diante de uma porta — a célula de qualquer lado dela,
     * na metade de baixo ou na de cima. Baú ali tranca a entrada.
     */
    private static boolean isDoorApproach(ServerWorld world, BlockPos spot) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            for (BlockPos door : List.of(spot.offset(direction), spot.offset(direction).up())) {
                if (world.getBlockState(door).getBlock() instanceof DoorBlock) {
                    return true;
                }
            }
        }
        return false;
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
