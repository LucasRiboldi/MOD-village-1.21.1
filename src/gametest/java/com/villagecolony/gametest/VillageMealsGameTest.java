package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.VillageMeals;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
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
 * ficar disposto; com a vila cheia, o baú fica intocado.
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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_meals")
    public void aFullVillageKeepsItsFoodInTheChest(TestContext context) {
        Scene scene = scene(context, 1);

        try {
            int fed = VillageMeals.serve(context.getWorld(), scene.colony());

            context.assertTrue(fed == 0, "a vila sem cama livre alimentou " + fed);
            context.assertTrue(scene.chest().getStack(0).getCount() == 3
                            && scene.chest().getStack(1).getCount() == 12,
                    "a comida saiu do bau de uma vila sem cama para o bebe");
        } finally {
            scene.fixture().cleanUp();
        }

        context.complete();
    }
}
