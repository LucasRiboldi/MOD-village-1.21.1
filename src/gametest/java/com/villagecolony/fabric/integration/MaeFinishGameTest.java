package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.data.save.MaeWorkStore;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** O que falta para o MAE terminar: água interna (Regra 227), ruína de save antigo (Regra 228), bairro e recusas (Regra 226). */
public final class MaeFinishGameTest implements FabricGameTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");
    private static final AtomicInteger NEXT = new AtomicInteger();
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000c0de");

    /** Uma vila de chão plano, longe das outras arenas, com a caixa medida em volta. */
    private static final class Village {
        final ServerWorld world;
        final BlockPos center;
        final ColonyFixture fixture;
        final Colony colony;
        final Map<BlockPos, BlockState> changed = new HashMap<>();

        Village(TestContext context, int halfBox) {
            world = context.getWorld();
            center = new BlockPos(500_008 + NEXT.getAndIncrement() * 256, context.getAbsolutePos(new BlockPos(1, 1, 1)).getY(), 500_008);
            chunks(true);

            for (BlockPos at : BlockPos.iterate(center.add(-40, -3, -40), center.add(40, 0, 40))) {
                set(at.toImmutable(), Blocks.DIRT.getDefaultState());
            }

            fixture = ColonyFixture.colonyAtWorld(center.up());
            colony = fixture.colony();
            colony.measure(new VillageBounds(center.getX() - halfBox, center.getY(), center.getZ() - halfBox,
                    center.getX() + halfBox, center.getY() + 10, center.getZ() + halfBox));
        }

        void set(BlockPos pos, BlockState state) {
            changed.putIfAbsent(pos, world.getBlockState(pos));
            world.setBlockState(pos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }

        void end() {
            MaeBuildQueue.ofColony(world, colony.id()).forEach(request -> MaeBuildQueue.remove(world, request.id()));
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            MaeRescue.foundALot(colony.id());
            changed.forEach((pos, state) -> world.setBlockState(pos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE));
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

    private static void run(TestContext context, int halfBox, java.util.function.Consumer<Village> scenario) {
        Village village = new Village(context, halfBox);

        try {
            scenario.accept(village);
        } finally {
            village.end();
        }

        context.complete();
    }

    /** Uma poça de água no chão, debaixo do lote. */
    private static void pond(Village village, BlockPos from, BlockPos to) {
        for (BlockPos at : BlockPos.iterate(from, to)) {
            village.set(at.toImmutable(), Blocks.WATER.getDefaultState());
        }
    }

    private static String refusal(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException | IllegalArgumentException refused) {
            return refused.getMessage();
        }

        return "aceito";
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_finish_pond", tickLimit = 40)
    public void aPondInsideTheVillageIsFilledForThePlayersRequest(TestContext context) {
        run(context, 30, village -> {
            BlockPos floor = village.center.add(-5, 1, -5);

            pond(village, village.center.add(-3, 0, -3), village.center.add(-1, 0, -1));

            var status = MaeSites.start(village.world, village.colony.id(), HOUSE, floor, 0, PLAYER);

            context.assertTrue(status.phase() == MaeSites.Phase.QUEUED, "Regra 227: a obra sobre a poça não entrou: " + status);
            context.assertTrue(village.world.getFluidState(village.center.add(-2, 0, -2)).isEmpty(), "Regra 227: a poça ficou sob a obra");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_finish_river", tickLimit = 40)
    public void aRiverLeavingTheVillageIsNotFilled(TestContext context) {
        run(context, 10, village -> {
            BlockPos floor = village.center.add(-5, 1, -5);

            pond(village, village.center.add(-3, 0, -3), village.center.add(30, 0, -1));

            String refused = refusal(() -> MaeSites.start(village.world, village.colony.id(), HOUSE, floor, 0, PLAYER));

            context.assertTrue("open_water".equals(refused), "Regra 227: o rio aberto não foi recusado: " + refused);
            context.assertFalse(village.world.getFluidState(village.center.add(-2, 0, -2)).isEmpty(), "Regra 227: o rio foi aterrado");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_finish_auto_water", tickLimit = 40)
    public void theAutomaticRequestDoesNotFillWater(TestContext context) {
        run(context, 30, village -> {
            BlockPos floor = village.center.add(-5, 1, -5);

            pond(village, village.center.add(-3, 0, -3), village.center.add(-1, 0, -1));

            String refused = refusal(() -> MaeSites.start(village.world, village.colony.id(), HOUSE, floor, 0, null));

            context.assertFalse("aceito".equals(refused), "ADR-049: a obra automática aterrou água");
            context.assertFalse(village.world.getFluidState(village.center.add(-2, 0, -2)).isEmpty(), "ADR-049: a poça foi aterrada");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_finish_ruin", tickLimit = 40)
    public void theRuinOfAnOldSaveLeavesButWhatOthersPutStays(TestContext context) {
        run(context, 30, village -> {
            Blueprint blueprint = MaeSites.blueprint(village.world, HOUSE, 0);
            BlockPos origin = village.center.add(-5, 1, -5);
            List<BlueprintBlock> solid = blueprint.blocks().stream()
                    .filter(block -> !block.block().path().contains("air") && !block.block().path().contains("door"))
                    .limit(4).toList();
            BlueprintBlock other = solid.getLast();

            solid = solid.subList(0, 3);

            for (BlueprintBlock block : solid) {
                village.set(origin.add(block.offset().x(), block.offset().y(), block.offset().z()),
                        Registries.BLOCK.get(Identifier.of(block.block().namespace(), block.block().path())).getDefaultState());
            }

            // Numa posição da planta, mas com outro bloco: foi o jogador quem pôs, e fica.
            BlockPos foreign = origin.add(other.offset().x(), other.offset().y(), other.offset().z());

            village.set(foreign, Blocks.GLASS.getDefaultState());

            var store = com.villagecolony.data.save.ColonySavedData.get(village.world.getServer()).maeWork();
            NbtCompound saved = new NbtCompound();
            NbtCompound work = new NbtCompound();
            NbtCompound region = new NbtCompound();
            NbtCompound site = new NbtCompound();
            NbtList regions = new NbtList();

            site.putString("namespace", HOUSE.namespace());
            site.putString("path", HOUSE.path());
            site.putLong("origin", origin.asLong());
            site.putInt("rotation", 0);
            region.putUuid("id", UUID.randomUUID());
            region.putUuid("colony", UUID.randomUUID());
            region.put(MaeSiteRecord.KEY, site);
            regions.add(region);
            work.putInt("version", 3);
            work.put("regions", regions);
            saved.put(MaeWorkStore.KEY, work);
            store.read(saved);

            MaeSites.retireOldRegions(village.world.getServer());

            for (BlueprintBlock block : solid) {
                BlockPos at = origin.add(block.offset().x(), block.offset().y(), block.offset().z());

                context.assertTrue(village.world.getBlockState(at).isAir(), "Regra 228: ficou bloco da obra velha em " + at.toShortString());
            }

            context.assertTrue(village.world.getBlockState(foreign).isOf(Blocks.GLASS), "Regra 228: saiu bloco que não era da obra");
            context.assertTrue(store.regions().isEmpty(), "Regra 228: a região da ruína ficou no save");
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_finish_neighbourhood", tickLimit = 40)
    public void theNextRescueBuildsNextToTheLastOne(TestContext context) {
        run(context, 5, village -> {
            List<Blueprint> plans = List.of(MaeSites.blueprint(village.world, HOUSE, 0));

            MaeRescue.sweptWithoutLot(village.world, village.colony, plans);

            MaeSites.Status first = MaeRescue.sweptWithoutLot(village.world, village.colony, plans).orElseThrow();
            Blueprint built = MaeSites.blueprint(village.world, HOUSE, first.quarterTurns());

            // A primeira obra ficou pronta: o volume dela está ocupado, e a fila, vazia.
            MaeBuildQueue.ofColony(village.world, village.colony.id()).forEach(request -> MaeBuildQueue.remove(village.world, request.id()));

            for (BlockPos at : BlockPos.iterate(first.origin().add(0, 1, 0),
                    first.origin().add(built.size().x() - 1, 2, built.size().z() - 1))) {
                village.set(at.toImmutable(), Blocks.OAK_PLANKS.getDefaultState());
            }

            MaeRescue.sweptWithoutLot(village.world, village.colony, plans);

            MaeSites.Status second = MaeRescue.sweptWithoutLot(village.world, village.colony, plans).orElseThrow();
            int gap = Math.abs(second.origin().getX() - first.origin().getX()) + Math.abs(second.origin().getZ() - first.origin().getZ());

            context.assertTrue(second.origin().getZ() < village.center.getZ() - 5, "Regra 226: o bairro mudou de lado: "
                    + second.origin().toShortString());
            context.assertTrue(gap <= built.size().x() + built.size().z() + 2 * MaeSites.LOT_MARGIN + 2,
                    "Regra 226: a segunda obra não ficou colada na primeira: " + gap);
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_finish_reasons", tickLimit = 20)
    public void theRefusalCountsEveryReason(TestContext context) {
        Map<String, Integer> reasons = new java.util.LinkedHashMap<>();

        reasons.put("no_base", 2);
        reasons.put("occupied", 5);
        reasons.put("original_structure", 2);

        context.assertTrue("occupied×5,no_base×2,original_structure×2".equals(MaeSites.refusals(reasons)),
                "Regra 226: a recusa não traz todos os motivos: " + MaeSites.refusals(reasons));
        context.complete();
    }
}
