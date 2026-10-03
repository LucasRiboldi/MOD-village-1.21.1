package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Acesso selado para a mina de uma colônia comprovadamente fundada na água.
 *
 * <p>A rota não é salva: os blocos de vidro e degraus são a fonte da verdade,
 * enquanto a {@link Mine} já guarda sua saída inferior. A qualificação lê
 * apenas chunks carregados e não altera o mundo; {@link Route#place(ServerWorld)}
 * confere tudo de novo antes de escrever qualquer bloco.
 */
public final class WaterMineAccess {

    private static final int FOUNDATION_RADIUS = 2;
    private static final int FOUNDATION_CELLS = 25;
    private static final int ACCESS_DISTANCE = 12;
    private static final int ACCESS_STEPS = 12;
    private static final int LANES = 3;
    private static final int CLEAR_HEIGHT = 3;
    private static final int EXIT_SIDE = 8;

    private WaterMineAccess() {
    }

    /**
     * Procura uma única rota aquática, sem carregar chunks e somente depois de
     * a busca normal de boca ter falhado.
     */
    public static Optional<Route> find(ServerWorld world, BlockPos center, Side descent) {
        if (!isWaterFounded(world, center)) {
            return Optional.empty();
        }

        BlockPos surface = center.offset(toDirection(descent), ACCESS_DISTANCE);
        Route route = new Route(surface, descent, ACCESS_STEPS);

        if (!route.canUse(world) || !hasNaturalStoneExit(world, route.entry())) {
            return Optional.empty();
        }

        return Optional.of(route);
    }

    /** A rota física ligada à saída que a mina persistida usa. */
    public record Route(BlockPos surface, Side descent, int steps) {

        public Route {
            if (steps <= 0) {
                throw new IllegalArgumentException("steps must be positive: " + steps);
            }
        }

        /** A entrada inferior passada para a geometria normal da mina. */
        public BlockPos entry() {
            return surface.offset(toDirection(descent), steps).down(steps);
        }

        /** Os três degraus de cada lance do piso. */
        public Set<BlockPos> stairs() {
            Set<BlockPos> positions = new HashSet<>();

            forEachStep((floor, lane) -> positions.add(floor.offset(sideways(descent), lane)));

            return Set.copyOf(positions);
        }

        /** As duas camadas de ar acima dos degraus, para três blocos úteis. */
        public Set<BlockPos> interior() {
            Set<BlockPos> positions = new HashSet<>();

            forEachStep((floor, lane) -> {
                BlockPos stair = floor.offset(sideways(descent), lane);
                for (int up = 1; up < CLEAR_HEIGHT; up++) {
                    positions.add(stair.up(up));
                }
            });

            return Set.copyOf(positions);
        }

        /** Piso externo, paredes e teto de vidro que isolam o corredor. */
        public Set<BlockPos> shell() {
            Set<BlockPos> positions = new HashSet<>();

            forEachStep((floor, ignored) -> {
                for (int lane = -1; lane <= LANES; lane++) {
                    BlockPos column = floor.offset(sideways(descent), lane);
                    positions.add(column.up(CLEAR_HEIGHT));
                }

                for (int up = 0; up <= CLEAR_HEIGHT; up++) {
                    positions.add(floor.offset(sideways(descent), -1).up(up));
                    positions.add(floor.offset(sideways(descent), LANES).up(up));
                }
            });

            return Set.copyOf(positions);
        }

        /** Tudo o que é infraestrutura e nunca pode voltar à fila de escavação. */
        public boolean contains(BlockPos at) {
            return stairs().contains(at) || interior().contains(at) || shell().contains(at);
        }

        /** Coloca a rota inteira ou não muda nada caso alguma revalidação falhe. */
        public boolean place(ServerWorld world) {
            if (!canUse(world)) {
                return false;
            }

            for (BlockPos at : shell()) {
                world.setBlockState(at, Blocks.GLASS.getDefaultState(), Block.NOTIFY_ALL);
            }

            for (BlockPos at : interior()) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }

            BlockState stair = Blocks.STONE_BRICK_STAIRS.getDefaultState()
                    .with(StairsBlock.FACING, toDirection(descent))
                    .with(StairsBlock.HALF, net.minecraft.block.enums.BlockHalf.BOTTOM);
            for (BlockPos at : stairs()) {
                world.setBlockState(at, stair, Block.NOTIFY_ALL);
            }

            return true;
        }

        private boolean canUse(ServerWorld world) {
            for (BlockPos at : allCells()) {
                if (!isLoaded(world, at) || isProtected(world, at)
                        || !isReplaceableByRoute(world, at)) {
                    return false;
                }
            }

            return true;
        }

        private boolean isReplaceableByRoute(ServerWorld world, BlockPos at) {
            return isStoneAtExitLayer(world, at)
                    ? MineRock.isDiggableRock(world, at)
                    : WaterMineAccess.isReplaceableByRoute(world, at);
        }

        /**
         * A última camada pode tocar a rocha da área que a galeria vai
         * explorar. Inclui a parede lateral de vidro; sem isso ela recusava
         * exatamente a borda natural que deve vedar.
         */
        private boolean isStoneAtExitLayer(ServerWorld world, BlockPos at) {
            return at.getY() == entry().getY() && MineRock.isDiggableRock(world, at);
        }

        private Set<BlockPos> allCells() {
            Set<BlockPos> positions = new HashSet<>(stairs());
            positions.addAll(interior());
            positions.addAll(shell());
            return positions;
        }

        private void forEachStep(StepCell consumer) {
            Direction forward = toDirection(descent);

            for (int step = 1; step <= steps; step++) {
                BlockPos floor = surface.offset(forward, step).down(step);

                for (int lane = 0; lane < LANES; lane++) {
                    consumer.accept(floor, lane);
                }
            }
        }
    }

    /** Reconhece a concha por blocos reais próximos da entrada persistida. */
    public static boolean protects(ServerWorld world, Mine mine, BlockPos at) {
        BlockState state = world.getBlockState(at);

        if (!state.isOf(Blocks.GLASS) && !state.isOf(Blocks.STONE_BRICK_STAIRS)) {
            return false;
        }

        BlockPos entry = MinecraftTypeAdapter.toBlockPos(mine.entry());
        int dx = at.getX() - entry.getX();
        int dy = at.getY() - entry.getY();
        int dz = at.getZ() - entry.getZ();

        return dx * dx + dy * dy + dz * dz <= (ACCESS_STEPS + 4) * (ACCESS_STEPS + 4)
                && touchesRouteMaterial(world, at);
    }

    private static boolean isWaterFounded(ServerWorld world, BlockPos center) {
        int water = 0;

        for (int dx = -FOUNDATION_RADIUS; dx <= FOUNDATION_RADIUS; dx++) {
            for (int dz = -FOUNDATION_RADIUS; dz <= FOUNDATION_RADIUS; dz++) {
                BlockPos sample = center.add(dx, 0, dz);

                if (!isLoaded(world, sample)) {
                    return false;
                }

                if (!world.getFluidState(sample).isEmpty()) {
                    water++;
                }
            }
        }

        return water >= FOUNDATION_CELLS * 3 / 4;
    }

    private static boolean hasNaturalStoneExit(ServerWorld world, BlockPos entry) {
        for (int x = 0; x < EXIT_SIDE; x++) {
            for (int z = 0; z < EXIT_SIDE; z++) {
                BlockPos at = entry.add(x, 0, z);

                if (!isLoaded(world, at) || !MineRock.isDiggableRock(world, at)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isLoaded(ServerWorld world, BlockPos at) {
        return world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4) != null;
    }

    private static boolean isProtected(ServerWorld world, BlockPos at) {
        return BlockProtection.isVillageOriginal(world, at)
                || BlockProtection.isColonyBuilt(at)
                || BlockProtection.isPlayerPlaced(world.getBlockState(at));
    }

    private static boolean isReplaceableByRoute(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);
        return state.isReplaceable() || !state.getFluidState().isEmpty();
    }

    private static boolean touchesRouteMaterial(ServerWorld world, BlockPos at) {
        for (Direction direction : Direction.values()) {
            BlockState neighbor = world.getBlockState(at.offset(direction));

            if (neighbor.isOf(Blocks.GLASS) || neighbor.isOf(Blocks.STONE_BRICK_STAIRS)) {
                return true;
            }
        }

        return false;
    }

    private static Direction toDirection(Side side) {
        return switch (side) {
            case NORTH -> Direction.NORTH;
            case EAST -> Direction.EAST;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
        };
    }

    private static Direction sideways(Side descent) {
        return toDirection(descent).rotateYCounterclockwise();
    }

    @FunctionalInterface
    private interface StepCell {
        void accept(BlockPos floor, int lane);
    }
}
