package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "roof_stale_target", tickLimit = 40)
    public void roofRescueClearsPreviousWalkTarget(TestContext context) {
        scene(context, Blocks.OAK_PLANKS);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, ROOF + 1, 5));
        context.waitAndRun(5, () -> {
            try {
                BlockPos oldTarget = context.getAbsolutePos(new BlockPos(5, ROOF + 3, 5));
                villager.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(oldTarget, 0.5f, 0));
                for (int attempt = 0; attempt <= RoofDescent.LEAPS; attempt++) {
                    context.assertTrue(RoofDescent.check(context.getWorld(), villager), "não resgatou do telhado");
                }
                context.assertFalse(villager.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET),
                        "o destino anterior continua ativo depois do resgate");
            } finally {
                villager.discard();
                clear(context);
                RoofDescent.clearAll();
            }
            context.complete();
        });
    }

    private static final int GROUND = 1;

    private static final int ROOF = 3;

    /** Chão de grama de 11 × 11 e um telhado de 3 × 3 no meio. */
    private static void scene(TestContext context, Block roof) {
        scene(context, roof, ROOF);
    }

    private static void scene(TestContext context, Block roof, int roofY) {
        ServerWorld world = context.getWorld();

        for (BlockPos at : BlockPos.iterate(context.getAbsolutePos(new BlockPos(0, GROUND, 0)),
                context.getAbsolutePos(new BlockPos(10, GROUND, 10)))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
        }

        for (BlockPos at : BlockPos.iterate(context.getAbsolutePos(new BlockPos(4, roofY, 4)),
                context.getAbsolutePos(new BlockPos(6, roofY, 6)))) {
            world.setBlockState(at, roof.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }

    private static void clear(TestContext context) {
        clear(context, ROOF);
    }

    private static void clear(TestContext context, int roofY) {
        for (BlockPos at : BlockPos.iterate(context.getAbsolutePos(new BlockPos(0, GROUND, 0)),
                context.getAbsolutePos(new BlockPos(10, roofY + 2, 10)))) {
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
            villager.setHealth(1.0f);
            context.assertTrue(RoofDescent.check(context.getWorld(), villager), "ele não pulou do telhado");
            context.assertTrue(villager.getVelocity().y > 0.0, "o desnível curto deve permitir um pulo");
        });
        context.waitAndRun(45, () -> {
            try {
                context.assertTrue(villager.isAlive() && villager.getHealth() == 1.0f,
                        "o pulo curto deve preservar a vida do aldeão");
                context.assertTrue(villager.getBlockPos().getY() == ground,
                        "depois do pulo ele está em " + villager.getBlockPos().toShortString() + ", não no chão");
            } finally {
                villager.discard();
                clear(context);
            }

            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "roof_high_drop", tickLimit = 80)
    public void aWorkerOnAHighRoofIsLetDownWithoutFallDamage(TestContext context) {
        int roof = GROUND + 8;
        scene(context, Blocks.OAK_PLANKS, roof);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, roof + 1, 5));
        villager.setAiDisabled(true);
        int ground = context.getAbsolutePos(new BlockPos(0, GROUND + 1, 0)).getY();

        context.waitAndRun(5, () -> {
            context.assertTrue(RoofDescent.onARoof(context.getWorld(), villager.getBlockPos()), "cenário sem telhado");
            villager.setHealth(1.0f);
            context.assertTrue(RoofDescent.check(context.getWorld(), villager), "não resgatou do telhado alto");
            context.assertTrue(villager.getBlockPos().getY() == ground && villager.getVelocity().lengthSquared() == 0.0,
                    "o resgate de oito blocos lançou o aldeão em vez de colocá-lo no chão");
        });
        context.waitAndRun(45, () -> {
            try {
                context.assertTrue(villager.isAlive() && villager.getHealth() == 1.0f,
                        "o resgate de oito blocos deve preservar a vida do aldeão");
                context.assertTrue(villager.getBlockPos().getY() == ground,
                        "o resgate de oito blocos deve deixar o aldeão no chão");
                context.assertTrue(StrandedEscape.isOut(context.getWorld(), villager.getBlockPos()),
                        "o aldeão deve ficar em terreno que permite voltar à escala");
            } finally {
                villager.discard();
                clear(context, roof);
            }
            context.complete();
        });
    }

    /** Regra 362: o composter da roça não é telhado; o fazendeiro em cima dele não pula (jogo da 0.4.141). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "roof_work_block", tickLimit = 40)
    public void aComposterIsNotARoof(TestContext context) {
        scene(context, Blocks.COMPOSTER);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, ROOF + 1, 5));
        villager.setAiDisabled(true);

        context.waitAndRun(5, () -> {
            try {
                context.assertFalse(RoofDescent.onARoof(context.getWorld(), villager.getBlockPos()),
                        "Regra 362: o composter contou como telhado");
            } finally {
                villager.discard();
                clear(context);
                RoofDescent.clearAll();
            }
            context.complete();
        });
    }

    /** Regra 362: três descidas seguidas do mesmo telhado põem o aldeão no chão de vez. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "roof_loop", tickLimit = 40)
    public void theThirdLeapFromTheSameRoofLetsHimDown(TestContext context) {
        scene(context, Blocks.OAK_PLANKS);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, ROOF + 1, 5));
        villager.setAiDisabled(true);
        BlockPos roof = context.getAbsolutePos(new BlockPos(5, ROOF + 1, 5));
        BlockPos grass = context.getAbsolutePos(new BlockPos(1, GROUND + 1, 1));
        int ground = grass.getY();

        context.waitAndRun(5, () -> {
            try {
                for (int round = 1; round <= RoofDescent.LOOPS; round++) {
                    villager.refreshPositionAndAngles(roof.getX() + 0.5, roof.getY(), roof.getZ() + 0.5, 0, 0);
                    context.assertTrue(RoofDescent.check(context.getWorld(), villager), "não resgatou do telhado");

                    if (round < RoofDescent.LOOPS) {
                        context.assertTrue(villager.getBlockPos().getY() > ground, "desceu antes da hora");
                        // Pousou e voltou a subir: no chão, a conta do pulo zera.
                        villager.refreshPositionAndAngles(grass.getX() + 0.5, grass.getY(), grass.getZ() + 0.5, 0, 0);
                        RoofDescent.check(context.getWorld(), villager);
                    }
                }

                context.assertTrue(villager.getBlockPos().getY() == ground,
                        "Regra 362: na terceira descida do mesmo telhado ele ainda pula, em " + villager.getBlockPos().toShortString());
            } finally {
                villager.discard();
                clear(context);
                RoofDescent.clearAll();
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
