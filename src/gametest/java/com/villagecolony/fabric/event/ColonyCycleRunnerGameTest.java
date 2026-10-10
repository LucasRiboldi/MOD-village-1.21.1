package com.villagecolony.fabric.event;

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
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Guardas do ciclo real da colônia que dependem de mundo carregado.
 */
public class ColonyCycleRunnerGameTest implements FabricGameTest {

    private static final BlockPos SITE = new BlockPos(4, 2, 4);
    private static final ResourceId TEST_BLOCK = ResourceId.vanilla("village/plains/houses/reservation_gate_test");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "construction_reservation_gate",
            tickLimit = 40)
    public void anOpenBuildTaskCanBeReservedEvenWhenTheFirstPieceHasNoStandingSpot(TestContext context) {
        fillStandingSearchVolume(context);

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE)));
        VillageColonyMod.COLONIES.register(colony);

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                Blueprint.of(TEST_BLOCK, List.of(new BlueprintBlock(
                        new ColonyPos(0, 0, 0),
                        MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS)))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE)));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        VillageColonyMod.CONSTRUCTIONS.register(project);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.BUILD, TaskPriority.CONSTRUCTION, ResourceType.OAK_PLANKS, 1);

        // Sem chão de trabalho ao redor, a trava antiga mantinha a tarefa
        // AVAILABLE with nobody; o BuilderWork é quem deve diagnosticar/adiar
        // a peça física, não a reserva da tarefa.
        context.assertTrue(
                ColonyCycleRunner.canReserveTask(context.getWorld(), colony.id(), task),
                "a obra aberta ficou sem dono antes do construtor avaliar o bloco");

        VillageColonyMod.CONSTRUCTIONS.forget(project.id(), RemovalAudit.playerCancellation());
        VillageColonyMod.TASKS.remove(task.id());
        VillageColonyMod.COLONIES.remove(colony.id());
        context.complete();
    }

    private static void fillStandingSearchVolume(TestContext context) {
        int radius = 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }

                for (int y = SITE.getY(); y <= SITE.getY() + 7; y++) {
                    context.setBlockState(new BlockPos(SITE.getX() + dx, y, SITE.getZ() + dz),
                            Blocks.STONE.getDefaultState());
                }
            }
        }
    }
}
