package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkDemand;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.FurnaceReach;
import com.villagecolony.fabric.integration.VillageBiomes;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;

/**
 * O fundidor só persegue o que o bioma dá — sessão longa de 26-09: 582 buscas
 * por arenito e areia numa vila de planície.
 */
public class FurnaceReachGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "furnace_reach", tickLimit = 20)
    public void aPlainsVillageDoesNotChaseSmoothSandstone(TestContext context) {
        BlockPos centre = context.getAbsolutePos(new BlockPos(1, 2, 1));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(centre));
        VillageColonyMod.COLONIES.register(colony);

        try {
            String style = VillageBiomes.paletteFor(context.getWorld().getBiome(centre).getKey().orElseThrow())
                    .map(palette -> palette.style()).orElse("?");

            // A arena da bateria é planície; se um dia não for, o teste diz em vez de mentir.
            context.assertTrue(!style.equals("desert"), "a arena é deserto, o teste não mede o caso");

            Map<ResourceType, Integer> kept = FurnaceReach.withoutUnreachable(
                    context.getWorld(), colony.id(),
                    Map.of(ResourceType.SMOOTH_SANDSTONE, 16, ResourceType.STONE, 16, ResourceType.OAK_LOG, 64),
                    WorkDemand.none());

            context.assertFalse(kept.containsKey(ResourceType.SMOOTH_SANDSTONE),
                    "arenito liso sem arenito no bioma continua como meta");
            context.assertTrue(kept.containsKey(ResourceType.STONE), "pedra lisa sai de pedregulho, que o bioma dá");
            context.assertTrue(kept.get(ResourceType.OAK_LOG) == 64, "o que não é de fornalha fica intacto");
        } finally {
            VillageColonyMod.COLONIES.remove(colony.id());
            FurnaceReach.clearAll();
        }

        context.complete();
    }
}
