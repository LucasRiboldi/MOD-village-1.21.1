package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import com.villagecolony.fabric.integration.MineGate;
import com.villagecolony.gametest.ColonyFixture;
import com.villagecolony.core.worker.model.Worker;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A casa do mineiro surge pronta ao lado da boca da mina na fundação da vila — Regra 260. */
public class MinerHomeFoundationGameTest implements FabricGameTest {

    /** Chão de grama em volta do centro: cabe a boca a 40 blocos e a casa a até 20 dela. */
    private static final int REACH = 72;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "aaa_miner_home_foundation", tickLimit = 120)
    public void theMinerHomeStandsBesideTheMouthOnFoundation(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos anchor = context.getAbsolutePos(new BlockPos(1, 1, 1)).add(1_000_000, 0, 3_100_000);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(anchor));
        VillageColonyMod.COLONIES.register(colony);
        grassland(world, anchor);

        try {
            VillageDetectionHandler.runFoundationNow(world, colony);

            Optional<Mine> mine = VillageColonyMod.MINES.of(colony.id());
            context.assertTrue(mine.isPresent(), "a fundação não abriu a mina");

            BlockPos mouth = MinecraftTypeAdapter.toBlockPos(mine.get().shaft().entry());
            context.assertTrue(MineGate.standingAtMouth(world, mouth,
                            MinecraftTypeAdapter.toDirection(mine.get().shaft().descent())).isPresent(),
                    "o arco não está de pé na boca " + mouth.toShortString());

            List<Building> homes = VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                    .filter(building -> MineHomePlans.isMinerHouse(building.blueprint())).toList();
            context.assertTrue(homes.size() == 1, "casas do mineiro registradas: " + homes.size());

            Building home = homes.getFirst();
            context.assertTrue(home.finished(), "a casa do mineiro não está terminada");
            context.assertTrue(MineHomePlans.near(mouth, home), "a casa " + home.min() + " está longe da boca " + mouth);
            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isEmpty(),
                    "a casa virou obra em vez de surgir pronta");

            int built = 0;
            for (BlockPos at : BlockPos.iterate(MinecraftTypeAdapter.toBlockPos(home.min()),
                    MinecraftTypeAdapter.toBlockPos(home.max()))) {
                if (world.getBlockState(at).getBlock() instanceof net.minecraft.block.BedBlock) {
                    built++;
                }
            }
            context.assertTrue(built > 0, "a casa do mineiro não tem cama no mundo");

            // O giro não desloca a casa da caixa registrada: os oito cantos (piso e telhado) estão postos.
            for (int x : new int[] {home.min().x(), home.max().x()}) {
                for (int y : new int[] {home.min().y(), home.max().y()}) {
                    for (int z : new int[] {home.min().z(), home.max().z()}) {
                        context.assertFalse(world.getBlockState(new BlockPos(x, y, z)).isAir(),
                                "canto vazio da casa em " + x + ", " + y + ", " + z);
                    }
                }
            }
            context.assertFalse(MineHomePlans.isOwed(world, colony), "a casa pronta ainda pede obra");
        } finally {
            cleanUp(world, colony, anchor);
        }

        context.complete();
    }

    /** Em cada um dos quatro giros, o molde posto coincide bloco a bloco com a planta girada do construtor. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "aaa_miner_home_foundation", tickLimit = 20)
    public void theStampedHomeMatchesTheTurnedPlanInEveryTurn(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos anchor = context.getAbsolutePos(new BlockPos(1, 1, 1)).add(1_000_000, 0, 3_200_000);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(anchor));
        com.villagecolony.core.construction.model.Blueprint plan = MineHomePlans.planFor(world, colony).getFirst();

        for (int turns = 0; turns < 4; turns++) {
            BlockPos origin = anchor.add(turns * 16, 0, 0);
            com.villagecolony.core.construction.model.Blueprint turned = plan.rotated(turns);

            context.assertTrue(MinerHomeFoundation.stamp(world, plan, MinecraftTypeAdapter.toColonyPos(origin), turns),
                    "o molde não foi posto no giro " + turns);

            for (var block : turned.blocks()) {
                BlockPos at = origin.add(block.offset().x(), block.offset().y(), block.offset().z());
                net.minecraft.block.Block expected = MinecraftTypeAdapter.toBlock(block.block()).orElseThrow();
                context.assertTrue(world.getBlockState(at).isOf(expected), "giro " + turns + ": em " + block.offset()
                        + " a planta pede " + block.block() + " e o molde pôs " + world.getBlockState(at));
            }

            for (BlockPos at : BlockPos.iterate(origin, origin.add(15, 8, 15))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }

    private static void grassland(ServerWorld world, BlockPos anchor) {
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                world.setBlockState(anchor.add(dx, -3, dz), Blocks.STONE.getDefaultState());
                world.setBlockState(anchor.add(dx, -2, dz), Blocks.DIRT.getDefaultState());
                world.setBlockState(anchor.add(dx, -1, dz), Blocks.GRASS_BLOCK.getDefaultState());

                for (int dy = 0; dy <= 10; dy++) {
                    world.setBlockState(anchor.add(dx, dy, dz), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static void cleanUp(ServerWorld world, Colony colony, BlockPos anchor) {
        List<UUID> workerIds = VillageColonyMod.WORKERS.ofColony(colony.id()).stream().map(Worker::villagerId).toList();

        world.getEntitiesByClass(VillagerEntity.class,
                net.minecraft.util.math.Box.of(anchor.toCenterPos(), 2.0 * REACH, 40.0, 2.0 * REACH),
                villager -> true).forEach(VillagerEntity::discard);

        ColonyFixture fixture = ColonyFixture.create().owning(colony);
        workerIds.forEach(fixture::owning);
        fixture.cleanUp();
    }
}
