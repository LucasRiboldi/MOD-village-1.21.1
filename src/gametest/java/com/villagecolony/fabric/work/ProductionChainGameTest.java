package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;
import java.util.UUID;

/**
 * E5, playtest de 2026-10-08: a obra pediu funil e o {@code /vc log} disse "uma
 * profissão consegue no bioma; falta entregar" sem ninguém com o pedido. A cadeia
 * nomeia cada elo e o que falta.
 */
public class ProductionChainGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "production_chain")
    public void theHopperChainNamesTheLinkNobodyWorksOn(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        Task ore = VillageColonyMod.TASKS.create(
                colonyId, TaskType.COLLECT_STONE, TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.RAW_IRON, 5);

        try {
            List<ProductionChain.Step> steps =
                    ProductionChain.of(context.getWorld(), colonyId, Items.HOPPER, List.of());
            String said = ProductionChain.describe(steps);

            context.assertTrue(steps.size() >= 3, "a cadeia do funil não desceu até o minério: " + said);
            context.assertTrue(steps.get(0).link() == ProductionChain.Link.AT_THE_BENCH,
                    "o funil não saiu como peça de bancada: " + said);
            context.assertTrue(steps.get(1).item() == Items.IRON_INGOT
                            && steps.get(1).link() == ProductionChain.Link.NO_TASK,
                    "o lingote sem fundidor não apareceu como elo sem tarefa: " + said);
            context.assertTrue(steps.get(2).item() == Items.RAW_IRON
                            && steps.get(2).link() == ProductionChain.Link.TASK_OPEN,
                    "o minério com tarefa aberta não apareceu assim: " + said);
            context.assertTrue(ProductionChain.missing(steps).map(step -> step.item() == Items.IRON_INGOT)
                            .orElse(false),
                    "o elo que falta não é o lingote: " + said);
        } finally {
            VillageColonyMod.TASKS.remove(ore.id());
        }

        context.complete();
    }
}
