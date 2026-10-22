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
    public void theCorridorCutsStepsDownTheDuneAndOpensAValleyOfThree(TestContext context) {
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

        int moved = CorridorCut.cut(world, UUID.randomUUID(), Corridor.asColonyPositions(cells));

        context.assertTrue(moved > 0, "a abertura não cortou nada");

        for (BlockPos cell : cells) {
            context.assertTrue(world.getBlockState(cell.up()).isAir() && world.getBlockState(cell.up(2)).isAir(),
                    "o caminho ficou sem espaço para andar em " + context.getRelativePos(cell));
        }

        // No pé da duna (z 4 no lote, o caminho seis abaixo do topo) o vale tem os dois lados abertos e para ali.
        BlockPos column = context.getAbsolutePos(new BlockPos(2, Y, 4));
        BlockPos foot = cells.stream()
                .filter(cell -> cell.getX() == column.getX() && cell.getZ() == column.getZ())
                .findFirst()
                .orElseThrow();

        context.assertTrue(foot.getY() < column.getY() + 6, "o caminho não ficou abaixo do topo da duna: " + foot);

        for (Direction side : new Direction[] {north.rotateYClockwise(), north.rotateYCounterclockwise()}) {
            context.assertTrue(world.getBlockState(foot.up().offset(side)).isAir(),
                    "o lado do vale não foi aberto para " + side);
            context.assertFalse(world.getBlockState(foot.up().offset(side, 2)).isAir(),
                    "o vale passou de três de largura para " + side);
        }

        clear(context);
        context.complete();
    }
}
