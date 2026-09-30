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

    /**
     * Chegar ao destino precisa deixar o construtor dentro do alcance.
     *
     * <p>Playtest de 2026-09-30: a casa do pastor de {@code 1756, 71, -5325}
     * abriu e nenhum bloco foi posto. Dois construtores pararam em grama
     * plana a 5-6 blocos do alvo. O destino era o ponto de pé mais próximo
     * deles na borda do alcance, e a folga de chegada os dava por chegados
     * até dois blocos (Manhattan) antes dele, fora do alcance. Sem andar e
     * sem construir, o guarda devolvia a tarefa em 300 tiques.
     *
     * <p>A geometria é a do save: alvo no piso, construtor seis blocos a
     * oeste e um ao sul. Todo bloco que o Vanilla aceita como chegada, em
     * qualquer canto onde o corpo esteja, tem que alcançar o alvo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_approach")
    public void arrivingAtTheApproachLeavesTheBuilderInReach(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos target = context.getAbsolutePos(TARGET);
        BlockPos worker = target.add(-6, 1, 1);
        ConstructionProject project = ConstructionProject.plan(
                UUID.randomUUID(),
                Blueprint.of(
                        ResourceId.vanilla("village/plains/houses/approach_arrival"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks")))),
                MinecraftTypeAdapter.toColonyPos(target));

        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                BlockPos floor = target.add(dx, 0, dz);

                world.setBlockState(floor, Blocks.GRASS_BLOCK.getDefaultState());
                world.setBlockState(floor.up(), Blocks.AIR.getDefaultState());
                world.setBlockState(floor.up(2), Blocks.AIR.getDefaultState());
            }
        }

        BlockPos approach = BuilderApproach.footOf(world, project, target, worker);
        int arrival = BuilderApproach.ARRIVAL;

        for (int dx = -arrival; dx <= arrival; dx++) {
            for (int dz = -arrival; dz <= arrival; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > arrival) {
                    continue;
                }

                BlockPos arrived = approach.add(dx, 0, dz);

                for (double cx : new double[] {0.0, 0.999}) {
                    for (double cz : new double[] {0.0, 0.999}) {
                        Vec3d body = new Vec3d(
                                arrived.getX() + cx, arrived.getY(), arrived.getZ() + cz);

                        context.assertTrue(
                                BuilderApproach.isWithinReach(body, target),
                                "o construtor chega em " + arrived.toShortString()
                                        + " (destino " + approach.toShortString()
                                        + ", folga " + arrival + ") e fica fora do alcance de "
                                        + target.toShortString());
                    }
                }
            }
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_approach")
    public void aBuildIsNotReservedWithoutAnyStandingSpotInReach(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos target = context.getAbsolutePos(new BlockPos(8, 10, 8));
        ConstructionProject project = ConstructionProject.plan(
                UUID.randomUUID(),
                Blueprint.of(
                        ResourceId.vanilla("village/plains/houses/reservation_spot"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks")))),
                MinecraftTypeAdapter.toColonyPos(target));

        // A arena vazia ainda tem piso abaixo da estrutura. Preenche toda a
        // faixa que a busca pode consultar para a primeira asserção ser física.
        for (int x = 3; x <= 13; x++) {
            for (int z = 3; z <= 13; z++) {
                for (int y = 4; y <= 16; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        context.assertTrue(
                !BuilderApproach.hasStandingSpotWithinReach(world, project, target),
                "a obra sem chão de trabalho passou pela pré-verificação");

        context.setBlockState(new BlockPos(9, 10, 8), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(9, 11, 8), Blocks.AIR.getDefaultState());

        context.assertTrue(
                BuilderApproach.hasStandingSpotWithinReach(world, project, target),
                "um ponto físico ao lado do bloco não foi aceito para a reserva");
        context.complete();
    }
}
