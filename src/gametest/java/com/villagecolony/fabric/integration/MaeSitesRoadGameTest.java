package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Duas obras pedidas na mesma região: a primeira semeia a rua MAE perto do centro e a segunda liga a
 * entrada a essa rua pelo grafo certificado — ADR-045/046.
 */
public final class MaeSitesRoadGameTest implements FabricGameTest {
    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_road", tickLimit = 2400)
    public void theSecondBuildingJoinsTheFirstRoadInTheSameRegion(TestContext context) {
        var arena = new WideArena(context);
        arena.guarded(() -> {
            var first = MaeSites.start(context.getWorld(), arena.writer(), arena.colony(), HOUSE,
                    arena.fixture.base.add(-1, 1, -1), 0, null);
            arena.await(first.regionId(), () -> {
                var second = MaeSites.start(context.getWorld(), arena.writer(), arena.colony(), HOUSE,
                        arena.fixture.base.add(-1, 1, 6), 0, null);
                context.assertTrue(second.regionId().equals(first.regionId()), "segunda obra reservou outra região");
                arena.await(second.regionId(), () -> {
                    var graph = ColonySavedData.get(context.getWorld().getServer()).roadGraphs().graph(arena.colony())
                            .orElseThrow();
                    var positions = MaeRoadExecution.allPositions(graph);
                    for (var origin : List.of(arena.fixture.base.add(-1, 1, -1), arena.fixture.base.add(-1, 1, 6))) {
                        var project = ConstructionProject.plan(arena.colony(),
                                StructureBlueprintReader.readForMae(context.getWorld(), HOUSE).orElseThrow(),
                                MinecraftTypeAdapter.toColonyPos(origin));
                        context.assertTrue(positions.contains(MaeStructureSite.entry(project)),
                                "entrada em " + origin.toShortString() + " fora da rua publicada");
                    }
                    long finished = VillageColonyMod.BUILDINGS.ofColony(arena.colony()).stream()
                            .filter(building -> building.blueprint().equals(HOUSE) && building.finished()).count();
                    context.assertTrue(finished == 2, "registrou " + finished + " casas");
                    arena.cleanup();
                    context.complete();
                });
            });
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_pen", tickLimit = 2400)
    public void largePenShapesWaterFarmlandAndRipeWheatInPlace(TestContext context) {
        var pen = ResourceId.parse("villagecolony:colony/large_animal_pen");
        var arena = new WideArena(context, pen, 1);
        arena.guarded(() -> {
            var origin = arena.fixture.base.add(-1, 1, -1);
            var status = MaeSites.start(context.getWorld(), arena.writer(), arena.colony(), pen, origin, 0, null);
            arena.await(status.regionId(), () -> {
                int water = 0, farmland = 0, wheat = 0;
                for (BlockPos at : BlockPos.iterate(origin, origin.add(14, 2, 14))) {
                    var state = context.getWorld().getBlockState(at);
                    if (state.isOf(Blocks.WATER) && state.getFluidState().isStill()) water++;
                    if (state.isOf(Blocks.FARMLAND) && state.get(Properties.MOISTURE) == 7) farmland++;
                    if (state.isOf(Blocks.WHEAT) && state.get(Properties.AGE_7) == 7) wheat++;
                }
                context.assertTrue(water == 1 && farmland == 12 && wheat == 12,
                        "curral sem horta estável: água=" + water + " arado=" + farmland + " trigo=" + wheat);
                arena.cleanup();
                context.complete();
            });
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_loot", tickLimit = 2400)
    public void savannaMasonChestIsBuiltEmptyWithoutTheTemplateLoot(TestContext context) {
        var mason = ResourceId.vanilla("village/savanna/houses/savanna_mason_1");
        var arena = new WideArena(context, mason, 1);
        arena.guarded(() -> {
            var origin = arena.fixture.base.add(-1, 1, -1);
            var status = MaeSites.start(context.getWorld(), arena.writer(), arena.colony(), mason, origin, 0, null);
            arena.await(status.regionId(), () -> {
                int chests = 0;
                for (BlockPos at : BlockPos.iterate(origin, origin.add(10, 7, 10))) {
                    if (context.getWorld().getBlockEntity(at) instanceof ChestBlockEntity box) {
                        chests++;
                        // Só a sobra da pavimentação (terra) pode entrar; loot e itens do template, nunca.
                        boolean onlyCuttings = java.util.stream.IntStream.range(0, box.size()).mapToObj(box::getStack)
                                .allMatch(stack -> stack.isEmpty() || stack.isOf(net.minecraft.item.Items.DIRT));
                        context.assertTrue(box.getLootTable() == null && onlyCuttings,
                                "baú da oficina saiu com conteúdo em " + at.toShortString() + ": loot=" + box.getLootTable()
                                        + " itens=" + java.util.stream.IntStream.range(0, box.size()).mapToObj(box::getStack)
                                        .filter(stack -> !stack.isEmpty()).map(Object::toString).toList());
                    }
                }
                context.assertTrue(chests > 0, "oficina sem baú construído");
                arena.cleanup();
                context.complete();
            });
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_storage_majest", tickLimit = 2400)
    public void largeStorageEntersByItsNewGroundDoor(TestContext context) {
        var storage = ResourceId.parse("villagecolony:colony/plains/storage_majest");
        var arena = new WideArena(context, storage, 1);
        arena.guarded(() -> {
            var origin = arena.fixture.base.add(-1, 1, -1);
            var status = MaeSites.start(context.getWorld(), arena.writer(), arena.colony(), storage, origin, 0, null);
            arena.await(status.regionId(), () -> {
                context.assertTrue(context.getWorld().getBlockState(origin.add(8, 1, 3)).isOf(Blocks.OAK_DOOR),
                        "depósito grande sem a porta térrea");
                arena.cleanup();
                context.complete();
            });
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_furniture_house", tickLimit = 2400)
    public void houseBedIsAHomeTheVillagersCanClaim(TestContext context) {
        furniture(context, ResourceId.vanilla("village/plains/houses/plains_small_house_1"));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_furniture_workshop", tickLimit = 2400)
    public void workshopBenchIsAJobSiteTheProfessionRecognizes(TestContext context) {
        furniture(context, ResourceId.vanilla("village/plains/houses/plains_masons_house_1"));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_furniture_storage", tickLimit = 2400)
    public void storageChestsAreFreeColonyChests(TestContext context) {
        furniture(context, ResourceId.parse("villagecolony:colony/storage_small"));
    }

    /**
     * Mobília funcional (item 6): todo bloco da obra que o jogo trata como POI (cama, bancada) está no registro
     * de POI do mundo, e todo baú construído é um baú livre da colônia — ADR-046.
     */
    private static void furniture(TestContext context, ResourceId model) {
        var arena = new WideArena(context, model, 1);
        arena.guarded(() -> {
            var origin = arena.fixture.base.add(-1, 1, -1);
            var status = MaeSites.start(context.getWorld(), arena.writer(), arena.colony(), model, origin, 0, null);
            arena.await(status.regionId(), () -> {
                var world = context.getWorld();
                var project = ConstructionProject.plan(arena.colony(),
                        StructureBlueprintReader.readForMae(world, model).orElseThrow(), MinecraftTypeAdapter.toColonyPos(origin));
                var lot = com.villagecolony.core.construction.model.Building.of(project);
                int pois = 0;
                var chests = new ArrayList<ColonyPos>();
                for (BlockPos at : BlockPos.iterate(MinecraftTypeAdapter.toBlockPos(lot.min()), MinecraftTypeAdapter.toBlockPos(lot.max()))) {
                    var state = world.getBlockState(at);
                    var type = net.minecraft.world.poi.PointOfInterestTypes.getTypeForState(state);
                    if (type.isPresent()) {
                        pois++;
                        var registered = world.getPointOfInterestStorage().getType(at);
                        context.assertTrue(registered.isPresent() && registered.get().equals(type.get()),
                                model + ": " + state.getBlock() + " em " + at.toShortString() + " não está no registro de POI");
                    }
                    if (world.getBlockEntity(at) instanceof ChestBlockEntity) chests.add(MinecraftTypeAdapter.toColonyPos(at.toImmutable()));
                }
                var colony = VillageColonyMod.COLONIES.find(arena.colony()).orElseThrow();
                var free = com.villagecolony.fabric.integration.VillageChests.around(world, colony, List.of());
                context.assertTrue(free.containsAll(chests), model + ": baús construídos fora dos baús livres da colônia: "
                        + chests.stream().filter(chest -> !free.contains(chest)).toList());
                context.assertTrue(pois + chests.size() > 0, model + ": obra sem mobília para conferir");
                arena.cleanup();
                context.complete();
            });
        });
    }

    /** Plataforma de três de altura em 3×3 chunks, com os recibos de geração recapturados e devolvidos. */
    private static final class WideArena {
        final TestContext context;
        final MaeSafetyGameTest.Fixture fixture;
        final List<BlockPos> chests = new ArrayList<>();
        final Map<WorldChunk, NbtCompound> receipts = new HashMap<>();
        MaeWorldWriter writer;

        WideArena(TestContext context) {
            this(context, HOUSE, 2);
        }

        WideArena(TestContext context, ResourceId model, int copies) {
            this.context = context;
            this.fixture = new MaeSafetyGameTest.Fixture(context);
            var base = fixture.base;
            for (BlockPos at : BlockPos.iterate(base.add(-20, -2, -20), base.add(25, 0, 25))) {
                fixture.set(at, Blocks.DIRT.getDefaultState());
            }
            for (BlockPos at : BlockPos.iterate(base.add(-20, 1, -20), base.add(25, 12, 25))) {
                fixture.changed.putIfAbsent(at.toImmutable(), context.getWorld().getBlockState(at));
            }
            for (int i = 0; i < 6; i++) {
                var chest = base.add(-5 + i * 2, 20, -5);
                fixture.set(chest, Blocks.CHEST.getDefaultState());
                chests.add(chest);
            }
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                WorldChunk chunk = context.getWorld().getWorldChunk(base.add(dx * 16, 0, dz * 16));
                if (chunk == fixture.chunk) continue;
                receipts.put(chunk, chunk.getAttached(MaeGenerationEvidence.RECEIPT));
                chunk.removeAttached(MaeGenerationEvidence.RECEIPT);
                ServerChunkEvents.CHUNK_GENERATE.invoker().onChunkGenerate(context.getWorld(), chunk);
            }
            fixture.capture();
            stock(model, copies);
            writer = new MaeWorldWriter(fixture.store, sources());
        }

        UUID colony() { return fixture.colony.colony().id(); }

        MaeWorldWriter writer() { return writer; }

        void await(UUID regionId, Runnable then) {
            guarded(() -> {
                if (MaeSites.active().stream().anyMatch(active -> active.regionId().equals(regionId))) {
                    context.waitAndRun(1, () -> await(regionId, then));
                    return;
                }
                var job = fixture.store.region(regionId).orElseThrow().getCompound("job");
                context.assertTrue(MaeRoadExecution.TYPE.equals(job.getString("type")) && "complete".equals(job.getString("state")),
                        "obra não terminou ligada à rua: " + job.getString("type") + " " + job.getString("state")
                                + " " + job.getString("reason"));
                then.run();
            });
        }

        /** Material de {@code copies} casas, pela regra de peça do contrato 1. */
        private void stock(ResourceId model, int copies) {
            var project = ConstructionProject.plan(colony(),
                    StructureBlueprintReader.readForMae(context.getWorld(), model).orElseThrow(), new ColonyPos(0, 100, 0));
            Map<Item, Integer> needed = new LinkedHashMap<>();
            for (var piece : MaeStructurePieces.ordered(project)) {
                MaeStructureMaterials.of(piece).forEach((item, count) -> needed.merge(item, count * copies, Integer::sum));
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
            var where = chests.stream().map(MinecraftTypeAdapter::toColonyPos).toList();
            return new MaeStructureExecution.Sources() {
                @Override public Optional<Blueprint> template(ServerWorld world, ResourceId resource) {
                    return StructureBlueprintReader.readForMae(world, resource);
                }

                @Override public List<ColonyPos> chests(ServerWorld world, UUID colonyId, ColonyPos from) {
                    return where;
                }
            };
        }

        void guarded(Runnable step) {
            try {
                step.run();
            } catch (RuntimeException | AssertionError failure) {
                cleanup();
                throw failure;
            }
        }

        void cleanup() {
            for (var region : fixture.store.regions()) MaeSites.stop(region.getUuid("id"));
            for (BlockPos chest : chests) {
                if (context.getWorld().getBlockEntity(chest) instanceof ChestBlockEntity box) box.clear();
            }
            for (BlockPos at : BlockPos.iterate(fixture.base.add(-20, 1, -20), fixture.base.add(25, 12, 25))) {
                if (context.getWorld().getBlockEntity(at) instanceof net.minecraft.inventory.Inventory box) box.clear();
            }
            fixture.cleanup();
            receipts.forEach((chunk, receipt) -> {
                if (receipt == null) chunk.removeAttached(MaeGenerationEvidence.RECEIPT);
                else chunk.setAttached(MaeGenerationEvidence.RECEIPT, receipt);
            });
        }
    }
}
