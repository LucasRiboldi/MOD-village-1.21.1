package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessionAssignerTest {

    private static final UUID COLONY = UUID.randomUUID();
    private static final UUID OTHER_COLONY = UUID.randomUUID();

    private WorkerService workers;

    @BeforeEach
    void setUp() {
        workers = new WorkerService();
    }

    private void addWorkers(UUID colonyId, int count) {
        for (int i = 0; i < count; i++) {
            workers.register(UUID.randomUUID(), colonyId);
        }
    }

    /** Profession-System.md: seis aldeões, um de cada função. */
    @Test
    void firstFourWorkersCoverEveryProfession() {
        addWorkers(COLONY, 4);

        assertEquals(4, ProfessionAssigner.assignMissing(workers, COLONY));

        Set<ProfessionType> assigned = EnumSet.noneOf(ProfessionType.class);

        for (Worker worker : workers.ofColony(COLONY)) {
            assigned.add(worker.profession().orElseThrow());
        }

        assertEquals(EnumSet.allOf(ProfessionType.class), assigned);
    }

    /**
     * O defeito que a recontagem por atribuição evita: sem ela, um lote
     * inteiro veria a mesma contagem e viraria quatro lenhadores.
     */
    @Test
    void aBatchIsNotAllTheSameProfession() {
        addWorkers(COLONY, 4);

        ProfessionAssigner.assignMissing(workers, COLONY);

        long lumberjacks = workers.ofColony(COLONY).stream()
                .filter(w -> w.profession().orElseThrow() == ProfessionType.LUMBERJACK)
                .count();

        assertEquals(1, lumberjacks);
    }

    /** Sem madeira nem material, um construtor não teria o que fazer. */
    @Test
    void theFirstWorkerIsALumberjack() {
        addWorkers(COLONY, 1);

        ProfessionAssigner.assignMissing(workers, COLONY);

        assertEquals(
                ProfessionType.LUMBERJACK,
                workers.ofColony(COLONY).get(0).profession().orElseThrow());
    }

    /** Coberta a base, o quinto reforça a função mais escassa. */
    @Test
    void theFifthWorkerDoublesTheScarcest() {
        addWorkers(COLONY, 5);

        ProfessionAssigner.assignMissing(workers, COLONY);

        long lumberjacks = workers.ofColony(COLONY).stream()
                .filter(w -> w.profession().orElseThrow() == ProfessionType.LUMBERJACK)
                .count();

        assertEquals(2, lumberjacks);
    }

    /** Roda a cada ciclo: sem aldeão novo não pode fazer nada. */
    @Test
    void runningAgainAssignsNobody() {
        addWorkers(COLONY, 3);
        ProfessionAssigner.assignMissing(workers, COLONY);

        assertEquals(0, ProfessionAssigner.assignMissing(workers, COLONY));
    }

    /** Realocar quem já trabalha é decisão da colônia, não daqui. */
    @Test
    void anExistingProfessionIsNeverOverwritten() {
        UUID villager = UUID.randomUUID();
        workers.restore(Worker.restore(villager, COLONY, ProfessionType.BUILDER));

        ProfessionAssigner.assignMissing(workers, COLONY);

        assertEquals(
                ProfessionType.BUILDER,
                workers.find(villager).orElseThrow().profession().orElseThrow());
    }

    /** A colônia vizinha não conta para a necessidade desta. */
    @Test
    void otherColoniesAreNotTouched() {
        addWorkers(COLONY, 2);
        addWorkers(OTHER_COLONY, 2);

        ProfessionAssigner.assignMissing(workers, COLONY);

        for (Worker worker : workers.ofColony(OTHER_COLONY)) {
            assertTrue(worker.profession().isEmpty());
        }
    }

    @Test
    void anEmptyColonyAssignsNobody() {
        assertEquals(0, ProfessionAssigner.assignMissing(workers, COLONY));
    }

    @Test
    void mostNeededOfNobodyIsTheFirstInTheChain() {
        assertEquals(ProfessionType.LUMBERJACK, ProfessionAssigner.mostNeeded(List.of()));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class,
                () -> ProfessionAssigner.assignMissing(workers, null));

        assertThrows(NullPointerException.class,
                () -> ProfessionAssigner.mostNeeded(null));
    }
}
