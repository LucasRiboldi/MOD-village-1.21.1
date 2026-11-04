package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.data.save.MaeWorkStore;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Nenhuma entrada legada altera ou adota seções supervisionadas — ADR-044, MAE Fase 2. */
public class MaeRoadGateGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_foreign_metadata", tickLimit = 20)
    public void neighbouringColonyCannotRediscoverSupervisedRoad(TestContext context) {
        try (Fixture f = new Fixture(context)) {
            f.supervise(f.base.add(15, 15, 15), 2);
            context.assertFalse(MaeRoadAccess.metadataAllowed(f.world, UUID.randomUUID()),
                    "outra colônia pode adotar trecho supervisionado incerto");
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_distant_vertical", tickLimit = 20)
    public void siteOpeningDoesNotBlockUnreachableSectionFarAboveTheCut(TestContext context) {
        try (Fixture f = new Fixture(context)) {
            BlockPos door = f.base.add(4, 4, 4);
            f.ground(door);
            f.supervise(f.base.add(15, 95, 15), 2);
            var site = new BuildSiteScanner.Site(pos(door), Direction.SOUTH, new ColonyPos(3, 3, 3), List.of(pos(door)));
            invoke("com.villagecolony.fabric.work.SiteOpening", "pavePathTo",
                    new Class<?>[] {ServerWorld.class, UUID.class, BuildSiteScanner.Site.class}, f.world, f.colony, site);
            context.assertTrue(f.world.getBlockState(door).isOf(Blocks.DIRT_PATH),
                    "reserva acima do alcance real bloqueou caminho plano");
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_road_gates", tickLimit = 20)
    public void supervisionUsesWholeSectionsInBothVersionsAndOnlyItsDimension(TestContext context) {
        try (Fixture f = new Fixture(context)) {
            // A versão 3 leva uma obra structure_v3; a caixa da região continua sendo a fronteira — ADR-046 P1.
            for (int version : List.of(1, 2, 3)) {
                for (BlockPos low : List.of(f.base, new BlockPos(-32, -32, -32))) {
                    BlockPos corner = low.add(15, 15, 15);
                    f.supervise(corner, version);
                    context.assertFalse(MaeRoadAccess.legacyAllowed(f.world, low, low),
                            "posição fora da caixa física na mesma seção foi liberada, v" + version);
                    context.assertTrue(MaeRoadAccess.legacyAllowed(f.world, low.west(), low.west()),
                            "seção vizinha sem reserva foi bloqueada");
                    context.assertTrue(MaeRoadAccess.legacyAllowed(f.world, low.down(), low.down()),
                            "seção vertical vizinha sem reserva foi bloqueada");
                    context.assertFalse(MaeRoadAccess.legacyAllowed(f.world, low.west(), low),
                            "volume que cruza a borda da seção foi liberado");
                }
                NbtCompound foreign = f.region(f.base, version);
                foreign.putString("dimension", "minecraft:the_nether");
                f.install(foreign, version);
                context.assertTrue(MaeRoadAccess.legacyAllowed(f.world, f.base, f.base), "dimensão diferente bloqueou legado");
                context.assertTrue(MaeRoadAccess.metadataAllowed(f.world, f.colony), "dimensão diferente bloqueou metadados");
            }
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_road_gates", tickLimit = 20)
    public void unknownPayloadAndMalformedRegionsFailClosedWithoutRewritingThem(TestContext context) {
        try (Fixture f = new Fixture(context)) {
            NbtCompound future = new NbtCompound();
            future.putInt("version", 99);
            future.put("regions", new NbtList());
            f.payload(future);
            assertClosed(context, f);
            NbtCompound opaque = new NbtCompound();
            f.store.write(opaque);
            context.assertTrue(opaque.getCompound(MaeWorkStore.KEY).equals(future), "consulta reescreveu payload futuro");
            for (String field : List.of("id", "colony", "dimension", "area", "proof", "created", "history", "ownedSections")) {
                NbtCompound malformed = f.region(f.base, 1);
                malformed.remove(field);
                f.install(malformed, 1);
                assertClosed(context, f);
            }
            NbtCompound inverted = f.region(f.base, 2);
            inverted.getCompound("area").putInt("maxX", f.base.getX() - 1);
            f.install(inverted, 2);
            assertClosed(context, f);
            NbtCompound policy = f.region(f.base, 2);
            policy.putString("policy", "unknown_future_policy");
            f.install(policy, 2);
            assertClosed(context, f);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_road_gates", tickLimit = 20)
    public void roadStretchIsRejectedBeforeAdoptionOrEdgesWhenOnlySideSectionIsSupervised(TestContext context) {
        try (Fixture f = new Fixture(context)) {
            BlockPos end = f.base.add(15, 4, 4);
            for (int step = 1; step <= RoadExtension.STRETCH; step++) {
                for (int side = -1; side <= 1; side++) f.ground(end.add(side, 0, step));
            }
            f.set(end.south(), Blocks.DIRT_PATH.getDefaultState());
            f.supervise(f.base.add(31, 15, 15), 2);
            var result = RoadPaving.pave(f.world, f.colony, new RoadExtension.End(end, Direction.SOUTH, 0), Blocks.DIRT_PATH);
            context.assertTrue(result.isEmpty(), "trecho supervisionado alegou sucesso");
            f.assertUnchanged(context);
            f.assertNoAdoption(context);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_road_gates", tickLimit = 20)
    public void pathStepIsRejectedBeforeCutAndClearanceInSectionAboveFloor(TestContext context) {
        try (Fixture f = new Fixture(context)) {
            BlockPos floor = f.base.add(4, 15, 4);
            f.ground(floor);
            f.set(floor.up(), Blocks.DIRT.getDefaultState());
            f.set(floor.up(2), Blocks.SHORT_GRASS.getDefaultState());
            f.supervise(f.base.add(15, 31, 15), 2);
            Object result = invoke("com.villagecolony.fabric.work.PathPaving", "pave",
                    new Class<?>[] {ServerWorld.class, UUID.class, int.class, int.class, int.class, int.class, boolean.class},
                    f.world, f.colony, floor.getX(), floor.getZ(), floor.getY() + 1, floor.getY(), false);
            context.assertTrue(Boolean.FALSE.equals(result), "caminho alegou sucesso com corte supervisionado");
            f.assertUnchanged(context);
            f.assertNoAdoption(context);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_road_gates", tickLimit = 20)
    public void workPathRejectsWholeOperationBeforeWritingEarlierUnsupervisedCell(TestContext context) {
        try (Fixture f = new Fixture(context)) {
            BlockPos safe = f.base.add(15, 4, 4);
            BlockPos blocked = safe.east();
            f.ground(safe);
            f.ground(blocked);
            f.supervise(f.base.add(31, 15, 15), 1);
            Object result = invoke(WorkPath.class.getName(), "pave",
                    new Class<?>[] {ServerWorld.class, UUID.class, List.class, ColonyPos.class},
                    f.world, f.colony, List.of(blocked, safe), pos(safe));
            context.assertTrue(Integer.valueOf(0).equals(result), "corredor escreveu antes de recusar parte supervisionada");
            f.assertUnchanged(context);
            f.assertNoAdoption(context);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_road_gates", tickLimit = 20)
    public void builderAndCallerRefuseBeforeClearanceDropsSwingAndSuccess(TestContext context) throws ReflectiveOperationException {
        try (Fixture f = new Fixture(context)) {
            BlockPos target = f.base.add(4, 4, 4);
            f.ground(target);
            f.set(target.north(), Blocks.DIRT_PATH.getDefaultState());
            f.set(target.south(), Blocks.DIRT_PATH.getDefaultState());
            f.supervise(f.base.add(15, 15, 15), 1);
            String type = "com.villagecolony.fabric.work.BuilderPaving";
            context.assertTrue("".equals(invoke(type, "pave", new Class<?>[] {ServerWorld.class, BlockPos.class}, f.world, target)),
                    "construtor alegou pavimentação supervisionada");
            context.assertTrue("".equals(invoke(type, "clear", new Class<?>[] {ServerWorld.class, UUID.class, BlockPos.class},
                    f.world, UUID.randomUUID(), target.up())), "limpeza supervisionada alegou sucesso");
            VillagerEntity builder = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
            try {
                builder.refreshPositionAndAngles(target.getX() + .5, target.getY() + 1, target.getZ() + .5, 0, 0);
                Class<?> jobType = Class.forName(type + "$Job");
                var constructor = jobType.getDeclaredConstructor(BlockPos.class, boolean.class);
                constructor.setAccessible(true);
                for (boolean weed : List.of(false, true)) {
                    invoke(type, "step", new Class<?>[] {ServerWorld.class, VillagerEntity.class, jobType},
                            f.world, builder, constructor.newInstance(target, weed));
                }
                context.assertFalse(builder.handSwinging, "chamador confirmou ação por animação depois da recusa");
            } finally {
                builder.discard();
            }
            f.assertUnchanged(context);
            f.assertNoAdoption(context);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_road_gates", tickLimit = 20)
    public void siteOpeningRejectsLateralCutBeforeRemovingGroundOrPlants(TestContext context) {
        try (Fixture f = new Fixture(context)) {
            BlockPos door = f.base.add(15, 4, 4);
            for (BlockPos floor : List.of(door, door.south(), door.east().up())) {
                f.ground(floor);
                f.set(floor.up(), Blocks.DIRT.getDefaultState());
                f.set(floor.up(2), Blocks.SHORT_GRASS.getDefaultState());
            }
            f.supervise(f.base.add(31, 15, 15), 2);
            var site = new BuildSiteScanner.Site(pos(door), Direction.SOUTH, new ColonyPos(3, 3, 3),
                    List.of(pos(door), pos(door.south())));
            invoke("com.villagecolony.fabric.work.SiteOpening", "pavePathTo",
                    new Class<?>[] {ServerWorld.class, UUID.class, BuildSiteScanner.Site.class}, f.world, f.colony, site);
            f.assertUnchanged(context);
            f.assertNoAdoption(context);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_road_gates", tickLimit = 20)
    public void streetGapRefusesBeforeFirstHoleAndWornSurfaceRemainsUntouched(TestContext context) {
        try (Fixture f = new Fixture(context)) {
            BlockPos street = f.base.add(14, 4, 4);
            for (int step = 0; step <= 3; step++) f.ground(street.east(step));
            f.set(street, Blocks.DIRT_PATH.getDefaultState());
            f.set(street.east(3), Blocks.DIRT_PATH.getDefaultState());
            f.supervise(f.base.add(31, 15, 15), 1);
            context.assertTrue(StreetGaps.fill(f.world, f.colony, street, Direction.EAST) == 0, "falha foi parcialmente preenchida");
            f.assertUnchanged(context);
            f.assertNoAdoption(context);
            BlockPos worn = street.east(2);
            f.set(worn, Blocks.COARSE_DIRT.getDefaultState());
            SweepState.ROADS.put(f.colony, new ColonyRoads(f.colony, pos(street), List.of(column(worn))));
            context.assertFalse(StreetSurface.unworn(f.world, worn), "manutenção supervisionada alegou sucesso");
            context.assertTrue(f.world.getBlockState(worn).isOf(Blocks.COARSE_DIRT), "manutenção alterou piso supervisionado");
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_road_gates", tickLimit = 20)
    public void reloadedReservationDefersMetadataAndPreservesConfirmedIndexAndGraph(TestContext context) throws ReflectiveOperationException {
        try (Fixture f = new Fixture(context)) {
            f.supervise(f.base.add(15, 15, 15), 2);
            NbtCompound disk = new NbtCompound();
            f.store.write(disk);
            f.store.read(disk);
            ColonyRoads confirmed = new ColonyRoads(f.colony, pos(f.base), List.of(column(f.base)));
            SweepState.ROADS.put(f.colony, confirmed);
            SweepState.BUILDING.put(f.colony, new HashSet<>(Set.of(column(f.base.east()))));
            context.assertTrue(RoadNetwork.indexIfMissing(f.world, f.colony).isEmpty(), "rede publicou redescoberta supervisionada");
            RoadIndex.indexWhatWasSeen(f.world, f.colony, pos(f.base));
            context.assertTrue(RoadIndex.roadsOf(f.colony).orElseThrow().equals(confirmed), "reconstrução substituiu índice confirmado");
            context.assertFalse(SweepState.BUILDING.containsKey(f.colony), "varredura parcial ficou disponível para adoção posterior");
            var graphs = ColonySavedData.get(f.world.getServer()).roadGraphs();
            var payloadsField = graphs.getClass().getDeclaredField("payloads");
            payloadsField.setAccessible(true);
            try {
                RoadGraph graph = RoadGraph.from(Set.of(pos(f.base)), pos(f.base), Set.of());
                graphs.update(f.colony, graph);
                Object roads = invoke("com.villagecolony.fabric.work.RoadGraphActivation", "roads",
                        new Class<?>[] {ServerWorld.class, Colony.class}, f.world, Colony.create(f.colony, pos(f.base)));
                context.assertTrue(Optional.of(Set.of(column(f.base))).equals(roads), "ativação não retornou somente grafo confirmado");
                context.assertTrue(graphs.graph(f.colony).orElseThrow().equals(graph), "ativação migrou ou substituiu grafo confirmado");
                ((Map<?, ?>) payloadsField.get(graphs)).remove(f.colony);
                context.assertTrue(Optional.of(Set.of()).equals(invoke("com.villagecolony.fabric.work.RoadGraphActivation", "roads",
                        new Class<?>[] {ServerWorld.class, Colony.class}, f.world, Colony.create(f.colony, pos(f.base)))),
                        "ativação redescobriu índice legado sem grafo confirmado");
            } finally {
                ((Map<?, ?>) payloadsField.get(graphs)).remove(f.colony);
            }
        }
        context.complete();
    }

    private static void assertClosed(TestContext context, Fixture f) {
        context.assertFalse(MaeRoadAccess.legacyAllowed(f.world, f.base, f.base), "payload incerto liberou legado");
        context.assertFalse(MaeRoadAccess.metadataAllowed(f.world, UUID.randomUUID()), "payload incerto liberou reconstrução");
    }

    private static Object invoke(String type, String name, Class<?>[] signature, Object... args) {
        try {
            Method method = Class.forName(type).getDeclaredMethod(name, signature);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(type + "." + name, failure);
        }
    }

    private static ColonyPos pos(BlockPos at) { return new ColonyPos(at.getX(), at.getY(), at.getZ()); }
    private static long column(BlockPos at) { return ColonyRoads.column(at.getX(), at.getZ()); }

    private static final class Fixture implements AutoCloseable {
        final ServerWorld world;
        final MaeWorkStore store;
        final UUID colony = UUID.randomUUID();
        final BlockPos base;
        final NbtCompound original = new NbtCompound();
        final Map<BlockPos, BlockState> before = new HashMap<>();
        final Map<BlockPos, BlockState> expected = new HashMap<>();

        Fixture(TestContext context) {
            world = context.getWorld();
            store = ColonySavedData.get(world.getServer()).maeWork();
            store.write(original);
            store.read(new NbtCompound());
            BlockPos origin = context.getAbsolutePos(new BlockPos(1, 64, 1));
            base = new BlockPos(origin.getX() & ~15, origin.getY() & ~15, origin.getZ() & ~15);
            SweepState.BUILDING.put(colony, new HashSet<>());
        }

        NbtCompound region(BlockPos at, int version) {
            NbtCompound region = new NbtCompound();
            region.putUuid("id", UUID.randomUUID());
            region.putUuid("colony", colony);
            region.putString("dimension", world.getRegistryKey().getValue().toString());
            region.putString("proof", "generation_sections_sha256_v1");
            region.putLong("created", world.getTime());
            region.put("area", MaeWorldWriter.encode(new com.villagecolony.core.construction.model.WorkArea(pos(at), pos(at))));
            region.put("history", new NbtList());
            region.put("ownedSections", new NbtCompound());
            if (version >= 2) region.putString("policy", "terrain_v2");
            if (version == 3) {
                NbtCompound job = new NbtCompound();
                job.putString("type", "structure_v3");
                job.putInt("contract", 1);
                job.putString("state", "approved");
                region.put("job", job);
            }
            return region;
        }

        void supervise(BlockPos at, int version) { install(region(at, version), version); }

        void install(NbtCompound region, int version) {
            NbtList regions = new NbtList();
            regions.add(region);
            NbtCompound payload = new NbtCompound();
            payload.putInt("version", version);
            payload.put("regions", regions);
            payload(payload);
        }

        void payload(NbtCompound payload) {
            NbtCompound root = new NbtCompound();
            root.put(MaeWorkStore.KEY, payload);
            store.read(root);
        }

        void ground(BlockPos at) {
            set(at.down(), Blocks.STONE.getDefaultState());
            set(at, Blocks.GRASS_BLOCK.getDefaultState());
            set(at.up(), Blocks.SHORT_GRASS.getDefaultState());
            set(at.up(2), Blocks.AIR.getDefaultState());
        }

        void set(BlockPos at, BlockState state) {
            before.putIfAbsent(at, world.getBlockState(at));
            world.setBlockState(at, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            expected.put(at, state);
        }

        void assertUnchanged(TestContext context) {
            expected.forEach((at, state) -> context.assertTrue(world.getBlockState(at).equals(state), "recusa alterou " + at));
        }

        void assertNoAdoption(TestContext context) {
            context.assertTrue(SweepState.BUILDING.get(colony).isEmpty(), "recusa adotou coluna no índice");
            context.assertTrue(RoadIndex.roadsOf(colony).isEmpty(), "recusa publicou índice");
        }

        @Override
        public void close() {
            before.forEach((at, state) -> world.setBlockState(at, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE));
            store.read(original);
            SweepState.BUILDING.remove(colony);
            SweepState.ROADS.remove(colony);
        }
    }
}
