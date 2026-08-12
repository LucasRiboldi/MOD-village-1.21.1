package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /** Todo mundo apto: o caso de uma vila só de adultos. */
    private Set<UUID> everyone() {
        Set<UUID> ids = new HashSet<>();

        for (Worker worker : workers.all()) {
            ids.add(worker.villagerId());
        }

        return ids;
    }

    /** Profession-System.md: seis aldeões, um de cada função. */
    @Test
    void firstFourWorkersCoverEveryProfession() {
        addWorkers(COLONY, 4);

        assertEquals(4, ProfessionAssigner.assignMissing(workers, COLONY, everyone()));

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

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        long lumberjacks = workers.ofColony(COLONY).stream()
                .filter(w -> w.profession().orElseThrow() == ProfessionType.LUMBERJACK)
                .count();

        assertEquals(1, lumberjacks);
    }

    /** Sem madeira nem material, um construtor não teria o que fazer. */
    @Test
    void theFirstWorkerIsALumberjack() {
        addWorkers(COLONY, 1);

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertEquals(
                ProfessionType.LUMBERJACK,
                workers.ofColony(COLONY).get(0).profession().orElseThrow());
    }

    /**
     * Cobertas as quatro vagas, o quinto aldeão continua Vanilla.
     *
     * <p>Decisão do autor em 2026-08-12: uma vila tem um trabalhador de
     * cada tipo. Antes disto a vaga era ilimitada, e a vila de quarenta e
     * três aldeões do autor acabou com seis lenhadores disputando tarefa
     * a cada ciclo.
     */
    @Test
    void theFifthWorkerGetsNothing() {
        addWorkers(COLONY, 5);

        int assigned = ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertEquals(4, assigned, "esperava as quatro vagas e nada além");

        long employed = workers.ofColony(COLONY).stream()
                .filter(Worker::hasProfession)
                .count();

        assertEquals(4, employed);
    }

    /** Uma vila grande emprega quatro, e só. */
    @Test
    void oneOfEachProfessionAndNoMore() {
        addWorkers(COLONY, 43);

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        for (ProfessionType type : ProfessionType.values()) {
            long count = workers.ofColony(COLONY).stream()
                    .filter(w -> w.profession().filter(type::equals).isPresent())
                    .count();

            assertEquals(1, count, "profissão duplicada: " + type);
        }
    }

    /** Com as quatro preenchidas, não há vaga. */
    @Test
    void thereIsNoVacancyOnceEveryProfessionIsFilled() {
        addWorkers(COLONY, 4);
        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertTrue(ProfessionAssigner.vacancy(workers.ofColony(COLONY)).isEmpty());
    }

    /**
     * O save antigo é acertado ao carregar.
     *
     * <p>A colônia do autor chegou com seis lenhadores gravados. Uma
     * regra que só valesse para aldeão novo nunca os desfaria.
     */
    @Test
    void anOldSaveWithSixLumberjacksIsTrimmed() {
        addWorkers(COLONY, 6);

        for (Worker worker : workers.ofColony(COLONY)) {
            worker.assign(ProfessionType.LUMBERJACK);
        }

        Set<UUID> demoted = ProfessionAssigner.enforceVacancies(workers, COLONY);

        assertEquals(5, demoted.size());

        long lumberjacks = workers.ofColony(COLONY).stream()
                .filter(w -> w.profession().filter(ProfessionType.LUMBERJACK::equals).isPresent())
                .count();

        assertEquals(1, lumberjacks);
    }

    /**
     * Quem perde a função volta a ser candidato.
     *
     * <p>Não é remoção: ele continua trabalhador da colônia e assume a
     * primeira vaga que abrir — quando o lenhador morrer, por exemplo.
     */
    @Test
    void aDismissedWorkerCanBeHiredAgain() {
        addWorkers(COLONY, 6);

        for (Worker worker : workers.ofColony(COLONY)) {
            worker.assign(ProfessionType.LUMBERJACK);
        }

        ProfessionAssigner.enforceVacancies(workers, COLONY);

        int assigned = ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertEquals(3, assigned, "as três vagas restantes deviam ser preenchidas");
    }

    /**
     * Entre dois candidatos, a vaga fica com quem pode trabalhar.
     *
     * <p>O servidor de 2026-08-12 mostrou o custo de não fazer isso: dos
     * treze trabalhadores com baú da vila, a vaga de lenhador ficou com
     * o único sem, e a tarefa voltava para a fila a cada ciclo.
     */
    @Test
    void theVacancyGoesToSomeoneWhoCanWork() {
        addWorkers(COLONY, 3);

        List<Worker> all = workers.ofColony(COLONY);

        for (Worker worker : all) {
            worker.assign(ProfessionType.LUMBERJACK);
        }

        UUID withChest = all.get(2).villagerId();

        ProfessionAssigner.enforceVacancies(workers, COLONY, withChest::equals);

        assertEquals(
                ProfessionType.LUMBERJACK,
                workers.find(withChest).orElseThrow().profession().orElseThrow(),
                "a vaga não ficou com quem tem baú");
    }

    /** Se ninguém tem baú, alguém fica com a vaga assim mesmo. */
    @Test
    void withoutAnyoneEquippedTheVacancyIsStillFilled() {
        addWorkers(COLONY, 3);

        for (Worker worker : workers.ofColony(COLONY)) {
            worker.assign(ProfessionType.LUMBERJACK);
        }

        ProfessionAssigner.enforceVacancies(workers, COLONY, villagerId -> false);

        long lumberjacks = workers.ofColony(COLONY).stream()
                .filter(w -> w.profession().filter(ProfessionType.LUMBERJACK::equals).isPresent())
                .count();

        assertEquals(1, lumberjacks);
    }

    /** Colônia já dentro da regra não perde ninguém. */
    @Test
    void enforcingChangesNothingWhenTheColonyIsAlreadyRight() {
        addWorkers(COLONY, 4);
        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertTrue(ProfessionAssigner.enforceVacancies(workers, COLONY).isEmpty());
    }

    /** Roda a cada ciclo: sem aldeão novo não pode fazer nada. */
    @Test
    void runningAgainAssignsNobody() {
        addWorkers(COLONY, 3);
        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertEquals(0, ProfessionAssigner.assignMissing(workers, COLONY, everyone()));
    }

    /** Realocar quem já trabalha é decisão da colônia, não daqui. */
    @Test
    void anExistingProfessionIsNeverOverwritten() {
        UUID villager = UUID.randomUUID();
        workers.restore(Worker.restore(villager, COLONY, ProfessionType.BUILDER));

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertEquals(
                ProfessionType.BUILDER,
                workers.find(villager).orElseThrow().profession().orElseThrow());
    }

    /** A colônia vizinha não conta para a necessidade desta. */
    @Test
    void otherColoniesAreNotTouched() {
        addWorkers(COLONY, 2);
        addWorkers(OTHER_COLONY, 2);

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        for (Worker worker : workers.ofColony(OTHER_COLONY)) {
            assertTrue(worker.profession().isEmpty());
        }
    }

    @Test
    void anEmptyColonyAssignsNobody() {
        assertEquals(0, ProfessionAssigner.assignMissing(workers, COLONY, everyone()));
    }

    /** Bebê e nitwit são registrados, mas não recebem função. */
    @Test
    void whoCannotWorkGetsNoProfession() {
        UUID adult = UUID.randomUUID();
        UUID baby = UUID.randomUUID();

        workers.register(adult, COLONY);
        workers.register(baby, COLONY);

        assertEquals(1, ProfessionAssigner.assignMissing(workers, COLONY, Set.of(adult)));

        assertTrue(workers.find(adult).orElseThrow().hasProfession());
        assertFalse(workers.find(baby).orElseThrow().hasProfession());
    }

    /** Crescido, ele recebe função no ciclo seguinte, sem nada especial. */
    @Test
    void theBabyIsHiredOnceItCanWork() {
        UUID baby = UUID.randomUUID();
        workers.register(baby, COLONY);

        ProfessionAssigner.assignMissing(workers, COLONY, Set.of());

        assertEquals(1, ProfessionAssigner.assignMissing(workers, COLONY, Set.of(baby)));
        assertTrue(workers.find(baby).orElseThrow().hasProfession());
    }

    /**
     * A vaga aberta por um morto é preenchida pelo próximo: a contagem
     * olha quem está registrado, e o handler de morte já o removeu.
     */
    @Test
    void aFreedProfessionIsFilledAgain() {
        addWorkers(COLONY, 4);
        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        UUID lumberjack = workers.ofColony(COLONY).stream()
                .filter(w -> w.profession().orElseThrow() == ProfessionType.LUMBERJACK)
                .findFirst()
                .orElseThrow()
                .villagerId();

        workers.remove(lumberjack);

        UUID newcomer = UUID.randomUUID();
        workers.register(newcomer, COLONY);

        ProfessionAssigner.assignMissing(workers, COLONY, Set.of(newcomer));

        assertEquals(
                ProfessionType.LUMBERJACK,
                workers.find(newcomer).orElseThrow().profession().orElseThrow());
    }

    @Test
    void mostNeededOfNobodyIsTheFirstInTheChain() {
        assertEquals(ProfessionType.LUMBERJACK, ProfessionAssigner.mostNeeded(List.of()));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class,
                () -> ProfessionAssigner.assignMissing(workers, null, everyone()));

        assertThrows(NullPointerException.class,
                () -> ProfessionAssigner.mostNeeded(null));
    }
}
