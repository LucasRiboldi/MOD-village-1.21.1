package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * O projeto de construção — TASK-030.
 *
 * <p>Uma lista de posições relativas e o bloco de cada uma. Onde a obra
 * será erguida não pertence a ele.
 */
class BlueprintTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/small_house");

    private static final ResourceId PLANKS = ResourceId.vanilla("oak_planks");

    private static final ResourceId COBBLE = ResourceId.vanilla("cobblestone");

    private static BlueprintBlock block(int x, int y, int z, ResourceId what) {
        return new BlueprintBlock(new ColonyPos(x, y, z), what);
    }

    @Test
    void aBlueprintKeepsItsBlocksInOrder() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(0, 0, 0, COBBLE),
                block(1, 0, 0, PLANKS)));

        assertEquals(2, blueprint.blockCount());
        assertEquals(COBBLE, blueprint.blocks().get(0).block());
        assertEquals(PLANKS, blueprint.blocks().get(1).block());
    }

    /**
     * Um projeto sem bloco algum é leitura de estrutura que falhou.
     *
     * <p>Deixá-lo passar produziria uma obra que termina antes de
     * começar, e a colônia diria que construiu.
     */
    @Test
    void anEmptyBlueprintIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Blueprint.of(HOUSE, List.of()));
    }

    @Test
    void theMaterialListCountsEachBlock() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(0, 0, 0, PLANKS),
                block(1, 0, 0, PLANKS),
                block(2, 0, 0, COBBLE)));

        assertEquals(Map.of(PLANKS, 2, COBBLE, 1), blueprint.materials());
    }

    @Test
    void oneBlockIsOneBlockWide() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(block(0, 0, 0, PLANKS)));

        assertEquals(new ColonyPos(1, 1, 1), blueprint.size());
    }

    @Test
    void theSizeIsTheBoxAroundEverything() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(0, 0, 0, PLANKS),
                block(4, 2, 6, PLANKS)));

        assertEquals(new ColonyPos(5, 3, 7), blueprint.size());
    }

    /** Estrutura lida do jogo pode vir com posição negativa. */
    @Test
    void theSizeSurvivesNegativeOffsets() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(-2, 0, -2, PLANKS),
                block(2, 0, 2, PLANKS)));

        assertEquals(new ColonyPos(5, 1, 5), blueprint.size());
    }

    /** Duas obras podem correr juntas, e nenhuma altera o projeto da outra. */
    @Test
    void aBlueprintCannotBeChangedFromOutside() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(block(0, 0, 0, PLANKS)));

        assertThrows(UnsupportedOperationException.class,
                () -> blueprint.blocks().add(block(1, 0, 0, COBBLE)));

        assertThrows(UnsupportedOperationException.class,
                () -> blueprint.materials().put(COBBLE, 99));
    }
}
