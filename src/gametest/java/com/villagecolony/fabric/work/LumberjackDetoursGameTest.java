package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.integration.TreeHarvester;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O lenhador preso tenta um desvio antes de largar a árvore — P7b, 2026-10-11.
 * Ele está fechado numa caixa de terra; a árvore fica do lado de fora.
 */
public class LumberjackDetoursGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_approach", tickLimit = 40)
    public void aBoxedInLumberjackDigsItsWayToTheTree(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }

        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                for (int y = 2; y <= 4; y++) {
                    boolean inside = x == 2 && z == 2 && y <= 3;
                    context.setBlockState(new BlockPos(x, y, z),
                            inside ? Blocks.AIR.getDefaultState() : Blocks.DIRT.getDefaultState());
                }
            }
        }

        BlockPos tree = new BlockPos(6, 2, 6);

        for (int y = 0; y < 4; y++) {
            context.setBlockState(tree.up(y), Blocks.OAK_LOG.getDefaultState());
        }

        context.setBlockState(tree.up(3).north(), Blocks.OAK_LEAVES.getDefaultState());

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        Task task = VillageColonyMod.TASKS.create(
                UUID.randomUUID(), TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 4);

        try {
            LumberjackWork.Job job = new LumberjackWork.Job(task, context.getAbsolutePos(new BlockPos(4, 2, 4)));
            job.plan = TreeHarvester.plan(context.getWorld(), context.getAbsolutePos(tree));

            context.assertTrue(LumberjackDetours.tryDetour(context.getWorld(), villager, job, "test"),
                    "preso na caixa, o lenhador não achou desvio até a árvore");
            context.assertTrue(job.detour != null && job.detour.steps() > 0, "o desvio começou sem passos");

            job.detour = null;
            job.detours = LumberjackDetours.PER_TREE;

            context.assertFalse(LumberjackDetours.tryDetour(context.getWorld(), villager, job, "test"),
                    "passou do limite de desvios por árvore");
        } finally {
            VillageColonyMod.TASKS.remove(task.id());
            villager.discard();

            // A bateria roda num mundo só: a caixa e a árvore não ficam para o vizinho.
            for (int x = 0; x <= 7; x++) {
                for (int z = 0; z <= 7; z++) {
                    for (int y = 1; y <= 6; y++) {
                        context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        context.complete();
    }
}
