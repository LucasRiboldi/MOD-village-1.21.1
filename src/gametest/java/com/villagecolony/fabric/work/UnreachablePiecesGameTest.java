package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionOutcome;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
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
 * A última peça que o construtor não alcança surge no lugar depois de {@link UnreachablePieces#TRIES}
 * vezes de lado — autor, 2026-10-20: na taiga a casa ficou 44 ciclos em "1 blocks left".
 */
public class UnreachablePiecesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "unreachable_pieces", tickLimit = 20)
    public void theLastPieceSurgesAfterTheTries(TestContext context) {
        ConstructionProject project = windowAt(context);
        BlueprintBlock piece = project.nextBlock().orElseThrow();
        BlockPos at = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(piece));

        try {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState());
            setAside(context, project, piece, UnreachablePieces.TRIES);

            BuilderPlacement.reconsiderDeferredPieces(context.getWorld(), project);

            context.assertTrue(context.getWorld().getBlockState(at).isOf(Blocks.GLASS_PANE),
                    "a peça inalcançável devia ter surgido no lugar: " + context.getWorld().getBlockState(at));
            context.assertTrue(project.isFinished(), "com a última peça no lugar a obra devia ter terminado");
        } finally {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    /** Controle: com menos tentativas ela só volta à fila, sem surgir. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "unreachable_pieces", tickLimit = 20)
    public void beforeTheTriesThePieceOnlyComesBack(TestContext context) {
        ConstructionProject project = windowAt(context);
        BlueprintBlock piece = project.nextBlock().orElseThrow();
        BlockPos at = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(piece));

        try {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState());
            setAside(context, project, piece, UnreachablePieces.TRIES - 1);

            BuilderPlacement.reconsiderDeferredPieces(context.getWorld(), project);

            context.assertTrue(context.getWorld().getBlockState(at).isAir(),
                    "antes das tentativas a peça não devia surgir: " + context.getWorld().getBlockState(at));
            context.assertTrue(project.nextBlock().isPresent(), "a peça devia ter voltado à fila");
        } finally {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    /** O bloco que já está no lugar não é sobrescrito (Regra 3): a peça só é riscada. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "unreachable_pieces", tickLimit = 20)
    public void aSolidBlockInThePlaceIsKept(TestContext context) {
        ConstructionProject project = windowAt(context);
        BlueprintBlock piece = project.nextBlock().orElseThrow();
        BlockPos at = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(piece));

        try {
            context.getWorld().setBlockState(at, Blocks.STONE.getDefaultState());
            setAside(context, project, piece, UnreachablePieces.TRIES);

            BuilderPlacement.reconsiderDeferredPieces(context.getWorld(), project);

            context.assertTrue(context.getWorld().getBlockState(at).isOf(Blocks.STONE),
                    "a pedra no lugar da peça foi sobrescrita");
            context.assertTrue(project.isFinished(), "a peça riscada devia ter fechado a obra");
        } finally {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }

    private static void setAside(TestContext context, ConstructionProject project, BlueprintBlock piece, int times) {
        for (int time = 0; time < times; time++) {
            project.defer(piece, ConstructionOutcome.skipped(project.worldPositionOf(piece), SkipReason.UNREACHABLE),
                    BuilderPlacement.supportFingerprint(context.getWorld(),
                            MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(piece))));
        }
    }

    private static ConstructionProject windowAt(TestContext context) {
        ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(),
                Blueprint.of(ResourceId.vanilla("test_window"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("glass_pane")))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2))));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        return project;
    }
}
