package com.villagecolony.fabric.work;

import com.villagecolony.fabric.brain.WorkTargets;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/** Regra 197: quem tem destino e parou corta a folha natural à frente, e só ela. */
public class LeafPassageGameTest implements FabricGameTest {

    private static final BlockPos FEET = new BlockPos(3, 30, 3);

    private static VillagerEntity standingStill(TestContext context) {
        context.setBlockState(FEET.down(), Blocks.STONE);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, FEET);

        villager.setAiDisabled(true);

        return villager;
    }

    /** Duas passadas parado, mais a primeira leitura da posição. */
    private static void lookUntilItActs(TestContext context, VillagerEntity villager) {
        for (int pass = 0; pass < 3; pass++) {
            LeafPassage.look(context.getWorld(), villager);
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "leaf_passage", tickLimit = 20)
    public void aNaturalLeafInTheWayIsCut(TestContext context) {
        VillagerEntity villager = standingStill(context);
        BlockPos leaf = FEET.east();

        context.setBlockState(leaf, Blocks.OAK_LEAVES);
        WorkTargets.set(villager.getUuid(), context.getAbsolutePos(FEET.east(8)));

        try {
            lookUntilItActs(context, villager);
            context.assertTrue(context.getBlockState(leaf).isAir(), "a folha à frente não foi cortada");
        } finally {
            WorkTargets.clear(villager.getUuid());
            villager.discard();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "leaf_passage_placed", tickLimit = 20)
    public void aPlacedLeafStays(TestContext context) {
        VillagerEntity villager = standingStill(context);
        BlockPos leaf = FEET.east();

        context.setBlockState(leaf, Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
        WorkTargets.set(villager.getUuid(), context.getAbsolutePos(FEET.east(8)));

        try {
            lookUntilItActs(context, villager);
            context.assertTrue(context.getBlockState(leaf).isOf(Blocks.OAK_LEAVES), "a folha posta à mão foi cortada");
        } finally {
            WorkTargets.clear(villager.getUuid());
            villager.discard();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "leaf_passage_arrived", tickLimit = 20)
    public void withoutADestinationOrAlreadyThereTheLeafStays(TestContext context) {
        VillagerEntity villager = standingStill(context);
        BlockPos leaf = FEET.east();

        context.setBlockState(leaf, Blocks.OAK_LEAVES);

        try {
            lookUntilItActs(context, villager);
            context.assertTrue(context.getBlockState(leaf).isOf(Blocks.OAK_LEAVES), "sem destino, cortou a folha");

            WorkTargets.set(villager.getUuid(), context.getAbsolutePos(FEET.east(2)));
            lookUntilItActs(context, villager);
            context.assertTrue(context.getBlockState(leaf).isOf(Blocks.OAK_LEAVES), "já chegando, cortou a folha");
        } finally {
            WorkTargets.clear(villager.getUuid());
            villager.discard();
        }

        context.complete();
    }
}
