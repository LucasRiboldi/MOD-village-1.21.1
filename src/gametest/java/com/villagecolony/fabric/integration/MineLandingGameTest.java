package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.content.ModContent;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.UUID;

/** A tocha do patamar das dobras da mina fica na parede, presa na rocha de fora — Regra 254. */
public class MineLandingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_landing", tickLimit = 5)
    public void theLandingTorchHangsOnTheRockBehindIt(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos open = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos noWall = context.getAbsolutePos(new BlockPos(5, 2, 2));
        BlockPos taken = context.getAbsolutePos(new BlockPos(8, 2, 2));

        world.setBlockState(open.south(), Blocks.STONE.getDefaultState());
        world.setBlockState(taken.south(), Blocks.STONE.getDefaultState());
        world.setBlockState(taken, Blocks.COBBLESTONE.getDefaultState());

        context.assertTrue(MineLighting.placeWallTorch(world, open, Direction.SOUTH), "a tocha não foi posta");
        context.assertTrue(world.getBlockState(open).isOf(ModContent.COLONY_WALL_TORCH), "não é tocha da colônia");
        context.assertTrue(world.getBlockState(open).get(WallTorchBlock.FACING) == Direction.NORTH,
                "a tocha não aponta para dentro do patamar");
        context.assertFalse(MineLighting.placeWallTorch(world, noWall, Direction.SOUTH), "tocha sem rocha atrás");
        context.assertFalse(MineLighting.placeWallTorch(world, taken, Direction.SOUTH), "tocha no lugar de um bloco");
        context.complete();
    }

    /** Regra 353: acende a descida de seis em seis degraus e barra monstros perto da tocha. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_stair_colony_torches", tickLimit = 20)
    public void theStairGetsColonyTorchesEverySixSteps(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos entry = context.getAbsolutePos(BlockPos.ORIGIN).add(4_300_000, 45, 4_300_000);
        MineShaft shaft = MineShaft.from(MinecraftTypeAdapter.toColonyPos(entry), Side.EAST);
        Mine mine = Mine.restore(UUID.randomUUID(), shaft, 0);
        BlockPos sixth = entry.add(5, -4, 3);
        BlockPos twelfth = entry.add(-2, -10, 0);
        BlockPos outside = entry.add(7, -4, 3);
        BlockPos ringSpot = sixth.south(10);

        try {
            for (BlockPos support : List.of(sixth.east(), twelfth.west(), outside.east(), ringSpot.down())) {
                world.getChunk(support);
                world.setBlockState(support, Blocks.STONE.getDefaultState());
            }
            for (BlockPos spot : List.of(sixth, twelfth, outside, ringSpot)) {
                world.getChunk(spot);
                world.setBlockState(spot, Blocks.AIR.getDefaultState());
            }

            ColonyTorches.clearAll();
            context.assertFalse(ColonyTorches.suppresses(world, SpawnReason.NATURAL, sixth), "a mina já está protegida");
            context.assertTrue(MineLighting.light(world, mine, mine.arm(0)).filter(sixth::equals).isPresent(),
                    "falta luz no sexto degrau");
            context.assertTrue(MineLighting.light(world, mine, mine.arm(0)).filter(twelfth::equals).isPresent(),
                    "falta luz no décimo segundo degrau");
            for (BlockPos spot : List.of(sixth, twelfth)) {
                context.assertTrue(world.getBlockState(spot).isOf(ModContent.COLONY_WALL_TORCH),
                        "esperava tocha de parede da colônia em " + spot);
            }
            context.assertTrue(world.getBlockState(ringSpot).isOf(Blocks.LIGHT), "a tocha da escada não acendeu o anel");
            context.assertTrue(ColonyTorches.suppresses(world, SpawnReason.NATURAL, sixth), "a tocha não barrou monstro");
            context.assertTrue(world.getBlockState(outside).isAir(), "tocha fora do traçado da escada");
        } finally {
            for (BlockPos spot : List.of(sixth, twelfth, outside, ringSpot, sixth.east(), twelfth.west(),
                    outside.east(), ringSpot.down())) {
                world.setBlockState(spot, Blocks.AIR.getDefaultState());
            }
            ColonyTorches.clearAll();
        }

        context.complete();
    }

    /** O arco novo surge de tora de pinheiro descascada, com o baú à direita de quem entra, e é reconhecido pela boca. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_landing", tickLimit = 5)
    public void theArchStandsAndIsKnownByTheMouth(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos mouth = context.getAbsolutePos(new BlockPos(3, 2, 3));

        for (BlockPos at : BlockPos.iterate(mouth.add(-3, -1, -3), mouth.add(4, -1, 3))) {
            world.setBlockState(at, Blocks.STONE.getDefaultState());
        }

        context.assertTrue(MineGate.standingAtMouth(world, mouth, Direction.SOUTH).isEmpty(), "arco reconhecido sem arco");
        MineGate.raise(world, mouth, Direction.SOUTH);

        // Para o sul, as três pistas vão da boca para o leste e a direita de quem entra é o oeste.
        for (int lane = 0; lane < 3; lane++) {
            context.assertTrue(world.getBlockState(mouth.east(lane)).isAir(), "o vão está fechado na pista " + lane);
        }
        context.assertTrue(BiomeBlueprints.sameRole(world.getBlockState(mouth.west(1)), Blocks.STRIPPED_SPRUCE_LOG),
                "falta o pilar da direita");
        context.assertTrue(BiomeBlueprints.sameRole(world.getBlockState(mouth.east(3)), Blocks.STRIPPED_SPRUCE_LOG),
                "falta o pilar da esquerda");
        context.assertTrue(world.getBlockState(mouth.west(3)).isOf(Blocks.CHEST), "o baú não está à direita, no chão");
        context.assertTrue(world.getBlockState(mouth.west(2).up(3)).isOf(Blocks.LANTERN), "falta o lampião da direita");
        context.assertTrue(world.getBlockState(mouth.east(4).up(3)).isOf(Blocks.LANTERN), "falta o lampião da esquerda");
        context.assertTrue(MineGate.standingAtMouth(world, mouth, Direction.SOUTH).isPresent(), "o arco não foi reconhecido");

        world.setBlockState(mouth.west(2).up(4), Blocks.AIR.getDefaultState());
        context.assertTrue(MineGate.standingAtMouth(world, mouth, Direction.SOUTH).isPresent(),
                "uma peça tirada apagou o arco");

        for (BlockPos at : BlockPos.iterate(mouth.add(-3, -1, -3), mouth.add(4, 5, 3))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    /** Tronco sem folha natural perto do alto não é árvore: o lenhador não o leva (Regra 256). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_landing", tickLimit = 5)
    public void aLogWithoutNaturalLeavesIsNotATree(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos bare = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos tree = context.getAbsolutePos(new BlockPos(8, 2, 2));
        BlockPos placed = context.getAbsolutePos(new BlockPos(14, 2, 2));

        for (BlockPos base : java.util.List.of(bare, tree, placed)) {
            world.setBlockState(base, Blocks.OAK_LOG.getDefaultState());
            world.setBlockState(base.up(), Blocks.OAK_LOG.getDefaultState());
        }

        world.setBlockState(tree.up(2), Blocks.OAK_LEAVES.getDefaultState());
        world.setBlockState(placed.up(2), Blocks.OAK_LEAVES.getDefaultState()
                .with(net.minecraft.block.LeavesBlock.PERSISTENT, true));

        context.assertFalse(TreeScanner.hasNaturalLeaves(world, bare), "tronco solto contou como árvore");
        context.assertTrue(TreeScanner.hasNaturalLeaves(world, tree), "a árvore com folha não contou");
        context.assertFalse(TreeScanner.hasNaturalLeaves(world, placed), "folha posta por alguém contou como árvore");
        context.complete();
    }
}
