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
            context.assertFalse(SmelterGathers.handOff(context.getWorld(), smelter, stone, List.of(Items.COBBLESTONE)),
                    "pedregulho não se recolhe na superfície: a fundição de pedra devia seguir com ele");
            stone.release("teste");

            glass.reserveFor(smelter);
            context.assertTrue(SmelterGathers.handOff(context.getWorld(), smelter, glass, List.of(Items.SAND, Items.RED_SAND)),
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

    /**
     * A argila do forno aparece no baú do fundidor, sem ele sair para buscá-la — T5 (autor, 2026-10-29: "argila
     * aparece automático"; 30 argilas surgidas só depois de quatro buscas vazias nas simulações).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "clay_appears", tickLimit = 20)
    public void clayForTheFurnaceAppearsInsteadOfASearch(TestContext context) {
        net.minecraft.util.math.BlockPos at = new net.minecraft.util.math.BlockPos(2, 2, 2);
        context.setBlockState(at, net.minecraft.block.Blocks.CHEST);
        com.villagecolony.core.type.ColonyPos chest =
                com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(at));
        com.villagecolony.core.colony.model.Colony colony = com.villagecolony.core.colony.model.Colony.create(
                UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        com.villagecolony.gametest.ColonyFixture owned = com.villagecolony.gametest.ColonyFixture.create().owning(colony);
        UUID smelter = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(smelter, colony.id())
                .assign(com.villagecolony.core.worker.model.ProfessionType.SMELTER);
        VillageColonyMod.STORAGES.register(com.villagecolony.core.storage.model.WorkerStorage.of(smelter, chest));
        owned.owning(smelter);
        Task terracotta = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.SMELT_MATERIAL, TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.TERRACOTTA, 8);

        try {
            terracotta.reserveFor(smelter);

            context.assertFalse(SmelterGathers.handOff(context.getWorld(), smelter, terracotta, List.of(Items.CLAY)),
                    "o fundidor devia seguir na fornalha: a argila aparece");
            context.assertTrue(com.villagecolony.fabric.integration.ColonyChests.countIn(context.getWorld(),
                            List.of(chest), Items.CLAY) >= SmelterGathers.ORDER,
                    "a argila não apareceu no baú do fundidor");
            context.assertFalse(VillageColonyMod.TASKS.assignedTo(smelter).stream().anyMatch(task ->
                            task.type() == TaskType.COLLECT_SURFACE_RESOURCE),
                    "o fundidor saiu para buscar argila");
        } finally {
            VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
            owned.cleanUp();
        }

        context.complete();
    }
}
