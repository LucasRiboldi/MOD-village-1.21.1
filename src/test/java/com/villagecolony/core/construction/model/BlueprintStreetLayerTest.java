package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A camada da rua — sessão de jogo de 2026-09-26, pedido do autor: "não deve
 * ser construído as camadas de terra na base das construções; o chão da
 * construção e a porta devem estar na altura da rua".
 *
 * <p>Toda casa de vila do jogo marca com o encaixe da rua a altura do caminho.
 * Na {@code plains_small_house_5} ele está na camada 1, e a camada 0 é só
 * terra: fundação enterrada. O mod punha a camada 0 um bloco acima do chão, e
 * a casa saía sobre um monte de terra com a porta três blocos acima da rua.
 */
class BlueprintStreetLayerTest {

    private static final ResourceId DIRT = ResourceId.vanilla("dirt");

    private static final ResourceId GRASS = ResourceId.vanilla("grass_block");

    private static final ResourceId COBBLE = ResourceId.vanilla("cobblestone");

    private static final ResourceId PLANKS = ResourceId.vanilla("oak_planks");

    private static final ResourceId DOOR = ResourceId.vanilla("oak_door");

    /** A casa 5 de planície, em miniatura: terra em 0, piso e degrau em 1, porta em 2. */
    private static Blueprint smallHouseFive() {
        return Blueprint.of(ResourceId.vanilla("village/plains/houses/test_five"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), DIRT),
                new BlueprintBlock(new ColonyPos(1, 0, 0), DIRT),
                new BlueprintBlock(new ColonyPos(0, 1, 0), COBBLE),
                new BlueprintBlock(new ColonyPos(1, 1, 0), PLANKS),
                new BlueprintBlock(new ColonyPos(2, 1, 0), GRASS),
                new BlueprintBlock(new ColonyPos(1, 2, 0), DOOR))).withStreetLayer(1);
    }

    @Test
    void aPlanReadWithoutAStreetConnectorHasNoStreetLayer() {
        Blueprint plain = Blueprint.of(ResourceId.vanilla("x"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), DIRT)));

        assertFalse(plain.hasStreetLayer());
        assertFalse(plain.isBuried(plain.blocks().get(0)), "sem camada da rua nada é enterrado");
        assertEquals(new ColonyPos(5, 70, 5), plain.originFor(new ColonyPos(5, 70, 5)),
                "sem camada da rua a origem é a de sempre, um acima do chão");
    }

    /** O que fica abaixo da rua é fundação enterrada; terra e grama na altura da rua, o chão já é. */
    @Test
    void theFoundationBelowTheStreetAndTheSoilOnItAreBuried() {
        Blueprint house = smallHouseFive();

        assertTrue(house.isBuried(house.blocks().get(0)), "terra abaixo da rua");
        assertTrue(house.isBuried(house.blocks().get(4)), "grama na altura da rua");
        assertFalse(house.isBuried(house.blocks().get(2)), "o pedregulho do piso é construído");
        assertFalse(house.isBuried(house.blocks().get(3)), "a tábua do piso é construída");
        assertFalse(house.isBuried(house.blocks().get(5)), "a porta é construída");
    }

    /**
     * O lote diz "o piso vai em y = 70" (um acima do chão, que está em 69). A
     * camada da rua tem de cair no chão — 69 —, e para isso a origem desce a
     * altura da camada mais um.
     */
    @Test
    void theStreetLayerLandsOnTheGround() {
        Blueprint house = smallHouseFive();

        ColonyPos origin = house.originFor(new ColonyPos(5, 70, 5));

        assertEquals(new ColonyPos(5, 68, 5), origin);
        assertEquals(69, origin.y() + house.streetLayer(), "a camada da rua no chão");
        assertEquals(70, origin.y() + 2, "a porta um acima do chão, na altura de quem anda na rua");
    }

    @Test
    void aStreetLayerAtTheBottomSinksTheOriginByOne() {
        Blueprint floorAtZero = Blueprint.of(ResourceId.vanilla("x"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), COBBLE))).withStreetLayer(0);

        assertEquals(new ColonyPos(1, 69, 1), floorAtZero.originFor(new ColonyPos(1, 70, 1)));
        assertFalse(floorAtZero.isBuried(floorAtZero.blocks().get(0)), "o piso na camada 0 é construído");
    }

    @Test
    void turningThePlanKeepsItsStreetLayer() {
        assertEquals(1, smallHouseFive().rotated(1).streetLayer());
        assertTrue(smallHouseFive().rotated(3).hasStreetLayer());
    }

    @Test
    void everyGroundBlockOfTheBiomesCountsAsSoil() {
        for (String soil : List.of("dirt", "grass_block", "coarse_dirt", "podzol", "mycelium",
                "rooted_dirt", "sand", "red_sand", "snow", "snow_block", "mud")) {
            Blueprint plan = Blueprint.of(ResourceId.vanilla("x"), List.of(
                    new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla(soil)))).withStreetLayer(0);

            assertTrue(plan.isBuried(plan.blocks().get(0)), soil + " na altura da rua devia ser o chão");
        }

        for (String built : List.of("dirt_path", "farmland", "gravel", "cobblestone", "oak_stairs")) {
            Blueprint plan = Blueprint.of(ResourceId.vanilla("x"), List.of(
                    new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla(built)))).withStreetLayer(0);

            assertFalse(plan.isBuried(plan.blocks().get(0)), built + " na altura da rua é construído");
        }
    }
}
