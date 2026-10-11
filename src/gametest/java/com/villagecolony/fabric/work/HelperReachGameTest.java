package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** O ajudante de obra só fica com a peça que alcança — regra do autor, 2026-10-11. */
public class HelperReachGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials", tickLimit = 20)
    public void aHelperLeavesAnUnreachablePieceAndTheBuilderTriesAnotherSide(TestContext context) {
        UUID colony = UUID.randomUUID();
        UUID shepherd = UUID.randomUUID();
        UUID builder = UUID.randomUUID();
        BlockPos spot = new BlockPos(1, 64, 1);
        BlockPos target = new BlockPos(2, 64, 1);

        VillageColonyMod.WORKERS.register(shepherd, colony).assign(ProfessionType.SHEPHERD);
        VillageColonyMod.WORKERS.register(builder, colony).assign(ProfessionType.BUILDER);
        Task helped = VillageColonyMod.TASKS.create(colony, TaskType.BUILD, TaskPriority.CONSTRUCTION, ResourceType.OAK_PLANKS, 1);
        Task built = VillageColonyMod.TASKS.create(colony, TaskType.BUILD, TaskPriority.CONSTRUCTION, ResourceType.OAK_PLANKS, 1);

        try {
            helped.reserveFor(shepherd);
            built.reserveFor(builder);

            boolean helperGoesOn = HelperReach.gaveUp(new BuilderWork.Job(helped, UUID.randomUUID()), shepherd, spot, target);
            boolean builderGoesOn = HelperReach.gaveUp(new BuilderWork.Job(built, UUID.randomUUID()), builder, spot, target);

            context.assertTrue(!helperGoesOn && helped.state() == TaskState.AVAILABLE,
                    "o pastor ajudante devia largar a peça que não alcança: " + helped.state());
            context.assertTrue(builderGoesOn && built.isHeld(),
                    "o construtor devia seguir tentando outro lado: " + built.state());
        } finally {
            VillageColonyMod.TASKS.remove(helped.id());
            VillageColonyMod.TASKS.remove(built.id());
            VillageColonyMod.WORKERS.remove(shepherd);
            VillageColonyMod.WORKERS.remove(builder);
        }

        context.complete();
    }
}
