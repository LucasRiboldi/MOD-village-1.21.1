package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintAccess;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Circulação prevista não escreve blocos nem concede autorização MAE — ADR-046. */
public final class MaeStructureAccessGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_derived", tickLimit = 200)
    public void derivedGoalCrossesTheDoorAndCirculationLeavesTheFootprint(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        var inside = MinecraftTypeAdapter.toColonyPos(fixture.origin().add(1, 0, 2));
        context.assertTrue(MaeStructureAccess.circulation(fixture.project(), List.of(inside, fixture.surfaces().getFirst()))
                .equals(fixture.surfaces()), "superfície sob a obra virou circulação");
        context.assertTrue(MaeStructureAccess.interiorGoal(fixture.project(), fixture.surfaces())
                .equals(MinecraftTypeAdapter.toColonyPos(fixture.origin().add(1, 1, 2))), "destino não é a célula depois da porta");
        await(context, fixture, MaeStructureAccess.beginFor(context.getWorld(), fixture.project(), List.of(),
                List.of(inside, fixture.surfaces().getFirst()), false), null,
                survey -> context.assertTrue(survey.result().goal().equals(MaeStructureAccess.interiorGoal(fixture.project(), fixture.surfaces())),
                        "prova não usou o destino derivado"));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_no_goal", tickLimit = 20)
    public void aBuildingWithoutWalkableInteriorHasNoDerivedGoal(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        var pieces = new ArrayList<>(fixture.project().blueprint().blocks());
        for (int x = 1; x <= 4; x++) for (int z = 0; z <= 4; z++) for (int y = 1; y <= 2; y++) {
            pieces.add(new BlueprintBlock(new ColonyPos(x, y, z), ResourceId.vanilla("oak_planks")));
        }
        var plan = Blueprint.of(ResourceId.vanilla("mae_solid_fixture"), pieces)
                .withAccesses(fixture.project().blueprint().accesses());
        var project = ConstructionProject.plan(UUID.randomUUID(), plan, fixture.project().origin());
        boolean refused = false;
        try {
            MaeStructureAccess.interiorGoal(project, fixture.surfaces());
        } catch (IllegalArgumentException expected) {
            refused = "no_interior_goal".equals(expected.getMessage());
        }
        context.assertTrue(refused, "obra maciça ganhou destino interno");
        done(context, fixture);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_budget", tickLimit = 20)
    public void extremeCirculationCoordinatesCannotBypassTheReadBudget(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        var y = fixture.project().origin().y();
        var z = fixture.project().origin().z();
        var circulation = List.of(new ColonyPos(Integer.MIN_VALUE + 1, y, z),
                new ColonyPos(Integer.MAX_VALUE - 1, y, z));
        boolean refused = false;
        try {
            MaeStructureAccess.begin(context.getWorld(), fixture.project(), List.of(), circulation, fixture.goal(), false);
        } catch (IllegalArgumentException expected) {
            refused = "access_budget".equals(expected.getMessage());
        }
        context.assertTrue(refused, "coordenadas extremas contornaram o orçamento antes da consulta ao mundo");
        done(context, fixture);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_piece_overlap", tickLimit = 20)
    public void secondaryPartsCannotOverwriteAnotherPlannedPiece(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        var pieces = new ArrayList<>(fixture.project().blueprint().blocks());
        pieces.add(new BlueprintBlock(new ColonyPos(0, 2, 2), ResourceId.vanilla("stone")));
        var plan = Blueprint.of(ResourceId.vanilla("mae_overlap_fixture"), pieces)
                .withAccesses(fixture.project().blueprint().accesses());
        var project = ConstructionProject.plan(UUID.randomUUID(), plan, fixture.project().origin());
        boolean refused = false;
        try {
            MaeStructureAccess.begin(context.getWorld(), project, List.of(), fixture.surfaces(), fixture.goal(), false);
        } catch (IllegalArgumentException expected) {
            refused = "overlapping_structure_effect".equals(expected.getMessage());
        }
        context.assertTrue(refused, "metade de porta sobrepôs outra peça sem recusa");
        done(context, fixture);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_piece_bounds", tickLimit = 20)
    public void aBedHeadOutsideTheBuildingCannotBeAnImplicitEffect(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        var pieces = new ArrayList<>(fixture.project().blueprint().blocks());
        pieces.add(new BlueprintBlock(new ColonyPos(4, 1, 2), ResourceId.vanilla("red_bed"), true,
                Optional.of(Side.EAST), Map.of()));
        var plan = Blueprint.of(ResourceId.vanilla("mae_bed_bounds_fixture"), pieces)
                .withAccesses(fixture.project().blueprint().accesses());
        var project = ConstructionProject.plan(UUID.randomUUID(), plan, fixture.project().origin());
        boolean refused = false;
        try {
            MaeStructureAccess.begin(context.getWorld(), project, List.of(), fixture.surfaces(), fixture.goal(), false);
        } catch (IllegalArgumentException expected) {
            refused = "structure_effect_outside_building".equals(expected.getMessage());
        }
        context.assertTrue(refused, "cabeceira fora da caixa foi planejada implicitamente");
        done(context, fixture);
    }
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_door", tickLimit = 200)
    public void plannedDoorIncludesItsUpperHalfWithoutBuildingTheHouse(TestContext context) {
        check(context, "oak_door", false, null, survey -> {
            var selected = survey.result();
            context.assertTrue(selected.gates().isEmpty(), "porta virou portão");
            context.assertTrue(context.getWorld().getBlockState(selected.opening()).isAir(), "validação construiu porta");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_gate", tickLimit = 200)
    public void aGateRequiresExplicitInteraction(TestContext context) {
        check(context, "oak_fence_gate", false, null, survey ->
                context.assertTrue(survey.result().gates().size() == 1, "interação com portão não foi informada"));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_route_gates", tickLimit = 200)
    public void onlyGatesOnRequiredRoutesNeedInteraction(TestContext context) {
        var fixture = fixture(context, "oak_fence_gate", false);
        var pieces = new ArrayList<>(fixture.project().blueprint().blocks());
        pieces.add(new BlueprintBlock(new ColonyPos(4, 1, 4), ResourceId.vanilla("oak_fence_gate")));
        var plan = Blueprint.of(ResourceId.vanilla("mae_route_gates_fixture"), pieces)
                .withAccesses(fixture.project().blueprint().accesses());
        var project = ConstructionProject.plan(UUID.randomUUID(), plan, fixture.project().origin());
        await(context, fixture, MaeStructureAccess.begin(context.getWorld(), project, List.of(), fixture.surfaces(),
                fixture.goal(), false), null, survey -> context.assertTrue(survey.result().gates().equals(
                java.util.Set.of(fixture.origin().add(0, 1, 2))), "portão fora das rotas úteis exige interação"));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_bed_head", tickLimit = 200)
    public void projectedBedHeadCannotBecomeWalkableAir(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        var pieces = new ArrayList<>(fixture.project().blueprint().blocks());
        pieces.add(new BlueprintBlock(new ColonyPos(2, 1, 1), ResourceId.vanilla("red_bed"), true,
                Optional.of(Side.SOUTH), Map.of()));
        var plan = Blueprint.of(ResourceId.vanilla("mae_bed_head_fixture"), pieces)
                .withAccesses(fixture.project().blueprint().accesses());
        var project = ConstructionProject.plan(UUID.randomUUID(), plan, fixture.project().origin());
        await(context, fixture, MaeStructureAccess.begin(context.getWorld(), project, List.of(), fixture.surfaces(),
                fixture.goal(), false), "no_physical_structure_access", survey -> { });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_iron", tickLimit = 200)
    public void ironDoorIsNotAssumedOpenable(TestContext context) {
        check(context, "iron_door", false, "no_physical_structure_access", survey -> { });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_support", tickLimit = 200)
    public void missingSupportIsRefused(TestContext context) {
        check(context, "oak_door", true, "no_physical_structure_access", survey -> { });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_obstructed", tickLimit = 200)
    public void worldObstructionInvalidatesThePlannedAccess(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        for (int z = 0; z <= 4; z++) {
            context.getWorld().setBlockState(fixture.origin().add(1, 2, z), Blocks.STONE.getDefaultState(), 3);
        }
        await(context, fixture, MaeStructureAccess.begin(context.getWorld(), fixture.project(), List.of(),
                fixture.surfaces(), fixture.goal(), false), "no_physical_structure_access", survey -> { });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_water", tickLimit = 200)
    public void waterIsNotDryCirculation(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        var water = fixture.origin().add(-1, 1, 2);
        // Contém o fluido real; a projeção da porta substitui somente a parede leste na prova.
        for (var direction : net.minecraft.util.math.Direction.Type.HORIZONTAL)
            context.getWorld().setBlockState(water.offset(direction), Blocks.STONE.getDefaultState(), 18);
        context.getWorld().setBlockState(water, Blocks.WATER.getDefaultState(), 18);
        await(context, fixture, survey(context, fixture), "no_physical_structure_access", result -> { });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_budget", tickLimit = 200)
    public void twoSurveysShareTheTerrainReadBudget(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        var first = survey(context, fixture);
        var second = survey(context, fixture);
        int reads = first.runSlice() + second.runSlice();
        context.assertTrue(reads + first.runSlice() + second.runSlice() <= 256, "orçamento duplicado no mesmo tick");
        await(context, fixture, first, null, result -> { });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_slab", tickLimit = 200)
    public void halfSlabUsesItsCollisionHeightAndNormalStep(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        context.getWorld().setBlockState(fixture.origin().add(-1, 0, 2), Blocks.OAK_SLAB.getDefaultState(), 3);
        await(context, fixture, survey(context, fixture), null, result -> result.result());
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_real", tickLimit = 200)
    public void builtHouseUsesActualBlocksAndAcceptsAnExplicitEntry(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        for (var piece : fixture.project().blueprint().blocks()) {
            var at = fixture.origin().add(piece.offset().x(), piece.offset().y(), piece.offset().z());
            var state = com.villagecolony.fabric.integration.PaletteStates.planned(piece);
            context.getWorld().setBlockState(at, state, 18);
            if (state.getBlock() instanceof net.minecraft.block.DoorBlock) {
                context.getWorld().setBlockState(at.up(), state.with(Properties.DOUBLE_BLOCK_HALF,
                        net.minecraft.block.enums.DoubleBlockHalf.UPPER), 18);
            }
        }
        var opposite = new BlueprintAccess(BlueprintAccess.Kind.DOOR, List.of(new ColonyPos(0, 1, 2)), Side.EAST);
        var blueprint = fixture.project().blueprint().withAccesses(List.of(opposite,
                fixture.project().blueprint().accesses().getFirst()));
        var project = ConstructionProject.plan(UUID.randomUUID(), blueprint, fixture.project().origin());
        var actual = MaeStructureAccess.begin(context.getWorld(), project, List.of(), fixture.surfaces(), fixture.goal(), true);
        await(context, fixture, actual, null, result -> {
            var proposal = MaeTerrainPreparation.Proposal.forProject(project, result.result(), fixture.surfaces(), null, List.of(), 0);
            context.assertTrue(proposal.entry().equals(fixture.surfaces().getFirst()), "piso do acesso explícito divergiu");
            context.assertTrue(context.getWorld().getBlockState(fixture.origin().add(0, 1, 2)).isOf(Blocks.OAK_DOOR),
                    "prova real não leu a casa construída");
            boolean wrongProject = false;
            try { MaeTerrainPreparation.Proposal.forProject(fixture.project(), result.result(), fixture.surfaces(), null, List.of(), 0); }
            catch (IllegalArgumentException expected) { wrongProject = "wrong_access_project".equals(expected.getMessage()); }
            context.assertTrue(wrongProject, "acesso aceito para outro projeto");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_access_changed", tickLimit = 20)
    public void changedWorldInvalidatesAnUnfinishedSurvey(TestContext context) {
        var fixture = fixture(context, "oak_door", false);
        var survey = survey(context, fixture);
        context.getWorld().setBlockState(fixture.origin().up(4), Blocks.STONE.getDefaultState(), 3);
        boolean refused = false;
        try { survey.runSlice(); }
        catch (IllegalStateException expected) { refused = "world_changed_during_access_survey".equals(expected.getMessage()); }
        context.assertTrue(refused, "levantamento usou amostra anterior à mudança");
        done(context, fixture);
    }

    private static void check(TestContext context, String block, boolean missingFloor, String refusal,
                              java.util.function.Consumer<MaeStructureAccess.Survey> assertions) {
        var fixture = fixture(context, block, missingFloor);
        await(context, fixture, survey(context, fixture), refusal, assertions);
    }

    private static MaeStructureAccess.Survey survey(TestContext context, Fixture fixture) {
        return MaeStructureAccess.begin(context.getWorld(), fixture.project(), List.of(),
                fixture.surfaces(), fixture.goal(), false);
    }

    private static final java.util.concurrent.atomic.AtomicInteger NEXT_ARENA = new java.util.concurrent.atomic.AtomicInteger();

    /**
     * A prova compara seções inteiras; na bateria completa, testes vizinhos da grade mudavam a mesma seção no
     * meio do levantamento ({@code world_changed_during_access_survey}). Cada fixture usa um chunk próprio,
     * longe da grade, como as arenas MAE — ADR-035 §7.
     */
    private static Fixture fixture(TestContext context, String block, boolean missingFloor) {
        var near = context.getAbsolutePos(new BlockPos(2, 2, 2));
        var origin = new BlockPos(300_005 + NEXT_ARENA.getAndIncrement() * 64, near.getY(), 300_005);
        context.getWorld().setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        for (BlockPos at : BlockPos.iterate(origin.add(-2, -1, -2), origin.add(5, 5, 5))) {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState(), 3);
        }
        var pieces = new ArrayList<BlueprintBlock>();
        for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++) {
            pieces.add(new BlueprintBlock(new ColonyPos(x, 0, z), ResourceId.vanilla("oak_planks")));
        }
        for (int z = 0; z <= 4; z++) for (int y = 1; y <= 3; y++) {
            if (z != 2 || y == 3) pieces.add(new BlueprintBlock(new ColonyPos(0, y, z), ResourceId.vanilla("oak_planks")));
        }
        pieces.add(new BlueprintBlock(new ColonyPos(0, 1, 2), ResourceId.vanilla(block), false,
                Optional.of(Side.EAST), Map.of()));
        var kind = block.endsWith("gate") ? BlueprintAccess.Kind.GATE : BlueprintAccess.Kind.DOOR;
        var blueprint = Blueprint.of(ResourceId.vanilla("mae_access_fixture"), pieces).withAccesses(List.of(
                new BlueprintAccess(kind, List.of(new ColonyPos(0, 1, 2)), Side.WEST)));
        var project = ConstructionProject.plan(UUID.randomUUID(), blueprint, MinecraftTypeAdapter.toColonyPos(origin));
        if (!missingFloor) context.getWorld().setBlockState(origin.add(-1, 0, 2), Blocks.DIRT.getDefaultState(), 3);
        return new Fixture(origin, project, List.of(MinecraftTypeAdapter.toColonyPos(origin.add(-1, 0, 2))),
                MinecraftTypeAdapter.toColonyPos(origin.add(2, 1, 2)));
    }

    private static void await(TestContext context, Fixture fixture, MaeStructureAccess.Survey survey,
                              String refusal, java.util.function.Consumer<MaeStructureAccess.Survey> assertions) {
        survey.runSlice();
        if (!survey.complete()) {
            context.waitAndRun(1, () -> await(context, fixture, survey, refusal, assertions));
            return;
        }
        if (refusal == null) assertions.accept(survey);
        else {
            boolean refused = false;
            try { survey.result(); }
            catch (IllegalStateException expected) { refused = refusal.equals(expected.getMessage()); }
            context.assertTrue(refused, "recusa física divergente: " + refusal);
        }
        done(context, fixture);
    }

    private static void done(TestContext context, Fixture fixture) {
        for (BlockPos at : BlockPos.iterate(fixture.origin().add(-2, -1, -2), fixture.origin().add(5, 5, 5)))
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        context.getWorld().setChunkForced(fixture.origin().getX() >> 4, fixture.origin().getZ() >> 4, false);
        context.complete();
    }

    private record Fixture(BlockPos origin, ConstructionProject project, List<ColonyPos> surfaces, ColonyPos goal) { }
}
