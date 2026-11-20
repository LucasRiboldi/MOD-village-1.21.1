package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.PlayerGround;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** O chão que o jogador mexeu fica como ele deixou (Regra 334). */
public class PlayerGroundGameTest implements FabricGameTest {

    private static final int SIZE = 10;

    private static void platform(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            world.setBlockState(at.down(), Blocks.DIRT.getDefaultState(), 18);
            world.setBlockState(at.down(2), Blocks.STONE.getDefaultState(), 18);
        }
    }

    private static void clean(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin.down(2), origin.add(SIZE, 4, SIZE))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }
    }

    /** O buraco que o jogador cavou não é tapado, e o calombo que ele pôs não é cortado; o resto da vila é. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "player_ground_level", tickLimit = 20)
    public void theLevellingLeavesThePlayersGroundAlone(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 150, 1));

        platform(world, origin);

        try {
            // O buraco do jogador: ele quebrou a grama.
            BlockPos dug = origin.add(2, 0, 2);
            BlockState grass = world.getBlockState(dug);
            world.setBlockState(dug, Blocks.AIR.getDefaultState(), 18);
            PlayerGround.broke(dug, grass);

            // O calombo do jogador: ele pôs terra em cima do chão.
            BlockPos bump = origin.add(6, 1, 6);
            world.setBlockState(bump, Blocks.DIRT.getDefaultState(), 18);
            PlayerGround.placed(bump, Blocks.AIR.getDefaultState(), Blocks.DIRT.getDefaultState());

            // Um buraco igual, que o jogador não fez.
            BlockPos hole = origin.add(8, 0, 2);
            world.setBlockState(hole, Blocks.AIR.getDefaultState(), 18);

            context.assertFalse(GroundSmoothing.column(world, dug.getX(), dug.getZ(), dug.getY() - 1, dug.getY()),
                    "Regra 334: o nivelamento tapou o buraco do jogador");
            context.assertTrue(world.getBlockState(dug).isAir(), "Regra 334: o buraco do jogador foi tapado");
            context.assertFalse(GroundSmoothing.column(world, bump.getX(), bump.getZ(), bump.getY(), bump.getY() - 1),
                    "Regra 334: o nivelamento cortou o calombo do jogador");
            context.assertTrue(world.getBlockState(bump).isOf(Blocks.DIRT), "Regra 334: o calombo do jogador foi cortado");
            context.assertTrue(GroundSmoothing.column(world, hole.getX(), hole.getZ(), hole.getY() - 1, hole.getY()),
                    "o buraco que o jogador não fez não foi tapado");
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    /** A calçada passa pelo chão do jogador no nível em que ele está, sem aterrar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "player_ground_path", tickLimit = 20)
    public void thePathDoesNotFillThePlayersHole(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 170, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(5, 1, 5));

        try {
            BlockPos dug = origin.add(3, 0, 3);
            BlockState grass = world.getBlockState(dug);
            world.setBlockState(dug, Blocks.AIR.getDefaultState(), 18);
            PlayerGround.broke(dug, grass);

            // O caminho no nível do chão em volta pediria aterrar o buraco.
            boolean laid = PathPaving.pave(world, fixture.colony().id(), dug.getX(), dug.getZ(), dug.getY() - 1,
                    dug.getY(), false);

            context.assertFalse(laid, "Regra 334: a calçada aterrou o buraco do jogador");
            context.assertTrue(world.getBlockState(dug).isAir(), "Regra 334: o buraco do jogador foi aterrado");
            context.assertTrue(PlayerGround.snapshot().containsAll(List.of(((long) dug.getX() << 32) | (dug.getZ() & 0xFFFFFFFFL))),
                    "a marca do jogador não vai ao save");
        } finally {
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }
}
