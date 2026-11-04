package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintAccess;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.data.save.MaeWorkStore;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Obra escrita só conclui com acesso conferido nos blocos reais, uma vez e sem refazer — ADR-046 item 10. */
public final class MaeStructureCompletionGameTest implements FabricGameTest {
    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_structure_complete", tickLimit = 400)
    public void completesOnceAfterTheRealAccessPassesAndNeverRebuildsAfterReload(TestContext context) {
        var house = new House(context);
        house.approve(selected -> house.until("complete", () -> {
            var buildings = house.buildings();
            context.assertTrue(buildings == 1, "obra concluída registrou " + buildings + " edifícios");
            context.assertTrue(house.chestEmpty(), "obra concluída não gastou o material planejado");
            house.reload();
            context.assertTrue(house.fixture.writer.runSlice(context.getWorld(), house.id) == 0
                    && "complete".equals(house.job().getString("state")) && house.buildings() == 1,
                    "obra concluída voltou a escrever ou registrar após recarga");
            house.finish();
        }));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_structure_access_fail", tickLimit = 400)
    public void writtenBuildingWithoutRealAccessBlocksAndConcludesOnlyAfterResume(TestContext context) {
        var house = new House(context);
        house.approve(selected -> {
            // Circulação gravada que não encosta no vão: a prova real precisa recusar — ADR-046.
            house.edit(job -> {
                var circulation = new NbtList();
                circulation.add(NbtLong.of(house.base.add(-1, 1, 6).asLong()));
                job.getCompound("access").put("circulation", circulation);
            });
            house.until("blocked", () -> {
                var job = house.job();
                context.assertTrue("access_not_verified".equals(job.getString("reason")) && house.buildings() == 0,
                        "obra sem acesso concluiu: " + job);
                house.refused(() -> new MaeWorldWriter(house.fixture.store, house.sources(house.blueprint(true)))
                        .resume(context.getWorld(), house.id), "template_changed");
                house.edit(fixed -> {
                    var circulation = new NbtList();
                    circulation.add(NbtLong.of(house.base.add(-1, 1, 2).asLong()));
                    fixed.getCompound("access").put("circulation", circulation);
                    fixed.putString("state", "blocked");
                });
                house.fixture.writer.resume(context.getWorld(), house.id);
                context.assertTrue("built".equals(house.job().getString("state")), "retomada não voltou a conferir");
                house.until("complete", () -> {
                    context.assertTrue(house.buildings() == 1, "retomada não concluiu uma vez");
                    house.finish();
                });
            });
        });
    }

    /** Casa 5×5 com parede oeste e porta; o terreno concluído é sintético, já provado em MaeTerrainGameTest. */
    private static final class House {
        final TestContext context;
        final MaeSafetyGameTest.Fixture fixture;
        final BlockPos base;
        final BlockPos origin;
        final BlockPos chest;
        final UUID id;
        ConstructionProject project;

        House(TestContext context) {
            this.context = context;
            this.fixture = new MaeSafetyGameTest.Fixture(context);
            this.base = fixture.base;
            this.origin = base.up();
            this.chest = base.add(0, 1, 9);
            for (BlockPos at : BlockPos.iterate(base.add(-2, 0, -2), base.add(5, 0, 6))) {
                fixture.set(at, Blocks.DIRT.getDefaultState());
            }
            fixture.set(base.add(-1, 1, 2), Blocks.DIRT.getDefaultState());
            fixture.set(chest, Blocks.CHEST.getDefaultState());
            // A casa é escrita pelo writer, fora de fixture.set; a limpeza precisa do estado original — ADR-035 §7.
            for (BlockPos at : BlockPos.iterate(origin, origin.add(4, 3, 4))) {
                fixture.changed.putIfAbsent(at.toImmutable(), context.getWorld().getBlockState(at));
            }
            fixture.capture();
            stock(new ItemStack(Items.OAK_PLANKS, 38), new ItemStack(Items.OAK_DOOR));
            id = fixture.writer.reserveTerrain(context.getWorld(), fixture.colony.colony().id(),
                    new WorkArea(pos(base.add(-3, -6, -3)), pos(base.add(7, 6, 8))));
            var region = fixture.store.region(id).orElseThrow();
            var terrain = new NbtCompound();
            terrain.putString("type", MaeTerrainExecution.TYPE);
            terrain.putInt("contract", 1);
            terrain.putUuid("id", UUID.randomUUID());
            terrain.putString("blueprint", HOUSE.toString());
            terrain.put("footprint", MaeWorldWriter.encode(new WorkArea(pos(origin), pos(origin.add(4, 0, 4)))));
            terrain.put("expectedSections", new NbtCompound());
            terrain.putString("state", "complete");
            region.put("job", terrain);
            fixture.store.update(region);
        }

        static ColonyPos pos(BlockPos at) {
            return MinecraftTypeAdapter.toColonyPos(at);
        }

        Blueprint blueprint(boolean changed) {
            var pieces = new ArrayList<BlueprintBlock>();
            for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++) {
                pieces.add(new BlueprintBlock(new ColonyPos(x, 0, z), ResourceId.vanilla("oak_planks")));
            }
            for (int z = 0; z <= 4; z++) for (int y = 1; y <= 3; y++) {
                if (z != 2 || y == 3) pieces.add(new BlueprintBlock(new ColonyPos(0, y, z), ResourceId.vanilla("oak_planks")));
            }
            pieces.add(new BlueprintBlock(new ColonyPos(0, 1, 2), ResourceId.vanilla("oak_door"), false,
                    Optional.of(Side.EAST), Map.of()));
            if (changed) pieces.add(new BlueprintBlock(new ColonyPos(4, 1, 4), ResourceId.vanilla("oak_planks")));
            return Blueprint.of(HOUSE, pieces).withAccesses(List.of(
                    new BlueprintAccess(BlueprintAccess.Kind.DOOR, List.of(new ColonyPos(0, 1, 2)), Side.WEST)));
        }

        MaeStructureExecution.Sources sources(Blueprint template) {
            var where = List.of(pos(chest));
            return new MaeStructureExecution.Sources() {
                @Override public Optional<Blueprint> template(ServerWorld world, ResourceId id) {
                    return Optional.of(template);
                }

                @Override public List<ColonyPos> chests(ServerWorld world, UUID colonyId, ColonyPos from) {
                    return where;
                }
            };
        }

        /** A prova prevista roda antes, como faria o chamador real; depois a obra é aprovada. */
        void approve(Consumer<MaeStructureAccess.Selected> then) {
            project = ConstructionProject.plan(fixture.colony.colony().id(), blueprint(false), pos(origin));
            var survey = MaeStructureAccess.begin(context.getWorld(), project, List.of(),
                    List.of(pos(base.add(-1, 1, 2))), pos(origin.add(2, 1, 2)), false);
            survey(survey, then);
        }

        private void survey(MaeStructureAccess.Survey survey, Consumer<MaeStructureAccess.Selected> then) {
            guarded(() -> {
                survey.runSlice();
                if (!survey.complete()) {
                    context.waitAndRun(1, () -> survey(survey, then));
                    return;
                }
                var selected = survey.result();
                fixture.writer = new MaeWorldWriter(fixture.store, sources(blueprint(false)));
                fixture.writer.approveStructure(context.getWorld(), id, project, 0, selected);
                context.waitAndRun(1, () -> guarded(() -> then.accept(selected)));
            });
        }

        void until(String state, Runnable then) {
            guarded(() -> {
                fixture.writer.runSlice(context.getWorld(), id);
                if (state.equals(job().getString("state"))) {
                    then.run();
                    return;
                }
                context.assertFalse(!"blocked".equals(state) && "blocked".equals(job().getString("state")),
                        "obra bloqueada: " + job().getString("reason") + " " + job().getString("detail"));
                context.waitAndRun(1, () -> until(state, then));
            });
        }

        NbtCompound job() {
            return fixture.store.region(id).orElseThrow().getCompound("job");
        }

        void edit(Consumer<NbtCompound> change) {
            var region = fixture.store.region(id).orElseThrow();
            var job = region.getCompound("job");
            change.accept(job);
            region.put("job", job);
            fixture.store.update(region);
        }

        long buildings() {
            return VillageColonyMod.BUILDINGS.ofColony(fixture.colony.colony().id()).stream()
                    .filter(building -> building.blueprint().equals(HOUSE) && building.finished()).count();
        }

        void reload() {
            var disk = new NbtCompound();
            fixture.store.write(disk);
            fixture.store = new MaeWorkStore(() -> { });
            fixture.store.read(disk);
            fixture.writer = new MaeWorldWriter(fixture.store, sources(blueprint(false)));
        }

        void stock(ItemStack... stacks) {
            if (!(context.getWorld().getBlockEntity(chest) instanceof ChestBlockEntity box)) {
                throw new AssertionError("baú do cenário ausente");
            }
            for (int slot = 0; slot < stacks.length; slot++) box.setStack(slot, stacks[slot].copy());
        }

        boolean chestEmpty() {
            return context.getWorld().getBlockEntity(chest) instanceof ChestBlockEntity box && box.isEmpty();
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
