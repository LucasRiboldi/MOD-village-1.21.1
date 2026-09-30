package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A contratação olha primeiro a profissão de que a obra depende — decisão
 * do autor, 2026-09-30. Ver {@link ProfessionDemand}.
 */
class ProfessionDemandTest {

    private static final UUID COLONY = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void forget() {
        ProfessionDemand.clearAll();
        HiringLog.clearAll();
    }

    private static Worker hired(ProfessionType profession) {
        return Worker.restore(UUID.randomUUID(), COLONY, profession);
    }

    /** Fundação completa, dezesseis adultos: há vaga para segundo mineiro e fazendeiro. */
    private static List<Worker> colonyWith(Worker candidate) {
        List<Worker> colony = new ArrayList<>(
                ProfessionAssigner.FOUNDATION_ORDER.stream()
                        .map(ProfessionDemandTest::hired).toList());
        colony.add(candidate);
        return colony;
    }

    @Test
    void withoutDemandTheFixedOrderDecides() {
        Worker candidate = Worker.register(UUID.randomUUID(), COLONY);

        assertEquals(Optional.of(ProfessionType.MINER),
                ProfessionAssigner.vacancyFor(candidate, colonyWith(candidate), 16));
    }

    @Test
    void theProfessionTheWorkDependsOnComesFirst() {
        Worker candidate = Worker.register(UUID.randomUUID(), COLONY);
        ProfessionDemand.set(COLONY, List.of(ProfessionType.CARPENTER));

        assertEquals(Optional.of(ProfessionType.CARPENTER),
                ProfessionAssigner.vacancyFor(candidate, colonyWith(candidate), 16));
    }

    @Test
    void theLargestShortageIsServedFirst() {
        Worker candidate = Worker.register(UUID.randomUUID(), COLONY);
        ProfessionDemand.set(COLONY, List.of(ProfessionType.SMELTER, ProfessionType.CARPENTER));

        assertEquals(Optional.of(ProfessionType.SMELTER),
                ProfessionAssigner.vacancyFor(candidate, colonyWith(candidate), 16));
    }

    /** A demanda redistribui vaga; ela não cria vaga onde a população não abre. */
    @Test
    void demandDoesNotOpenASlotThePopulationDoesNotHave() {
        Worker candidate = Worker.register(UUID.randomUUID(), COLONY);
        List<Worker> colony = colonyWith(candidate);
        colony.add(hired(ProfessionType.FARMER));
        ProfessionDemand.set(COLONY, List.of(ProfessionType.CARPENTER));

        assertEquals(Optional.empty(),
                ProfessionAssigner.vacancyFor(candidate, colony, 8));
    }

    /** Uma cabeça acima da cota é o teto: a demanda não leva todas as vagas. */
    @Test
    void demandStopsOneAboveTheQuota() {
        Worker candidate = Worker.register(UUID.randomUUID(), COLONY);
        List<Worker> colony = colonyWith(candidate);
        colony.add(hired(ProfessionType.CARPENTER));
        ProfessionDemand.set(COLONY, List.of(ProfessionType.CARPENTER));

        assertEquals(Optional.of(ProfessionType.MINER),
                ProfessionAssigner.vacancyFor(candidate, colony, 16));
    }
}
