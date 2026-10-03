package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A contratação sob sequências aleatórias de acontecimentos — teste de
 * propriedade com estado (jqwik), 2026-09-30.
 *
 * <p><b>Por que existe.</b> Os defeitos mais caros da contratação foram de
 * <b>transição</b>, e não de estado: a vaga que se reabre para quem a
 * largou, o castigo que vira rodízio. Um cenário escrito à mão olha um
 * estado; aqui o jqwik gera centenas de roteiros (chegam adultos, morrem
 * trabalhadores, a obra muda de demanda, a colônia contrata) e confere os
 * invariantes depois de cada contratação. Quando um cai, ele encolhe o
 * roteiro até o menor que ainda falha.
 */
class HiringProperties {

    private static final UUID COLONY = UUID.randomUUID();

    private WorkerService workers;

    @BeforeTry
    void freshColony() {
        workers = new WorkerService();
        ProfessionDemand.clearAll();
        HiringLog.clearAll();
    }

    @AfterTry
    void forgetDemand() {
        ProfessionDemand.clearAll();
    }

    /** Um acontecimento na vida da colônia. */
    sealed interface Step permits Arrive, Die, Demand, Hire {
    }

    record Arrive(int adults) implements Step {
    }

    record Die(int index) implements Step {
    }

    record Demand(List<ProfessionType> urgentFirst) implements Step {
    }

    record Hire() implements Step {
    }

    @Provide
    Arbitrary<List<Step>> scripts() {
        Arbitrary<Step> arrive = Arbitraries.integers().between(1, 6).map(Arrive::new);
        Arbitrary<Step> die = Arbitraries.integers().between(0, 40).map(Die::new);
        Arbitrary<Step> demand = Arbitraries.of(ProfessionAssigner.PRODUCER_ORDER)
                .list().ofMaxSize(3).map(Demand::new);
        Arbitrary<Step> hire = Arbitraries.just(new Hire());

        return Arbitraries.frequencyOf(
                        net.jqwik.api.Tuple.of(4, arrive),
                        net.jqwik.api.Tuple.of(1, die),
                        net.jqwik.api.Tuple.of(2, demand),
                        net.jqwik.api.Tuple.of(4, hire))
                .list().ofMinSize(1).ofMaxSize(40);
    }

    @Property(tries = 300)
    void hiringKeepsItsInvariants(@ForAll("scripts") List<Step> script) {
        for (Step step : script) {
            switch (step) {
                case Arrive arrive -> {
                    for (int i = 0; i < arrive.adults(); i++) {
                        workers.register(UUID.randomUUID(), COLONY);
                    }
                }
                case Die die -> {
                    List<Worker> colony = workers.ofColony(COLONY);

                    if (!colony.isEmpty()) {
                        workers.remove(colony.get(die.index() % colony.size()).villagerId());
                    }
                }
                case Demand demand -> ProfessionDemand.set(COLONY, demand.urgentFirst());
                case Hire hire -> hireAndCheck();
            }
        }
    }

    private void hireAndCheck() {
        int adults = workers.ofColony(COLONY).size();
        Map<ProfessionType, Integer> before = counts();

        ProfessionAssigner.assignMissing(workers, COLONY, everyone(), adults);

        Map<ProfessionType, Integer> after = counts();

        // 1. A demanda não dispara: quem acabou de ser contratado não passa de
        //    uma cabeça acima da cota da profissão (e toda função da fundação
        //    pode ter o seu primeiro titular).
        for (ProfessionType type : ProfessionType.values()) {
            if (after.get(type) > before.get(type)) {
                int ceiling = Math.max(1, ProfessionAssigner.targetCount(type, adults) + 1);

                if (after.get(type) > ceiling) {
                    throw new AssertionError(type + " foi a " + after.get(type)
                            + " com cota " + ProfessionAssigner.targetCount(type, adults)
                            + " e " + adults + " adultos");
                }
            }
        }

        // 2. Vaga aberta e adulto parado não convivem: a contratação só para
        //    quando ninguém mais cabe.
        for (Worker idle : workers.ofColony(COLONY)) {
            if (idle.profession().isEmpty()
                    && ProfessionAssigner.vacancyFor(idle, workers.ofColony(COLONY), adults).isPresent()) {
                throw new AssertionError("sobrou vaga ("
                        + ProfessionAssigner.vacancyFor(idle, workers.ofColony(COLONY), adults).get()
                        + ") com um adulto sem função, " + adults + " adultos");
            }
        }

        // 3. Contratar de novo sem nada mudar não muda nada.
        int again = ProfessionAssigner.assignMissing(workers, COLONY, everyone(), adults);

        if (again != 0) {
            throw new AssertionError("a segunda passada contratou " + again);
        }
    }

    private Map<ProfessionType, Integer> counts() {
        Map<ProfessionType, Integer> counts = new EnumMap<>(ProfessionType.class);

        for (ProfessionType type : ProfessionType.values()) {
            counts.put(type, 0);
        }

        for (Worker worker : workers.ofColony(COLONY)) {
            worker.profession().ifPresent(type -> counts.merge(type, 1, Integer::sum));
        }

        return counts;
    }

    private Set<UUID> everyone() {
        Set<UUID> ids = new HashSet<>();

        for (Worker worker : new ArrayList<>(workers.all())) {
            ids.add(worker.villagerId());
        }

        return ids;
    }
}
