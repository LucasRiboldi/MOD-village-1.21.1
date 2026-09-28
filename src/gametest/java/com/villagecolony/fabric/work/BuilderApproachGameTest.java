package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

/** Regressões da posição de aproximação do construtor. */
public final class BuilderApproachGameTest implements FabricGameTest {

    private static final BlockPos TARGET = new BlockPos(8, 4, 8);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_approach")
    public void theBuilderUsesTheReachableSideOfTheLot(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos target = context.getAbsolutePos(TARGET);
        BlockPos worker = target.east(12);
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(target);
        ConstructionProject project = ConstructionProject.plan(
                UUID.randomUUID(),
                Blueprint.of(
                        ResourceId.vanilla("village/plains/houses/approach_side"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks")))),
                origin);

        for (int offset = 0; offset <= 5; offset++) {
            context.setBlockState(TARGET.east(offset).down(), Blocks.STONE.getDefaultState());
        }

        BlockPos approach = BuilderApproach.footOf(world, project, target, worker);

        context.assertTrue(approach.getX() > target.getX(),
                "o construtor recebeu a coluna do bloco em vez do lado do lote mais perto dele");
        context.assertTrue(BuilderApproach.standable(world, approach),
                "o lado escolhido não deixa o construtor de pé");
        context.complete();
    }

    /**
     * O bloco em que o aldeao esta e uma grade, nao sua posicao real.
     *
     * <p>Um construtor em {@code x=13.1, z=7.9} alcanca o centro do bloco
     * {@code x=8, z=8}; arredondar essa posicao para {@code 13,7} inventa
     * 5,1 blocos e o manda caminhar para um ponto que ele ja alcanca. Foi o
     * ciclo de {@code WORK_STALLED} visto no save em 2026-09-27.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_approach")
    public void theBuilderDoesNotWalkWhenItsExactPositionIsAlreadyInReach(TestContext context) {
        BlockPos target = context.getAbsolutePos(TARGET);
        Vec3d worker = new Vec3d(target.getX() + 5.1, target.getY() + 1.0, target.getZ() - 0.1);

        context.assertTrue(
                BuilderApproach.isWithinReach(worker, target),
                "a grade de BlockPos mandou caminhar mesmo dentro do alcance fisico do bloco");
        context.complete();
    }
}
