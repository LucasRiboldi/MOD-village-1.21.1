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
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O que surge no baú surge na quantidade toda que a obra pede, e não um por vez — regra
 * do autor, 2026-10-11 (no playtest da 0.3.29 a terracota branca saiu em 47 avisos).
 */
public class StockWholeOrderGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials", tickLimit = 20)
    public void theWholeOrderAppearsAtOnce(TestContext context) {
        BlockPos chestAt = new BlockPos(1, 2, 1);
        context.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chestAt));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        UUID builder = UUID.randomUUID();
        List<BlueprintBlock> wall = new ArrayList<>();

        for (int x = 0; x < 5; x++) {
            wall.add(new BlueprintBlock(new ColonyPos(x, 0, 0), ResourceId.vanilla("white_terracotta")));
        }

        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("test_wall"), wall),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(4, 2, 4))));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(builder, colony.id()).assign(ProfessionType.BUILDER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(builder, chest));
        VillageColonyMod.CONSTRUCTIONS.register(project);

        try {
            context.assertTrue(BiomeConstructionSupply.stockForConstruction(
                            context.getWorld(), colony.id(), chest, Items.WHITE_TERRACOTTA),
                    "a peça não surgiu no baú");

            int stocked = ColonyChests.countIn(context.getWorld(), List.of(chest), Items.WHITE_TERRACOTTA);

            context.assertTrue(stocked == 5, "a obra pede 5 e surgiram " + stocked);
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.STORAGES.remove(builder);
            VillageColonyMod.WORKERS.remove(builder);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
