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
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A tábua das peças de madeira da obra — P7c, 2026-10-11. */
public class WoodInPiecesGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "wood_in_pieces", tickLimit = 20)
    public void theGameRecipeSaysHowManyPlanksThePiecesTake(TestContext context) {
        int stairs = WoodInPieces.planksIn(context.getWorld(), Map.of(ResourceId.vanilla("oak_stairs"), 4));
        int slabs = WoodInPieces.planksIn(context.getWorld(), Map.of(ResourceId.vanilla("oak_slab"), 6));
        int planks = WoodInPieces.planksIn(context.getWorld(), Map.of(ResourceId.vanilla("oak_planks"), 5));
        int stone = WoodInPieces.planksIn(context.getWorld(), Map.of(ResourceId.vanilla("cobblestone_stairs"), 4));

        context.assertTrue(stairs == 6, "quatro escadas custam seis tábuas: " + stairs);
        context.assertTrue(slabs == 3, "seis lajes custam três tábuas: " + slabs);
        context.assertTrue(planks == 0, "a tábua posta como bloco já é contada à parte: " + planks);
        context.assertTrue(stone == 0, "escada de pedra não leva tábua: " + stone);
        context.complete();
    }

    /**
     * A obra espera quatro escadas, e a colônia tem 2 toras e 4 tábuas. Pela
     * metade a tora ficava guardada; a escada pede 6 tábuas, então a obra libera
     * a conversão.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "wood_in_pieces", tickLimit = 20)
    public void theStairsTheBuildWaitsForOpenTheWoodGate(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ChestDepositor.deposit(context.getWorld(), chest, Items.OAK_LOG, 2);
        ChestDepositor.deposit(context.getWorld(), chest, Items.OAK_PLANKS, 4);

        Colony colony = Colony.create(UUID.randomUUID(), chest);
        UUID worker = UUID.randomUUID();
        List<BlueprintBlock> stairs = new ArrayList<>();

        for (int x = 0; x < 4; x++) {
            stairs.add(new BlueprintBlock(new ColonyPos(x, 0, 0), ResourceId.vanilla("oak_stairs")));
        }

        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("test_stairs"), stairs),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(4, 2, 4))));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            VillageColonyMod.COLONIES.register(colony);
            VillageColonyMod.WORKERS.register(worker, colony.id());
            VillageColonyMod.STORAGES.register(WorkerStorage.of(worker, chest));
            VillageColonyMod.CONSTRUCTIONS.register(project);

            context.assertTrue(CraftingSteps.halfTheWoodMayStillBeConverted(context.getWorld(), colony.id()),
                    "a obra espera escada e faltam tábuas: a tora devia poder virar tábua");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.STORAGES.remove(worker);
            VillageColonyMod.WORKERS.remove(worker);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
