package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.SandNearWater;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * A janela sem vidro — P2d, 2026-10-11: a vidraça aceita a cerca de madeira
 * só quando o vidro não tem rota (sem areia de beira d'água e sem britador).
 */
public class WindowEquivalentGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "window_equivalent", tickLimit = 20)
    public void aPaneTakesAFenceOnlyWhenGlassHasNoRoute(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        UUID mason = UUID.randomUUID();
        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("test_window"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("glass_pane")))),
                chest);

        try {
            VillageColonyMod.COLONIES.register(colony);

            // A bateria roda num mundo só: outro teste pode ter deixado areia e
            // água a menos de 48 blocos. A resposta certa é a da própria sonda.
            boolean sandNearWater = SandNearWater.within(
                    context.getWorld(), context.getAbsolutePos(CHEST), SandNearWater.RADIUS);

            context.assertTrue(
                    WindowEquivalent.choicesFor(context.getWorld(), project, Blocks.GLASS_PANE)
                            .contains(Items.OAK_FENCE) == !sandNearWater,
                    "sem pedreiro, a vidraça aceita a cerca exatamente quando não há areia de beira d'água"
                            + " (há areia: " + sandNearWater + ")");
            context.assertFalse(
                    WindowEquivalent.choicesFor(context.getWorld(), project, Blocks.OAK_DOOR).contains(Items.OAK_FENCE),
                    "só a vidraça troca por cerca");

            VillageColonyMod.WORKERS.register(mason, colony.id()).assign(ProfessionType.MASON);
            VillageColonyMod.STORAGES.register(WorkerStorage.of(mason, chest));
            ChestDepositor.deposit(context.getWorld(), chest, Items.COBBLESTONE, 4);

            context.assertFalse(
                    WindowEquivalent.choicesFor(context.getWorld(), project, Blocks.GLASS_PANE).contains(Items.OAK_FENCE),
                    "com pedreiro e pedra o britador faz areia: a janela espera o vidro");
        } finally {
            VillageColonyMod.STORAGES.remove(mason);
            VillageColonyMod.WORKERS.remove(mason);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
