package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Regra 339: o fundidor não segura o pedido sem o cru em nenhum baú. Sem nada para as obras, ele solta o pedido e fica
 * livre para o que o ofício faz sem tarefa — no jogo da 0.4.93 ele segurou o pedido de ferro sem minério ~28 mil tiques.
 */
public class SmelterReleaseGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_release", tickLimit = 300)
    public void theSmelterLetsGoOfAnOrderWithNoRawMaterialAnywhere(TestContext context) {
        BlockPos at = new BlockPos(2, 2, 2);
        context.setBlockState(at, Blocks.CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(at));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID smelter = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(smelter, colony.id()).assign(ProfessionType.SMELTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(smelter, chest));
        owned.owning(smelter);
        Task iron = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.SMELT_MATERIAL, TaskPriority.PRODUCTION, ResourceType.IRON_INGOT, 8);

        iron.reserveFor(smelter);
        SmelterWork.run(context.getWorld(), colony);

        context.runAtEveryTick(() -> SmelterWork.tick(context.getWorld()));

        context.runAtTick(250, () -> {
            try {
                context.assertTrue(iron.state() == TaskState.AVAILABLE, "Regra 339: o fundidor segurou o pedido de"
                        + " ferro sem minério em nenhum baú: " + iron.state());
            } finally {
                SmelterWork.forget(smelter);
                VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
                owned.cleanUp();
            }

            context.complete();
        });
    }
}
