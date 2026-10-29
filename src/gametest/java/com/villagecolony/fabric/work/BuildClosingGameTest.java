package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * A obra nunca fica largada meio pronta — Regra 148: sem peça a pôr ela fecha pelo ciclo da colônia, e
 * parada no fim o que falta surge no lugar.
 */
public class BuildClosingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_closing", tickLimit = 20)
    public void aStalledHalfBuiltHouseSurgesWhatIsMissingAndCloses(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(BlockPos.ORIGIN)));
        ConstructionProject project = building(context, "test_end_of_build", List.of("stone", "glass", "spruce_planks"));
        BlueprintBlock first = project.nextBlock().orElseThrow();

        // A primeira peça foi posta pelo construtor; as outras esperam material.
        context.getWorld().setBlockState(at(project, first), Blocks.STONE.getDefaultState());
        project.markPlaced(first);
        project.remaining().forEach(project::deferForMaterial);

        BuildClosing.finishBySurging(context.getWorld(), colony, project);

        context.assertTrue(project.state() == ConstructionState.COMPLETED, "a obra devia fechar: " + project.state());
        context.assertTrue(context.getBlockState(new BlockPos(3, 2, 2)).isOf(Blocks.GLASS),
                "o vidro que faltava devia surgir no lugar");
        context.assertTrue(context.getBlockState(new BlockPos(4, 2, 2)).isOf(Blocks.SPRUCE_PLANKS),
                "a tábua que faltava devia surgir no lugar");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_closing", tickLimit = 20)
    public void aBuildWithNothingLeftClosesWithoutABuilder(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(BlockPos.ORIGIN)));
        ConstructionProject project = building(context, "test_nothing_left", List.of("stone"));
        BlueprintBlock only = project.nextBlock().orElseThrow();

        context.getWorld().setBlockState(at(project, only), Blocks.STONE.getDefaultState());
        project.markPlaced(only);

        context.assertTrue(BuildClosing.closeIfDone(context.getWorld(), colony, project),
                "a obra sem peça a pôr devia fechar");
        context.assertTrue(project.state() == ConstructionState.COMPLETED, "a obra devia estar pronta");
        context.assertTrue(!BuildClosing.closeIfDone(context.getWorld(), colony, project), "fecha uma vez só");
        context.complete();
    }

    private static ConstructionProject building(TestContext context, String id, List<String> blocks) {
        List<BlueprintBlock> pieces = new java.util.ArrayList<>();

        for (int index = 0; index < blocks.size(); index++) {
            pieces.add(new BlueprintBlock(new ColonyPos(index, 0, 0), ResourceId.vanilla(blocks.get(index))));
        }

        ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(),
                Blueprint.of(ResourceId.vanilla(id), pieces),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2))));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        return project;
    }

    private static BlockPos at(ConstructionProject project, BlueprintBlock block) {
        return MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(block));
    }
}
