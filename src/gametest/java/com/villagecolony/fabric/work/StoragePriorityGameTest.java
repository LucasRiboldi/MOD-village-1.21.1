package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ProfessionChestOverflow;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Sem baú livre na vila, a próxima obra é o armazém (ADR-036 9): o pequeno antes do grande (Regra 165). */
public class StoragePriorityGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 1, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_priority")
    public void aFullProfessionChestWithNoFreeChestAsksForTheStorage(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();

        try {
            context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
            ChestBlockEntity chest = (ChestBlockEntity) context.getBlockEntity(CHEST);

            for (int slot = 0; slot < chest.size(); slot++) {
                chest.setStack(slot, new ItemStack(Items.COBBLESTONE, 64));
            }

            ColonyPos own = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

            // Só o baú da profissão: nenhum baú livre para receber o excesso.
            ProfessionChestOverflow.relieve(context.getWorld(), colony.id(), List.of(own), Set.of(own));

            context.assertTrue(ProfessionChestOverflow.needsStorage(colony.id()),
                    "o baú lotado sem baú livre não marcou a falta de armazém");

            List<Blueprint> next = HousePlans.plansForNext(context.getWorld(), colony);

            // Regra 165: sem armazém pequeno de pé, o da vez é o pequeno.
            context.assertTrue(!next.isEmpty() && next.stream().allMatch(plan -> StoragePlans.isSmallStorage(plan.id())),
                    "a próxima obra devia ser o storage_small, e veio " + next.stream().map(Blueprint::id).toList());

            // Com um pequeno de pé (e uma casa depois dele), o grande vem primeiro.
            ColonyPos at = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(6, 1, 6)));
            ColonyPos corner = new ColonyPos(at.x() + 6, at.y() + 4, at.z() + 6);
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                    next.getFirst().id(), at, corner));
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                    ResourceId.vanilla("village/plains/houses/plains_small_house_1"), at, corner));

            List<Blueprint> after = HousePlans.plansForNext(context.getWorld(), colony);

            context.assertTrue(!after.isEmpty() && after.getFirst().id().path().endsWith("/" + StoragePlans.MODEL),
                    "com o pequeno de pé o grande devia vir primeiro, e veio "
                            + after.stream().map(Blueprint::id).toList());

            // Esvaziado o baú, a marca cai e o rodízio volta.
            chest.clear();
            ProfessionChestOverflow.relieve(context.getWorld(), colony.id(), List.of(own), Set.of(own));

            context.assertTrue(!ProfessionChestOverflow.needsStorage(colony.id()),
                    "a marca de falta de armazém ficou depois de o baú esvaziar");
        } finally {
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            fixture.cleanUp();
        }

        context.complete();
    }
}
