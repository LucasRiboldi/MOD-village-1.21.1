package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.VillageHappiness;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;

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

import java.util.List;

/** Feliz tem mais filhos; infeliz, nenhum filho novo (ADR-036 20). */
public class VillageMoodGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 1, 1);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_mood", tickLimit = 20)
    public void anUnhappyVillageHoldsTheBreedingWait(TestContext context) {
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 1, 3));
        villager.setBreedingAge(0);

        VillageMood.apply(context.getWorld(), villager, VillageHappiness.Mood.UNHAPPY, List.of());

        context.assertTrue(villager.getBreedingAge() >= VillageMood.HOLD_TICKS,
                "a vila infeliz deixou o aldeão pronto para ter filho: espera " + villager.getBreedingAge());
        context.assertTrue(!villager.isBaby(), "segurar a espera transformou o adulto em filhote");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_mood", tickLimit = 20)
    public void aHappyVillageShortensTheWaitAndFeedsFromTheChest(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ((ChestBlockEntity) context.getBlockEntity(CHEST)).setStack(0, new ItemStack(Items.BREAD, 5));
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 1, 3));
        villager.setBreedingAge(1_000);

        VillageMood.apply(context.getWorld(), villager, VillageHappiness.Mood.HAPPY, List.of(chest));

        context.assertTrue(villager.getBreedingAge() == 500, "a espera não caiu pela metade: " + villager.getBreedingAge());
        context.assertTrue(villager.getInventory().count(Items.BREAD) == 1, "o aldeão não recebeu o pão");
        context.assertTrue(ColonyChests.countIn(context.getWorld(), List.of(chest), Items.BREAD) == 4,
                "o pão não saiu do baú: o filho não pode nascer de comida inventada");

        VillageMood.apply(context.getWorld(), villager, VillageHappiness.Mood.CONTENT, List.of(chest));

        context.assertTrue(villager.getInventory().count(Items.BREAD) == 1 && villager.getBreedingAge() == 500,
                "a vila satisfeita mexeu no aldeão");
        context.complete();
    }
}
