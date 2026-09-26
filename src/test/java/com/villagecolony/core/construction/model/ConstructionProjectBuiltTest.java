package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Construiu alguma coisa?" ignora o chão enterrado — 2026-09-26. Desde a
 * camada da rua, a terra da fundação é dada por assentada ao abrir a obra; a
 * obra que só tem isso não construiu nada, pode ceder lugar e não prende o lote.
 */
class ConstructionProjectBuiltTest {

    private static final ResourceId DIRT = ResourceId.vanilla("dirt");

    private static final ResourceId PLANKS = ResourceId.vanilla("oak_planks");

    private static ConstructionProject project() {
        Blueprint plan = Blueprint.of(ResourceId.vanilla("village/plains/houses/test_built"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), DIRT),
                new BlueprintBlock(new ColonyPos(0, 1, 0), PLANKS),
                new BlueprintBlock(new ColonyPos(1, 1, 0), PLANKS))).withStreetLayer(1);

        return ConstructionProject.plan(UUID.randomUUID(), plan, new ColonyPos(0, 60, 0));
    }

    @Test
    void onlyTheBuriedGroundIsNotBuilding() {
        ConstructionProject project = project();
        project.markPlaced(project.blueprint().blocks().get(0));

        assertFalse(project.hasBuiltAnything(), "a terra enterrada contou como obra");
        assertTrue(project.isSupersededBy(ResourceId.vanilla("village/plains/houses/other")),
                "a obra só com chão enterrado devia poder ceder lugar");
    }

    @Test
    void aPlacedPieceIsBuilding() {
        ConstructionProject project = project();
        project.markPlaced(project.blueprint().blocks().get(0));
        project.markPlaced(project.blueprint().blocks().get(1));

        assertTrue(project.hasBuiltAnything());
        assertFalse(project.isSupersededBy(ResourceId.vanilla("village/plains/houses/other")),
                "casa com peça de pé não cede lugar");
    }

    @Test
    void aFreshProjectHasBuiltNothing() {
        assertFalse(project().hasBuiltAnything());
    }
}
