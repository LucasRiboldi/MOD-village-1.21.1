package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O que a colônia levantou — Fase 11, TASK-036 e TASK-037.
 */
class BuildingRegistryTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/small");

    private static final ResourceId PLANKS = ResourceId.vanilla("oak_planks");

    private BuildingRegistry registry;

    private ConstructionService projects;

    @BeforeEach
    void setUp() {
        registry = new BuildingRegistry();
        projects = new ConstructionService();
    }

    /** Uma casa de dois por dois por dois, no canto informado. */
    private static ConstructionProject houseAt(UUID colonyId, ColonyPos origin) {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), PLANKS),
                new BlueprintBlock(new ColonyPos(1, 1, 1), PLANKS)));

        return ConstructionProject.plan(colonyId, blueprint, origin);
    }

    // --- o registro de construções ---

    @Test
    void aFinishedProjectBecomesABuilding() {
        Building building = Building.of(houseAt(UUID.randomUUID(), new ColonyPos(10, 64, 10)));

        registry.register(building);

        assertEquals(1, registry.count());
        assertTrue(registry.isColonyInfrastructure(new ColonyPos(10, 64, 10)));
        assertTrue(registry.isColonyInfrastructure(new ColonyPos(11, 65, 11)));
    }

    @Test
    void whatIsOutsideTheBoxIsNotColonyInfrastructure() {
        registry.register(Building.of(houseAt(UUID.randomUUID(), new ColonyPos(10, 64, 10))));

        assertFalse(registry.isColonyInfrastructure(new ColonyPos(12, 64, 10)));
        assertFalse(registry.isColonyInfrastructure(new ColonyPos(10, 63, 10)));
    }

    @Test
    void theRegistryKnowsWhoseBuildingItIs() {
        UUID colony = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        registry.register(Building.of(houseAt(colony, new ColonyPos(0, 64, 0))));
        registry.register(Building.of(houseAt(other, new ColonyPos(100, 64, 100))));

        assertEquals(1, registry.ofColony(colony).size());
        assertEquals(1, registry.ofColony(other).size());
    }

    /**
     * A pergunta da fusão: duas vilas viram uma quando um bloco de uma
     * encosta no bloco da outra.
     */
    @Test
    void touchingBuildingsOfDifferentColoniesAreNeighbours() {
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        Building mine = Building.of(houseAt(one, new ColonyPos(0, 64, 0)));
        Building yours = Building.of(houseAt(other, new ColonyPos(2, 64, 0)));

        registry.register(mine);
        registry.register(yours);

        assertEquals(List.of(yours), registry.foreignNeighboursOf(mine));
    }

    /** Casa da mesma colônia encostada não é fusão de coisa alguma. */
    @Test
    void twoHousesOfTheSameColonyAreNotForeignNeighbours() {
        UUID colony = UUID.randomUUID();

        Building first = Building.of(houseAt(colony, new ColonyPos(0, 64, 0)));

        registry.register(first);
        registry.register(Building.of(houseAt(colony, new ColonyPos(2, 64, 0))));

        assertTrue(registry.foreignNeighboursOf(first).isEmpty());
    }

    @Test
    void buildingsFarApartDoNotTouch() {
        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        Building mine = Building.of(houseAt(one, new ColonyPos(0, 64, 0)));

        registry.register(mine);
        registry.register(Building.of(houseAt(other, new ColonyPos(50, 64, 50))));

        assertTrue(registry.foreignNeighboursOf(mine).isEmpty());
    }

    @Test
    void invertedBoundsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Building(
                UUID.randomUUID(),
                UUID.randomUUID(),
                HOUSE,
                new ColonyPos(10, 64, 10),
                new ColonyPos(0, 64, 10)));
    }

    // --- o registro de obras ---

    @Test
    void aColonyHasOneOpenProjectAtATime() {
        UUID colony = UUID.randomUUID();

        projects.register(houseAt(colony, new ColonyPos(0, 64, 0)));

        assertThrows(IllegalStateException.class,
                () -> projects.register(houseAt(colony, new ColonyPos(20, 64, 20))));
    }

    /** Terminada uma, a colônia pode começar outra. */
    @Test
    void afterFinishingOneAnotherCanStart() {
        UUID colony = UUID.randomUUID();

        ConstructionProject first = houseAt(colony, new ColonyPos(0, 64, 0));

        projects.register(first);

        first.moveTo(ConstructionState.PREPARING);
        first.moveTo(ConstructionState.BUILDING);
        first.moveTo(ConstructionState.COMPLETED);

        assertTrue(projects.openOf(colony).isEmpty());

        projects.register(houseAt(colony, new ColonyPos(20, 64, 20)));

        assertTrue(projects.openOf(colony).isPresent());
    }

    /** O canteiro sai do registro quando a casa fica pronta. */
    @Test
    void finishedProjectsArePurged() {
        UUID colony = UUID.randomUUID();

        ConstructionProject project = houseAt(colony, new ColonyPos(0, 64, 0));

        projects.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        project.moveTo(ConstructionState.COMPLETED);

        assertEquals(1, projects.purgeFinished());
        assertEquals(0, projects.count());
    }

    @Test
    void anOngoingProjectIsNotPurged() {
        projects.register(houseAt(UUID.randomUUID(), new ColonyPos(0, 64, 0)));

        assertEquals(0, projects.purgeFinished());
        assertEquals(1, projects.count());
    }
}
