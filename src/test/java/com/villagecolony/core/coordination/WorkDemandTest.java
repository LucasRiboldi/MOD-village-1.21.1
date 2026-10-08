package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * O ferro da obra e o minério dele são material de obra — playtest de
 * 2026-10-08: o funil esperou ferro e o mineiro, com a pedra de prioridade
 * maior, nunca cavou minério.
 */
class WorkDemandTest {

    @Test
    void theIronOfTheWorkMakesItsOreAConstructionMaterial() {
        WorkDemand work = new WorkDemand(0, ResourceType.COBBLESTONE, 0, 0, 0, 0, 5,
                Map.of(ResourceType.IRON_INGOT, 5), Map.of(), 0);

        Map<ResourceType, Integer> materials = work.constructionMaterials();

        assertEquals(5, materials.get(ResourceType.RAW_IRON), "o minério do funil não virou material de obra");
        assertEquals(5, materials.get(ResourceType.IRON_INGOT),
                "o lingote que chega pela receita e pelo ferro da obra foi contado duas vezes");
    }

    @Test
    void noIronInTheWorkAsksNoOre() {
        WorkDemand work = new WorkDemand(0, ResourceType.COBBLESTONE, 0, 0, 0, 0, 0,
                Map.of(), Map.of(), 0);

        assertFalse(work.constructionMaterials().containsKey(ResourceType.RAW_IRON));
    }
}
