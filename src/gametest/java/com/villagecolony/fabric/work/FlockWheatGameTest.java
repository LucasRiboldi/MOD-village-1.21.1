package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Mais trigo para o rebanho — decisão do autor de 2026-10-22. No deserto (0.3.86) o pastor cevou
 * galinha com semente de trigo e o fazendeiro semeou beterraba 27 vezes contra 4 de trigo, com o
 * trigo da vila em 1: ovelha e vaca ficaram sem comida.
 */
public class FlockWheatGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    /** O fazendeiro semeia a lavoura que a vila tem menos, e não a primeira semente do baú. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_wheat", tickLimit = 20)
    public void theFarmerSowsTheCropTheVillageHasLeast(TestContext context) {
        UUID farmer = UUID.randomUUID();
        ColonyPos chest = chest(context);
        Colony colony = colonyWith(chest, farmer);

        try {
            ChestDepositor.deposit(context.getWorld(), chest, Items.BEETROOT_SEEDS, 4);
            ChestDepositor.deposit(context.getWorld(), chest, Items.WHEAT_SEEDS, 4);
            ChestDepositor.deposit(context.getWorld(), chest, Items.BEETROOT, 5);

            Optional<net.minecraft.item.Item> seed = FarmerChores.scarcestSeed(context.getWorld(), colony.id(), chest);

            context.assertTrue(seed.equals(Optional.of(Items.WHEAT_SEEDS)),
                    "sem trigo e com beterraba na vila, devia semear trigo, e escolheu " + seed);

            ChestDepositor.deposit(context.getWorld(), chest, Items.WHEAT, 10);

            seed = FarmerChores.scarcestSeed(context.getWorld(), colony.id(), chest);

            context.assertTrue(seed.equals(Optional.of(Items.BEETROOT_SEEDS)),
                    "com mais trigo que beterraba, devia semear beterraba, e escolheu " + seed);
        } finally {
            forget(colony, farmer);
        }

        context.complete();
    }

    /** A lavoura de cada semente: o nome sem "_seeds", ou ela mesma. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_wheat", tickLimit = 20)
    public void eachSeedKnowsItsCrop(TestContext context) {
        context.assertTrue(FarmerChores.cropOf(Items.WHEAT_SEEDS) == Items.WHEAT, "semente de trigo não deu trigo");
        context.assertTrue(FarmerChores.cropOf(Items.BEETROOT_SEEDS) == Items.BEETROOT, "semente de beterraba não deu beterraba");
        context.assertTrue(FarmerChores.cropOf(Items.CARROT) == Items.CARROT, "a cenoura é a própria semente");

        context.complete();
    }

    /** A criação deixa as sementes do plantio: só ceva com o que passa delas. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_wheat", tickLimit = 20)
    public void breedingLeavesTheSeedsForSowing(TestContext context) {
        ColonyPos chest = chest(context);
        List<ColonyPos> chests = List.of(chest);
        int justEnough = ShepherdFlock.SEEDS_FOR_SOWING + ShepherdFlock.PAIR_FOOD;

        ChestDepositor.deposit(context.getWorld(), chest, Items.WHEAT_SEEDS, justEnough - 1);
        context.assertFalse(ShepherdFlock.mayFeed(context.getWorld(), chests, Items.WHEAT_SEEDS),
                "o pastor cevou galinha com as sementes que o fazendeiro precisa para plantar");

        ChestDepositor.deposit(context.getWorld(), chest, Items.WHEAT_SEEDS, 1);
        context.assertTrue(ShepherdFlock.mayFeed(context.getWorld(), chests, Items.WHEAT_SEEDS),
                "com sementes acima da reserva, o par devia sair");

        ChestDepositor.deposit(context.getWorld(), chest, Items.BEETROOT, ShepherdFlock.PAIR_FOOD);
        context.assertTrue(ShepherdFlock.mayFeed(context.getWorld(), chests, Items.BEETROOT),
                "beterraba não é semente: um par basta");

        context.complete();
    }

    /** O trigo que a obra aberta ainda pede (a horta da casa) fica para ela: o pastor não ceva com ele. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_wheat", tickLimit = 20)
    public void breedingLeavesTheWheatTheBuildAsksFor(TestContext context) {
        ColonyPos chest = chest(context);
        List<ColonyPos> chests = List.of(chest);
        UUID colonyId = UUID.randomUUID();
        com.villagecolony.core.construction.model.ConstructionProject project =
                com.villagecolony.core.construction.model.ConstructionProject.plan(colonyId,
                        com.villagecolony.core.construction.model.Blueprint.of(
                                com.villagecolony.core.type.ResourceId.vanilla("flock_garden"), List.of(
                                        new com.villagecolony.core.construction.model.BlueprintBlock(
                                                new ColonyPos(0, 1, 0), MinecraftTypeAdapter.toResourceId(Items.WHEAT)),
                                        new com.villagecolony.core.construction.model.BlueprintBlock(
                                                new ColonyPos(1, 1, 0), MinecraftTypeAdapter.toResourceId(Items.WHEAT)))),
                        chest);
        VillageColonyMod.CONSTRUCTIONS.register(project);

        try {
            ChestDepositor.deposit(context.getWorld(), chest, Items.WHEAT, ShepherdFlock.PAIR_FOOD + 1);
            context.assertTrue(ShepherdFlock.buildsNeed(colonyId, Items.WHEAT) == 2,
                    "a obra devia pedir 2 de trigo, veio " + ShepherdFlock.buildsNeed(colonyId, Items.WHEAT));
            context.assertFalse(ShepherdFlock.mayFeed(context.getWorld(), colonyId, chests, Items.WHEAT),
                    "o pastor cevou com o trigo que a obra espera");

            ChestDepositor.deposit(context.getWorld(), chest, Items.WHEAT, 1);
            context.assertTrue(ShepherdFlock.mayFeed(context.getWorld(), colonyId, chests, Items.WHEAT),
                    "com trigo acima do que a obra pede, o par devia sair");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.forget(project.id(),
                    com.villagecolony.core.construction.service.RemovalAudit.playerCancellation());
        }

        context.complete();
    }

    private static ColonyPos chest(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        return MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
    }

    private static Colony colonyWith(ColonyPos chest, UUID worker) {
        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(worker, colony.id());
        VillageColonyMod.STORAGES.register(WorkerStorage.of(worker, chest));

        return colony;
    }

    private static void forget(Colony colony, UUID worker) {
        VillageColonyMod.STORAGES.remove(worker);
        VillageColonyMod.WORKERS.remove(worker);
        VillageColonyMod.COLONIES.remove(colony.id());
    }
}
