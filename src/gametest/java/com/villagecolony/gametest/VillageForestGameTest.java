package com.villagecolony.gametest;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.TreeSpecies;
import com.villagecolony.fabric.integration.VillageForest;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** Provas de que a reserva madura nasce sem tomar o lugar de um bloco existente. */
public class VillageForestGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_forest",
            tickLimit = 100)
    public void aNewVillageGetsTwoDifferentMatureTrees(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(8, 1, 8)).add(3000, 0, -3000);
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(center.getX(), center.getY(), center.getZ()));

        prepareNaturalRing(world, center);

        int planted = VillageForest.seedInitial(world, colony, TreeSpecies.OAK, TreeSpecies.BIRCH);

        if (planted != 2) {
            throw new AssertionError("o bosque fundacional plantou " + planted + " arvores, e eram duas");
        }

        if (!hasLog(world, center, TreeSpecies.OAK) || !hasLog(world, center, TreeSpecies.BIRCH)) {
            throw new AssertionError("o bosque nao deixou as duas especies de tronco no mundo");
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_forest",
            tickLimit = 100)
    public void anOccupiedForestRingIsLeftUntouched(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(8, 1, 8)).add(-3000, 0, -3000);
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(center.getX(), center.getY(), center.getZ()));

        prepareNaturalRing(world, center);
        BlockPos occupied = center.add(48, 0, 0).up();
        occupyRing(world, center);

        int planted = VillageForest.seedInitial(world, colony, TreeSpecies.OAK, TreeSpecies.BIRCH);

        if (planted != 0) {
            throw new AssertionError("o bosque encontrou lugar dentro de um anel todo ocupado");
        }

        if (!world.getBlockState(occupied).isOf(Blocks.DIAMOND_BLOCK)) {
            throw new AssertionError("o bosque alterou o bloco que devia proteger");
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_forest")
    public void populationMilestonePlantsOnlyOnce(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(-3_000, 1, -3_000));
        Colony colony = Colony.create(
                UUID.randomUUID(), new ColonyPos(center.getX(), center.getY(), center.getZ()));
        prepareNaturalRing(world, center);

        context.assertTrue(
                VillageForest.plantForPopulation(world, colony, 9)
                        == VillageForest.PopulationPlanting.NOT_DUE,
                "a nona pessoa não deve plantar a árvore da dezena");
        context.assertTrue(
                VillageForest.plantForPopulation(world, colony, 10)
                        == VillageForest.PopulationPlanting.PLANTED,
                "a décima pessoa deve plantar uma árvore madura");
        context.assertTrue(colony.forestPopulationMilestone() == 10,
                "o marco só avança depois de a árvore existir");
        context.assertTrue(
                VillageForest.plantForPopulation(world, colony, 10)
                        == VillageForest.PopulationPlanting.NOT_DUE,
                "a mesma dezena não pode duplicar a árvore");
        context.complete();
    }

    private static void prepareNaturalRing(ServerWorld world, BlockPos center) {
        for (int x = -56; x <= 56; x++) {
            for (int z = -56; z <= 56; z++) {
                int distance = x * x + z * z;

                if (distance < 48 * 48 || distance > 56 * 56) {
                    continue;
                }

                BlockPos ground = center.add(x, 0, z);
                world.setBlockState(ground, Blocks.GRASS_BLOCK.getDefaultState());

                for (int y = 1; y <= 16; y++) {
                    world.setBlockState(ground.up(y), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static void occupyRing(ServerWorld world, BlockPos center) {
        for (int x = -56; x <= 56; x++) {
            for (int z = -56; z <= 56; z++) {
                int distance = x * x + z * z;

                if (distance >= 48 * 48 && distance <= 56 * 56) {
                world.setBlockState(center.add(x, 0, z).up(), Blocks.DIAMOND_BLOCK.getDefaultState());
                }
            }
        }
    }

    private static boolean hasLog(ServerWorld world, BlockPos center, TreeSpecies species) {
        for (int x = -62; x <= 62; x++) {
            for (int z = -62; z <= 62; z++) {
                for (int y = 1; y <= 16; y++) {
                    if (world.getBlockState(center.add(x, y, z)).isOf(species.log())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
