package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A horta da obra é moldada no lugar (Regra 69): terra arada, trigo e água não viram pedido ao
 * fabricante nem busca no chão (playtest 0.3.97: 19 pedidos de trigo ao carpinteiro).
 */
public class GardenPiecesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "garden_pieces", tickLimit = 20)
    public void theGardenOfTheBuildNeedsNoStock(TestContext context) {
        for (String garden : List.of("farmland", "wheat", "water", "carrots")) {
            context.assertTrue(AppearingPieces.needsNoStock(ResourceId.vanilla(garden)),
                    garden + " devia ser moldado no lugar, sem estoque");
        }
        context.assertFalse(AppearingPieces.needsNoStock(ResourceId.vanilla("oak_planks")),
                "tábua é material de estoque");

        ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(),
                Blueprint.of(ResourceId.vanilla("garden_house"), List.of(
                        new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("farmland")),
                        new BlueprintBlock(new ColonyPos(0, 1, 0), ResourceId.vanilla("wheat")),
                        new BlueprintBlock(new ColonyPos(1, 0, 0), ResourceId.vanilla("oak_planks")))),
                new ColonyPos(0, 64, 0));

        Set<net.minecraft.item.Item> wanted = GroundPickup.itemsOf(project);

        context.assertTrue(wanted.equals(Set.of(Items.OAK_PLANKS)),
                "do chão só a tábua devia ser recolhida para a obra, veio " + wanted);
        context.complete();
    }
}
