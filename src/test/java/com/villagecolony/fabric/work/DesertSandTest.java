package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A areia da planta do deserto vira arenito — 2026-09-19.
 *
 * <p>Emenda à Regra 27 decidida pelo autor. O que estes testes trancam não
 * é a troca em si — é o <b>alcance</b> dela: fora do deserto nada muda, e
 * dentro dele só a areia muda.
 */
class DesertSandTest {

    private static final ResourceId DESERT_HOUSE =
            ResourceId.vanilla("village/desert/houses/desert_small_house_6");

    private static final ResourceId PLAINS_HOUSE =
            ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    private static BlueprintBlock at(int x, String block) {
        return new BlueprintBlock(new ColonyPos(x, 0, 0), ResourceId.vanilla(block));
    }

    /** No deserto, a areia da planta sai arenito. */
    @Test
    void theDesertPlanBakesItsSand() {
        List<BlueprintBlock> baked =
                DesertSand.baked(DESERT_HOUSE, List.of(at(0, "sand"), at(1, "red_sand")));

        assertEquals(
                List.of(ResourceId.vanilla("sandstone"), ResourceId.vanilla("red_sandstone")),
                baked.stream().map(BlueprintBlock::block).toList(),
                "a areia da planta do deserto nao virou arenito");
    }

    /**
     * Fora do deserto a Regra 27 continua inteira.
     *
     * <p>A areia da praia de uma vila de planície é da planta dela, e
     * trocá-la seria estender uma emenda que o autor não fez.
     */
    @Test
    void thePlainsPlanKeepsItsSand() {
        List<BlueprintBlock> blocks = List.of(at(0, "sand"));

        assertSame(
                blocks,
                DesertSand.baked(PLAINS_HOUSE, blocks),
                "planta fora do deserto foi alterada — a emenda vazou de estilo");

        assertFalse(
                DesertSand.isSwapped(ResourceId.vanilla("sand"), "plains"),
                "a areia da planicie foi marcada para troca");
    }

    /**
     * O arenito que a planta já pedia não é tocado.
     *
     * <p>É a metade que impede a troca de virar uma varredura: nove
     * variantes de arenito estão na mesma casa, e nenhuma delas cai.
     */
    @Test
    void theSandstoneAlreadyAskedForIsLeftAlone() {
        List<BlueprintBlock> blocks =
                List.of(at(0, "cut_sandstone"), at(1, "smooth_sandstone"), at(2, "sandstone"));

        assertSame(
                blocks,
                DesertSand.baked(DESERT_HOUSE, blocks),
                "planta sem areia foi copiada a toa");
    }

    /**
     * A troca preserva a posição e a marca de mobília.
     *
     * <p>Sem isto o bloco trocado sairia no lugar errado, ou uma peça de
     * mobília viraria estrutura — e a obra montaria a casa embaralhada.
     */
    @Test
    void theSwapKeepsThePositionAndTheFurnitureMark() {
        BlueprintBlock furniture =
                BlueprintBlock.furniture(new ColonyPos(4, 5, 6), ResourceId.vanilla("sand"));

        BlueprintBlock baked = DesertSand.baked(DESERT_HOUSE, List.of(furniture)).get(0);

        assertEquals(new ColonyPos(4, 5, 6), baked.offset(), "a troca moveu o bloco");

        assertTrue(baked.furniture(), "a troca perdeu a marca de mobilia");
    }

    /**
     * O estilo sai do caminho da planta, e não do bioma.
     *
     * <p>A vila de borda tem dois biomas debaixo dela; a planta é uma só.
     */
    @Test
    void theStyleComesFromThePlanPath() {
        assertEquals("desert", DesertSand.styleOf(DESERT_HOUSE));

        assertEquals("plains", DesertSand.styleOf(PLAINS_HOUSE));

        assertEquals(
                "",
                DesertSand.styleOf(ResourceId.vanilla("some/other/thing")),
                "caminho que nao e de vila devolveu estilo");
    }
}
