package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.type.ColonyPos;
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

/** Sem baú livre na vila, a próxima obra é o storage_majest (ADR-036 9). */
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

            context.assertTrue(!next.isEmpty() && next.stream().allMatch(plan -> StoragePlans.isStorage(plan.id())),
                    "a próxima obra devia ser o storage_majest, e veio " + next.stream().map(Blueprint::id).toList());

            // Esvaziado o baú, a marca cai e o rodízio volta.
            chest.clear();
            ProfessionChestOverflow.relieve(context.getWorld(), colony.id(), List.of(own), Set.of(own));

            context.assertTrue(!ProfessionChestOverflow.needsStorage(colony.id()),
                    "a marca de falta de armazém ficou depois de o baú esvaziar");
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }
}
