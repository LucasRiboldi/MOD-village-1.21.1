package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A caixa de uma construção e a pergunta da fusão de vilas — 2026-09-25.
 *
 * <p>O PIT deixou 12 mutações passar aqui: nenhum teste media o canto de
 * cima da caixa em cada eixo, nem a fronteira exata do "encosta". São as
 * duas contas em que errar por um bloco não aparece na hora: a casa
 * protege um bloco a mais ou a menos, e duas vilas se fundem, ou não,
 * por um bloco de vão.
 */
class BuildingTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/small");

    private static final ResourceId PLANKS = ResourceId.vanilla("oak_planks");

    /** Uma caixa de 3 x 3 x 3, de (10, 60, 20) a (12, 62, 22). */
    private static final Building BOX = box(10, 60, 20, 12, 62, 22);

    private static Building box(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new Building(UUID.randomUUID(), UUID.randomUUID(), HOUSE,
                new ColonyPos(x1, y1, z1), new ColonyPos(x2, y2, z2));
    }

    /**
     * Um bloco de lado, colado em BOX pela face no eixo dado.
     *
     * @param gap blocos de ar entre os dois: 0 é encostar
     */
    private static Building beside(char axis, boolean after, int gap) {
        int x = axis == 'x' ? (after ? 13 + gap : 9 - gap) : 11;
        int y = axis == 'y' ? (after ? 63 + gap : 59 - gap) : 61;
        int z = axis == 'z' ? (after ? 23 + gap : 19 - gap) : 21;

        return box(x, y, z, x, y, z);
    }

    @Test
    void theBoxOfAProjectEndsAtItsLastBlockOnEveryAxis() {
        // Tamanhos diferentes por eixo: com os três iguais, trocar a conta
        // de um eixo pela de outro não mudaria nada.
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), PLANKS),
                new BlueprintBlock(new ColonyPos(4, 2, 6), PLANKS)));
        ColonyPos origin = new ColonyPos(100, 64, 200);

        Building building = Building.of(ConstructionProject.plan(UUID.randomUUID(), blueprint, origin));

        assertEquals(origin, building.min());
        assertEquals(new ColonyPos(104, 66, 206), building.max());
        assertTrue(building.contains(new ColonyPos(104, 66, 206)));
        assertFalse(building.contains(new ColonyPos(105, 66, 206)));
        assertFalse(building.contains(new ColonyPos(104, 67, 206)));
        assertFalse(building.contains(new ColonyPos(104, 66, 207)));
    }

    @Test
    void touchingByAFaceMergesOnEveryAxisAndBothSides() {
        for (char axis : new char[] {'x', 'y', 'z'}) {
            for (boolean after : new boolean[] {true, false}) {
                Building neighbour = beside(axis, after, 0);

                assertTrue(BOX.touches(neighbour),
                        "encostados pela face no eixo " + axis + (after ? " (depois)" : " (antes)"));
                assertTrue(neighbour.touches(BOX), "a pergunta é simétrica, eixo " + axis);
            }
        }
    }

    @Test
    void aOneBlockGapKeepsThemApartOnEveryAxisAndBothSides() {
        for (char axis : new char[] {'x', 'y', 'z'}) {
            for (boolean after : new boolean[] {true, false}) {
                Building neighbour = beside(axis, after, 1);

                assertFalse(BOX.touches(neighbour),
                        "um bloco de vão no eixo " + axis + (after ? " (depois)" : " (antes)"));
                assertFalse(neighbour.touches(BOX), "a pergunta é simétrica, eixo " + axis);
            }
        }
    }

    @Test
    void anOverlapTouches() {
        assertTrue(BOX.touches(box(11, 61, 21, 11, 61, 21)));
        assertTrue(BOX.touches(BOX));
    }
}
