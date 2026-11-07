package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.ConstructionPlanner;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** A vila sem lote chama o MAE sozinha, perto da caixa e longe da água — Regra 222, ADR-049. */
public final class MaeRescueGameTest implements FabricGameTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");
    private static final AtomicInteger NEXT = new AtomicInteger();
    private static final int HALF_BOX = 5;

    /** Uma vila de caixa pequena em chão plano, longe das arenas dos outros testes. */
    private static final class Village {
        final TestContext context;
        final ServerWorld world;
        final BlockPos center;
        final ColonyFixture fixture;
        final Colony colony;
        final Blueprint house;
        final VillageBounds box;
        final Map<BlockPos, net.minecraft.block.BlockState> changed = new HashMap<>();

        Village(TestContext context) {
            this.context = context;
            world = context.getWorld();
            center = new BlockPos(300_008 + NEXT.getAndIncrement() * 256, context.getAbsolutePos(new BlockPos(1, 1, 1)).getY(), 300_008);
            chunks(true);

            for (BlockPos at : BlockPos.iterate(center.add(-40, 0, -40), center.add(40, 0, 40))) {
                set(at.toImmutable(), Blocks.DIRT.getDefaultState());
            }

            fixture = ColonyFixture.colonyAtWorld(center.up());
            colony = fixture.colony();
            house = MaeSites.blueprint(world, HOUSE, 0);
            box = new VillageBounds(center.getX() - HALF_BOX, center.getY(), center.getZ() - HALF_BOX,
                    center.getX() + HALF_BOX, center.getY() + 10, center.getZ() + HALF_BOX);
            colony.measure(box);
        }

        void set(BlockPos pos, net.minecraft.block.BlockState state) {
            changed.putIfAbsent(pos, world.getBlockState(pos));
            world.setBlockState(pos, state, net.minecraft.block.Block.NOTIFY_LISTENERS | net.minecraft.block.Block.FORCE_STATE);
        }

        List<MaeBuildQueue.Request> requests() {
            return MaeBuildQueue.ofColony(world, colony.id());
        }

        void end() {
            requests().forEach(request -> MaeBuildQueue.remove(world, request.id()));
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
            MaeRescue.foundALot(colony.id());
            changed.forEach((pos, state) -> world.setBlockState(pos, state, net.minecraft.block.Block.NOTIFY_LISTENERS | net.minecraft.block.Block.FORCE_STATE));
            fixture.cleanUp();
            chunks(false);
        }

        private void chunks(boolean force) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    world.setChunkForced((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz, force);
                }
            }
        }
    }

    private static void run(TestContext context, java.util.function.Consumer<Village> scenario) {
        Village village = new Village(context);

        try {
            scenario.accept(village);
        } finally {
            village.end();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_rescue_second", tickLimit = 40)
    public void theSecondEmptySweepQueuesALotOutsideTheBoxWithTheDoorToTheVillage(TestContext context) {
        run(context, village -> {
            List<Blueprint> plans = List.of(village.house);

            context.assertTrue(MaeRescue.sweptWithoutLot(village.world, village.colony, plans).isEmpty(), "a primeira volta é da rua");
            context.assertTrue(MaeRescue.sweptWithoutLot(village.world, village.colony, plans).isPresent(), "a segunda volta devia chamar o MAE");

            MaeBuildQueue.Request request = village.requests().getFirst();
            Blueprint built = MaeSites.blueprint(village.world, HOUSE, request.quarterTurns());
            int lowZ = request.floor().getZ();
            int highZ = lowZ + built.size().z() - 1;
            int gap = village.box.minZ() - highZ;

            context.assertTrue(highZ < village.box.minZ(), "o primeiro lado é o norte, e o lote ficou em z=" + lowZ);
            context.assertTrue(lowZ >= village.box.minZ() - MaeRescue.REACH, "o lote passou de 24 blocos da caixa: z=" + lowZ);
            context.assertTrue(gap <= MaeSites.LOT_MARGIN + 2, "devia ser o mais perto da vila, e ficou a " + gap);
            context.assertTrue(built.doorSide().filter(Side.SOUTH::equals).isPresent(), "a porta devia olhar a vila: " + built.doorSide());
            context.assertTrue(request.owner() == null, "o pedido automático não tem dono");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_rescue_found", tickLimit = 40)
    public void aLotFoundByTheNormalSearchRestartsTheCount(TestContext context) {
        run(context, village -> {
            List<Blueprint> plans = List.of(village.house);

            MaeRescue.sweptWithoutLot(village.world, village.colony, plans);
            MaeRescue.foundALot(village.colony.id());

            context.assertTrue(MaeRescue.sweptWithoutLot(village.world, village.colony, plans).isEmpty(), "achar lote devia zerar a conta");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_rescue_water", tickLimit = 40)
    public void aLotTouchingWaterIsRefusedAndTheNextSideIsTried(TestContext context) {
        run(context, village -> {
            List<Blueprint> plans = List.of(village.house);
            BlockPos center = village.center;

            for (BlockPos at : BlockPos.iterate(center.add(-40, 0, -40), center.add(40, 0, -HALF_BOX - 1))) {
                village.set(at.toImmutable(), Blocks.WATER.getDefaultState());
            }

            MaeRescue.sweptWithoutLot(village.world, village.colony, plans);

            context.assertTrue(MaeRescue.sweptWithoutLot(village.world, village.colony, plans).isEmpty(), "ao norte só há água, e o MAE aceitou");
            context.assertTrue(village.requests().isEmpty(), "um lote em cima da água entrou na fila");
            context.assertTrue(MaeRescue.sweptWithoutLot(village.world, village.colony, plans).isPresent(), "o lado seguinte é seco");

            BlockPos floor = village.requests().getFirst().floor();

            context.assertTrue(floor.getZ() + MaeSites.LOT_MARGIN >= center.getZ() - HALF_BOX, "o lote ficou encostado na água: " + floor.toShortString());
        });
    }

    /** Chão bom a mais de 24 blocos da caixa não serve: o limite é do autor. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_rescue_reach", tickLimit = 40)
    public void aGoodLotBeyondTheReachIsNotTaken(TestContext context) {
        run(context, village -> {
            List<Blueprint> plans = List.of(village.house);
            BlockPos center = village.center;

            for (BlockPos at : BlockPos.iterate(center.add(-40, 0, -HALF_BOX - MaeRescue.REACH), center.add(40, 0, -HALF_BOX - 1))) {
                village.set(at.toImmutable(), Blocks.WATER.getDefaultState());
            }

            MaeRescue.sweptWithoutLot(village.world, village.colony, plans);

            context.assertTrue(MaeRescue.sweptWithoutLot(village.world, village.colony, plans).isEmpty(),
                    "o lote além dos 24 blocos entrou: " + village.requests());
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_rescue_queue", tickLimit = 40)
    public void aVillageWithARequestOrWithoutABoxDoesNotCallTheMae(TestContext context) {
        run(context, village -> {
            List<Blueprint> plans = List.of(village.house);

            MaeRescue.sweptWithoutLot(village.world, village.colony, plans);
            MaeRescue.sweptWithoutLot(village.world, village.colony, plans);

            int queued = village.requests().size();

            MaeRescue.sweptWithoutLot(village.world, village.colony, plans);
            MaeRescue.sweptWithoutLot(village.world, village.colony, plans);

            context.assertTrue(queued == 1 && village.requests().size() == 1, "um pedido de cada vez, e a fila tem " + village.requests().size());

            Colony noBox = ColonyFixture.colonyAtWorld(village.center.add(0, 1, 2)).colony();

            MaeRescue.sweptWithoutLot(village.world, noBox, plans);

            context.assertTrue(MaeRescue.sweptWithoutLot(village.world, noBox, plans).isEmpty(), "vila sem caixa não sabe onde é a borda");
            VillageColonyMod.COLONIES.remove(noBox.id());
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_rescue_notice", tickLimit = 40)
    public void theNoticeSaysWhereAndHowToCancel(TestContext context) {
        run(context, village -> {
            List<Blueprint> plans = List.of(village.house);

            MaeRescue.sweptWithoutLot(village.world, village.colony, plans);

            MaeSites.Status status = MaeRescue.sweptWithoutLot(village.world, village.colony, plans).orElseThrow();
            String notice = MaeRescue.notice(status).getString();

            context.assertTrue(notice.contains(status.origin().toShortString()) && notice.contains("/vc mae parar"), notice);
        });
    }

    /** O caminho inteiro pelo planejador: sem rua não há lote, o socorro pede e o construtor pega a obra. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_rescue_cycle", tickLimit = 40)
    public void aVillageWithNoLotGetsItsWorkFromTheBuilder(TestContext context) {
        run(context, village -> {
            village.fixture.equippedWorker(context, ProfessionType.BUILDER,
                    village.center.up().subtract(context.getAbsolutePos(BlockPos.ORIGIN)));

            var opened = java.util.Optional.<com.villagecolony.core.construction.model.ConstructionProject>empty();

            for (int pass = 0; pass < 200 && opened.isEmpty(); pass++) {
                opened = ConstructionPlanner.plan(village.world, village.colony);
            }

            context.assertTrue(opened.isPresent(), "em 200 passadas a vila não abriu obra nenhuma");
            context.assertTrue(village.requests().size() == 1 && village.requests().getFirst().opened(),
                    "a obra aberta devia ser a do socorro: " + opened.get().blueprint().id() + " " + village.requests());
            context.assertTrue(!MinecraftTypeAdapter.toBlockPos(opened.get().origin()).equals(village.center), "obra no centro");
        });
    }
}
