package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.Supplies;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComposterBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Regra 310: o fazendeiro põe pó de osso em cada pé da plantação, um por volta; fechada a volta, colhe o que cresceu
 * e começa outra; a cada dia, tudo de novo. O pó sai da composteira, depois dos baús da vila acima de 4 (Regra 349), e só sem
 * nenhum ele surge.
 */
public class BoneMealRoundGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    private static final BlockPos COMPOSTER = new BlockPos(1, 2, 5);

    private static final List<BlockPos> CROPS = List.of(new BlockPos(3, 2, 3), new BlockPos(5, 2, 3), new BlockPos(7, 2, 3));

    /** Três pés verdes: cada um recebe uma vez antes de qualquer um receber a segunda. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bone_meal_round_each_once", tickLimit = 20)
    public void everyCropGetsOneApplicationPerRound(TestContext context) {
        UUID colony = UUID.randomUUID();
        List<BlockPos> crops = plant(context);
        List<ColonyPos> chests = chest(context, 64);
        List<BlockPos> order = new ArrayList<>();

        try {
            for (int i = 0; i < 4; i++) {
                BlockPos next = BoneMealRound.next(context.getWorld(), colony, crops, crops.getFirst(), 0).orElseThrow();
                order.add(next);
                BoneMealRound.feed(context.getWorld(), colony, next, Optional.empty(), chests);
            }

            context.assertTrue(order.equals(List.of(crops.get(0), crops.get(1), crops.get(2), crops.get(0))),
                    "Regra 310: a volta não passou uma vez em cada pé antes de recomeçar: " + order);
        } finally {
            BoneMealRound.forget(colony);
            clear(context);
        }

        context.complete();
    }

    /** Dia novo, volta nova: o pé que já recebeu hoje volta a valer amanhã. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bone_meal_round_new_day", tickLimit = 20)
    public void aNewDayStartsANewRound(TestContext context) {
        UUID colony = UUID.randomUUID();
        List<BlockPos> crops = plant(context);
        List<ColonyPos> chests = chest(context, 64);

        try {
            BoneMealRound.feed(context.getWorld(), colony,
                    BoneMealRound.next(context.getWorld(), colony, crops, crops.getFirst(), 0).orElseThrow(),
                    Optional.empty(), chests);
            Optional<BlockPos> sameDay = BoneMealRound.next(context.getWorld(), colony, crops, crops.getFirst(), 0);
            Optional<BlockPos> nextDay = BoneMealRound.next(context.getWorld(), colony, crops, crops.getFirst(), 1);

            context.assertTrue(sameDay.equals(Optional.of(crops.get(1))), "Regra 310: no mesmo dia, o pé repetiu: " + sameDay);
            context.assertTrue(nextDay.equals(Optional.of(crops.get(0))), "Regra 310: o dia novo não recomeçou: " + nextDay);
        } finally {
            BoneMealRound.forget(colony);
            clear(context);
        }

        context.complete();
    }

    /** A composteira primeiro, depois o baú até sobrarem quatro, e só então o pó surge (Regras 310 e 349). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bone_meal_round_sources", tickLimit = 20)
    public void theComposterThenTheChestsAboveFourThenItAppears(TestContext context) {
        UUID colony = UUID.randomUUID();
        List<BlockPos> crops = plant(context);
        List<ColonyPos> chests = chest(context, 5);
        context.setBlockState(COMPOSTER, Blocks.COMPOSTER.getDefaultState().with(ComposterBlock.LEVEL, 8));
        Optional<BlockPos> composter = Optional.of(context.getAbsolutePos(COMPOSTER));
        long before = Supplies.sessionCount(colony, SupplyException.BONE_MEAL_FOR_THE_FARM);

        try {
            Optional<BoneMealRound.Source> first = BoneMealRound.feed(context.getWorld(), colony, crops.get(0), composter, chests);
            int level = context.getBlockState(COMPOSTER).get(ComposterBlock.LEVEL);
            Optional<BoneMealRound.Source> second = BoneMealRound.feed(context.getWorld(), colony, crops.get(1), composter, chests);
            int inChest = ColonyChests.countIn(context.getWorld(), chests, Items.BONE_MEAL);
            Optional<BoneMealRound.Source> third = BoneMealRound.feed(context.getWorld(), colony, crops.get(2), composter, chests);

            context.assertTrue(first.equals(Optional.of(BoneMealRound.Source.COMPOSTER)) && level == 0,
                    "Regra 310: a composteira cheia não foi a primeira: " + first + ", nível " + level);
            context.assertTrue(second.equals(Optional.of(BoneMealRound.Source.CHEST)) && inChest == 4,
                    "Regra 349: o baú não deu o pó acima de 4: " + second + ", " + inChest + " no baú");
            context.assertTrue(third.equals(Optional.of(BoneMealRound.Source.APPEARED))
                            && ColonyChests.countIn(context.getWorld(), chests, Items.BONE_MEAL) == 4
                            && Supplies.sessionCount(colony, SupplyException.BONE_MEAL_FOR_THE_FARM) > before,
                    "Regra 349: com 4 no baú, o pó devia surgir e os 4 ficar: " + third);
        } finally {
            BoneMealRound.forget(colony);
            context.setBlockState(COMPOSTER, Blocks.AIR.getDefaultState());
            clear(context);
        }

        context.complete();
    }

    /** O fazendeiro sem pó de osso em lugar nenhum: o pó surge, o trigo cresce, e ele colhe. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bone_meal_round_farmer", tickLimit = 300)
    public void theFarmerGrowsAndHarvestsTheGreenWheat(TestContext context) {
        BlockPos field = CROPS.getFirst();
        context.setBlockState(field.down(), Blocks.FARMLAND.getDefaultState());
        context.setBlockState(field, Blocks.WHEAT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos absolute = context.getAbsolutePos(field);
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST).add(200, 0, 200)));
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                ResourceId.vanilla("village/plains/houses/plains_small_farm_1"),
                MinecraftTypeAdapter.toColonyPos(absolute.down()), MinecraftTypeAdapter.toColonyPos(absolute)));

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, field.north());
        villager.setBreedingAge(0);
        owned.owning(villager.getUuid());
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.FARMER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST))));
        Task task = VillageColonyMod.TASKS.create(colony.id(), TaskType.COLLECT_FOOD, TaskPriority.PRODUCTION,
                ResourceType.WHEAT, 8);
        task.reserveFor(villager.getUuid());

        FarmerWork.shortenSearchTo(4);
        FarmerWork.run(context.getWorld(), colony);

        context.runAtTick(240, () -> {
            try {
                int wheat = ChestInventoryReader.read(context.getWorld(), context.getAbsolutePos(CHEST))
                        .amountOf(ResourceType.WHEAT);

                context.assertTrue(wheat > 0, "Regra 310: sem pó em lugar nenhum, o trigo verde não cresceu nem foi colhido");
            } finally {
                VillageColonyMod.TASKS.remove(task.id());
                VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
                FarmOwners.forget(villager.getUuid());
                BoneMealRound.forget(colony.id());
                FarmerWork.forgetColony(colony.id());
                FarmerWork.clearAll();
                owned.cleanUp();
                villager.discard();
            }

            context.complete();
        });
    }

    private static List<BlockPos> plant(TestContext context) {
        List<BlockPos> crops = new ArrayList<>();

        for (BlockPos crop : CROPS) {
            context.setBlockState(crop.down(), Blocks.FARMLAND.getDefaultState());
            context.setBlockState(crop, Blocks.WHEAT.getDefaultState());
            crops.add(context.getAbsolutePos(crop));
        }

        return crops;
    }

    private static List<ColonyPos> chest(TestContext context, int boneMeal) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ChestDepositor.deposit(context.getWorld(), chest, Items.BONE_MEAL, boneMeal);

        return List.of(chest);
    }

    private static void clear(TestContext context) {
        context.setBlockState(CHEST, Blocks.AIR.getDefaultState());

        for (BlockPos crop : CROPS) {
            context.setBlockState(crop, Blocks.AIR.getDefaultState());
        }
    }
}
