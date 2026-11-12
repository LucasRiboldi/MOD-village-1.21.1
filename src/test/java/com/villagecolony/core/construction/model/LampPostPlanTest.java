package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A planta do poste de luz — Regra 261: pedra, duas cercas e a tocha da colônia, relida pelo nome. */
class LampPostPlanTest {

    private static final ResourceId SANDSTONE = ResourceId.vanilla("sandstone");

    private static final ResourceId SPRUCE_FENCE = ResourceId.vanilla("spruce_fence");

    @Test
    void thePostIsStoneTwoFencesAndTheColonyTorchFromTheGroundUp() {
        Blueprint plan = LampPostPlan.blueprint(SANDSTONE, SPRUCE_FENCE);

        assertEquals(List.of(SANDSTONE, SPRUCE_FENCE, SPRUCE_FENCE, LampPostPlan.COLONY_TORCH),
                plan.blocks().stream().map(BlueprintBlock::block).toList());
        assertEquals(List.of(0, 1, 2, 3), plan.blocks().stream().map(block -> block.offset().y()).toList());
        assertEquals(new ColonyPos(1, 4, 1), plan.size());
        assertTrue(plan.isBase(plan.blocks().getFirst()), "a base fica na linha do solo, a camada da rua");
        assertFalse(plan.isBuried(plan.blocks().getFirst()), "a pedra é posta no lugar do solo, não dada por assentada");
    }

    @Test
    void theNameCarriesTheMaterialsAndReadsBack() {
        ResourceId id = LampPostPlan.idOf(SANDSTONE, SPRUCE_FENCE);

        assertEquals("villagecolony:colony/lamp_post/sandstone/spruce_fence", id.toString());
        assertTrue(LampPostPlan.isLampPost(id));
        assertTrue(id.path().contains("lamp"), "o nome traz 'lamp': enfeite sem entrada (Regras 223 e 252)");
        assertEquals(LampPostPlan.blueprint(SANDSTONE, SPRUCE_FENCE).blocks(), LampPostPlan.blueprintOf(id).orElseThrow().blocks());
        assertFalse(LampPostPlan.isLampPost(ResourceId.vanilla("village/plains/plains_lamp_1")));
        assertTrue(LampPostPlan.blueprintOf(new ResourceId("villagecolony", "colony/lamp_post/cobblestone")).isEmpty());
    }

    @Test
    void theFenceComesFromTheVillageWood() {
        assertEquals(ResourceId.vanilla("acacia_fence"), LampPostPlan.fenceOf(ResourceId.vanilla("acacia_planks")));
        assertEquals(ResourceId.vanilla("oak_fence"), LampPostPlan.fenceOf(ResourceId.vanilla("bamboo_block")));
    }
}
