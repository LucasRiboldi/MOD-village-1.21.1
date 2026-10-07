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
    public void aNewVillageGetsEightMatureTreesOfBothSpecies(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(8, 1, 8)).add(3000, 0, -3000);
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(center.getX(), center.getY(), center.getZ()));

        prepareNaturalRing(world, center);

        int planted = VillageForest.seedInitial(world, colony, TreeSpecies.OAK, TreeSpecies.BIRCH, 8);

        if (planted != 8) {
            throw new AssertionError("o bosque fundacional plantou " + planted + " arvores, e eram oito");
        }

        if (!hasLog(world, center, TreeSpecies.OAK) || !hasLog(world, center, TreeSpecies.BIRCH)) {
            throw new AssertionError("o bosque nao deixou as duas especies de tronco no mundo");
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_forest",
            tickLimit = 100)
    public void aNewVillageGetsEightMatureTreesThroughNaturalVegetation(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(-8, 1, 8)).add(3_000, 0, 3_000);
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(center.getX(), center.getY(), center.getZ()));

        prepareNaturalRing(world, center);
        coverNaturalRingWithLeaves(world, center);

        int planted = VillageForest.seedInitial(world, colony, TreeSpecies.OAK, TreeSpecies.BIRCH, 8);

        if (planted != 8) {
            throw new AssertionError("a vila nova plantou " + planted + " arvores em vez de oito");
        }

        if (countLogs(world, center) < 8) {
            throw new AssertionError("o bosque fundacional nao deixou oito arvores maduras no mundo");
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

        int planted = VillageForest.seedInitial(world, colony, TreeSpecies.OAK, TreeSpecies.BIRCH, 8);

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
                "a décima pessoa deve plantar as cinco árvores da dezena");
        context.assertTrue(colony.forestPopulationMilestone() == 10,
                "o marco só avança depois de a árvore existir");
        context.assertTrue(
                VillageForest.plantForPopulation(world, colony, 10)
                        == VillageForest.PopulationPlanting.NOT_DUE,
                "a mesma dezena não pode duplicar a árvore");
        context.complete();
    }

    /**
     * Sem lugar para a árvore da dezena, a busca espera antes de repetir —
     * F13, 2026-09-30.
     *
     * <p>No playtest das 02:45 a fase que o log chama de "chests" custou 85
     * ms de mediana por ciclo, e ela inclui este plantio: com dez adultos e
     * o anel ocupado, cada ciclo refazia a busca de 64 ângulos com copa de
     * 5×5×16. O viveiro do fazendeiro já espera 6.000 tiques no mesmo caso.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_forest")
    public void anOccupiedRingIsNotSearchedAgainRightAway(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(3_000, 1, 3_000));
        Colony colony = Colony.create(
                UUID.randomUUID(), new ColonyPos(center.getX(), center.getY(), center.getZ()));
        prepareNaturalRing(world, center);
        occupyRing(world, center);

        context.assertTrue(
                VillageForest.plantForPopulation(world, colony, 10)
                        == VillageForest.PopulationPlanting.WAITING_FOR_SPACE,
                "o anel ocupado deveria deixar a árvore da dezena esperando lugar");
        context.assertTrue(
                VillageForest.plantForPopulation(world, colony, 10)
                        == VillageForest.PopulationPlanting.RESTING,
                "a busca sem lugar foi refeita logo no ciclo seguinte");
        context.complete();
    }

    /** A obra que espera madeira ganha árvores naturais maduras da espécie dela (ADR-037 F2). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_forest")
    public void aBuildWaitingForWoodGetsNaturalTrees(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(-3_000, 1, 3_000));
        Colony colony = Colony.create(
                UUID.randomUUID(), new ColonyPos(center.getX(), center.getY(), center.getZ()));
        prepareNaturalRing(world, center);

        int grown = VillageForest.plantRequestedTrees(world, colony, TreeSpecies.OAK, 2);

        context.assertTrue(grown == 2, "a obra pediu duas árvores e nasceram " + grown);
        context.assertTrue(hasLog(world, center, TreeSpecies.OAK), "nenhum tronco de carvalho no anel");
        context.complete();
    }

    /**
     * Capim baixo em cima da grama não impede a árvore (ADR-037 F3). Era o
     * defeito do playtest de 07-10: as duas vilas nasceram com zero árvores.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_forest")
    public void aGrassyRingStillGetsItsTrees(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(3_000, 1, -3_000));
        Colony colony = Colony.create(
                UUID.randomUUID(), new ColonyPos(center.getX(), center.getY(), center.getZ()));
        prepareNaturalRing(world, center);

        for (int x = -56; x <= 56; x++) {
            for (int z = -56; z <= 56; z++) {
                int distance = x * x + z * z;

                if (distance >= 48 * 48 && distance <= 56 * 56) {
                    world.setBlockState(center.add(x, 1, z), Blocks.SHORT_GRASS.getDefaultState());
                }
            }
        }

        int planted = VillageForest.seedInitial(world, colony, TreeSpecies.OAK, TreeSpecies.BIRCH, 5);

        context.assertTrue(planted == 5, "o capim barrou o bosque: plantou " + planted + " de 5");
        context.complete();
    }

    /** Com a vila medida, as árvores nascem nas bordas, fora da caixa (ADR-037 F1). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_forest")
    public void aMeasuredVillageGrowsItsTreesAtTheEdges(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(-6_000, 1, 9_000));
        Colony colony = Colony.create(
                UUID.randomUUID(), new ColonyPos(center.getX(), center.getY(), center.getZ()));
        colony.measure(new com.villagecolony.core.colony.model.VillageBounds(
                center.getX() - 8, center.getY() - 4, center.getZ() - 8,
                center.getX() + 8, center.getY() + 4, center.getZ() + 8));
        BlockPos middle = new BlockPos(colony.center().x(), center.getY(), colony.center().z());

        for (int x = -22; x <= 22; x++) {
            for (int z = -22; z <= 22; z++) {
                BlockPos ground = middle.add(x, 0, z);
                world.setBlockState(ground, Blocks.GRASS_BLOCK.getDefaultState());
                for (int y = 1; y <= 16; y++) {
                    world.setBlockState(ground.up(y), Blocks.AIR.getDefaultState());
                }
            }
        }

        int planted = VillageForest.seedInitial(world, colony, TreeSpecies.OAK, TreeSpecies.BIRCH, 3);
        com.villagecolony.core.colony.model.VillageBounds box = colony.bounds().orElseThrow();
        int inside = 0;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (world.getBlockState(new BlockPos(x, center.getY() + 1, z)).isIn(net.minecraft.registry.tag.BlockTags.LOGS)) {
                    inside++;
                }
            }
        }

        context.assertTrue(planted == 3, "a vila medida plantou " + planted + " de 3");
        context.assertTrue(inside == 0, inside + " tronco(s) nasceram dentro da caixa da vila");
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

    private static void coverNaturalRingWithLeaves(ServerWorld world, BlockPos center) {
        for (int x = -56; x <= 56; x++) {
            for (int z = -56; z <= 56; z++) {
                int distance = x * x + z * z;
                if (distance >= 48 * 48 && distance <= 56 * 56) {
                    world.setBlockState(center.add(x, 0, z).up(), Blocks.OAK_LEAVES.getDefaultState());
                }
            }
        }
    }

    private static int countLogs(ServerWorld world, BlockPos center) {
        int logs = 0;
        for (int x = -56; x <= 56; x++) {
            for (int z = -56; z <= 56; z++) {
                for (int y = 1; y <= 16; y++) {
                    if (world.getBlockState(center.add(x, y, z)).isOf(Blocks.OAK_LOG)
                            || world.getBlockState(center.add(x, y, z)).isOf(Blocks.BIRCH_LOG)) {
                        logs++;
                        break;
                    }
                }
            }
        }
        return logs;
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
