package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O encalhado em cima de um telhado desce ao chão — jogo da 0.3.109: o construtor ficou preso no telhado de uma casa,
 * 5 blocos acima do chão, e a fuga o dava por solto sem ele andar.
 */
public class RoofDescentGameTest implements FabricGameTest {

    private static final int GROUND = 1;

    private static final int ROOF = 6;

    /** Chão de grama de 11 × 11 e um telhado de tábuas de 3 × 3 no meio, 5 acima. */
    private static void scene(TestContext context, Block roof) {
        ServerWorld world = context.getWorld();

        for (BlockPos at : BlockPos.iterate(context.getAbsolutePos(new BlockPos(0, GROUND, 0)),
                context.getAbsolutePos(new BlockPos(10, GROUND, 10)))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
        }

        for (BlockPos at : BlockPos.iterate(context.getAbsolutePos(new BlockPos(4, ROOF, 4)),
                context.getAbsolutePos(new BlockPos(6, ROOF, 6)))) {
            world.setBlockState(at, roof.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    private static void clear(TestContext context) {
        for (BlockPos at : BlockPos.iterate(context.getAbsolutePos(new BlockPos(0, GROUND, 0)),
                context.getAbsolutePos(new BlockPos(10, ROOF + 2, 10)))) {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "roof_descent", tickLimit = 80)
    public void aWorkerOnARoofLeapsDownToTheGround(TestContext context) {
        scene(context, Blocks.OAK_PLANKS);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, ROOF + 1, 5));
        int ground = context.getAbsolutePos(new BlockPos(0, GROUND + 1, 0)).getY();

        context.waitAndRun(5, () -> {
            context.assertTrue(RoofDescent.onARoof(context.getWorld(), villager.getBlockPos()), "em cima das tábuas não é telhado");
            context.assertTrue(RoofDescent.check(context.getWorld(), villager), "ele não pulou do telhado");
        });
        context.waitAndRun(45, () -> {
            try {
                context.assertTrue(villager.getBlockPos().getY() == ground,
                        "depois do pulo ele está em " + villager.getBlockPos().toShortString() + ", não no chão");
            } finally {
                villager.discard();
                clear(context);
            }

            context.complete();
        });
    }

    /** No alto de um morro de terra ele está no chão: não desce por esta regra. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "roof_descent", tickLimit = 40)
    public void aHilltopIsNotARoof(TestContext context) {
        scene(context, Blocks.GRASS_BLOCK);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, ROOF + 1, 5));

        context.waitAndRun(5, () -> {
            try {
                context.assertFalse(RoofDescent.onARoof(context.getWorld(), villager.getBlockPos()),
                        "o alto do morro de grama contou como telhado");
                context.assertFalse(RoofDescent.check(context.getWorld(), villager), "desceu do morro");
            } finally {
                villager.discard();
                clear(context);
            }

            context.complete();
        });
    }
}
