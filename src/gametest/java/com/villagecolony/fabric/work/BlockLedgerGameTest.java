package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Regra 342: a linha diária {@code VC_BLOCKS} traz, por bloco, a classe, a reserva, o estoque nos baús e o que a obra pôs,
 * esperou e viu surgir.
 */
public class BlockLedgerGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "block_ledger", tickLimit = 20)
    public void theDailyLineSaysClassReserveStockAndTheBuildCounts(TestContext context) {
        BlockPos at = new BlockPos(2, 2, 2);
        context.setBlockState(at, Blocks.CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(at));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID builder = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(builder, colony.id());
        VillageColonyMod.STORAGES.register(com.villagecolony.core.storage.model.WorkerStorage.of(builder, chest));
        owned.owning(builder);

        try {
            ChestDepositor.deposit(context.getWorld(), chest, Items.OAK_PLANKS, 40);
            ChestDepositor.deposit(context.getWorld(), chest, Items.OAK_LOG, 9);
            ChestDepositor.deposit(context.getWorld(), chest, Items.STICK, 5);

            ActionJournal.action(builder, "PLACED", "oak_planks", 2, null, "");
            ActionJournal.action(builder, "SET_ASIDE", "oak_planks", 1, null, "no material");
            BlockLedger.surged(colony.id(), "cobblestone", 36);

            String line = BlockLedger.report(context.getWorld(), colony, 7);

            context.assertTrue(line.startsWith("VC_BLOCKS version=1 colony=" + colony.id().toString().substring(0, 8)
                    + " day=7 blocks="), "Regra 342: o começo da linha: " + line);
            context.assertTrue(line.contains(" oak_planks=V,12,40,2,1,0"), "Regra 342: a tábua das vilas: " + line);
            context.assertTrue(line.contains(" oak_log=R,256,9,0,0,0"), "Regra 342: a tora é cru: " + line);
            context.assertTrue(line.contains(" cobblestone=R,256,0,0,0,36"), "Regra 342: o pedregulho que surgiu: " + line);
            context.assertFalse(line.contains("stick"), "Regra 342: graveto não é bloco: " + line);
        } finally {
            BlockLedger.clear(colony.id());
            owned.cleanUp();
        }

        context.complete();
    }
}
