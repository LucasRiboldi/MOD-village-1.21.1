package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.MasonStock;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import com.villagecolony.fabric.integration.ChestDepositor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sem obra, o ciclo pede ao pedreiro as peças da pedra da vila — ADR-036 item
 * 8: o pedreiro não fica parado.
 */
public final class MasonStockGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mason_stock")
    public void withoutWorkTheCycleAsksTheMasonForStonePieces(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ChestDepositor.deposit(context.getWorld(), chest, Items.COBBLESTONE, 32);
        ChestDepositor.deposit(context.getWorld(), chest, Items.SANDSTONE, 32);

        ColonyFixture fixture = ColonyFixture.colonyAt(context, CHEST);
        Worker mason = fixture.emptyHandedWorker(context, ProfessionType.MASON, new BlockPos(3, 2, 2));
        VillageColonyMod.STORAGES.register(WorkerStorage.of(mason.villagerId(), chest));

        Set<ResourceType> pieces = new HashSet<>(MasonStock.piecesFor(ResourceType.COBBLESTONE));
        pieces.addAll(MasonStock.piecesFor(ResourceType.SANDSTONE));

        try {
            VillageDetectionHandler.runCycleNow(context.getWorld(), context.getAbsolutePos(CHEST));

            // E as peças que as plantas da vila usam — ADR-039 item 4.
            pieces.addAll(com.villagecolony.core.coordination.AdvanceStock.piecesFor(
                    fixture.colony().id(), "mason", List.of()));

            boolean asked = false;
            for (Task task : VillageColonyMod.TASKS.ofColony(fixture.colony().id())) {
                asked |= task.type() == TaskType.CRAFT_STONE_MATERIAL && pieces.contains(task.targetResource());
            }

            context.assertTrue(asked, "sem obra, o ciclo não pediu ao pedreiro nenhuma peça de estoque: "
                    + VillageColonyMod.TASKS.ofColony(fixture.colony().id()).stream()
                            .map(task -> task.type() + "/" + task.targetResource()).toList());
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }
}
