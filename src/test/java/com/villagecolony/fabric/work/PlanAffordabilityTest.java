package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A planta que a colônia consegue terminar vem primeiro — 2026-09-19.
 *
 * <p>A alternativa, caso o viveiro do fazendeiro não entregue madeira a
 * tempo: a vila de deserto de 19:29 parou a uma peça do fim esperando
 * {@code fletching_table} num bioma sem floresta ao alcance.
 */
class PlanAffordabilityTest {

    private static Blueprint planOf(String name, String... blocks) {
        List<BlueprintBlock> pieces = new java.util.ArrayList<>();

        for (int at = 0; at < blocks.length; at++) {
            pieces.add(new BlueprintBlock(
                    new ColonyPos(at, 0, 0), ResourceId.vanilla(blocks[at])));
        }

        return Blueprint.of(ResourceId.vanilla(name), pieces);
    }

    /** Conta bloco de madeira, e não tipo. */
    @Test
    void theScarceBlocksAreCountedOneByOne() {
        assertEquals(
                2,
                PlanAffordability.scarceBlocksIn(
                        planOf("a", "sandstone", "oak_planks", "oak_log", "sandstone")),
                "a conta de madeira saiu errada");

        assertEquals(
                0,
                PlanAffordability.scarceBlocksIn(planOf("b", "sandstone", "cut_sandstone")));
    }

    /** A casa de pedra vem antes da casa de madeira. */
    @Test
    void theStoneHouseComesBeforeTheWoodenOne() {
        Blueprint wooden = planOf("wooden", "oak_planks", "oak_planks", "oak_log");

        Blueprint stone = planOf("stone", "sandstone", "cut_sandstone", "oak_planks");

        List<Blueprint> order = PlanAffordability.cheapestFirst(List.of(wooden, stone));

        assertEquals(
                "stone",
                order.get(0).id().path(),
                "a casa que mais depende de madeira foi escolhida primeiro — a vila de"
                        + " deserto para na porta de novo");
    }

    /**
     * Sem madeira em planta nenhuma, a lista não é nem copiada.
     *
     * <p>O caso comum não paga nada: é a mesma escolha do
     * {@code DesertSand.baked}.
     */
    @Test
    void theListWithoutScarceMaterialIsUntouched() {
        List<Blueprint> plans = List.of(
                planOf("a", "sandstone"), planOf("b", "cut_sandstone"));

        assertSame(plans, PlanAffordability.cheapestFirst(plans));
    }

    /**
     * Ordena, e <b>não</b> descarta.
     *
     * <p>É a diferença que impede a regra de travar a vila: no deserto
     * toda casa tem porta, e filtrar deixaria a colônia sem planta
     * nenhuma.
     */
    @Test
    void everyPlanSurvivesTheOrdering() {
        List<Blueprint> plans = List.of(
                planOf("a", "oak_planks", "oak_log"),
                planOf("b", "oak_planks"),
                planOf("c", "sandstone"));

        assertEquals(3, PlanAffordability.cheapestFirst(plans).size(),
                "a ordenacao descartou planta — a vila fica sem o que construir");
    }
}
