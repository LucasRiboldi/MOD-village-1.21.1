package com.villagecolony.core.worker.service;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.VillagerScanner.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Regra 11 — uma de cada profissão em cada vila.
 *
 * <p>É o <b>piso</b>; a Regra 4, dois por profissão, é o teto. Os testes
 * do teto moram em {@link ProfessionAssignerTest} e são muitos. Este
 * arquivo é do piso, e existe porque o §18 registrava a regra como "já
 * satisfeita pelo mecanismo" com duas ressalvas que ninguém tinha
 * afirmado:
 *
 * <pre>
 * vila com menos empregáveis   o piso vira "tantas quantas couberem",
 * que profissões               e isso precisa estar escrito
 *
 * a dispensa                   nada a impede de tirar o último de uma
 *                              profissão. Não foi visto acontecer, e
 *                              não foi verificado
 * </pre>
 *
 * <p>A segunda ressalva é a que este arquivo responde de verdade, e a
 * resposta não é a que se esperava: <b>a dispensa pode, sim</b>, se
 * alguém lhe pedir mais trocas do que há substitutos. O que segura a
 * regra é o número que a varredura passa — e um número que segura uma
 * regra sem dizer que a segura é um acidente esperando o dia em que
 * alguém o calcule de outro jeito.
 */
class ProfessionFloorTest {

    private static final UUID COLONY = UUID.randomUUID();

    private WorkerService workers;

    @BeforeEach
    void setUp() {
        workers = new WorkerService();
    }

    private List<UUID> addWorkers(int count) {
        List<UUID> added = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            added.add(workers.register(UUID.randomUUID(), COLONY).villagerId());
        }

        return added;
    }

    private Set<UUID> everyone() {
        Set<UUID> ids = new HashSet<>();

        for (Worker worker : workers.ofColony(COLONY)) {
            ids.add(worker.villagerId());
        }

        return ids;
    }

    private Map<ProfessionType, Integer> headcount() {
        Map<ProfessionType, Integer> counts = new EnumMap<>(ProfessionType.class);

        for (ProfessionType type : ProfessionType.values()) {
            counts.put(type, 0);
        }

        for (Worker worker : workers.ofColony(COLONY)) {
            worker.profession().ifPresent(type -> counts.merge(type, 1, Integer::sum));
        }

        return counts;
    }

    /**
     * Uma vila pequena cobre tantas funções quantas couberem.
     *
     * <p>É a ressalva que o §18 mandava escrever, e ela deixou de ser
     * pequena: com sete profissões, uma vila de três adultos não tem como
     * ter uma de cada. O que a regra pede nesse caso é que os três sejam
     * <b>três funções diferentes</b> — dobrar uma antes de cobrir as
     * outras seria a vila com dois lenhadores e nenhum construtor, que é
     * exatamente o que a regra existe para impedir.
     */
    @Test
    void fewerVillagersThanProfessionsCoverAsManyAsFit() {
        addWorkers(3);

        assertEquals(3, ProfessionAssigner.assignMissing(workers, COLONY, everyone()));

        Set<ProfessionType> covered = EnumSet.noneOf(ProfessionType.class);

        for (Worker worker : workers.ofColony(COLONY)) {
            covered.add(worker.profession().orElseThrow());
        }

        assertEquals(3, covered.size(), "três aldeões viraram menos de três funções");
    }

    /** Um só aldeão vira um só trabalhador, e nenhuma função fica dobrada. */
    @Test
    void aSingleVillagerIsASingleWorker() {
        addWorkers(1);

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertEquals(
                1,
                headcount().values().stream().mapToInt(Integer::intValue).sum(),
                "um aldeão devia dar um trabalhador");
    }

    /**
     * Nenhuma função chega a dois antes de todas terem uma.
     *
     * <p>O piso e o teto na mesma frase, e é a propriedade que sustenta a
     * regra: enquanto houver função vazia, o aldeão novo vai para ela.
     */
    @Test
    void noJobIsDoubledWhileAnotherIsEmpty() {
        int professions = ProfessionType.values().length;

        addWorkers(professions + 1);

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        Map<ProfessionType, Integer> counts = headcount();

        for (ProfessionType type : ProfessionType.values()) {
            assertTrue(
                    counts.get(type) >= 1,
                    "a função " + type + " ficou vazia com aldeão de sobra na vila");
        }
    }

    /**
     * A dispensa por falta de baú <b>pode</b> esvaziar uma função.
     *
     * <p>É a ressalva que estava por verificar, e a verificação diz que
     * sim. Cada profissão tem um trabalhador e nenhum tem baú; pedindo
     * três trocas, três funções ficam vazias. A dispensa não conhece o
     * piso — ela conhece o número de trocas que lhe pedem.
     *
     * <p>Este teste não descreve um defeito: descreve <b>onde a garantia
     * mora</b>. Quem a segura é quem chama, passando no máximo tantas
     * trocas quantos substitutos existem — ver
     * {@code VillagerScanner.ScanResult.substitutes}. O teste seguinte é
     * o que prende essa parte.
     */
    @Test
    void theDismissalDoesNotKnowAboutTheFloor() {
        List<UUID> everyone = addWorkers(ProfessionType.values().length);

        ProfessionAssigner.assignMissing(workers, COLONY, new HashSet<>(everyone));

        Set<UUID> demoted = ProfessionAssigner.enforceVacancies(
                workers, COLONY, villagerId -> false, 3);

        assertEquals(3, demoted.size());

        long empty = headcount().values().stream().filter(count -> count == 0).count();

        assertEquals(3, empty, "as três trocas deviam ter esvaziado três funções");
    }

    /**
     * Com substituto para cada troca, o piso atravessa o ciclo.
     *
     * <p>É como a colônia roda de verdade: dispensa e atribuição
     * acontecem na mesma passagem, nessa ordem. Quem perdeu a vaga por
     * não ter baú é substituído por quem tem, e a função que ficou vazia
     * é justamente a mais escassa — que é a primeira que a atribuição
     * procura.
     */
    @Test
    void withOneSubstitutePerSwapTheFloorHolds() {
        List<UUID> employed = addWorkers(ProfessionType.values().length);

        ProfessionAssigner.assignMissing(workers, COLONY, new HashSet<>(employed));

        // Três aldeões sem função, e com baú: são eles os substitutos.
        List<UUID> substitutes = addWorkers(3);

        Set<UUID> withChest = new HashSet<>(substitutes);

        ProfessionAssigner.enforceVacancies(
                workers, COLONY, withChest::contains, substitutes.size());

        ProfessionAssigner.assignMissing(
                workers, COLONY, everyone(), withChest::contains);

        for (ProfessionType type : ProfessionType.values()) {
            assertTrue(
                    headcount().get(type) >= 1,
                    "a função " + type + " ficou vazia depois da troca");
        }
    }

    /**
     * O número que a varredura passa nunca promete mais do que tem.
     *
     * <p>É a frase que faltava, e ela mora do lado de fora do core
     * porque é a varredura do mundo que a produz. Dois aldeões do mesmo
     * cômodo enxergam o mesmo baú: contar candidatos daria duas trocas
     * onde cabe uma — é o E11 —, e contar baús daria uma troca onde não
     * há ninguém para ocupá-la, que é a Regra 11 pelo outro lado.
     */
    @Test
    void theSwapsPromisedAreNeverMoreThanWhatExists() {
        Set<ColonyPos> oneChest =
                Set.of(new ColonyPos(0, 64, 0));

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        // Dois candidatos, um baú: uma troca, e não duas.
        assertEquals(
                1,
                new ScanResult(
                        0, 0, Set.of(first, second), Set.of(first, second), oneChest)
                        .substitutes());

        // Um candidato, dois baús: uma troca, e não duas.
        assertEquals(
                1,
                new ScanResult(
                        0,
                        0,
                        Set.of(first),
                        Set.of(first),
                        Set.of(
                                new ColonyPos(0, 64, 0),
                                new ColonyPos(4, 64, 0)))
                        .substitutes());

        // Baú nenhum: troca nenhuma, e o último lenhador fica onde está.
        assertEquals(
                0,
                new ScanResult(
                        0, 0, Set.of(first), Set.of(first), Set.of())
                        .substitutes());
    }

    /** Sem substituto, ninguém é dispensado e o piso nem é ameaçado. */
    @Test
    void withoutSubstitutesNobodyIsDismissed() {
        List<UUID> everyone = addWorkers(ProfessionType.values().length);

        ProfessionAssigner.assignMissing(workers, COLONY, new HashSet<>(everyone));

        assertTrue(
                ProfessionAssigner.enforceVacancies(
                        workers, COLONY, villagerId -> false, 0).isEmpty(),
                "dispensou alguém sem ter quem pusesse no lugar");

        for (ProfessionType type : ProfessionType.values()) {
            assertEquals(1, headcount().get(type));
        }
    }
}
