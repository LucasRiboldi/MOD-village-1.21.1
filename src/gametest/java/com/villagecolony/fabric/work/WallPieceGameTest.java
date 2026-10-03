package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionOutcome;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * A peça de parede no miolo da casa se apoia na parede que existe — 2026-09-25.
 *
 * <p><b>Visto em jogo.</b> O templo de planície da vila ficou aberto por três
 * sessões com nove peças adiadas "sem apoio": quatro {@code ladder} e cinco
 * {@code wall_torch}, todas com pedregulho encostado. A planta não guarda a
 * direção do bloco, e o {@code BlockShaping.facing} só vira as peças da
 * borda da caixa; no miolo a peça ficava no estado padrão, virada para o
 * norte, procurando apoio ao sul — onde havia ar. O jogo recusava, a peça
 * era adiada, e como a vizinhança nunca mudava ela nunca voltava. A obra não
 * fechava, e a vaga única de obra da colônia ficava presa nela.
 */
public class WallPieceGameTest implements FabricGameTest {

    /** Onde fica a escada, no miolo de uma planta 3 x 1 x 3. */
    private static final BlockPos LADDER = new BlockPos(2, 2, 2);

    private static final ColonyPos INTERIOR = new ColonyPos(1, 0, 1);

    private static BlueprintBlock ladder() {
        return new BlueprintBlock(INTERIOR, ResourceId.vanilla("ladder"));
    }

    /** Uma planta 3 x 1 x 3 com a escada no centro: ela não toca a borda. */
    private static ConstructionProject project(TestContext context) {
        Blueprint blueprint = Blueprint.of(ResourceId.vanilla("village/plains/houses/wall_piece"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("cobblestone")),
                ladder(),
                new BlueprintBlock(new ColonyPos(2, 0, 2), ResourceId.vanilla("cobblestone"))));

        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(LADDER.add(-1, 0, -1)));

        return ConstructionProject.plan(UUID.randomUUID(), blueprint, origin);
    }

    /**
     * A escada adiada por uma versão que não sabia se apoiar volta sem que a
     * vizinhança mude: a parede a oeste já a sustenta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "wall_piece")
    public void aDeferredLadderBesideAWallIsRetriedWithoutANeighbourChanging(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(LADDER.west(), Blocks.COBBLESTONE.getDefaultState());

        ConstructionProject project = project(context);
        BlockPos target = context.getAbsolutePos(LADDER);

        // Como veio do save: adiada, com a leitura de apoio de hoje. Nada em
        // volta vai mudar daqui para a frente.
        project.defer(
                ladder(),
                ConstructionOutcome.skipped(project.worldPositionOf(ladder()), SkipReason.UNSUPPORTED),
                BuilderPlacement.supportFingerprint(world, target));

        BuilderPlacement.reconsiderDeferredPieces(world, project);

        context.assertTrue(
                project.deferredPieces().isEmpty(),
                "a escada com parede a oeste continuou adiada — a obra nunca fecha");

        context.complete();
    }

    /** A peça sem parede nenhuma continua adiada: nada a sustenta de fato. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "wall_piece")
    public void aDeferredLadderWithNoWallStaysDeferred(TestContext context) {
        ServerWorld world = context.getWorld();

        ConstructionProject project = project(context);
        BlockPos target = context.getAbsolutePos(LADDER);

        project.defer(
                ladder(),
                ConstructionOutcome.skipped(project.worldPositionOf(ladder()), SkipReason.UNSUPPORTED),
                BuilderPlacement.supportFingerprint(world, target));

        BuilderPlacement.reconsiderDeferredPieces(world, project);

        context.assertTrue(
                project.deferredPieces().size() == 1,
                "a escada sem parede nenhuma foi devolvida ao construtor");

        context.complete();
    }

    /**
     * A regra que o construtor usa: sem apoio na direção deduzida, a peça vira
     * para a parede que existe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "wall_piece")
    public void aWallPieceTurnsToTheWallThatExists(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(LADDER.west(), Blocks.COBBLESTONE.getDefaultState());

        BlockPos target = context.getAbsolutePos(LADDER);
        var leaned = BlockShaping.leanOnAWall(world, target, Blocks.LADDER.getDefaultState());

        context.assertTrue(leaned.canPlaceAt(world, target),
                "a escada não achou a parede a oeste: " + leaned);

        var torch = BlockShaping.leanOnAWall(world, target, Blocks.WALL_TORCH.getDefaultState());

        context.assertTrue(torch.canPlaceAt(world, target),
                "a tocha de parede não achou a parede a oeste: " + torch);

        context.complete();
    }
}
