package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageFocus;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Vila abandonada para de trabalhar: as profissões se desativam e o baú fica
 * como estava — ADR-036 item 10. No pacote de {@link VillagerLifecycleHandler}
 * porque a dispensa é visível só a ele.
 */
public final class AbandonedColonyGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "abandoned_colony")
    public void anAbandonedColonyDismissesItsWorkersAndKeepsTheChests(TestContext context) {
        BlockPos chestAt = new BlockPos(3, 2, 3);
        context.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        Inventory chest = (Inventory) context.getBlockEntity(chestAt);
        chest.setStack(0, new ItemStack(Items.OAK_LOG, 7));

        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 2, 1));
        Worker worker = fixture.emptyHandedWorker(context, ProfessionType.LUMBERJACK, new BlockPos(2, 2, 2));
        VillageColonyMod.STORAGES.register(WorkerStorage.of(
                worker.villagerId(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chestAt))));

        try {
            fixture.colony().setState(ColonyState.ABANDONED);
            int dismissed = VillagerLifecycleHandler.dismissColony(fixture.colony().id());

            context.assertTrue(dismissed == 1, "a colônia abandonada não soltou o trabalhador: " + dismissed);
            context.assertTrue(VillageColonyMod.WORKERS.find(worker.villagerId()).isEmpty(),
                    "o lenhador continuou registrado na colônia abandonada");
            context.assertTrue(VillageColonyMod.STORAGES.all().stream()
                            .noneMatch(storage -> storage.workerId().equals(worker.villagerId())),
                    "o baú continuou reservado ao trabalhador dispensado");
            context.assertTrue(chest.getStack(0).isOf(Items.OAK_LOG) && chest.getStack(0).getCount() == 7,
                    "o conteúdo do baú mudou com o abandono");
            context.assertFalse(VillageFocus.isWorking(context.getWorld(), fixture.colony().id()),
                    "a colônia abandonada continuou trabalhando");
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }
}
