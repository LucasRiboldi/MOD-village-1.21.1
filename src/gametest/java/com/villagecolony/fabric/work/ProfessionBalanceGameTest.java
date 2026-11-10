package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.RemovalAudit;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.integration.TreeHarvester;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** A obra manda no artesão, e o lenhador que se aproxima não larga a árvore — Regra 232. */
public class ProfessionBalanceGameTest implements FabricGameTest {

    private static final ResourceId TEST_BLOCK = ResourceId.vanilla("oak_planks");

    /** Com a obra aberta esperando material, o estoque de enfeite espera; obra andando, ele volta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "balance_stock_yields", tickLimit = 20)
    public void theStockWaitsWhileTheOpenBuildWaitsForMaterial(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(7_000_000, 64, 7_000_000));

        VillageColonyMod.COLONIES.register(colony);

        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(TEST_BLOCK, List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), TEST_BLOCK))),
                new ColonyPos(7_000_000, 64, 7_000_000));

        try {
            project.moveTo(ConstructionState.PREPARING);
            project.moveTo(ConstructionState.BUILDING);
            VillageColonyMod.CONSTRUCTIONS.register(project);

            context.assertFalse(ItemStock.buildWaits(colony.id()), "Regra 232: a obra andando segurou o estoque");

            project.moveTo(ConstructionState.WAITING_RESOURCES);

            context.assertTrue(ItemStock.buildWaits(colony.id()), "Regra 232: a obra parada sem material não segurou o estoque");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.forget(project.id(), RemovalAudit.playerCancellation());
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }

    /** Andar até a árvore longe, chegando mais perto, zera o relógio de travamento. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "balance_lumberjack", tickLimit = 20)
    public void walkingCloserToAFarTreeIsNotAStall(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(7_100_000, 64, 7_100_000));

        VillageColonyMod.COLONIES.register(colony);

        Task task = VillageColonyMod.TASKS.create(colony.id(), TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 1);

        try {
            BlockPos tree = new BlockPos(7_100_100, 64, 7_100_000);
            LumberjackWork.Job job = new LumberjackWork.Job(task, new BlockPos(7_100_000, 64, 7_100_000));

            job.plan = new TreeHarvester.Plan(null, tree, List.of(tree), 1, 0);

            TreeChoice.noCloser(job, new BlockPos(7_100_000, 64, 7_100_000));
            job.stalled = 900;
            TreeChoice.noCloser(job, new BlockPos(7_100_010, 64, 7_100_000));

            context.assertTrue(job.stalled == 0, "Regra 232: chegou mais perto e o relógio de travamento seguiu: " + job.stalled);

            job.stalled = 900;
            TreeChoice.noCloser(job, new BlockPos(7_100_010, 64, 7_100_000));

            context.assertTrue(job.stalled == 900, "Regra 232: parado no mesmo lugar zerou o relógio");
        } finally {
            VillageColonyMod.TASKS.remove(task.id());
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
