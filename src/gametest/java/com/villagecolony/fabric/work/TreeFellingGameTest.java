package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.TreeHarvester;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** A tarefa que acaba no meio da árvore não deixa a árvore pela metade (autor, 2026-10-12). */
public class TreeFellingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_felling", tickLimit = 20)
    public void aTaskEndingMidTreeFellsTheRestOfTheTrunk(TestContext context) {
        ServerWorld world = context.getWorld();
        // Longe da arena (21-10): na bateria, o viveiro de um teste de lenhador vizinho plantava na
        // borda dele, dentro desta arena, e a árvore deixava de ser uma árvore de quatro troncos.
        BlockPos origin = context.getAbsolutePos(BlockPos.ORIGIN).add(1_000_000, 0, 2_500_000);
        BlockPos tree = origin.add(3, 2, 3);
        world.getChunk(tree);

        world.setBlockState(tree.down(), Blocks.DIRT.getDefaultState());

        for (int y = 0; y < 4; y++) {
            world.setBlockState(tree.up(y), Blocks.OAK_LOG.getDefaultState());
        }

        world.setBlockState(tree.up(3).north(), Blocks.OAK_LEAVES.getDefaultState());

        BlockPos chestAt = origin.add(0, 2, 0);
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(chestAt);
        UUID lumberjack = UUID.randomUUID();
        Task task = VillageColonyMod.TASKS.create(
                UUID.randomUUID(), TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 1);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(lumberjack, chest));

        try {
            LumberjackWork.Job job = new LumberjackWork.Job(task, tree);
            job.plan = TreeHarvester.plan(world, tree);

            context.assertTrue(job.plan.logs() == 4, "o plano não tem os quatro troncos: " + job.plan.logs());

            // Um tronco cortado, e a tarefa acaba.
            TreeHarvester.breakOne(world, job.plan, job.currentBlock());
            job.index = 1;
            TreeFelling.closePlan(world, lumberjack, job);

            for (int y = 0; y < 4; y++) {
                context.assertFalse(world.getBlockState(tree.up(y)).isIn(BlockTags.LOGS),
                        "ficou tronco de pé a " + y + " da base");
            }

            context.assertTrue(ColonyChests.countIn(world, List.of(chest), Items.OAK_LOG) == 3,
                    "os três troncos que faltavam não foram para o baú");
        } finally {
            VillageColonyMod.STORAGES.remove(lumberjack);
            VillageColonyMod.TASKS.remove(task.id());
            ((Inventory) world.getBlockEntity(chestAt)).clear();
            world.setBlockState(chestAt, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    /** Sem árvore madura no alcance depois de cortar, a tarefa fecha CONCLUÍDA — Regra 120. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_felling_emptied", tickLimit = 20)
    public void anEmptiedReachClosesTheTaskAsDone(TestContext context) {
        UUID lumberjack = UUID.randomUUID();
        Task task = VillageColonyMod.TASKS.create(
                UUID.randomUUID(), TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 256);

        try {
            task.reserveFor(lumberjack);

            LumberjackWork.Job job = new LumberjackWork.Job(task, context.getAbsolutePos(BlockPos.ORIGIN));
            job.collected = 12;

            TreeFelling.closeEmptied(job, lumberjack);

            context.assertTrue(task.state() == com.villagecolony.core.task.model.TaskState.COMPLETED,
                    "a tarefa de 256 toras devia fechar concluída com 12, e ficou " + task.state());
        } finally {
            VillageColonyMod.TASKS.remove(task.id());
        }

        context.complete();
    }
}
