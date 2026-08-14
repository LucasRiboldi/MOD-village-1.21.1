package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A obra: este projeto, neste lugar, neste estado — TASK-032.
 */
class ConstructionProjectTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/small_house");

    private static final ResourceId PLANKS = ResourceId.vanilla("oak_planks");

    private static final ResourceId COBBLE = ResourceId.vanilla("cobblestone");

    private static final ColonyPos ORIGIN = new ColonyPos(100, 64, 200);

    private ConstructionProject project;

    private static BlueprintBlock block(int x, int y, int z, ResourceId what) {
        return new BlueprintBlock(new ColonyPos(x, y, z), what);
    }

    @BeforeEach
    void setUp() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(0, 0, 0, COBBLE),
                block(1, 0, 0, COBBLE),
                block(0, 1, 0, PLANKS)));

        project = ConstructionProject.plan(UUID.randomUUID(), blueprint, ORIGIN);
    }

    @Test
    void aNewProjectIsPlannedAndUntouched() {
        assertEquals(ConstructionState.PLANNED, project.state());
        assertEquals(3, project.remainingCount());
        assertFalse(project.isFinished());
    }

    @Test
    void theProjectSitsWhereItsOriginIs() {
        BlueprintBlock top = block(0, 1, 0, PLANKS);

        assertEquals(new ColonyPos(100, 65, 200), project.worldPositionOf(top));
    }

    @Test
    void theMaterialListCountsTheWholeHouse() {
        assertEquals(Map.of(COBBLE, 2, PLANKS, 1), project.materials());
    }

    /** Pedir de novo o que já está na parede mandaria cortar madeira à toa. */
    @Test
    void whatIsBuiltLeavesTheShoppingList() {
        project.markPlaced(block(0, 0, 0, COBBLE));

        assertEquals(Map.of(COBBLE, 1, PLANKS, 1), project.remainingMaterials());
        assertEquals(2, project.remainingCount());
    }

    @Test
    void placingEverythingFinishesTheProject() {
        for (BlueprintBlock block : project.remaining()) {
            assertTrue(project.markPlaced(block));
        }

        assertTrue(project.isFinished());
        assertTrue(project.nextBlock().isEmpty());
        assertEquals(Map.of(), project.remainingMaterials());
    }

    /** O construtor pode repetir um passo, e o bloco não conta duas vezes. */
    @Test
    void placingTheSameBlockTwiceCountsOnce() {
        BlueprintBlock first = block(0, 0, 0, COBBLE);

        assertTrue(project.markPlaced(first));
        assertFalse(project.markPlaced(first));
        assertEquals(2, project.remainingCount());
    }

    // --- estados ---

    @Test
    void aProjectWalksThroughItsStates() {
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        project.moveTo(ConstructionState.COMPLETED);

        assertEquals(ConstructionState.COMPLETED, project.state());
        assertFalse(project.state().isOpen());
    }

    /** Faltar material no meio da obra é normal, e se volta dele. */
    @Test
    void aProjectCanWaitForMaterialAndGoOn() {
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        project.moveTo(ConstructionState.WAITING_RESOURCES);
        project.moveTo(ConstructionState.BUILDING);

        assertEquals(ConstructionState.BUILDING, project.state());
    }

    /**
     * Não se volta para o começo.
     *
     * <p>Uma obra que voltasse a PLANNED teria blocos já colocados e um
     * plano que os ignora.
     */
    @Test
    void aProjectNeverGoesBackToPlanned() {
        project.moveTo(ConstructionState.PREPARING);

        assertThrows(IllegalStateException.class,
                () -> project.moveTo(ConstructionState.PLANNED));
    }

    @Test
    void aFinishedProjectIsFinal() {
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        project.moveTo(ConstructionState.COMPLETED);

        assertThrows(IllegalStateException.class,
                () -> project.moveTo(ConstructionState.BUILDING));
    }

    @Test
    void buildingCannotStartBeforePreparing() {
        assertThrows(IllegalStateException.class,
                () -> project.moveTo(ConstructionState.BUILDING));
    }
}
