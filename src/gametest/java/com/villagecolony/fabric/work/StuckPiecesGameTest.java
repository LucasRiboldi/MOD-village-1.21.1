package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionOutcome;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.PieceRescue;
import com.villagecolony.core.construction.model.SkipReason;
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
 * A obra que só tem peças de lado as traz de volta a cada ciclo e, passadas {@link PieceRescue#TRIES}
 * voltas, a peça surge e a obra fecha — Regra 113 (playtest de 22-10: o armazém do deserto ficou duas
 * horas em "5 blocks left — no build task" e a vila parou).
 */
public class StuckPiecesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "stuck_pieces", tickLimit = 20)
    public void thePieceComesBackFourTimesThenSurgesAndTheBuildCloses(TestContext context) {
        ConstructionProject project = windowAt(context);
        BlueprintBlock piece = project.nextBlock().orElseThrow();
        BlockPos at = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(piece));

        try {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState());

            for (int time = 1; time <= PieceRescue.TRIES; time++) {
                setAside(context, project, piece);
                StuckPieces.rescue(context.getWorld(), project);

                context.assertTrue(project.nextBlock().isPresent(), "na volta " + time + " a peça devia voltar à fila");
                context.assertTrue(context.getWorld().getBlockState(at).isAir(), "na volta " + time + " ela não surge");
            }

            setAside(context, project, piece);
            StuckPieces.rescue(context.getWorld(), project);

            context.assertTrue(context.getWorld().getBlockState(at).isOf(Blocks.GLASS_PANE),
                    "depois de quatro voltas a peça devia surgir: " + context.getWorld().getBlockState(at));
            context.assertTrue(project.state() == ConstructionState.COMPLETED,
                    "com a última peça no lugar a obra devia fechar: " + project.state());
        } finally {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    /** Controle: com outra peça a pôr, a de lado não é contada nem trazida. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "stuck_pieces", tickLimit = 20)
    public void withSomethingElseToPlaceNothingIsRescued(TestContext context) {
        ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(),
                Blueprint.of(ResourceId.vanilla("test_two_windows"), List.of(
                        new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("glass_pane")),
                        new BlueprintBlock(new ColonyPos(1, 0, 0), ResourceId.vanilla("glass_pane")))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2))));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        BlueprintBlock first = project.nextBlock().orElseThrow();

        for (int time = 0; time <= PieceRescue.TRIES; time++) {
            setAside(context, project, first);
            StuckPieces.rescue(context.getWorld(), project);
        }

        context.assertTrue(project.deferredPieces().size() == 1, "a peça de lado devia continuar de lado");
        context.assertTrue(project.state() == ConstructionState.BUILDING, "a obra segue aberta");
        context.complete();
    }

    private static void setAside(TestContext context, ConstructionProject project, BlueprintBlock piece) {
        project.defer(piece, ConstructionOutcome.skipped(project.worldPositionOf(piece), SkipReason.UNSUPPORTED),
                BuilderPlacement.supportFingerprint(context.getWorld(),
                        MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(piece))));
    }

    private static ConstructionProject windowAt(TestContext context) {
        ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(),
                Blueprint.of(ResourceId.vanilla("test_stuck_window"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("glass_pane")))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2))));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        return project;
    }
}
