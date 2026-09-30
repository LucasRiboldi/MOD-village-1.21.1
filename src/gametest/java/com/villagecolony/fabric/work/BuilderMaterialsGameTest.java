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
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Peça sem rota no bioma chega na primeira falta — F5 e F6, 2026-09-30.
 *
 * <p>No playtest das 02:45 a casa do pastor esperou 7 min 45 s pelo tear (a
 * linha não tem fonte na colônia) e 4 min 17 s pela vidraça (na planície a
 * areia não tem rota, mas a pergunta de rota não olhava o bioma e dava "tem",
 * então a peça nunca era entregue e ninguém ia buscar areia). O mundo da
 * bateria é planície.
 */
public final class BuilderMaterialsGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void aLoomWithNoStringAnywhereIsSuppliedAtTheFirstShortage(TestContext context) {
        suppliedAtTheFirstShortage(context, Items.LOOM);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void aGlassPaneWithNoSandInThePlainsIsSuppliedAtTheFirstShortage(TestContext context) {
        suppliedAtTheFirstShortage(context, Items.GLASS_PANE);
    }

    private static void suppliedAtTheFirstShortage(TestContext context, Item piece) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        UUID builder = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(builder, colony.id()).assign(ProfessionType.BUILDER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(builder, chest));

        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("village/plains/houses/materials"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0),
                                MinecraftTypeAdapter.toResourceId(piece)))),
                chest);

        try {
            context.assertTrue(
                    BuilderMaterials.hasOrStocksConstructionMaterial(context.getWorld(), project, piece),
                    piece + " sem rota no bioma não foi entregue na primeira falta");
            context.assertTrue(
                    ColonyChests.countIn(context.getWorld(), List.of(chest), piece) > 0,
                    piece + " não apareceu no baú do construtor");
        } finally {
            BiomeConstructionSupply.routeDelivered(colony.id(), piece);
            VillageColonyMod.STORAGES.remove(builder);
            VillageColonyMod.WORKERS.remove(builder);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
