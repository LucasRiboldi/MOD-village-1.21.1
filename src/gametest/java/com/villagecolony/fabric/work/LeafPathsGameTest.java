package com.villagecolony.fabric.work;

import com.villagecolony.fabric.brain.WorkTargets;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/** A folhagem natural sai um bloco por passagem; a folha colocada pelo jogador fica. */
public final class LeafPathsGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "leaf_paths", tickLimit = 20)
    public void aWalkingVillagerClearsOneNaturalLeafAtATime(TestContext context) {
        var villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        BlockPos first = new BlockPos(3, 2, 2);
        BlockPos second = first.up();
        context.setBlockState(first, Blocks.OAK_LEAVES);
        context.setBlockState(second, Blocks.OAK_LEAVES);
        WorkTargets.set(villager.getUuid(), context.getAbsolutePos(new BlockPos(6, 2, 2)), 1);
        try {
            context.assertTrue(LeafPaths.clearOne(context.getWorld(), villager), "primeira folha não saiu");
            context.assertTrue(context.getBlockState(first).isAir(), "primeira folha continua");
            context.assertTrue(context.getBlockState(second).isOf(Blocks.OAK_LEAVES), "duas folhas saíram de uma vez");
            context.assertTrue(LeafPaths.clearOne(context.getWorld(), villager), "segunda folha não saiu");
            context.assertTrue(context.getBlockState(second).isAir(), "segunda folha continua");
        } finally {
            WorkTargets.clear(villager.getUuid());
            villager.discard();
        }
        context.complete();
    }

    /** Regra 280: a árvore em cima da coluna do caminho sai inteira, de onde o calçador estiver. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "leaf_paths", tickLimit = 20)
    public void aTreeOnThePathColumnIsFelledFromAfar(TestContext context) {
        BlockPos floor = context.getAbsolutePos(new BlockPos(5, 1, 5));
        context.getWorld().setBlockState(floor, Blocks.GRASS_BLOCK.getDefaultState());

        for (int up = 1; up <= 4; up++) {
            context.getWorld().setBlockState(floor.up(up), Blocks.SPRUCE_LOG.getDefaultState());
        }

        context.getWorld().setBlockState(floor.up(4).east(), Blocks.SPRUCE_LEAVES.getDefaultState());

        int cleared = PathPaving.clearTreeOver(context.getWorld(),
                new com.villagecolony.core.construction.model.PathStep(floor.getX(), floor.getY(), floor.getZ(),
                        com.villagecolony.core.type.Side.values()[0], 0, 0),
                java.util.List.of());

        context.assertTrue(cleared >= 4 && context.getWorld().getBlockState(floor.up()).isAir(),
                "a árvore na coluna do caminho ficou de pé: " + cleared + " blocos saíram");
        context.assertTrue(context.getWorld().getBlockState(floor).isOf(Blocks.GRASS_BLOCK), "o chão da coluna saiu");
        context.complete();
    }

    /** Regra 281: o tronco posto pelo mod sai no log com quem pôs; o que não é tronco, não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "leaf_paths", tickLimit = 20)
    public void everyLogTheModPlacesIsLogged(TestContext context) {
        BlockPos at = context.getAbsolutePos(new BlockPos(2, 2, 2));

        context.assertTrue(com.villagecolony.fabric.integration.PlacedLogs.note(
                at, Blocks.SPRUCE_LOG.getDefaultState(), "test"), "o tronco posto não saiu no log");
        context.assertFalse(com.villagecolony.fabric.integration.PlacedLogs.note(
                at, Blocks.SPRUCE_PLANKS.getDefaultState(), "test"), "a tábua saiu no log de tronco");

        context.getWorld().setBlockState(at, Blocks.OAK_LOG.getDefaultState());
        context.getWorld().setBlockState(at.up(), Blocks.STRIPPED_OAK_LOG.getDefaultState());

        int logs = com.villagecolony.fabric.integration.PlacedLogs.scan(context.getWorld(),
                net.minecraft.util.math.BlockBox.create(at.add(-1, -1, -1), at.add(1, 2, 1)), "test");

        context.assertTrue(logs == 2, "a varredura do molde contou " + logs + " troncos, e não 2");
        context.complete();
    }
}
