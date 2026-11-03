package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.MaeWorkStore;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import net.minecraft.util.math.BlockPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;

/** Reservas explícitas de terreno não autorizam escrita nem flexibilizam o contrato antigo — MAE Fase 2. */
public final class MaeTerrainGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_terrain_flowers", tickLimit = 20)
    public void flowersRequireExplicitPolicyAndReservationNeverWrites(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            fixture.set(fixture.base.down(), Blocks.DIRT.getDefaultState());
            fixture.set(fixture.base, Blocks.DANDELION.getDefaultState());
            fixture.capture();
            refused(context, fixture::reserve, "protected_volume");
            UUID id = reserve(fixture);
            context.assertTrue(fixture.store.region(id).orElseThrow().getString("policy").equals("terrain_v2"),
                    "reserva perdeu política explícita");
            context.assertTrue(fixture.writer.runSlice(context.getWorld(), id) == 0, "reserva executou obra");
            context.assertTrue(context.getWorld().getBlockState(fixture.base).isOf(Blocks.DANDELION), "reserva removeu flor");
            refused(context, () -> fixture.writer.approve(context.getWorld(), id, fixture.lot(),
                    List.of(fixture.change(fixture.base))), "terrain_region_requires_typed_approval");
            NbtCompound root = new NbtCompound();
            fixture.store.write(root);
            context.assertTrue(root.getCompound(MaeWorkStore.KEY).getInt("version") == 2, "reserva não promoveu envelope");
            var restored = new MaeWorkStore(() -> { });
            restored.read(root);
            context.assertTrue(restored.region(id).equals(fixture.store.region(id)), "recarga perdeu política");
        } finally {
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_terrain_margin", tickLimit = 20)
    public void flowerInInspectionMarginIsPreserved(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            var flower = fixture.base.add(-2, 0, 0);
            fixture.set(flower.down(), Blocks.DIRT.getDefaultState());
            fixture.set(flower, Blocks.POPPY.getDefaultState());
            fixture.capture();
            UUID id = reserve(fixture);
            context.assertTrue(fixture.writer.runSlice(context.getWorld(), id) == 0, "reserva escreveu margem");
            context.assertTrue(context.getWorld().getBlockState(flower).isOf(Blocks.POPPY), "flor da margem removida");
        } finally {
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_terrain_exclusions", tickLimit = 20)
    public void plantsOutsideClosedListTreesWaterAndCropsRemainBlocked(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            for (var block : List.of(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.OAK_SAPLING,
                    Blocks.WATER, Blocks.WHEAT, Blocks.FARMLAND, Blocks.TALL_GRASS, Blocks.WITHER_ROSE)) {
                fixture.set(fixture.base, block.getDefaultState());
                fixture.capture();
                refused(context, () -> reserve(fixture), "protected_volume");
                context.assertTrue(fixture.store.regions().isEmpty(), "recusa persistiu reserva");
                context.assertTrue(context.getWorld().getBlockState(fixture.base).isOf(block), "recusa escreveu bloco");
                NbtCompound root = new NbtCompound();
                fixture.store.write(root);
                context.assertFalse(root.contains(MaeWorkStore.KEY), "recusa promoveu envelope");
            }
        } finally {
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_terrain_evidence", tickLimit = 20)
    public void changedOrMissingEvidenceDoesNotPromoteEnvelope(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            fixture.chunk.removeAttached(MaeGenerationEvidence.RECEIPT);
            refused(context, () -> reserve(fixture), "missing_generation_evidence");
            fixture.set(fixture.base, Blocks.AIR.getDefaultState());
            fixture.capture();
            fixture.set(fixture.base, Blocks.DIRT.getDefaultState());
            refused(context, () -> reserve(fixture), "changed_generation_evidence");
            context.assertTrue(fixture.store.regions().isEmpty(), "evidência divergente criou reserva");
        } finally {
            fixture.cleanup();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_terrain_protection", tickLimit = 20)
    public void explicitVegetationPolicyNeverOverridesBuildingProtection(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            fixture.set(fixture.base.down(), Blocks.DIRT.getDefaultState());
            fixture.set(fixture.base, Blocks.DANDELION.getDefaultState());
            com.villagecolony.VillageColonyMod.BUILDINGS.register(fixture.lot());
            fixture.capture();
            refused(context, () -> reserve(fixture), "protected_volume");
            context.assertTrue(context.getWorld().getBlockState(fixture.base).isOf(Blocks.DANDELION), "proteção foi removida");
        } finally {
            fixture.cleanup();
        }
        context.complete();
    }

    private static UUID reserve(MaeSafetyGameTest.Fixture fixture) {
        var min = fixture.base.add(-3, -3, -3);
        var max = fixture.base.add(7, 3, 3);
        return fixture.writer.reserveTerrain(fixture.context.getWorld(), fixture.colony.colony().id(),
                new WorkArea(new ColonyPos(min.getX(), min.getY(), min.getZ()),
                        new ColonyPos(max.getX(), max.getY(), max.getZ())));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_terrain_survey", tickLimit = 160)
    public void flatSurveyIsIncrementalAndDoesNotApproveEmptyWork(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            prepareGround(fixture);
            fixture.capture();
            UUID id = reserveSurvey(fixture);
            var proposal = proposal(fixture);
            var survey = MaeTerrainPreparation.begin(context.getWorld(), fixture.writer, id,
                    List.of(proposal), fixture.colony.colony().center());
            finishSurvey(context, fixture, id, survey);
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    private static void finishSurvey(TestContext context, MaeSafetyGameTest.Fixture fixture, UUID id,
                                     MaeTerrainPreparation.Survey survey) {
        try {
            context.assertTrue(survey.runSlice() <= 256, "fatia ultrapassou 256 leituras");
            if (!survey.complete()) {
                context.waitAndRun(1, () -> finishSurvey(context, fixture, id, survey));
                return;
            }
            var prepared = survey.result();
            context.assertTrue(prepared.terrain().feasible(), "terreno plano recusado");
            context.assertTrue(prepared.changes().isEmpty(), "terreno plano ganhou mudanças");
            context.assertTrue(prepared.lot().equals(survey.proposals().getFirst().lot()), "levantamento falsificou Building");
            context.assertFalse(fixture.store.region(id).orElseThrow().contains("job"), "levantamento aprovou obra implicitamente");
            context.assertTrue(prepared.terrain().reason().equals("external_access_unverified"), "levantamento inventou acesso");
            fixture.cleanup();
            context.complete();
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    private static void prepareGround(MaeSafetyGameTest.Fixture fixture) {
        for (BlockPos at : BlockPos.iterate(fixture.base.add(-2, -4, -2), fixture.base.add(5, 0, 2))) {
            fixture.set(at, Blocks.DIRT.getDefaultState());
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_terrain_candidates", tickLimit = 160)
    public void rejectedCandidateDoesNotPreventEvaluatingNextCandidate(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            prepareGround(fixture);
            fixture.capture();
            UUID id = reserveSurvey(fixture);
            var valid = proposal(fixture);
            var disconnected = new MaeTerrainPreparation.Proposal(valid.lot(),
                    List.of(valid.surfaces().get(0), valid.surfaces().get(2), valid.surfaces().get(3)),
                    valid.entry(), null, 0);
            var survey = MaeTerrainPreparation.begin(context.getWorld(), fixture.writer, id,
                    List.of(disconnected, valid), fixture.colony.colony().center());
            awaitSurvey(context, fixture, survey, () -> {
                context.assertTrue(survey.result().terrain().feasible(), "candidato seguinte não foi avaliado");
                context.assertTrue(survey.result().terrain().candidate().columns().size() == 4, "selecionou superfície incompleta");
            });
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_terrain_cut", tickLimit = 160)
    public void surveyPlansFlowersBeforeCutsWithoutChangingWorld(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            prepareGround(fixture);
            for (int x = 0; x < 4; x++) {
                fixture.set(fixture.base.add(x, 1, 0), Blocks.DIRT.getDefaultState());
                fixture.set(fixture.base.add(x, 2, 0), Blocks.DANDELION.getDefaultState());
            }
            fixture.capture();
            UUID id = reserveSurvey(fixture);
            var survey = MaeTerrainPreparation.begin(context.getWorld(), fixture.writer, id,
                    List.of(proposal(fixture)), fixture.colony.colony().center());
            awaitSurvey(context, fixture, survey, () -> {
                var prepared = survey.result();
                context.assertTrue(prepared.changes().size() == 8, "corte perdeu plantas ou blocos");
                context.assertTrue(prepared.terrain().plants() == 4 && prepared.terrain().earthwork() == 4,
                        "métricas divergentes do plano físico");
                context.assertTrue(prepared.changes().getFirst().before().isOf(Blocks.DANDELION), "corte precedeu planta");
                context.assertTrue(context.getWorld().getBlockState(fixture.base.up(2)).isOf(Blocks.DANDELION),
                        "levantamento removeu planta");
                context.assertFalse(fixture.store.region(id).orElseThrow().contains("job"), "levantamento aprovou corte");
            });
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_terrain_shared_budget", tickLimit = 20)
    public void surveyBudgetIsSharedByWorldAndExternalEditBlocksContinuation(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            prepareGround(fixture);
            fixture.capture();
            UUID id = reserveSurvey(fixture);
            var first = MaeTerrainPreparation.begin(context.getWorld(), fixture.writer, id,
                    List.of(proposal(fixture)), fixture.colony.colony().center());
            var second = MaeTerrainPreparation.begin(context.getWorld(), fixture.writer, id,
                    List.of(proposal(fixture)), fixture.colony.colony().center());
            int read = first.runSlice() + second.runSlice();
            context.assertTrue(read > 0 && read <= 256, "orçamento multiplicado por solicitante");
            context.assertFalse(first.complete(), "fixture não exige continuação");
            var edited = fixture.base.add(5, 2, 2);
            fixture.set(edited, Blocks.STONE.getDefaultState());
            context.waitAndRun(1, () -> {
                try {
                    refused(context, first::runSlice, "world_changed_during_survey");
                    context.assertTrue(context.getWorld().getBlockState(edited).isOf(Blocks.STONE), "levantamento sobrescreveu edição");
                    context.assertFalse(fixture.store.region(id).orElseThrow().contains("job"), "edição gerou aprovação");
                } finally {
                    fixture.cleanup();
                }
                context.complete();
            });
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    private static void awaitSurvey(TestContext context, MaeSafetyGameTest.Fixture fixture,
                                    MaeTerrainPreparation.Survey survey, Runnable assertions) {
        try {
            context.assertTrue(survey.runSlice() <= 256, "fatia ultrapassou limite");
            if (!survey.complete()) {
                context.waitAndRun(1, () -> awaitSurvey(context, fixture, survey, assertions));
                return;
            }
            assertions.run();
            fixture.cleanup();
            context.complete();
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_terrain_execute", tickLimit = 240)
    public void typedCutResumesFromDiskAndNeverRepeatsMaterialDelivery(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        context.waitAndRun(20, () -> startCut(context, fixture));
    }

    private static void startCut(TestContext context, MaeSafetyGameTest.Fixture fixture) {
        try {
            prepareGround(fixture);
            for (int x = 0; x < 4; x++) {
                fixture.set(fixture.base.add(x, 1, 0), Blocks.DIRT.getDefaultState());
                fixture.set(fixture.base.add(x, 2, 0), Blocks.DANDELION.getDefaultState());
            }
            fixture.capture();
            UUID id = reserveSurvey(fixture);
            var survey = MaeTerrainPreparation.begin(context.getWorld(), fixture.writer, id,
                    List.of(proposal(fixture)), fixture.colony.colony().center());
            executeCut(context, fixture, id, survey, false);
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    private static void executeCut(TestContext context, MaeSafetyGameTest.Fixture fixture, UUID id,
                                   MaeTerrainPreparation.Survey survey, boolean approved) {
        try {
            if (!survey.complete()) survey.runSlice();
            if (survey.complete() && !approved) {
                context.assertTrue(fixture.writer.approveTerrain(context.getWorld(), survey.result()), "corte não aprovado");
                NbtCompound disk = new NbtCompound();
                fixture.store.write(disk);
                fixture.store = new MaeWorkStore(() -> { });
                fixture.store.read(disk);
                fixture.writer = new MaeWorldWriter(fixture.store);
                approved = true;
            }
            if (approved) fixture.writer.runSlice(context.getWorld(), id);
            var job = fixture.store.region(id).orElseThrow().getCompound("job");
            if (!"complete".equals(job.getString("state"))) {
                context.assertFalse("blocked".equals(job.getString("state")), "corte bloqueado: " + job.getString("reason"));
                boolean nextApproved = approved;
                context.waitAndRun(1, () -> executeCut(context, fixture, id, survey, nextApproved));
                return;
            }
            var history = fixture.store.region(id).orElseThrow().getList("history", 10);
            context.assertTrue(history.size() == 8 && job.getInt("cursor") == 8, "corte repetido ou incompleto");
            for (int i = 0; i < history.size(); i++) {
                context.assertTrue("confirmed".equals(history.getCompound(i).getString("materials")), "material sem confirmação");
            }
            var drops = context.getWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class,
                    new net.minecraft.util.math.Box(fixture.base).expand(6), e -> true);
            int dirt = drops.stream().filter(e -> e.getStack().isOf(net.minecraft.item.Items.DIRT))
                    .mapToInt(e -> e.getStack().getCount()).sum();
            int flowers = drops.stream().filter(e -> e.getStack().isOf(net.minecraft.item.Items.DANDELION))
                    .mapToInt(e -> e.getStack().getCount()).sum();
            context.assertTrue(dirt == 4 && flowers == 4, "corte não conservou itens: terra=" + dirt + ", flores=" + flowers);
            context.assertTrue(fixture.writer.runSlice(context.getWorld(), id) == 0, "obra concluída repetiu efeitos");
            drops.forEach(net.minecraft.entity.Entity::discard);
            fixture.cleanup();
            context.complete();
        } catch (RuntimeException | AssertionError failure) {
            fixture.cleanup();
            throw failure;
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_fill_metrics", tickLimit = 160)
    public void fillCountsPlantsWithoutDuplicatingChangedPositions(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            prepareGround(fixture);
            for (int x = 0; x < 4; x++) fixture.set(fixture.base.add(x, 1, 0), Blocks.DANDELION.getDefaultState());
            fixture.capture();
            UUID id = fixture.writer.reserveTerrain(context.getWorld(), fixture.colony.colony().id(),
                    new WorkArea(MinecraftTypeAdapter.toColonyPos(fixture.base.add(-3, -6, -3)),
                            MinecraftTypeAdapter.toColonyPos(fixture.base.add(7, 8, 3))));
            var original = proposal(fixture);
            var surfaces = original.surfaces().stream().map(at -> new ColonyPos(at.x(), at.y() + 2, at.z())).toList();
            var raised = new MaeTerrainPreparation.Proposal(original.lot(), surfaces, surfaces.getFirst(), null, 0);
            var survey = MaeTerrainPreparation.begin(context.getWorld(), fixture.writer, id,
                    List.of(raised), fixture.colony.colony().center());
            awaitSurvey(context, fixture, survey, () -> {
                var prepared = survey.result();
                context.assertTrue(prepared.terrain().plants() == 4, "aterro esqueceu plantas incorporadas");
                context.assertTrue(prepared.terrain().earthwork() == 8 && prepared.terrain().changedPositions() == 8
                        && prepared.changes().size() == 8, "aterro contou a posição da planta duas vezes");
            });
        } catch (RuntimeException | AssertionError failure) { fixture.cleanup(); throw failure; }
    }

    private static UUID reserveSurvey(MaeSafetyGameTest.Fixture fixture) {
        return fixture.writer.reserveTerrain(fixture.context.getWorld(), fixture.colony.colony().id(),
                new WorkArea(MinecraftTypeAdapter.toColonyPos(fixture.base.add(-3, -6, -3)),
                        MinecraftTypeAdapter.toColonyPos(fixture.base.add(7, 6, 3))));
    }

    private static MaeTerrainPreparation.Proposal proposal(MaeSafetyGameTest.Fixture fixture) {
        var surface = new ArrayList<ColonyPos>();
        for (int x = 0; x < 4; x++) surface.add(MinecraftTypeAdapter.toColonyPos(fixture.base.add(x, 0, 0)));
        return new MaeTerrainPreparation.Proposal(fixture.lot(), surface, surface.getFirst(), null, 0);
    }

    private static void refused(TestContext context, Runnable action, String reason) {
        try {
            action.run();
        } catch (IllegalStateException failure) {
            context.assertTrue(reason.equals(failure.getMessage()), "recusa incorreta: " + failure.getMessage());
            return;
        }
        context.assertTrue(false, "operação deveria recusar: " + reason);
    }
}
