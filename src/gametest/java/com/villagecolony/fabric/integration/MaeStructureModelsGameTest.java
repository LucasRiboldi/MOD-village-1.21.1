package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintAccess;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Exercício supervisionado com modelos reais: prova prevista, escrita, prova real e conclusão — ADR-046.
 *
 * <p>O terreno concluído é sintético (provado em {@code MaeTerrainGameTest}); o resto é o caminho do jogo,
 * com o modelo lido por {@code readForMae}. Tudo cabe no chunk da arena, o único com recibo recapturado.
 */
public final class MaeStructureModelsGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_house", tickLimit = 600)
    public void plainsSmallHouseIsBuiltAndConcludedOnce(TestContext context) {
        new Site(context, "minecraft:village/plains/houses/plains_small_house_1").run();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_workshop", tickLimit = 600)
    public void plainsMasonsHouseIsBuiltAndConcludedOnce(TestContext context) {
        new Site(context, "minecraft:village/plains/houses/plains_masons_house_1").run();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_pen", tickLimit = 600)
    public void plainsAnimalPenIsBuiltAndConcludedOnce(TestContext context) {
        new Site(context, "minecraft:village/plains/houses/plains_animal_pen_1").run();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_storage", tickLimit = 600)
    public void smallStorageIsBuiltAndConcludedOnce(TestContext context) {
        new Site(context, "villagecolony:colony/storage_small").run();
    }

    private static final class Site {
        final TestContext context;
        final MaeSafetyGameTest.Fixture fixture;
        final ResourceId model;
        final Blueprint blueprint;
        final BlockPos origin;
        final List<BlockPos> chests = new ArrayList<>();
        UUID id;
        ConstructionProject project;

        Site(TestContext context, String name) {
            this.context = context;
            this.fixture = new MaeSafetyGameTest.Fixture(context);
            this.model = ResourceId.parse(name);
            this.blueprint = StructureBlueprintReader.readForMae(context.getWorld(), model).orElseThrow();
            this.origin = fixture.base.add(-3, 1, -3);
        }

        static ColonyPos pos(BlockPos at) {
            return MinecraftTypeAdapter.toColonyPos(at);
        }

        BlockPos absolute(ColonyPos relative) {
            return origin.add(relative.x(), relative.y(), relative.z());
        }

        /** Pés do vão para fora até a primeira célula fora do lote, no máximo seis passos. */
        List<BlockPos> exit(Building lot, BlueprintAccess access) {
            var cells = new ArrayList<BlockPos>();
            var at = absolute(access.opening().getFirst());
            for (int step = 0; step < 7; step++) {
                at = at.add(access.outward().offsetX(), 0, access.outward().offsetZ());
                cells.add(at);
                if (!lot.contains(pos(at))) return cells;
            }
            cells.addAll(java.util.Collections.nCopies(64, at));
            return cells;
        }

        void run() {
            try {
                project = ConstructionProject.plan(fixture.colony.colony().id(), blueprint, pos(origin));
                var lot = Building.of(project);
                context.assertTrue(lot.max().x() <= pos(fixture.base).x() + 8 && lot.max().z() <= pos(fixture.base).z() + 8,
                        model + " não cabe no chunk da arena: " + lot.min() + ".." + lot.max());
                for (BlockPos at : BlockPos.iterate(fixture.base.add(-5, 0, -5), fixture.base.add(10, 0, 10))) {
                    fixture.set(at, Blocks.DIRT.getDefaultState());
                }
                for (BlockPos at : BlockPos.iterate(MinecraftTypeAdapter.toBlockPos(lot.min()), MinecraftTypeAdapter.toBlockPos(lot.max()))) {
                    fixture.changed.putIfAbsent(at.toImmutable(), context.getWorld().getBlockState(at));
                }
                // Cada vão vem com as duas direções; o lado que sai do lote em menos passos é o de fora.
                var access = blueprint.accesses().stream()
                        .sorted(Comparator.comparingInt((BlueprintAccess a) -> exit(lot, a).size())
                                .thenComparing(a -> a.kind() != BlueprintAccess.Kind.DOOR))
                        .findFirst().orElseThrow();
                var opening = absolute(access.opening().getFirst());
                var circulation = new ArrayList<ColonyPos>();
                for (BlockPos feet : exit(lot, access)) {
                    var floor = feet.down();
                    circulation.add(pos(floor));
                    // Fora do lote o piso é preparado aqui; dentro dele, é peça da planta.
                    if (!lot.contains(pos(floor)) && floor.getY() != fixture.base.getY()) {
                        fixture.set(floor, Blocks.DIRT.getDefaultState());
                    }
                }
                var goal = opening.add(-access.outward().offsetX(), 0, -access.outward().offsetZ());
                for (int i = 0; i < 4; i++) {
                    var chest = fixture.base.add(-5 + i * 2, 20, -5);
                    fixture.set(chest, Blocks.CHEST.getDefaultState());
                    chests.add(chest);
                }
                fixture.capture();
                id = fixture.writer.reserveTerrain(context.getWorld(), fixture.colony.colony().id(),
                        new WorkArea(pos(fixture.base.add(-5, -6, -5)), pos(fixture.base.add(10, 10, 10))));
                var region = fixture.store.region(id).orElseThrow();
                var terrain = new NbtCompound();
                terrain.putString("type", MaeTerrainExecution.TYPE);
                terrain.putInt("contract", 1);
                terrain.putUuid("id", UUID.randomUUID());
                terrain.putString("blueprint", model.toString());
                terrain.put("footprint", MaeWorldWriter.encode(new WorkArea(lot.min(), lot.max())));
                terrain.put("expectedSections", new NbtCompound());
                terrain.putString("state", "complete");
                region.put("job", terrain);
                fixture.store.update(region);
                var survey = MaeStructureAccess.begin(context.getWorld(), project, List.of(), circulation, pos(goal), false);
                context.waitAndRun(1, () -> survey(survey));
            } catch (RuntimeException | AssertionError failure) {
                cleanup();
                throw failure;
            }
        }

        private void survey(MaeStructureAccess.Survey survey) {
            guarded(() -> {
                survey.runSlice();
                if (!survey.complete()) {
                    context.waitAndRun(1, () -> survey(survey));
                    return;
                }
                fixture.writer = new MaeWorldWriter(fixture.store, sources());
                fixture.writer.approveStructure(context.getWorld(), id, project, 0, survey.result());
                stock();
                context.waitAndRun(1, this::build);
            });
        }

        private void build() {
            guarded(() -> {
                fixture.writer.runSlice(context.getWorld(), id);
                var job = fixture.store.region(id).orElseThrow().getCompound("job");
                if (!"complete".equals(job.getString("state"))) {
                    context.assertFalse("blocked".equals(job.getString("state")) || "waiting".equals(job.getString("state")),
                            model + " parou: " + job.getString("state") + " " + job.getString("reason") + " " + job.getString("detail"));
                    context.waitAndRun(1, this::build);
                    return;
                }
                long finished = VillageColonyMod.BUILDINGS.ofColony(fixture.colony.colony().id()).stream()
                        .filter(building -> building.blueprint().equals(model) && building.finished()).count();
                context.assertTrue(finished == 1, model + " registrou " + finished + " edifícios");
                for (var piece : MaeStructurePieces.ordered(project)) {
                    for (var part : piece.parts()) {
                        context.assertTrue(context.getWorld().getBlockState(part.getKey()).equals(part.getValue()),
                                model + " sem a peça planejada em " + part.getKey().toShortString());
                    }
                }
                context.assertTrue(chests.stream().allMatch(at -> context.getWorld().getBlockEntity(at) instanceof ChestBlockEntity box
                        && box.isEmpty()), model + " não gastou exatamente o material do plano");
                context.assertTrue(fixture.writer.runSlice(context.getWorld(), id) == 0, model + " concluído voltou a escrever");
                cleanup();
                context.complete();
            });
        }

        /** O baú recebe exatamente o que o plano aprovado pede, nem mais nem menos. */
        private void stock() {
            Map<Item, Integer> needed = new LinkedHashMap<>();
            var plan = fixture.store.region(id).orElseThrow().getCompound("job").getList("plan", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < plan.size(); i++) {
                var material = plan.getCompound(i).getCompound("material");
                needed.merge(Registries.ITEM.get(Identifier.of(material.getString("item"))), material.getInt("count"), Integer::sum);
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
            var where = chests.stream().map(Site::pos).toList();
            return new MaeStructureExecution.Sources() {
                @Override public Optional<Blueprint> template(ServerWorld world, ResourceId id) {
                    return StructureBlueprintReader.readForMae(world, id);
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
            if (project != null) {
                var lot = Building.of(project);
                for (BlockPos at : BlockPos.iterate(MinecraftTypeAdapter.toBlockPos(lot.min()), MinecraftTypeAdapter.toBlockPos(lot.max()))) {
                    if (context.getWorld().getBlockEntity(at) instanceof net.minecraft.inventory.Inventory box) box.clear();
                }
            }
            fixture.cleanup();
        }
    }
}
