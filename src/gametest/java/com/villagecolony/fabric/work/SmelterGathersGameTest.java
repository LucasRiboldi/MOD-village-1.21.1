package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;
import java.util.UUID;

/** O fundidor sem areia solta a fornalha e vai buscá-la — regra do autor, 2026-10-11. */
public class SmelterGathersGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "crusher", tickLimit = 20)
    public void aSmelterWithoutSandGoesToGatherIt(TestContext context) {
        UUID colony = UUID.randomUUID();
        UUID smelter = UUID.randomUUID();
        Task glass = VillageColonyMod.TASKS.create(
                colony, TaskType.SMELT_MATERIAL, TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.GLASS, 6);
        Task stone = VillageColonyMod.TASKS.create(
                colony, TaskType.SMELT_MATERIAL, TaskPriority.PRODUCTION, ResourceType.STONE, 6);

        try {
            stone.reserveFor(smelter);
            context.assertFalse(SmelterGathers.handOff(smelter, stone, List.of(Items.COBBLESTONE)),
                    "pedregulho não se recolhe na superfície: a fundição de pedra devia seguir com ele");
            stone.release("teste");

            glass.reserveFor(smelter);
            context.assertTrue(SmelterGathers.handOff(smelter, glass, List.of(Items.SAND, Items.RED_SAND)),
                    "sem areia, o fundidor devia soltar o vidro e ir buscá-la");
            context.assertTrue(glass.state() == TaskState.AVAILABLE, "o vidro não voltou à fila: " + glass.state());
            context.assertTrue(VillageColonyMod.TASKS.assignedTo(smelter).stream().anyMatch(task ->
                            task.type() == TaskType.COLLECT_SURFACE_RESOURCE && task.targetResource() == ResourceType.SAND),
                    "a coleta de areia não ficou com o fundidor");
        } finally {
            VillageColonyMod.TASKS.ofColony(colony).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
        }

        context.complete();
    }
}
