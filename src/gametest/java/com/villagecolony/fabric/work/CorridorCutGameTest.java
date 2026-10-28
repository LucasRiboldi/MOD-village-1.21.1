package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.Corridor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O degrau cortado na duna — Regra 70, autor 2026-10-22: o corredor da faixa anexada desce a encosta um
 * bloco por vez cortando a areia acima, e a abertura da obra abre um vale de três com o caminho no meio.
 * Cenário quarenta blocos acima do piso, longe das outras arenas, e desfeito no fim.
 */
public class CorridorCutGameTest implements FabricGameTest {

    private static final int Y = 40;

    /** A rua em z 0; a duna sobe três blocos por coluna a partir de z 3 até o platô seis acima. */
    private static void dune(TestContext context) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 14; z++) {
                context.setBlockState(new BlockPos(x, Y, z),
                        z == 0 ? Blocks.DIRT_PATH.getDefaultState() : Blocks.DIRT.getDefaultState());

                for (int y = Y + 1; y <= Y + Math.min(6, Math.max(0, 3 * (z - 2))); y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.SAND.getDefaultState());
                }
            }
        }
    }

    private static int topOf(ServerWorld world, int x, int z) {
        return world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    private static void clear(TestContext context) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 14; z++) {
                for (int y = Y; y <= Y + 7; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "corridor_cut")
    public void theCorridorCutsStepsDownTheDuneWithSlopedSides(TestContext context) {
        ServerWorld world = context.getWorld();

        dune(context);

        BlockPos door = context.getAbsolutePos(new BlockPos(2, Y + 6, 12));
        Direction north = context.getRotation().rotate(Direction.NORTH);
        Optional<List<BlockPos>> natural = Corridor.plan(world, UUID.randomUUID(), door, north, 32, at -> false);
        Optional<List<BlockPos>> cut = Corridor.plan(
                world, UUID.randomUUID(), door, north, 32, Corridor.MAX_CUT, at -> false, why -> { });

        context.assertTrue(natural.isEmpty(), "sem cortar, a parede de três por bloco devia parar o corredor");
        context.assertTrue(cut.isPresent(), "cortando, o corredor não desceu a duna");

        List<BlockPos> cells = cut.get();

        for (int i = 1; i < cells.size(); i++) {
            BlockPos a = cells.get(i - 1);
            BlockPos b = cells.get(i);

            context.assertTrue(Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ()) == 1,
                    "o corredor pulou ou andou na diagonal: " + a + " → " + b);
            context.assertTrue(Math.abs(a.getY() - b.getY()) <= 1, "degrau de mais de um: " + a + " → " + b);
        }

        java.util.Map<Long, Integer> before = new java.util.HashMap<>();
        for (BlockPos cell : cells) {
            for (int dx = -Corridor.MAX_CUT; dx <= Corridor.MAX_CUT; dx++) {
                for (int dz = -Corridor.MAX_CUT; dz <= Corridor.MAX_CUT; dz++) {
                    before.put(net.minecraft.util.math.ChunkPos.toLong(cell.getX() + dx, cell.getZ() + dz),
                            topOf(world, cell.getX() + dx, cell.getZ() + dz));
                }
            }
        }

        int moved = CorridorCut.cut(world, UUID.randomUUID(), Corridor.asColonyPositions(cells));

        context.assertTrue(moved > 0, "a abertura não cortou nada");

        for (BlockPos cell : cells) {
            context.assertTrue(world.getBlockState(cell.up()).isAir() && world.getBlockState(cell.up(2)).isAir(),
                    "o caminho ficou sem espaço para andar em " + context.getRelativePos(cell));
        }

        // No pé da duna os lados descem em talude, um degrau por bloco (opção 3 do autor, 2026-10-28): o
        // degrau a um bloco do caminho fica no piso + 1 e se abre por cima, sem a parede reta do cânion.
        BlockPos column = context.getAbsolutePos(new BlockPos(2, Y, 4));
        BlockPos foot = cells.stream()
                .filter(cell -> cell.getX() == column.getX() && cell.getZ() == column.getZ())
                .findFirst()
                .orElseThrow();

        context.assertTrue(foot.getY() < column.getY() + 6, "o caminho não ficou abaixo do topo da duna: " + foot);

        // Sem parede de cânion: onde o passo cortou d blocos, a coluna a k ≤ d dele fica no máximo k acima do
        // piso — os degraus do talude. O cânion antigo, com a parede reta a dois blocos do caminho, falha aqui.
        for (BlockPos cell : cells) {
            int depth = before.get(net.minecraft.util.math.ChunkPos.toLong(cell.getX(), cell.getZ())) - cell.getY();

            for (Direction side : Direction.Type.HORIZONTAL) {
                for (int k = 1; k <= Math.min(depth, Corridor.MAX_CUT); k++) {
                    BlockPos beside = cell.offset(side, k);

                    // Caminho do lado, ou chão natural que já era mais alto que a coluna cortada (o paredão da
                    // duna ao pé do qual o corredor vira): não é parede que o corte abriu.
                    if (cells.stream().anyMatch(other -> other.getX() == beside.getX() && other.getZ() == beside.getZ())
                            || before.get(net.minecraft.util.math.ChunkPos.toLong(beside.getX(), beside.getZ()))
                                    > before.get(net.minecraft.util.math.ChunkPos.toLong(cell.getX(), cell.getZ()))) {
                        break;
                    }

                    int top = topOf(world, beside.getX(), beside.getZ());

                    context.assertTrue(top <= cell.getY() + k, "parede ao lado de " + context.getRelativePos(cell)
                            + " para " + side + ": a coluna a " + k + " fica " + (top - cell.getY()) + " acima do piso");
                }
            }
        }

        clear(context);
        context.complete();
    }

    /**
     * O corte reto de três numa encosta plana desce os lados em degraus de um — piso + 1 a um bloco, piso + 2
     * a dois, e o chão intacto a três —, e não abre o cânion de paredes retas (opção 3 do autor, 2026-10-28).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "corridor_cut")
    public void aThreeDeepCutStepsItsSidesDown(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(4_600_000, 40, 4_600_000);
        List<BlockPos> cells = new java.util.ArrayList<>();

        try {
            for (int x = 0; x <= 10; x++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos column = base.add(x, 0, z);
                    world.getChunk(column);
                    world.setBlockState(column, Blocks.DIRT.getDefaultState());

                    for (int y = 1; y <= 3; y++) {
                        world.setBlockState(column.up(y), y == 3 ? Blocks.GRASS_BLOCK.getDefaultState()
                                : Blocks.DIRT.getDefaultState());
                    }
                }
            }

            for (int x = 2; x <= 8; x++) {
                cells.add(base.add(x, 0, 0));
            }

            CorridorCut.cut(world, UUID.randomUUID(), Corridor.asColonyPositions(cells));

            BlockPos middle = base.add(5, 0, 0);

            context.assertTrue(world.getBlockState(middle.up()).isAir() && world.getBlockState(middle.up(3)).isAir(),
                    "o caminho não foi aberto");

            for (int side : new int[] {-1, 1}) {
                for (int k = 1; k <= 3; k++) {
                    int top = topOf(world, middle.getX(), middle.getZ() + side * k) - middle.getY();

                    context.assertTrue(top == k, "a coluna a " + k + " do caminho devia ficar " + k
                            + " acima do piso, ficou " + top + " (lado " + side + ")");
                }

                BlockPos step = middle.add(0, 1, side);
                context.assertTrue(world.getBlockState(step).isOf(Blocks.GRASS_BLOCK),
                        "o degrau do talude devia ganhar a grama de cima, ficou " + world.getBlockState(step));
            }
        } finally {
            for (BlockPos at : BlockPos.iterate(base.add(-1, -1, -7), base.add(11, 5, 7))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }
}
