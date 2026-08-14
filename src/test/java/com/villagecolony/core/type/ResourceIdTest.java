package com.villagecolony.core.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * O nome de uma coisa do jogo, sem depender do jogo. ADR-005.
 */
class ResourceIdTest {

    @Test
    void aVanillaNameNeedsNoNamespace() {
        assertEquals(new ResourceId("minecraft", "oak_planks"), ResourceId.vanilla("oak_planks"));
    }

    /** Sem namespace vale minecraft, que é a mesma regra do jogo. */
    @Test
    void parsingWithoutANamespaceMeansVanilla() {
        assertEquals(ResourceId.vanilla("oak_log"), ResourceId.parse("oak_log"));
    }

    @Test
    void parsingKeepsTheNamespaceItIsGiven() {
        assertEquals(new ResourceId("villagecolony", "road"), ResourceId.parse("villagecolony:road"));
    }

    /** Caminho de estrutura tem barra, e barra não é separador. */
    @Test
    void aStructurePathSurvivesParsing() {
        ResourceId id = ResourceId.parse("minecraft:village/plains/houses/small_house");

        assertEquals("minecraft", id.namespace());
        assertEquals("village/plains/houses/small_house", id.path());
    }

    @Test
    void itWritesItselfTheWayTheGameShowsIt() {
        assertEquals("minecraft:oak_planks", ResourceId.vanilla("oak_planks").toString());
    }

    /**
     * Nome ambíguo é erro de quem escreveu.
     *
     * <p>Adivinhar qual parte é o namespace esconderia isso.
     */
    @Test
    void twoSeparatorsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("a:b:c"));
    }

    @Test
    void blankPartsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceId("", "oak_planks"));
        assertThrows(IllegalArgumentException.class, () -> new ResourceId("minecraft", " "));
    }
}
