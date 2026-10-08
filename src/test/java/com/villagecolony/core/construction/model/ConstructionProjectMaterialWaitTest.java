package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A peça sem material espera de lado e a obra segue pelas outras — decisão do
 * autor, 2026-10-08 (o celeiro parou 817 blocos por um funil).
 */
class ConstructionProjectMaterialWaitTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/small_house");

    private static final ResourceId HOPPER = ResourceId.vanilla("hopper");

    private static final ResourceId COBBLE = ResourceId.vanilla("cobblestone");

    private final BlueprintBlock hopper = new BlueprintBlock(new ColonyPos(0, 0, 0), HOPPER);

    private final BlueprintBlock cobble = new BlueprintBlock(new ColonyPos(1, 0, 0), COBBLE);

    private ConstructionProject project;

    @BeforeEach
    void setUp() {
        project = ConstructionProject.plan(UUID.randomUUID(),
                Blueprint.of(HOUSE, List.of(hopper, cobble)), new ColonyPos(10, 64, 10));
    }

    @Test
    void aPieceWaitingForMaterialLetsTheNextOneThrough() {
        assertTrue(project.hasAnotherPieceThan(hopper));

        project.deferForMaterial(hopper);

        assertEquals(cobble, project.nextBlock().orElseThrow(), "a obra não seguiu para a próxima peça");
        assertEquals(SkipReason.WAITING_MATERIAL, project.deferredPieces().get(0).reason());
        assertEquals(2, project.remainingCount(), "a peça de lado deixou de contar como pendente");
    }

    @Test
    void theLastPieceHasNothingToGoOnWith() {
        project.deferForMaterial(hopper);

        assertFalse(project.hasAnotherPieceThan(cobble), "a última peça achou outra para seguir");
    }

    @Test
    void waitingForMaterialIsNeverAFailureToPlace() {
        for (int wait = 0; wait < ConstructionProject.PLACEMENT_FAILURES_BEFORE_SKIP + 2; wait++) {
            project.deferForMaterial(hopper);
            project.retry(project.deferredPieces().get(0));
        }

        assertFalse(project.failsForTheLastTime(hopper), "esperar material fez a peça ser pulada");
    }
}
