package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.ShepherdFlock;
import com.villagecolony.fabric.work.VillageMeals;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * A comida do fim do expediente — N1, 2026-09-24.
 *
 * <p>Sem reposição de mortos, a vila só cresce se os aldeões ficarem
 * dispostos a procriar, e isso no Vanilla é comida no inventário. Os dois
 * casos medem a decisão: com cama sobrando, o baú alimenta até o aldeão
 * ficar disposto; com a vila cheia, também (ADR-037 R1).
 */
public class VillageMealsGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 1, 2);

    private record Scene(Colony colony, VillagerEntity first, VillagerEntity second,
                         ChestBlockEntity chest, ColonyFixture fixture) {
    }

    private static Scene scene(TestContext context, int beds) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ChestBlockEntity chest = (ChestBlockEntity) context.getBlockEntity(CHEST);
        chest.setStack(0, new ItemStack(Items.BREAD, 3));
        chest.setStack(1, new ItemStack(Items.CARROT, 12));

        ColonyPos center = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), center);
        colony.observe(center, beds);
        VillageColonyMod.COLONIES.register(colony);

        VillagerEntity first = context.spawnEntity(EntityType.VILLAGER, new BlockPos(4, 1, 4));
        VillagerEntity second = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, 1, 4));
        ColonyFixture fixture = ColonyFixture.create().owning(colony);

        for (VillagerEntity villager : new VillagerEntity[] {first, second}) {
            VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
            VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), center));
            fixture.owning(villager.getUuid());
        }

        return new Scene(colony, first, second, chest, fixture);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_meals")
    public void aVillageWithSpareBedsFeedsItsAdultsUntilTheyWantToBreed(TestContext context) {
        Scene scene = scene(context, 500);

        try {
            context.assertFalse(scene.first().isReadyToBreed(),
                    "o aldeao ja nasceu disposto — o caso nao mediria nada");

            int fed = VillageMeals.serve(context.getWorld(), scene.colony());

            context.assertTrue(fed == 2, "comeram " + fed + " de 2 aldeoes");
            context.assertTrue(scene.first().isReadyToBreed() && scene.second().isReadyToBreed(),
                    "a comida nao deixou os dois dispostos a procriar");
            context.assertTrue(scene.chest().isEmpty(),
                    "o bau deveria ter dado 3 paes e 12 cenouras, sobrou comida nele");
        } finally {
            scene.fixture().cleanUp();
        }

        context.complete();
    }

    /**
     * A ceia deixa no baú um par da comida do rebanho — D2, decisão do autor de 2026-10-21.
     *
     * <p>No deserto o pastor disse ~200 vezes "no food" com os porcos pedindo par, porque a ceia
     * levava a cenoura, a batata e a beterraba antes dele. Com pastor e dois porcos adultos, ficam
     * duas cenouras; o resto da ceia é a mesma.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_meals")
    public void supperLeavesAPairOfTheFlocksFood(TestContext context) {
        Scene scene = scene(context, 500);
        UUID shepherd = UUID.randomUUID();
        PigEntity sow = context.spawnEntity(EntityType.PIG, new BlockPos(6, 1, 6));
        PigEntity boar = context.spawnEntity(EntityType.PIG, new BlockPos(7, 1, 6));

        try {
            VillageColonyMod.WORKERS.register(shepherd, scene.colony().id()).assign(ProfessionType.SHEPHERD);
            scene.fixture().owning(shepherd);

            VillageMeals.serve(context.getWorld(), scene.colony());

            int carrots = scene.chest().count(Items.CARROT);

            context.assertTrue(carrots == ShepherdFlock.PAIR_FOOD,
                    "a ceia devia deixar " + ShepherdFlock.PAIR_FOOD + " cenouras para os porcos, e deixou " + carrots);
            context.assertTrue(scene.chest().count(Items.BREAD) == 0, "o pão não é comida do rebanho e devia sair todo");
        } finally {
            sow.discard();
            boar.discard();
            scene.fixture().cleanUp();
        }

        context.complete();
    }

    /** Sem pastor ninguém cria o rebanho, e a ceia leva toda a comida, como antes do D2. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_meals")
    public void withoutAShepherdSupperTakesTheFlocksFoodToo(TestContext context) {
        Scene scene = scene(context, 500);
        PigEntity sow = context.spawnEntity(EntityType.PIG, new BlockPos(6, 1, 6));
        PigEntity boar = context.spawnEntity(EntityType.PIG, new BlockPos(7, 1, 6));

        try {
            VillageMeals.serve(context.getWorld(), scene.colony());

            context.assertTrue(scene.chest().isEmpty(), "sem pastor, a ceia guardou comida para os porcos");
        } finally {
            sow.discard();
            boar.discard();
            scene.fixture().cleanUp();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_meals")
    public void aFullVillageStillSharesItsFood(TestContext context) {
        Scene scene = scene(context, 1);

        try {
            int fed = VillageMeals.serve(context.getWorld(), scene.colony());

            // A reunião doa a comida com cama sobrando ou não — ADR-037 R1.
            context.assertTrue(fed == 2, "a vila sem cama livre alimentou " + fed + " de 2");
            context.assertTrue(scene.chest().isEmpty(), "a comida ficou no baú da vila cheia");
        } finally {
            scene.fixture().cleanUp();
        }

        context.complete();
    }
}
