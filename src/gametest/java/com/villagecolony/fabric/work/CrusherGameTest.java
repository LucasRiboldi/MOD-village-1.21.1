package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** O britador do pedreiro — P2c, 2026-10-11: pedregulho vira cascalho, cascalho vira areia, 2 para 1. */
public class CrusherGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "crusher", tickLimit = 20)
    public void fourCobblestoneBecomeOneSand(TestContext context) {
        UUID worker = UUID.randomUUID();
        ColonyPos chest = chestWith(context, 4);
        Colony colony = colonyWith(chest, worker);

        try {
            List<Optional<Item>> steps = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                steps.add(Crusher.crushOne(context.getWorld(), colony, chest));
            }

            List<ColonyPos> chests = List.of(chest);

            context.assertTrue(steps.equals(List.of(Optional.of(Items.GRAVEL), Optional.of(Items.GRAVEL),
                            Optional.of(Items.SAND), Optional.empty())),
                    "a ordem dos passos devia ser cascalho, cascalho, areia, nada: " + steps);
            context.assertTrue(ColonyChests.countIn(context.getWorld(), chests, Items.SAND) == 1
                            && ColonyChests.countIn(context.getWorld(), chests, Items.COBBLESTONE) == 0
                            && ColonyChests.countIn(context.getWorld(), chests, Items.GRAVEL) == 0,
                    "quatro pedregulhos deviam render uma areia e nada mais");
        } finally {
            forget(colony, worker);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "crusher", tickLimit = 20)
    public void theCobblestoneTheOpenBuildNeedsIsNotCrushed(TestContext context) {
        UUID worker = UUID.randomUUID();
        ColonyPos chest = chestWith(context, 4);
        Colony colony = colonyWith(chest, worker);
        List<BlueprintBlock> wall = new ArrayList<>();

        for (int x = 0; x < 4; x++) {
            wall.add(new BlueprintBlock(new ColonyPos(x, 0, 0), ResourceId.vanilla("cobblestone")));
        }

        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("test_wall"), wall),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(4, 2, 4))));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            VillageColonyMod.CONSTRUCTIONS.register(project);

            Optional<Item> made = Crusher.crushOne(context.getWorld(), colony, chest);

            context.assertTrue(made.isEmpty(), "a obra pede os quatro pedregulhos; o britador moeu: " + made);
            context.assertTrue(ColonyChests.countIn(context.getWorld(), List.of(chest), Items.COBBLESTONE) == 4,
                    "o pedregulho da obra saiu do baú");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            forget(colony, worker);
        }

        context.complete();
    }

    private static ColonyPos chestWith(TestContext context, int cobblestone) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ChestDepositor.deposit(context.getWorld(), chest, Items.COBBLESTONE, cobblestone);

        return chest;
    }

    private static Colony colonyWith(ColonyPos chest, UUID worker) {
        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(worker, colony.id());
        VillageColonyMod.STORAGES.register(WorkerStorage.of(worker, chest));

        return colony;
    }

    private static void forget(Colony colony, UUID worker) {
        VillageColonyMod.STORAGES.remove(worker);
        VillageColonyMod.WORKERS.remove(worker);
        VillageColonyMod.COLONIES.remove(colony.id());
    }
}
