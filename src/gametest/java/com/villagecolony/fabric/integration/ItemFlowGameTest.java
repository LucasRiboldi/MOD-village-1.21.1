package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Regra 344, telemetria T3: o balanço diário dos itens nos baús da vila. O que entrou e saiu pelas portas do mod
 * ({@link ChestDepositor}, {@link ChestWithdrawer}) fecha com o estoque de um dia para o outro; a diferença é o que
 * mudou por fora (o jogador, ou um caminho que não passa pelas portas).
 */
public class ItemFlowGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "item_flow", tickLimit = 20)
    public void theDailyBalanceSaysWhatCameInWentOutAndWhatChangedOutside(TestContext context) {
        BlockPos at = new BlockPos(2, 2, 2);
        context.setBlockState(at, Blocks.CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(at));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID worker = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(worker, colony.id());
        VillageColonyMod.STORAGES.register(WorkerStorage.of(worker, chest));
        owned.owning(worker);

        try {
            ItemFlow.clear();
            ItemFlow.report(context.getWorld(), 1);

            ChestDepositor.deposit(context.getWorld(), chest, Items.OAK_PLANKS, 10);
            ChestWithdrawer.withdraw(context.getWorld(), chest, Items.OAK_PLANKS, 3);
            // O jogador põe 5 tábuas na mão: não passa pelas portas do mod.
            Inventory inventory = (Inventory) context.getWorld().getBlockEntity(context.getAbsolutePos(at));
            inventory.setStack(20, new ItemStack(Items.OAK_PLANKS, 5));

            String line = ItemFlow.report(context.getWorld(), 2);

            context.assertTrue(line.startsWith("VC_FLOW version=1 day=2 items="), "Regra 344: o começo: " + line);
            context.assertTrue(line.contains(" oak_planks=10,3,0,0,12,5"),
                    "Regra 344: entrou 10, saiu 3, recusado 0, de 0 para 12, 5 por fora: " + line);
        } finally {
            ItemFlow.clear();
            owned.cleanUp();
        }

        context.complete();
    }
}
