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

    /**
     * Cada fonte da obra vira material da obra, com a quantidade dela — AUD-06.
     *
     * <p>É a lista que destrava a obra antes da produção de reserva; sem este
     * teste, apagar qualquer uma das fontes passava pelo PIT.
     */
    @Test
    void everySourceOfTheWorkBecomesAConstructionMaterial() {
        WorkDemand work = new WorkDemand(3, ResourceType.SANDSTONE, 7, 2, 4, 1, 0,
                Map.of(ResourceType.SMOOTH_STONE, 6),
                Map.of(ResourceType.CLAY_BALL, 8),
                Map.of(ResourceType.CALCITE, 9));

        assertEquals(Map.of(
                        ResourceType.SANDSTONE, 7,
                        ResourceType.OAK_PLANKS, 3,
                        ResourceType.WHITE_WOOL, 2,
                        ResourceType.GLASS, 4,
                        ResourceType.COAL, 1,
                        ResourceType.SMOOTH_STONE, 6,
                        ResourceType.CLAY_BALL, 8,
                        ResourceType.CALCITE, 9),
                work.constructionMaterials());
    }

    /** O mesmo material pedido por duas fontes soma — vidro da obra e vidro assado. */
    @Test
    void theSameMaterialFromTwoSourcesIsSummed() {
        WorkDemand work = new WorkDemand(0, ResourceType.COBBLESTONE, 0, 0, 3, 0, 0,
                Map.of(ResourceType.GLASS, 2), Map.of(), Map.of());

        assertEquals(Map.of(ResourceType.GLASS, 5), work.constructionMaterials());
    }

    /** Quantidade zero não pede nada: nem a chave entra na lista. */
    @Test
    void nothingAskedMeansNoMaterial() {
        WorkDemand work = new WorkDemand(0, ResourceType.COBBLESTONE, 0, 0, 0, 0, 0,
                Map.of(ResourceType.SMOOTH_STONE, 0), Map.of(ResourceType.CLAY_BALL, 0),
                Map.of(ResourceType.CALCITE, 0));

        assertEquals(Map.of(), work.constructionMaterials());
    }

    /** Lingote pela receita e pelo ferro da obra: vale o maior, nunca a soma. */
    @Test
    void theIngotTakesTheLargerOfTheTwoAsks() {
        WorkDemand work = new WorkDemand(0, ResourceType.COBBLESTONE, 0, 0, 0, 0, 5,
                Map.of(ResourceType.IRON_INGOT, 3), Map.of(), Map.of());

        assertEquals(5, work.constructionMaterials().get(ResourceType.IRON_INGOT));
        assertEquals(5, work.constructionMaterials().get(ResourceType.RAW_IRON));
    }

    @Test
    void noIronInTheWorkAsksNoOre() {
        WorkDemand work = new WorkDemand(0, ResourceType.COBBLESTONE, 0, 0, 0, 0, 0,
                Map.of(), Map.of(), 0);

        assertFalse(work.constructionMaterials().containsKey(ResourceType.RAW_IRON));
    }
}
