package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A auditoria de remoção de obra — decisão 10A, 2026-09-24.
 */
class ConstructionServiceTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/small_house");

    private static final ResourceId COBBLE = ResourceId.vanilla("cobblestone");

    private static final ColonyPos ORIGIN = new ColonyPos(100, 64, 200);

    private ConstructionService service;

    private ConstructionProject project;

    private static BlueprintBlock block(int x, int y, int z, ResourceId what) {
        return new BlueprintBlock(new ColonyPos(x, y, z), what);
    }

    private static ConstructionProject freshProject() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(block(0, 0, 0, COBBLE)));

        return ConstructionProject.plan(UUID.randomUUID(), blueprint, ORIGIN);
    }

    @BeforeEach
    void setUp() {
        service = new ConstructionService();
        project = freshProject();
        service.register(project);
    }

    @Test
    void forgetRejectsActiveOrUnauditedProject() {
        assertFalse(service.forget(project.id(), RemovalAudit.absent()));
        assertTrue(service.find(project.id()).isPresent());
    }

    @Test
    void forgetRejectsUnknownProject() {
        assertFalse(service.forget(UUID.randomUUID(), RemovalAudit.playerCancellation()));
    }

    @Test
    void playerCancellationRemovesAnyOpenProject() {
        assertTrue(project.state().isOpen());

        assertTrue(service.forget(project.id(), RemovalAudit.playerCancellation()));
        assertTrue(service.find(project.id()).isEmpty());
    }

    @Test
    void patienceAbandonmentDoesNotRemoveAProjectStillPlanned() {
        // PLANNED, nem WAITING_RESOURCES nem BUILDING: a colônia não
        // está esperando material nem parada sem andar.
        assertFalse(service.forget(project.id(), RemovalAudit.patienceAbandonment()));
        assertTrue(service.find(project.id()).isPresent());
    }

    @Test
    void patienceAbandonmentRemovesAProjectWaitingForResources() {
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.WAITING_RESOURCES);

        assertTrue(service.forget(project.id(), RemovalAudit.patienceAbandonment()));
        assertTrue(service.find(project.id()).isEmpty());
    }

    /**
     * O "fundo de poço" de 2026-09-19 — {@code WaitingWork.givesUpIfItIsNotMoving}:
     * obra com tudo em mãos, parada em BUILDING sem assentar peça.
     */
    @Test
    void patienceAbandonmentRemovesAProjectStalledWhileBuilding() {
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        assertTrue(service.forget(project.id(), RemovalAudit.patienceAbandonment()));
        assertTrue(service.find(project.id()).isEmpty());
    }

    @Test
    void completedProjectPurgeOnlyRemovesAFinishedProject() {
        assertFalse(service.forget(project.id(), RemovalAudit.completedProjectPurge()));
        assertTrue(service.find(project.id()).isPresent());
    }

    @Test
    void completedProjectPurgeRemovesAFinishedProject() {
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        project.moveTo(ConstructionState.COMPLETED);

        assertTrue(service.forget(project.id(), RemovalAudit.completedProjectPurge()));
    }

    @Test
    void nullProjectIdIsAlwaysRejected() {
        assertFalse(service.forget(null, RemovalAudit.playerCancellation()));
    }
}
