package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.data.save.MaeWorkStore;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Escrita structure_v3: material uma vez, ordem estrita e retomada que nunca repete — ADR-046. */
public final class MaeStructureWriteGameTest implements FabricGameTest {
    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_structure_write", tickLimit = 400)
    public void writesEachPieceOnceConsumesItsMaterialOnceAndSurvivesReload(TestContext context) {
        var scene = new Scene(context);
        scene.stock(new ItemStack(Items.STONE, 3), new ItemStack(Items.OAK_DOOR), new ItemStack(Items.COBBLESTONE));
        scene.terrain(id -> {
            context.assertTrue(version(scene.fixture.store) == 2, "terreno promoveu o envelope antes da obra");
            scene.approve(id, scene.house(), scene.sources(scene.house()));
            context.assertTrue(version(scene.fixture.store) == 3, "obra structure_v3 não promoveu o envelope");
            scene.reload();
            scene.until(id, "built", () -> {
                var door = scene.base.add(2, 1, 0);
                context.assertTrue(context.getWorld().getBlockState(door).getBlock() instanceof DoorBlock
                        && context.getWorld().getBlockState(door.up()).get(DoorBlock.HALF) == DoubleBlockHalf.UPPER,
                        "porta sem a metade de cima no mesmo passo");
                context.assertTrue(scene.chestEmpty(), "material não saiu do baú exatamente uma vez");
                var history = scene.history(id);
                context.assertTrue(history.size() == 5 && history.stream().allMatch(record ->
                        "consumed".equals(record.getString("materials"))), "passos repetidos ou sem consumo: " + history.size());
                context.assertTrue(scene.fixture.writer.runSlice(context.getWorld(), id) == 0, "obra pronta escreveu de novo");
                scene.finish();
            });
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_structure_wait", tickLimit = 400)
    public void missingMaterialWaitsWithoutIntentAndContinuesWhenItArrives(TestContext context) {
        var scene = new Scene(context);
        scene.stock(new ItemStack(Items.STONE, 3), new ItemStack(Items.OAK_DOOR));
        scene.terrain(id -> {
            scene.approve(id, scene.house(), scene.sources(scene.house()));
            scene.until(id, "waiting", () -> {
                var job = scene.job(id);
                context.assertTrue(job.getInt("cursor") == 3 && "material:minecraft:cobblestone".equals(job.getString("reason"))
                        && !job.getBoolean("pendingWrite"), "espera com intenção ou fora da ordem: " + job);
                context.assertTrue(scene.history(id).size() == 3, "falta de material registrou intenção");
                context.assertTrue(context.getWorld().getBlockState(scene.base.add(0, 2, 0)).isAir(),
                        "peça seguinte foi posta antes da que espera");
                scene.stock(new ItemStack(Items.COBBLESTONE));
                scene.until(id, "built", scene::finish);
            });
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_structure_refuse", tickLimit = 400)
    public void approvalAndExecutionRefuseWhatTheContractCannotProve(TestContext context) {
        var scene = new Scene(context);
        scene.stock(new ItemStack(Items.STONE, 3), new ItemStack(Items.OAK_DOOR), new ItemStack(Items.COBBLESTONE));
        scene.terrain(id -> {
            var changed = scene.house(new BlueprintBlock(new ColonyPos(1, 1, 0), ResourceId.vanilla("stone")));
            scene.refused(() -> scene.approve(id, scene.house(), scene.sources(changed)), "template_changed");
            var water = scene.house(new BlueprintBlock(new ColonyPos(3, 1, 0), ResourceId.vanilla("water")));
            scene.refused(() -> scene.approve(id, water, scene.sources(water)), "unsupported_structure_piece");
            // Mesmo modelo e pegada, mas de outra colônia: a região não pertence a ela — ADR-044.
            var foreign = ConstructionProject.plan(UUID.randomUUID(), scene.house(), MinecraftTypeAdapter.toColonyPos(scene.base.up()));
            scene.refused(() -> new MaeWorldWriter(scene.fixture.store, scene.sources(scene.house())).approveStructure(
                    context.getWorld(), id, foreign, 0, new MaeStructureAccess.Selected(foreign.id(), scene.base.add(2, 1, 0),
                            scene.base.add(2, 0, 0), Set.of(), MinecraftTypeAdapter.toColonyPos(scene.base.add(1, 1, 0)),
                            List.of(MinecraftTypeAdapter.toColonyPos(scene.base.add(-1, 0, 0))))), "wrong_project_or_colony");
            scene.approve(id, scene.house(), scene.sources(scene.house()));
            scene.edit(id, job -> job.putInt("contract", 2));
            // O orçamento de escrita é do tick; a fatia seguinte começa com ele cheio — ADR-044.
            scene.nextTick(() -> {
                scene.fixture.writer.runSlice(context.getWorld(), id);
                context.assertTrue("unknown_structure_contract".equals(scene.job(id).getString("reason")),
                        "contrato desconhecido não bloqueou: " + scene.job(id).getString("reason"));
                scene.edit(id, job -> job.putInt("contract", 1));
                scene.fixture.writer.resume(context.getWorld(), id);
                scene.fixture.set(scene.base.add(5, 1, 0), Blocks.DIRT.getDefaultState());
                scene.nextTick(() -> {
                    scene.fixture.writer.runSlice(context.getWorld(), id);
                    context.assertTrue("world_changed_since_approval".equals(scene.job(id).getString("reason")),
                            "mundo alterado não bloqueou: " + scene.job(id).getString("reason"));
                    scene.finish();
                });
            });
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_structure_resume", tickLimit = 400)
    public void interruptedStepBlocksWithItsCauseAndIsNeverRepeated(TestContext context) {
        var scene = new Scene(context);
        scene.stock(new ItemStack(Items.STONE, 3));
        scene.terrain(id -> {
            scene.approve(id, scene.house(), scene.sources(scene.house()));
            scene.until(id, "waiting", () -> {
                scene.stock(new ItemStack(Items.OAK_DOOR));
                scene.edit(id, job -> {
                    job.putBoolean("pendingWrite", true);
                    job.putString("state", "approved");
                });
                scene.nextTick(() -> {
                    scene.fixture.writer.runSlice(context.getWorld(), id);
                    context.assertTrue("invalid_or_unconfirmed_job".equals(scene.job(id).getString("reason")),
                            "passo interrompido rodou de novo: " + scene.job(id));
                    scene.refused(() -> scene.fixture.writer.resume(context.getWorld(), id), "materials_uncertain");
                    var door = scene.base.add(2, 1, 0);
                    scene.fixture.set(door, Blocks.OAK_DOOR.getDefaultState());
                    scene.refused(() -> scene.fixture.writer.resume(context.getWorld(), id), "partial_parts");
                    // Todas as partes já no estado planejado: a escrita pode ter ocorrido, o baú não diz — ADR-046.
                    var job = scene.job(id);
                    var parts = job.getList("plan", NbtElement.COMPOUND_TYPE).getCompound(job.getInt("cursor"))
                            .getList("parts", NbtElement.COMPOUND_TYPE);
                    for (int i = 0; i < parts.size(); i++) {
                        scene.fixture.set(BlockPos.fromLong(parts.getCompound(i).getLong("position")),
                                MaeWorldWriter.decodeState(parts.getCompound(i).getCompound("after")));
                    }
                    scene.refused(() -> scene.fixture.writer.resume(context.getWorld(), id), "materials_uncertain");
                    context.assertTrue(scene.history(id).size() == 2 && "blocked".equals(scene.job(id).getString("state")),
                            "retomada ambígua repetiu ou liberou o passo");
                    context.assertTrue(!scene.chestEmpty(), "retomada ambígua consumiu material");
                    scene.finish();
                });
            });
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_structure_terrain", tickLimit = 40)
    public void aBuildingNeedsTheCompletedTerrainOfTheSameWork(TestContext context) {
        var fixture = new MaeSafetyGameTest.Fixture(context);
        try {
            fixture.capture();
            UUID id = fixture.writer.reserveTerrain(context.getWorld(), fixture.colony.colony().id(),
                    new WorkArea(MinecraftTypeAdapter.toColonyPos(fixture.base.add(-3, -6, -3)),
                            MinecraftTypeAdapter.toColonyPos(fixture.base.add(7, 6, 3))));
            var blueprint = Blueprint.of(HOUSE, List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("stone"))));
            var project = ConstructionProject.plan(fixture.colony.colony().id(), blueprint,
                    MinecraftTypeAdapter.toColonyPos(fixture.base.up()));
            boolean refused = false;
            try {
                fixture.writer.approveStructure(context.getWorld(), id, project, 0,
                        new MaeStructureAccess.Selected(project.id(), fixture.base.up(), fixture.base, Set.of(),
                                MinecraftTypeAdapter.toColonyPos(fixture.base.up()), List.of(MinecraftTypeAdapter.toColonyPos(fixture.base))));
            } catch (IllegalStateException expected) {
                refused = "terrain_job_required".equals(expected.getMessage());
            }
            context.assertTrue(refused, "edifício aprovado sem terreno concluído");
            var unknown = new NbtCompound();
            unknown.putInt("version", 4);
            unknown.put("regions", new net.minecraft.nbt.NbtList());
            var root = new NbtCompound();
            root.put(MaeWorkStore.KEY, unknown);
            var store = new MaeWorkStore(() -> { });
            store.read(root);
            var written = new NbtCompound();
            store.write(written);
            context.assertTrue(!store.supported() && written.get(MaeWorkStore.KEY).equals(unknown),
                    "envelope futuro não ficou opaco e preservado");
        } finally {
            fixture.cleanup();
        }
        context.complete();
    }

    private static int version(MaeWorkStore store) {
        var disk = new NbtCompound();
        store.write(disk);
        return disk.getCompound(MaeWorkStore.KEY).getInt("version");
    }

    /** Terreno concluído por corte real, baú fora da região e obra pequena com porta. */
    private static final class Scene {
        final TestContext context;
        final MaeSafetyGameTest.Fixture fixture;
        final BlockPos base;
        final BlockPos chest;
        ConstructionProject project;

        Scene(TestContext context) {
            this.context = context;
            this.fixture = new MaeSafetyGameTest.Fixture(context);
            this.base = fixture.base;
            this.chest = base.add(0, 1, 8);
            for (BlockPos at : BlockPos.iterate(base.add(-2, -4, -2), base.add(5, 0, 2))) {
                fixture.set(at, Blocks.DIRT.getDefaultState());
            }
            for (int x = 0; x < 4; x++) {
                fixture.set(base.add(x, 1, 0), Blocks.DIRT.getDefaultState());
                fixture.set(base.add(x, 2, 0), Blocks.DANDELION.getDefaultState());
            }
            fixture.set(chest, Blocks.CHEST.getDefaultState());
            fixture.capture();
        }

        Blueprint house(BlueprintBlock... extra) {
            var blocks = new ArrayList<>(List.of(
                    new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("stone")),
                    new BlueprintBlock(new ColonyPos(1, 0, 0), ResourceId.vanilla("stone")),
                    new BlueprintBlock(new ColonyPos(0, 1, 0), ResourceId.vanilla("stone")),
                    new BlueprintBlock(new ColonyPos(2, 0, 0), ResourceId.vanilla("oak_door")),
                    new BlueprintBlock(new ColonyPos(3, 0, 0), ResourceId.vanilla("cobblestone"))));
            blocks.addAll(List.of(extra));
            return Blueprint.of(HOUSE, blocks);
        }

        MaeStructureExecution.Sources sources(Blueprint template) {
            var where = List.of(MinecraftTypeAdapter.toColonyPos(chest));
            return new MaeStructureExecution.Sources() {
                @Override public Optional<Blueprint> template(ServerWorld world, ResourceId id) {
                    return Optional.of(template);
                }

                @Override public List<ColonyPos> chests(ServerWorld world, UUID colonyId, ColonyPos from) {
                    return where;
                }
            };
        }

        void approve(UUID id, Blueprint blueprint, MaeStructureExecution.Sources sources) {
            fixture.writer = new MaeWorldWriter(fixture.store, sources);
            project = ConstructionProject.plan(fixture.colony.colony().id(), blueprint,
                    MinecraftTypeAdapter.toColonyPos(base.up()));
            fixture.writer.approveStructure(context.getWorld(), id, project, 0, new MaeStructureAccess.Selected(
                    project.id(), base.add(2, 1, 0), base.add(2, 0, 0), Set.of(),
                    MinecraftTypeAdapter.toColonyPos(base.add(1, 1, 0)), List.of(MinecraftTypeAdapter.toColonyPos(base.add(-1, 0, 0)))));
            current = sources;
        }

        private MaeStructureExecution.Sources current;

        void reload() {
            var disk = new NbtCompound();
            fixture.store.write(disk);
            fixture.store = new MaeWorkStore(() -> { });
            fixture.store.read(disk);
            fixture.writer = new MaeWorldWriter(fixture.store, current);
        }

        void stock(ItemStack... stacks) {
            if (!(context.getWorld().getBlockEntity(chest) instanceof ChestBlockEntity box)) {
                throw new AssertionError("baú do cenário ausente");
            }
            for (ItemStack stack : stacks) {
                for (int slot = 0; slot < box.size(); slot++) {
                    if (box.getStack(slot).isEmpty()) {
                        box.setStack(slot, stack.copy());
                        break;
                    }
                }
            }
        }

        boolean chestEmpty() {
            return context.getWorld().getBlockEntity(chest) instanceof ChestBlockEntity box && box.isEmpty();
        }

        NbtCompound job(UUID id) {
            return fixture.store.region(id).orElseThrow().getCompound("job");
        }

        List<NbtCompound> history(UUID id) {
            var list = fixture.store.region(id).orElseThrow().getList("history", NbtElement.COMPOUND_TYPE);
            var result = new ArrayList<NbtCompound>();
            for (int i = 0; i < list.size(); i++) {
                if (list.getCompound(i).containsUuid("operation")
                        && list.getCompound(i).getUuid("operation").equals(project.id())) result.add(list.getCompound(i));
            }
            return result;
        }

        void edit(UUID id, Consumer<NbtCompound> change) {
            var region = fixture.store.region(id).orElseThrow();
            var job = region.getCompound("job");
            change.accept(job);
            region.put("job", job);
            fixture.store.update(region);
        }

        void refused(Runnable action, String reason) {
            try {
                action.run();
            } catch (IllegalStateException | IllegalArgumentException failure) {
                context.assertTrue(reason.equals(failure.getMessage()), "recusa incorreta: " + failure.getMessage());
                return;
            }
            context.assertTrue(false, "operação deveria recusar: " + reason);
        }

        /** Corta terra e flor sobre a pegada, como a preparação MAE faria, e entrega a região pronta. */
        void terrain(Consumer<UUID> then) {
            context.waitAndRun(20, () -> guarded(() -> {
                UUID id = fixture.writer.reserveTerrain(context.getWorld(), fixture.colony.colony().id(),
                        new WorkArea(MinecraftTypeAdapter.toColonyPos(base.add(-3, -6, -3)),
                                MinecraftTypeAdapter.toColonyPos(base.add(7, 6, 3))));
                var surface = new ArrayList<ColonyPos>();
                for (int x = 0; x < 4; x++) surface.add(MinecraftTypeAdapter.toColonyPos(base.add(x, 0, 0)));
                var proposal = new MaeTerrainPreparation.Proposal(fixture.lot(), surface, surface.getFirst(), null, 0);
                var survey = MaeTerrainPreparation.begin(context.getWorld(), fixture.writer, id,
                        List.of(proposal), fixture.colony.colony().center());
                cut(id, survey, false, then);
            }));
        }

        private void cut(UUID id, MaeTerrainPreparation.Survey survey, boolean approved, Consumer<UUID> then) {
            guarded(() -> {
                if (!survey.complete()) survey.runSlice();
                boolean nowApproved = approved;
                if (survey.complete() && !approved) {
                    context.assertTrue(fixture.writer.approveTerrain(context.getWorld(), survey.result()), "corte não aprovado");
                    nowApproved = true;
                }
                if (nowApproved) fixture.writer.runSlice(context.getWorld(), id);
                if (nowApproved && "complete".equals(job(id).getString("state"))) {
                    then.accept(id);
                    return;
                }
                context.assertFalse("blocked".equals(job(id).getString("state")), "corte bloqueado: " + job(id).getString("reason"));
                boolean next = nowApproved;
                context.waitAndRun(1, () -> cut(id, survey, next, then));
            });
        }

        void until(UUID id, String state, Runnable then) {
            guarded(() -> {
                fixture.writer.runSlice(context.getWorld(), id);
                if (state.equals(job(id).getString("state"))) {
                    then.run();
                    return;
                }
                context.assertFalse("blocked".equals(job(id).getString("state")), "obra bloqueada: " + job(id).getString("reason"));
                context.waitAndRun(1, () -> until(id, state, then));
            });
        }

        void nextTick(Runnable step) {
            context.waitAndRun(1, () -> guarded(step));
        }

        void guarded(Runnable step) {
            try {
                step.run();
            } catch (RuntimeException | AssertionError failure) {
                cleanup();
                throw failure;
            }
        }

        void finish() {
            cleanup();
            context.complete();
        }

        private void cleanup() {
            if (context.getWorld().getBlockEntity(chest) instanceof ChestBlockEntity box) box.clear();
            fixture.cleanup();
        }
    }
}
