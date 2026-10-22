package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.integration.BuildSiteScanner.Footprint;
import com.villagecolony.fabric.integration.BuildSiteScanner.Site;
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
 * O lote dentro da faixa anexada, com o caminho até a rua — decisão do autor de 2026-10-22 (A5). No
 * deserto a rua da faixa não andou um bloco porque a duna subia dois na entrada, e a faixa plana não
 * deu lote. Os cenários ficam quarenta blocos acima do piso, longe das outras arenas.
 */
public class StripLotGameTest implements FabricGameTest {

    private static final int Y = 40;

    /** A rota segue o terreno e contorna a parede pela passagem; sem passagem, não há rota. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "strip_lot")
    public void thePathGoesAroundTheDuneAndNeverOverIt(TestContext context) {
        ServerWorld world = context.getWorld();

        // Uma linha de caminho em x=0 (z 0..8), uma parede em x=5 (z 0..5) com passagem em z 6..8.
        floor(context, 0, 10, 0, 8);

        for (int z = 0; z <= 8; z++) {
            context.setBlockState(new BlockPos(0, Y, z), Blocks.DIRT_PATH.getDefaultState());
        }

        wall(context, 5, 0, 5);

        BlockPos door = context.getAbsolutePos(new BlockPos(10, Y, 3));
        BlockPos farAway = context.getAbsolutePos(new BlockPos(30, Y, 30));

        Optional<List<ColonyPos>> path = StripLot.pathToTheRoad(
                world, UUID.randomUUID(), door, Direction.WEST, farAway.getX(), farAway.getZ(), new ColonyPos(1, 1, 1));

        context.assertTrue(path.isPresent(), "havia volta pela passagem em z=6 e a rota não a achou");
        context.assertTrue(path.get().size() <= StripLot.MAX_PATH, "rota maior que o teto: " + path.get().size());

        BlockPos wallFrom = context.getAbsolutePos(new BlockPos(5, Y, 0));

        for (ColonyPos cell : path.get()) {
            context.assertFalse(cell.x() == wallFrom.getX() && cell.z() < wallFrom.getZ() + 6,
                    "a rota passou pela parede em " + cell);
            context.assertTrue(cell.y() == door.getY(), "a rota subiu ou desceu num chão plano: " + cell);
        }

        List<BlockPos> cells = path.get().stream().map(cell -> new BlockPos(cell.x(), cell.y(), cell.z())).toList();

        context.assertTrue(CorridorGameTest.turnsOf(cells) <= Corridor.MAX_TURNS,
                "o caminho virou mais de " + Corridor.MAX_TURNS + " vezes: " + cells);
        CorridorGameTest.assertOrthogonal(context, cells);

        // Fecha a passagem: a duna barra tudo, e a rota não corta nem sobe.
        wall(context, 5, 6, 8);

        context.assertTrue(StripLot.pathToTheRoad(
                        world, UUID.randomUUID(), door, Direction.WEST, farAway.getX(), farAway.getZ(),
                        new ColonyPos(1, 1, 1)).isEmpty(),
                "com a duna fechada a rota ainda passou");

        context.complete();
    }

    /**
     * Com a rua da faixa barrada pela duna na entrada, o lote nasce no fundo da faixa, com a porta para a
     * vila e o caminho contornando a duna.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "strip_lot")
    public void theStripGetsADeepLotWithAPathAroundTheDune(TestContext context) {
        ServerWorld world = context.getWorld();

        // Vila em z 0..5 com a rua em z=5; faixa ao sul em z 6..21; duna em z=7, com volta em x 9..10.
        floor(context, 0, 10, 0, 21);

        for (int x = 0; x <= 10; x++) {
            context.setBlockState(new BlockPos(x, Y, 5), Blocks.DIRT_PATH.getDefaultState());
        }

        for (int x = 0; x <= 8; x++) {
            wall(context, x, 7, 7);
        }

        BlockPos low = context.getAbsolutePos(new BlockPos(0, Y - 2, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(10, Y + 8, 5));
        VillageBounds box = new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ());
        ColonyPos size = new ColonyPos(5, 5, 5);

        Optional<Site> site = Optional.empty();

        for (int pass = 0; pass < 20 && site.isEmpty(); pass++) {
            site = StripLot.find(world, UUID.randomUUID(), box, Side.SOUTH, List.of(new Footprint(size, Direction.NORTH)));
        }

        context.assertTrue(site.isPresent(), "a faixa plana não deu lote");
        context.assertTrue(site.get().doorSide() == Direction.NORTH, "a porta não ficou para a vila: " + site.get().doorSide());

        int deepest = context.getAbsolutePos(new BlockPos(0, Y, 16)).getZ();

        context.assertTrue(site.get().origin().z() == deepest,
                "o lote devia nascer no fundo da faixa (z " + deepest + "), e nasceu em " + site.get().origin());
        context.assertFalse(site.get().path().isEmpty(), "sem caminho até a rua");

        int duneZ = context.getAbsolutePos(new BlockPos(0, Y, 7)).getZ();
        int duneEndX = context.getAbsolutePos(new BlockPos(8, Y, 0)).getX();

        for (ColonyPos cell : site.get().path()) {
            context.assertFalse(cell.z() == duneZ && cell.x() <= duneEndX, "o caminho passou pela duna em " + cell);
        }

        StripLot.clearAll();
        context.complete();
    }

    private static void floor(TestContext context, int fromX, int toX, int fromZ, int toZ) {
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                context.setBlockState(new BlockPos(x, Y, z), Blocks.DIRT.getDefaultState());
            }
        }
    }

    /** Uma parede de dois blocos sobre o chão, de z a z. */
    private static void wall(TestContext context, int x, int fromZ, int toZ) {
        for (int z = fromZ; z <= toZ; z++) {
            context.setBlockState(new BlockPos(x, Y + 1, z), Blocks.STONE.getDefaultState());
            context.setBlockState(new BlockPos(x, Y + 2, z), Blocks.STONE.getDefaultState());
        }
    }
}
