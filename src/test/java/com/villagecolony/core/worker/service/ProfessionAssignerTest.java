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

    /** Um aldeão de cada função, na ordem da cadeia produtiva. */
    @Test
    void theFirstWorkersCoverEveryProfession() {
        int professions = ProfessionType.values().length;

        addWorkers(COLONY, professions);

        assertEquals(
                professions, ProfessionAssigner.assignMissing(workers, COLONY, everyone()));

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
     * Cobertas as oito vagas, o nono aldeão continua o que já era.
     *
     * <p>Decisão do autor em 2026-08-13: a vila começa com dois
     * trabalhadores de cada tipo. Antes a vaga era ilimitada, e a vila de
     * quarenta e três aldeões do autor acabou com seis lenhadores
     * disputando tarefa a cada ciclo.
     */
    @Test
    void theWorkerPastTheLastVacancyGetsNothing() {
        // A Regra 4: dois de cada. Com sete profissões são catorze vagas,
        // e o décimo quinto aldeão fica sem função.
        int vacancies = 2 * ProfessionType.values().length;

        addWorkers(COLONY, vacancies + 1);

        int assigned = ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertEquals(vacancies, assigned, "esperava as vagas e nada além");

        long employed = workers.ofColony(COLONY).stream()
                .filter(Worker::hasProfession)
                .count();

        assertEquals(vacancies, employed);
    }

    /** Uma vila grande emprega oito, e só. */
    @Test
    void twoOfEachProfessionAndNoMore() {
        addWorkers(COLONY, 43);

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        for (ProfessionType type : ProfessionType.values()) {
            long count = workers.ofColony(COLONY).stream()
                    .filter(w -> w.profession().filter(type::equals).isPresent())
                    .count();

            assertEquals(2, count, "profissão fora do teto: " + type);
        }
    }

    /**
     * O primeiro a dobrar dobra o lenhador, e não o construtor.
     *
     * <p>Com teto de dois, preencher por ordem de declaração daria dois
     * lenhadores antes do primeiro fabricante — uma vila com dois
     * lenhadores e nenhum construtor é pior do que uma com um de cada. A
     * primeira vaga depois de todas cobertas é a primeira que pode
     * dobrar, e dobra a primeira da cadeia produtiva.
     */
    @Test
    void theFirstSpareWorkerDoublesTheFirstOfTheChain() {
        addWorkers(COLONY, ProfessionType.values().length + 1);

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        long lumberjacks = workers.ofColony(COLONY).stream()
                .filter(w -> w.profession().filter(ProfessionType.LUMBERJACK::equals).isPresent())
                .count();

        assertEquals(2, lumberjacks, "o excedente devia ter dobrado o lenhador");

        for (ProfessionType type : ProfessionType.values()) {
            assertTrue(
                    workers.ofColony(COLONY).stream()
                            .anyMatch(w -> w.profession().filter(type::equals).isPresent()),
                    "profissão descoberta: " + type);
        }
    }

    /** Com todas as vagas preenchidas — duas por profissão —, não há vaga. */
    @Test
    void thereIsNoVacancyOnceEveryProfessionIsFilled() {
        addWorkers(COLONY, 2 * ProfessionType.values().length);
        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertTrue(ProfessionAssigner.vacancy(workers.ofColony(COLONY)).isEmpty());
    }

    /**
     * A vaga vai para quem consegue baú.
     *
     * <p>O log de 2026-08-13 mostrou o custo de não fazer isso: dois
     * lenhadores sem baú numa vila que tinha baú livre, pegando a tarefa
     * e devolvendo à fila a cada trinta segundos, para sempre. A vaga
     * tinha ido para a cama errada.
     */
    @Test
    void theVacancyPrefersSomeoneWhoCanGetAChest() {
        addWorkers(COLONY, 2);

        List<Worker> all = workers.ofColony(COLONY);
        UUID withChestNearby = all.get(1).villagerId();

        ProfessionAssigner.assignMissing(
                workers, COLONY, everyone(), withChestNearby::equals);

        assertEquals(
                ProfessionType.LUMBERJACK,
                workers.find(withChestNearby).orElseThrow().profession().orElseThrow(),
                "a primeira vaga não foi para quem consegue baú");
    }

    /**
     * Ninguém consegue baú, e as vagas são preenchidas assim mesmo.
     *
     * <p>É preferência, não exigência: vaga vazia não é melhor que um
     * trabalhador que ainda não tem onde guardar. O jogador pode
     * construir o baú depois.
     */
    @Test
    void withoutAnyChestAroundTheVacanciesAreStillFilled() {
        addWorkers(COLONY, 4);

        int assigned = ProfessionAssigner.assignMissing(
                workers, COLONY, everyone(), villagerId -> false);

        assertEquals(4, assigned);
    }

    /** Quem consegue baú não é atendido duas vezes. */
    @Test
    void thePreferredCandidateIsNotAssignedTwice() {
        addWorkers(COLONY, 3);

        UUID preferred = workers.ofColony(COLONY).get(0).villagerId();

        int assigned = ProfessionAssigner.assignMissing(
                workers, COLONY, everyone(), preferred::equals);

        assertEquals(3, assigned, "cada aldeão devia receber uma função só");

        long employed = workers.ofColony(COLONY).stream()
                .filter(Worker::hasProfession)
                .count();

        assertEquals(3, employed);
    }

    /**
     * Quem não consegue baú perde a vaga para quem consegue.
     *
     * <p>Decisão do autor em 2026-08-13. A vila `c18264c9` passou duas
     * sessões com dois lenhadores sem baú devolvendo a tarefa à fila a
     * cada trinta segundos: a preferência de atribuição não os alcançava,
     * porque ela escolhe quem <b>recebe</b> a função e eles já a tinham
     * do save.
     */
    @Test
    void aChestlessWorkerLosesTheJobWhenSomeoneElseCanGetAChest() {
        addWorkers(COLONY, 2);

        for (Worker worker : workers.ofColony(COLONY)) {
            worker.assign(ProfessionType.LUMBERJACK);
        }

        Set<UUID> demoted = ProfessionAssigner.enforceVacancies(
                workers, COLONY, villagerId -> false, 1);

        assertEquals(1, demoted.size(), "um candidato, uma troca");

        long lumberjacks = workers.ofColony(COLONY).stream()
                .filter(w -> w.profession().filter(ProfessionType.LUMBERJACK::equals).isPresent())
                .count();

        assertEquals(1, lumberjacks, "o outro devia continuar na vaga");
    }

    /**
     * Sem candidato, ninguém é dispensado.
     *
     * <p>Vaga vazia não é melhor que trabalhador sem baú: o jogador pode
     * construir o baú depois, e aí ele o reivindica no ciclo seguinte.
     */
    @Test
    void withoutAReplacementTheChestlessWorkerKeepsTheJob() {
        addWorkers(COLONY, 2);

        for (Worker worker : workers.ofColony(COLONY)) {
            worker.assign(ProfessionType.LUMBERJACK);
        }

        assertTrue(ProfessionAssigner.enforceVacancies(
                workers, COLONY, villagerId -> false, 0).isEmpty());
    }

    /** Quem tem baú não é trocado, haja candidato ou não. */
    @Test
    void anEquippedWorkerIsNeverSwapped() {
        addWorkers(COLONY, 2);

        for (Worker worker : workers.ofColony(COLONY)) {
            worker.assign(ProfessionType.LUMBERJACK);
        }

        assertTrue(ProfessionAssigner.enforceVacancies(
                workers, COLONY, villagerId -> true, 5).isEmpty());
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

        assertEquals(4, demoted.size());

        long lumberjacks = workers.ofColony(COLONY).stream()
                .filter(w -> w.profession().filter(ProfessionType.LUMBERJACK::equals).isPresent())
                .count();

        assertEquals(2, lumberjacks);
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

        assertEquals(4, assigned, "os quatro dispensados deviam voltar a ter função");
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
        addWorkers(COLONY, 4);

        List<Worker> all = workers.ofColony(COLONY);

        for (Worker worker : all) {
            worker.assign(ProfessionType.LUMBERJACK);
        }

        UUID withChest = all.get(3).villagerId();

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

        assertEquals(2, lumberjacks);
    }

    /** Colônia já dentro da regra não perde ninguém. */
    @Test
    void enforcingChangesNothingWhenTheColonyIsAlreadyRight() {
        addWorkers(COLONY, 8);
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
