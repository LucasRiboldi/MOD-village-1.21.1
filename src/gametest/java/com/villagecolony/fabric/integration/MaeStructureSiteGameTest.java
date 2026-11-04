package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Fluxo supervisionado inteiro com modelo real: terreno, prova do acesso, obra e conclusão — ADR-046 item 7. */
public final class MaeStructureSiteGameTest implements FabricGameTest {
    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_site_flat", tickLimit = 1000)
    public void flatGroundBuildsWithoutATerrainJob(TestContext context) {
        var site = new Arena(context, true);
        site.run(arena -> {
            var job = arena.job();
            context.assertTrue(job.getCompound("terrain").getBoolean("flat"), "terreno plano não foi aceito pela prova sem mudança");
            context.assertTrue(arena.foreignRecords() == 0, "terreno plano gravou escrita de terreno");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_site_fill", tickLimit = 1000)
    public void missingEntryFloorIsPreparedBeforeTheBuilding(TestContext context) {
        var site = new Arena(context, false);
        site.run(arena -> {
            context.assertTrue(arena.job().getCompound("terrain").containsUuid("id"), "obra não ligou o job de terreno");
            context.assertTrue(arena.foreignRecords() > 0, "piso da entrada não passou pelo writer de terreno");
            context.assertTrue(!context.getWorld().getBlockState(MinecraftTypeAdapter.toBlockPos(
                    MaeStructureSite.entry(arena.project))).isAir(), "piso da entrada continua vazio");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_site_stale_flat", tickLimit = 400)
    public void flatProofIsRefusedOnceTheWorldChanges(TestContext context) {
        var arena = new Arena(context, true);
        arena.guarded(() -> {
            arena.id = arena.fixture.writer.reserveTerrain(context.getWorld(), arena.fixture.colony.colony().id(),
                    new WorkArea(Arena.pos(arena.fixture.base.add(-5, -6, -5)), Arena.pos(arena.fixture.base.add(10, 10, 10))));
            arena.fixture.writer = new MaeWorldWriter(arena.fixture.store, arena.sources());
            var survey = MaeTerrainPreparation.begin(context.getWorld(), arena.fixture.writer, arena.id, List.of(
                    new MaeTerrainPreparation.Proposal(Building.of(arena.project), MaeStructureSite.surfaces(arena.project),
                            MaeStructureSite.entry(arena.project), null, 0)), arena.fixture.colony.colony().center());
            arena.stale(survey);
        });
    }

    private static final class Arena {
        final TestContext context;
        final MaeSafetyGameTest.Fixture fixture;
        final BlockPos origin;
        final List<BlockPos> chests = new ArrayList<>();
        final ConstructionProject project;
        UUID id;
        MaeStructureSite site;

        Arena(TestContext context, boolean entryReady) {
            this.context = context;
            this.fixture = new MaeSafetyGameTest.Fixture(context);
            // Pegada, entrada e margem do terreno cabem no chunk da arena, o único com recibo recapturado.
            this.origin = fixture.base.add(-1, 1, -1);
            var blueprint = StructureBlueprintReader.readForMae(context.getWorld(), HOUSE).orElseThrow();
            this.project = ConstructionProject.plan(fixture.colony.colony().id(), blueprint, pos(origin));
            for (BlockPos at : BlockPos.iterate(fixture.base.add(-5, -2, -5), fixture.base.add(10, 0, 10))) {
                fixture.set(at, Blocks.DIRT.getDefaultState());
            }
            var lot = Building.of(project);
            for (BlockPos at : BlockPos.iterate(MinecraftTypeAdapter.toBlockPos(lot.min()).add(-2, 0, -2),
                    MinecraftTypeAdapter.toBlockPos(lot.max()).add(2, 2, 2))) {
                fixture.changed.putIfAbsent(at.toImmutable(), context.getWorld().getBlockState(at));
            }
            if (entryReady) fixture.set(MinecraftTypeAdapter.toBlockPos(MaeStructureSite.entry(project)), Blocks.DIRT.getDefaultState());
            for (int i = 0; i < 4; i++) {
                var chest = fixture.base.add(-5 + i * 2, 20, -5);
                fixture.set(chest, Blocks.CHEST.getDefaultState());
                chests.add(chest);
            }
            fixture.capture();
            stock();
        }

        static ColonyPos pos(BlockPos at) {
            return MinecraftTypeAdapter.toColonyPos(at);
        }

        void run(Consumer<Arena> then) {
            guarded(() -> {
                id = fixture.writer.reserveTerrain(context.getWorld(), fixture.colony.colony().id(),
                        new WorkArea(pos(fixture.base.add(-5, -6, -5)), pos(fixture.base.add(10, 10, 10))));
                fixture.writer = new MaeWorldWriter(fixture.store, sources());
                site = MaeStructureSite.begin(context.getWorld(), fixture.writer, id, project, 0,
                        fixture.colony.colony().center());
                context.waitAndRun(1, () -> tick(then));
            });
        }

        private void tick(Consumer<Arena> then) {
            guarded(() -> {
                for (int i = 0; i < 3; i++) site.runSlice();
                if (site.phase() == MaeStructureSite.Phase.FAILED) {
                    context.assertTrue(false, "fluxo falhou: " + site.failure());
                }
                context.assertTrue(site.waiting().isEmpty(), "obra esperou material: " + site.waiting());
                if (site.phase() != MaeStructureSite.Phase.DONE) {
                    context.waitAndRun(1, () -> tick(then));
                    return;
                }
                long finished = VillageColonyMod.BUILDINGS.ofColony(fixture.colony.colony().id()).stream()
                        .filter(building -> building.blueprint().equals(HOUSE) && building.finished()).count();
                context.assertTrue(finished == 1, "fluxo registrou " + finished + " edifícios");
                context.assertTrue(chests.stream().allMatch(at -> context.getWorld().getBlockEntity(at) instanceof ChestBlockEntity box
                        && box.isEmpty()), "obra não gastou exatamente o material do plano");
                then.accept(this);
                cleanup();
                context.complete();
            });
        }

        /** Prova de terreno plano tirada, mundo alterado na margem depois: a obra não pode aceitá-la. */
        void stale(MaeTerrainPreparation.Survey survey) {
            guarded(() -> {
                survey.runSlice();
                if (!survey.complete()) {
                    context.waitAndRun(1, () -> stale(survey));
                    return;
                }
                var flat = survey.result();
                context.assertTrue(flat.changes().isEmpty(), "arena plana pediu mudança de terreno");
                var lot = Building.of(project);
                fixture.set(MinecraftTypeAdapter.toBlockPos(lot.max()).add(1, 0, 1), Blocks.DIRT.getDefaultState());
                var entry = MinecraftTypeAdapter.toBlockPos(MaeStructureSite.entry(project));
                var selected = new MaeStructureAccess.Selected(project.id(), entry.up(), entry, java.util.Set.of(),
                        MaeStructureAccess.interiorGoal(project, List.of(MaeStructureSite.entry(project))),
                        List.of(MaeStructureSite.entry(project)));
                boolean refused = false;
                try {
                    fixture.writer.approveStructure(context.getWorld(), id, project, 0, selected, flat);
                } catch (IllegalStateException expected) {
                    refused = "terrain_job_required".equals(expected.getMessage());
                }
                context.assertTrue(refused, "prova de terreno plano velha foi aceita");
                cleanup();
                context.complete();
            });
        }

        NbtCompound job() {
            return fixture.store.region(id).orElseThrow().getCompound("job");
        }

        /** Registros do diário que não são da obra: escrita do terreno. */
        long foreignRecords() {
            var history = fixture.store.region(id).orElseThrow().getList("history", NbtElement.COMPOUND_TYPE);
            long count = 0;
            for (int i = 0; i < history.size(); i++) {
                if (!history.getCompound(i).getUuid("operation").equals(project.id())) count++;
            }
            return count;
        }

        /** O material de cada peça da planta, pela mesma regra do contrato 1. */
        private void stock() {
            Map<Item, Integer> needed = new LinkedHashMap<>();
            for (var piece : MaeStructurePieces.ordered(project)) {
                if (piece.main().isAir()) continue;
                int count = piece.main().contains(Properties.SLAB_TYPE)
                        && piece.main().get(Properties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1;
                needed.merge(piece.main().getBlock().asItem(), count, Integer::sum);
            }
            int slot = 0;
            for (var entry : needed.entrySet()) {
                int left = entry.getValue();
                while (left > 0) {
                    int count = Math.min(left, entry.getKey().getMaxCount());
                    var box = (ChestBlockEntity) context.getWorld().getBlockEntity(chests.get(slot / 27));
                    box.setStack(slot % 27, new ItemStack(entry.getKey(), count));
                    left -= count;
                    slot++;
                }
            }
        }

        private MaeStructureExecution.Sources sources() {
            var where = chests.stream().map(Arena::pos).toList();
            return new MaeStructureExecution.Sources() {
                @Override public Optional<Blueprint> template(ServerWorld world, ResourceId resource) {
                    return StructureBlueprintReader.readForMae(world, resource);
                }

                @Override public List<ColonyPos> chests(ServerWorld world, UUID colonyId, ColonyPos from) {
                    return where;
                }
            };
        }

        private void guarded(Runnable step) {
            try {
                step.run();
            } catch (RuntimeException | AssertionError failure) {
                cleanup();
                throw failure;
            }
        }

        private void cleanup() {
            for (BlockPos chest : chests) {
                if (context.getWorld().getBlockEntity(chest) instanceof ChestBlockEntity box) box.clear();
            }
            var lot = Building.of(project);
            for (BlockPos at : BlockPos.iterate(MinecraftTypeAdapter.toBlockPos(lot.min()), MinecraftTypeAdapter.toBlockPos(lot.max()))) {
                if (context.getWorld().getBlockEntity(at) instanceof net.minecraft.inventory.Inventory box) box.clear();
            }
            fixture.cleanup();
        }
    }
}
