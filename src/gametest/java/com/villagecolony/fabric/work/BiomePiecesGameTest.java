package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** As filas de adiantamento seguem as peças das plantas do bioma (ADR-039 item 4). */
public class BiomePiecesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours", tickLimit = 20)
    public void aTaigaHouseAsksForSprucePieces(TestContext context) {
        List<BlueprintBlock> blocks = new ArrayList<>();
        add(blocks, "spruce_stairs", 3);
        add(blocks, "spruce_slab", 1);
        add(blocks, "spruce_planks", 9);
        add(blocks, "stone_bricks", 2);
        add(blocks, "smooth_stone", 1);

        Map<String, List<ResourceType>> lanes =
                BiomePieces.lanesFrom(List.of(Blueprint.of(ResourceId.vanilla("test_taiga_house"), blocks)));

        context.assertTrue(List.of(ResourceType.SPRUCE_STAIRS, ResourceType.SPRUCE_SLAB).equals(lanes.get("carpenter")),
                "o carpinteiro da taiga devia adiantar pinheiro, mais usado primeiro: " + lanes.get("carpenter"));
        context.assertTrue(List.of(ResourceType.STONE_BRICKS).equals(lanes.get("mason")),
                "o pedreiro devia adiantar o tijolo da planta: " + lanes.get("mason"));
        context.assertTrue(List.of(ResourceType.SMOOTH_STONE).equals(lanes.get("smelter")),
                "o fundidor devia adiantar a pedra lisa da planta: " + lanes.get("smelter"));
        context.complete();
    }

    /**
     * O teto do adiantamento é a maior casa do estilo mais o que a obra aberta
     * ainda pede (P1, 2026-10-11): a próxima casa é sorteada, e a maior cobre
     * qualquer uma.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours", tickLimit = 20)
    public void theCeilingIsTheLargestHousePlusTheOpenBuild(TestContext context) {
        List<BlueprintBlock> small = new ArrayList<>();
        add(small, "oak_stairs", 4);
        add(small, "cobblestone_slab", 6);

        List<BlueprintBlock> large = new ArrayList<>();
        add(large, "oak_stairs", 10);

        Map<ResourceType, Integer> largest = BiomePieces.largestUse(List.of(
                Blueprint.of(ResourceId.vanilla("test_small"), small),
                Blueprint.of(ResourceId.vanilla("test_large"), large)));

        Map<ResourceType, Integer> ceilings = BiomePieces.ceilings(largest,
                Map.of(ResourceId.vanilla("oak_stairs"), 3, ResourceId.vanilla("glass_pane"), 2));

        context.assertTrue(Integer.valueOf(13 + BiomePieces.RESERVE).equals(ceilings.get(ResourceType.OAK_STAIRS)),
                "escada: a maior casa (10), a reserva e a obra aberta (3), e não a soma das casas: " + ceilings);
        context.assertTrue(Integer.valueOf(6 + BiomePieces.RESERVE).equals(ceilings.get(ResourceType.COBBLESTONE_SLAB)),
                "laje: só a casa que a usa, mais a reserva: " + ceilings);
        context.complete();
    }

    private static void add(List<BlueprintBlock> blocks, String block, int count) {
        for (int i = 0; i < count; i++) {
            blocks.add(new BlueprintBlock(new ColonyPos(blocks.size(), 0, 0), ResourceId.vanilla(block), false, Optional.empty()));
        }
    }
}
