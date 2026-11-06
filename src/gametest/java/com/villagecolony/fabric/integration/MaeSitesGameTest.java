package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.data.save.MaeWorkStore;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.ConstructionCancellation;
import com.villagecolony.fabric.work.ConstructionPlanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A obra pedida é a próxima do construtor e sempre termina — Regra 213, ADR-047. O servidor de GameTest roda com
 * {@code randomTickSpeed} 0 e sem jogador: {@link #nature} chama os tiques aleatórios na mão, 3 por seção, como o {@code ServerWorld}.
 */
public final class MaeSitesGameTest implements FabricGameTest {
    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    private static final int RANDOM_TICKS_PER_SECTION = 3;

    private static Arena arenaWithBuilder(TestContext context) {
        var arena = new Arena(context, HOUSE, false);

        arena.fixture.colony.equippedWorker(context, ProfessionType.BUILDER, new BlockPos(1, 2, 1));

        return arena;
    }

    private static void end(TestContext context, Arena arena) {
        clear(context.getWorld(), arena);
        arena.cleanup();
        context.complete();
    }

    private static void clear(ServerWorld world, Arena arena) {
        UUID colony = arena.fixture.colony.colony().id();

        MaeBuildQueue.ofColony(world, colony).forEach(request -> MaeBuildQueue.remove(world, request.id()));
        VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony);
        VillageColonyMod.BUILDINGS.removeOfColony(colony);
    }

    private static MaeBuildQueue.Request onlyRequest(TestContext context, Arena arena) {
        List<MaeBuildQueue.Request> requests = MaeBuildQueue.ofColony(context.getWorld(), arena.fixture.colony.colony().id());

        context.assertTrue(requests.size() == 1, "Regra 213: esperava um pedido na fila: " + requests);

        return requests.getFirst();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_next", tickLimit = 40)
    public void aRequestIsTheBuildersNextWork(TestContext context) {
        var arena = arenaWithBuilder(context);

        arena.guarded(() -> {
            var colony = arena.fixture.colony.colony();
            var status = MaeSites.start(context.getWorld(), colony.id(), HOUSE, arena.origin, 0, null);

            context.assertTrue(status.phase() == MaeSites.Phase.QUEUED, "Regra 213: o pedido não entrou na fila: " + status);

            Optional<ConstructionProject> opened = ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(opened.isPresent() && opened.get().blueprint().id().equals(HOUSE),
                    "Regra 213: a obra pedida não foi a próxima: " + opened);
            context.assertTrue(opened.get().state() == ConstructionState.BUILDING, "a obra pedida não está com o construtor");
            context.assertTrue(VillageColonyMod.TASKS.ofColony(colony.id()).stream()
                    .anyMatch(task -> task.type() == TaskType.BUILD && task.isOpen()), "Regra 213: o construtor ficou sem tarefa");
            context.assertTrue(onlyRequest(context, arena).opened(), "o pedido não sabe que abriu");
            end(context, arena);
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_wait", tickLimit = 40)
    public void aRequestWaitsForTheWorkInHand(TestContext context) {
        var arena = arenaWithBuilder(context);

        arena.guarded(() -> {
            var colony = arena.fixture.colony.colony();
            ConstructionProject inHand = ConstructionProject.plan(colony.id(), arena.project.blueprint(),
                    MinecraftTypeAdapter.toColonyPos(arena.fixture.base.add(30, 0, 30)));

            VillageColonyMod.CONSTRUCTIONS.register(inHand);
            inHand.moveTo(ConstructionState.PREPARING);
            inHand.moveTo(ConstructionState.BUILDING);

            MaeSites.start(context.getWorld(), colony.id(), HOUSE, arena.origin, 0, null);
            ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertFalse(onlyRequest(context, arena).opened(), "Regra 213: a obra pedida abriu por cima da obra em andamento");

            VillageColonyMod.CONSTRUCTIONS.forget(inHand.id(), com.villagecolony.core.construction.service.RemovalAudit.playerCancellation());
            ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(onlyRequest(context, arena).opened(), "Regra 213: a vaga abriu e a obra pedida não entrou");
            end(context, arena);
        });
    }

    /** A grama sob a obra morre e a terra em volta vira grama: era o que parava a obra MAE no jogo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_living", tickLimit = 400)
    public void aRequestSurvivesTheLivingWorldAndFinishes(TestContext context) {
        var arena = arenaWithBuilder(context);

        arena.guarded(() -> {
            var colony = arena.fixture.colony.colony();

            for (BlockPos at : BlockPos.iterate(arena.fixture.base.add(-5, 0, -5), arena.fixture.base.add(10, 0, 10))) {
                arena.fixture.set(at.toImmutable(), Blocks.GRASS_BLOCK.getDefaultState());
            }

            arena.fixture.capture();
            MaeSites.start(context.getWorld(), colony.id(), HOUSE, arena.origin, 0, null);

            ConstructionProject project = ConstructionPlanner.plan(context.getWorld(), colony).orElseThrow();

            // O chão muda enquanto a obra está aberta, como no jogo.
            for (BlockPos at : BlockPos.iterate(arena.origin.add(0, 0, 0), arena.origin.add(3, 0, 3))) {
                context.getWorld().setBlockState(at, Blocks.COBBLESTONE.getDefaultState());
            }

            living(context, arena, project, 200);
        });
    }

    private static void living(TestContext context, Arena arena, ConstructionProject project, int ticksLeft) {
        arena.guarded(() -> {
            var colony = arena.fixture.colony.colony();

            nature(context.getWorld(), arena.fixture.base);

            if (ticksLeft > 0) {
                if (ticksLeft % 40 == 0) {
                    ConstructionPlanner.plan(context.getWorld(), colony);
                    context.assertTrue(VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).filter(open -> open.id().equals(project.id())).isPresent(),
                            "Regra 213: a obra pedida foi largada");
                }

                context.waitAndRun(1, () -> living(context, arena, project, ticksLeft - 1));
                return;
            }

            for (var block : List.copyOf(project.blueprint().blocks())) {
                project.markPlaced(block);
            }

            ConstructionPlanner.plan(context.getWorld(), colony);
            ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                    .anyMatch(building -> building.finished() && building.blueprint().equals(HOUSE)), "Regra 213: a obra pedida não virou edifício");
            context.assertTrue(MaeBuildQueue.ofColony(context.getWorld(), colony.id()).isEmpty(), "Regra 213: a obra terminou e o pedido ficou");
            end(context, arena);
        });
    }

    private static void nature(ServerWorld world, BlockPos base) {
        int chunkX = base.getX() >> 4;
        int chunkZ = base.getZ() >> 4;

        for (int sectionY = base.getY() >> 4; sectionY <= (base.getY() + 12) >> 4; sectionY++) {
            for (int tick = 0; tick < RANDOM_TICKS_PER_SECTION; tick++) {
                BlockPos at = new BlockPos((chunkX << 4) + world.random.nextInt(16), (sectionY << 4) + world.random.nextInt(16),
                        (chunkZ << 4) + world.random.nextInt(16));
                BlockState state = world.getBlockState(at);

                if (state.hasRandomTicks()) {
                    state.randomTick(world, at, world.random);
                }
            }
        }
    }

    /** Longe de tudo e sem material: uma obra comum seria largada; a pedida fica até terminar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_far", tickLimit = 40)
    public void aRequestFarFromTheVillageIsNotDropped(TestContext context) {
        var arena = arenaWithBuilder(context);

        arena.guarded(() -> {
            var colony = arena.fixture.colony.colony();
            ConstructionProject far = ConstructionProject.plan(colony.id(), arena.project.blueprint(),
                    MinecraftTypeAdapter.toColonyPos(arena.fixture.base.add(4_000, 0, 4_000)));
            BlockPos origin = MinecraftTypeAdapter.toBlockPos(far.origin());

            MaeBuildQueue.add(context.getWorld(), new MaeBuildQueue.Request(UUID.randomUUID(), colony.id(), HOUSE, origin, origin, 0, null, true));
            VillageColonyMod.CONSTRUCTIONS.register(far);
            far.moveTo(ConstructionState.PREPARING);
            far.moveTo(ConstructionState.BUILDING);

            ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent(), "Regra 213: a obra pedida longe foi largada");
            end(context, arena);
        });
    }

    /** Sumiu sem terminar (reparo, save, outra versão): volta para a fila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_back", tickLimit = 40)
    public void aRequestWhoseWorkVanishedComesBack(TestContext context) {
        var arena = arenaWithBuilder(context);

        arena.guarded(() -> {
            var colony = arena.fixture.colony.colony();

            MaeSites.start(context.getWorld(), colony.id(), HOUSE, arena.origin, 0, null);

            ConstructionProject first = ConstructionPlanner.plan(context.getWorld(), colony).orElseThrow();

            VillageColonyMod.CONSTRUCTIONS.forget(first.id(), com.villagecolony.core.construction.service.RemovalAudit.playerCancellation());
            ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent() && onlyRequest(context, arena).opened(),
                    "Regra 213: a obra sumiu e o pedido não voltou");
            end(context, arena);
        });
    }

    /** A tocha das almas é o jogador cancelando (Regra 108): a obra não volta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_soul", tickLimit = 40)
    public void theSoulTorchCancelsTheRequestToo(TestContext context) {
        var arena = arenaWithBuilder(context);

        arena.guarded(() -> {
            var colony = arena.fixture.colony.colony();

            MaeSites.start(context.getWorld(), colony.id(), HOUSE, arena.origin, 0, null);

            ConstructionProject opened = ConstructionPlanner.plan(context.getWorld(), colony).orElseThrow();
            BlockPos marker = MinecraftTypeAdapter.toBlockPos(Building.of(opened).min());

            context.getWorld().setBlockState(marker, Blocks.SOUL_TORCH.getDefaultState());

            context.assertTrue(ConstructionCancellation.cancelAtSoulTorch(context.getWorld(), marker), "a tocha não cancelou a obra");
            context.assertTrue(MaeBuildQueue.ofColony(context.getWorld(), colony.id()).isEmpty(), "Regra 108: o pedido ficou e a obra voltaria");
            end(context, arena);
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_save", tickLimit = 20)
    public void theQueueSurvivesTheSave(TestContext context) {
        BlockPos floor = new BlockPos(10, 64, -20);
        BlockPos origin = floor.down(2);
        var request = new MaeBuildQueue.Request(UUID.randomUUID(), UUID.randomUUID(), HOUSE, floor, origin, 3, UUID.randomUUID(), true);
        MaeWorkStore before = new MaeWorkStore(() -> { });

        before.setRequests(List.of(request.toNbt()));

        NbtCompound saved = new NbtCompound();

        before.write(saved);

        MaeWorkStore after = new MaeWorkStore(() -> { });

        after.read(saved);

        context.assertTrue(after.supported(), "o save com a fila não é mais lido");
        context.assertTrue(MaeBuildQueue.Request.of(after.requests().getFirst()).equals(request), "Regra 213: o pedido mudou no save");
        context.complete();
    }

    /** A obra que uma versão anterior deixou numa região vira pedido, e a região volta para a vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_old", tickLimit = 40)
    public void anOldRegionBecomesARequest(TestContext context) {
        var arena = arenaWithBuilder(context);

        arena.guarded(() -> {
            var colony = arena.fixture.colony.colony();
            var store = com.villagecolony.data.save.ColonySavedData.get(context.getWorld().getServer()).maeWork();
            NbtCompound saved = new NbtCompound();
            NbtCompound work = new NbtCompound();
            NbtCompound region = new NbtCompound();
            NbtCompound site = new NbtCompound();
            net.minecraft.nbt.NbtList regions = new net.minecraft.nbt.NbtList();

            site.putString("namespace", HOUSE.namespace());
            site.putString("path", HOUSE.path());
            site.putLong("origin", arena.origin.asLong());
            site.putInt("rotation", 0);
            region.putUuid("id", UUID.randomUUID());
            region.putUuid("colony", colony.id());
            region.put(MaeSiteRecord.KEY, site);
            regions.add(region);
            work.putInt("version", 3);
            work.put("regions", regions);
            saved.put(MaeWorkStore.KEY, work);
            store.read(saved);

            MaeSites.retireOldRegions(context.getWorld().getServer());

            context.assertTrue(onlyRequest(context, arena).model().equals(HOUSE), "Regra 213: a obra antiga não virou pedido");
            context.assertTrue(store.regions().isEmpty(), "a região antiga continuou no save");
            end(context, arena);
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_refuse", tickLimit = 40)
    public void requestsAreRefusedWithTheirReason(TestContext context) {
        var arena = new Arena(context, HOUSE, false);

        arena.guarded(() -> {
            UUID colony = arena.fixture.colony.colony().id();

            refused(context, () -> MaeSites.start(context.getWorld(), colony,
                    ResourceId.vanilla("village/plains/houses/no_such_house"), arena.origin, 0, null), "unknown_model");
            refused(context, () -> MaeSites.start(context.getWorld(), colony, HOUSE, arena.origin, 4, null), "invalid_rotation");

            var first = MaeSites.start(context.getWorld(), colony, HOUSE, arena.origin, 0, null);

            refused(context, () -> MaeSites.start(context.getWorld(), colony, HOUSE, arena.origin, 0, null), "overlapping_request");
            context.assertTrue(MaeSites.stop(context.getWorld(), first.id()) && !MaeSites.stop(context.getWorld(), first.id()),
                    "parar não tirou a obra da fila");
            end(context, arena);
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_sites_search", tickLimit = 40)
    public void searchFindsALevelLotNearTheRequest(TestContext context) {
        var arena = new Arena(context, HOUSE, false);

        arena.guarded(() -> {
            var status = MaeSites.startNear(context.getWorld(), arena.fixture.colony.colony().id(), HOUSE, arena.fixture.base, 0, null);

            // O único chão nivelado no alcance é a plataforma da arena, logo abaixo da origem do lote.
            context.assertTrue(status.origin().getY() == arena.fixture.base.getY() + 1,
                    "busca escolheu lote fora da plataforma: " + status.origin().toShortString());
            end(context, arena);
        });
    }

    private static void refused(TestContext context, Runnable action, String reason) {
        try {
            action.run();
        } catch (IllegalStateException | IllegalArgumentException failure) {
            context.assertTrue(reason.equals(failure.getMessage()), "recusa incorreta: " + failure.getMessage());
            return;
        }
        context.assertTrue(false, "pedido deveria recusar: " + reason);
    }

    /** Uma arena longe das colônias de outros testes, no chunk com o recibo de geração recapturado. */
    private static final class Arena {
        private static final java.util.concurrent.atomic.AtomicInteger NEXT = new java.util.concurrent.atomic.AtomicInteger();
        final TestContext context;
        final Fixture fixture;
        final BlockPos origin;
        final ConstructionProject project;

        Arena(TestContext context, ResourceId model, boolean unused) {
            this.context = context;
            this.fixture = new Fixture(context, NEXT.getAndIncrement());
            this.origin = fixture.base.add(-1, 1, -1);
            this.project = ConstructionProject.plan(fixture.colony.colony().id(), MaeSites.blueprint(context.getWorld(), model, 0),
                    MinecraftTypeAdapter.toColonyPos(origin));

            for (BlockPos at : BlockPos.iterate(fixture.base.add(-5, -2, -5), fixture.base.add(10, 0, 10))) {
                fixture.set(at.toImmutable(), Blocks.DIRT.getDefaultState());
            }

            fixture.capture();
        }

        void guarded(Runnable step) {
            try {
                step.run();
            } catch (RuntimeException | AssertionError failure) {
                clear(context.getWorld(), this);
                cleanup();
                throw failure;
            }
        }

        void cleanup() {
            fixture.cleanup();
        }
    }

    private static final class Fixture {
        final TestContext context;
        final com.villagecolony.gametest.ColonyFixture colony;
        final net.minecraft.world.chunk.WorldChunk chunk;
        final BlockPos base;
        final NbtCompound originalReceipt;
        final java.util.Map<BlockPos, BlockState> changed = new java.util.HashMap<>();

        Fixture(TestContext context, int arena) {
            this.context = context;
            base = new BlockPos(100_005 + arena * 256, context.getAbsolutePos(new BlockPos(1, 40, 1)).getY(), 100_005);
            chunks(true);
            chunk = context.getWorld().getWorldChunk(base);
            originalReceipt = chunk.getAttached(MaeGenerationEvidence.RECEIPT);
            colony = com.villagecolony.gametest.ColonyFixture.colonyAt(context, base.subtract(context.getAbsolutePos(BlockPos.ORIGIN)));
        }

        void set(BlockPos pos, BlockState state) {
            changed.putIfAbsent(pos, context.getWorld().getBlockState(pos));
            context.getWorld().setBlockState(pos, state, net.minecraft.block.Block.NOTIFY_LISTENERS | net.minecraft.block.Block.FORCE_STATE);
        }

        void capture() {
            chunk.removeAttached(MaeGenerationEvidence.RECEIPT);
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_GENERATE.invoker().onChunkGenerate(context.getWorld(), chunk);
        }

        void cleanup() {
            changed.forEach((pos, state) -> context.getWorld().setBlockState(pos, state, net.minecraft.block.Block.NOTIFY_LISTENERS | net.minecraft.block.Block.FORCE_STATE));

            if (originalReceipt == null) {
                chunk.removeAttached(MaeGenerationEvidence.RECEIPT);
            } else {
                chunk.setAttached(MaeGenerationEvidence.RECEIPT, originalReceipt);
            }

            colony.cleanUp();
            chunks(false);
        }

        private void chunks(boolean force) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    context.getWorld().setChunkForced((base.getX() >> 4) + dx, (base.getZ() >> 4) + dz, force);
                }
            }
        }
    }
}
