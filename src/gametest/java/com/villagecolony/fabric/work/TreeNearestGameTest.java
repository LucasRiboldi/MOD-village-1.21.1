package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.integration.VillageTrees;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * O lenhador corta primeiro a árvore do centro da vila — pedido do autor, 2026-11-01:
 * árvores no miolo obstruem caminhos e portas.
 */
public class TreeNearestGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_grove", tickLimit = 20)
    public void theLumberjackTakesTheCentralTreeBeforeTheEdge(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        // Quarenta acima do piso, fora do alcance das outras arenas.
        BlockPos centre = context.getAbsolutePos(new BlockPos(1, 42, 1));
        BlockPos edge = context.getAbsolutePos(new BlockPos(7, 42, 7));
        VillagerEntity lumberjack = context.spawnEntity(EntityType.VILLAGER, new BlockPos(7, 2, 6));
        lumberjack.refreshPositionAndAngles(edge.getX() + 0.5, edge.getY(), edge.getZ() - 0.5, 0, 0);
        VillageBounds box = new VillageBounds(
                centre.getX() - 7, centre.getY() - 5, centre.getZ() - 7,
                centre.getX() + 7, centre.getY() + 5, centre.getZ() + 7);
        Task task = Task.create(colonyId, TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 8);

        try {
            world.setBlockState(centre, Blocks.OAK_LOG.getDefaultState());
            world.setBlockState(edge, Blocks.OAK_LOG.getDefaultState());
            VillageTrees.rememberTree(colonyId, centre);
            VillageTrees.rememberTree(colonyId, edge);

            Optional<BlockPos> chosen = TreeChoice.byZone(world, new LumberjackWork.Job(task, centre), centre, box,
                    log -> true);

            context.assertTrue(chosen.map(centre::equals).orElse(false),
                    "a árvore do centro devia vir antes da borda: " + chosen);
        } finally {
            world.setBlockState(centre, Blocks.AIR.getDefaultState());
            world.setBlockState(edge, Blocks.AIR.getDefaultState());
            lumberjack.discard();
        }

        context.complete();
    }
}
