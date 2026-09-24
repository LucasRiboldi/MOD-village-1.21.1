package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Quem perde a função quando falta baú — separado de {@link ProfessionAssigner}
 * em 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>O {@code ProfessionAssigner} decide <b>quem recebe</b> uma vaga; esta
 * classe decide o lado oposto: tirar a função do trabalhador sem baú quando
 * existe candidato que conseguiria um. As regras e os motivos estão nos
 * comentários de cada método, que vieram junto sem mudança.
 */
public final class VacancyEnforcer {

    private VacancyEnforcer() {
    }

    /**
     * Substitui trabalhadores sem baú quando há candidatos equipáveis.
     *
     * <p>Profissões existentes nunca são removidas apenas porque a
     * população caiu. A substituição aqui só trata a falta de baú, e só
     * quando existe candidato que possa receber a vaga.
     *
     * @return os aldeões que perderam a função, para quem chama soltar o
     *     que eles seguravam
     */
    public static Set<UUID> enforceVacancies(WorkerService workers, UUID colonyId) {
        return enforceVacancies(workers, colonyId, villagerId -> true);
    }

    /**
     * @param equipped diz se um trabalhador tem o que precisa para
     *     trabalhar — hoje, um baú. A primeira versão desta regra ficava
     *     com o primeiro da lista, e o servidor de 2026-08-12 mostrou o
     *     custo disso: dos treze trabalhadores com baú da vila, a vaga de
     *     lenhador ficou com o único sem, e a tarefa voltava para a fila
     *     a cada ciclo, para sempre. Entre dois candidatos iguais, quem
     *     pode trabalhar tem preferência
     */
    public static Set<UUID> enforceVacancies(
            WorkerService workers, UUID colonyId, Predicate<UUID> equipped) {

        return enforceVacancies(workers, colonyId, equipped, 0, false);
    }

    /**
     * @param replacements quantos aldeões sem função conseguiriam baú
     *     agora. É o teto de trocas: um trabalhador sem baú perde a
     *     função para quem consegue um, e só quando esse alguém existe.
     *
     *     <p>Decisão do autor em 2026-08-13, depois de a vila
     *     {@code c18264c9} passar duas sessões com dois lenhadores sem
     *     baú devolvendo a tarefa à fila a cada trinta segundos. A
     *     preferência de atribuição não os alcançava: ela escolhe quem
     *     <b>recebe</b> a função, e eles já a tinham do save.
     *
     *     <p>Sem candidato, ninguém é dispensado: vaga vazia não é
     *     melhor que trabalhador sem baú, e o jogador pode construir o
     *     baú depois. É a mesma regra da atribuição, vista do outro
     *     lado.
     */
    public static Set<UUID> enforceVacancies(
            WorkerService workers, UUID colonyId, Predicate<UUID> equipped, int replacements) {

        return enforceVacancies(workers, colonyId, equipped, replacements, false);
    }

    /**
     * Substitui trabalhadores sem baú sem retirar o último ocupante de uma
     * função da fundação da vila.
     */
    public static Set<UUID> enforceVacanciesPreservingFoundation(
            WorkerService workers, UUID colonyId, Predicate<UUID> equipped, int replacements) {

        return enforceVacancies(workers, colonyId, equipped, replacements, true);
    }

    static Set<UUID> enforceVacancies(
            WorkerService workers,
            UUID colonyId,
            Predicate<UUID> equipped,
            int replacements,
            boolean preserveFoundation) {

        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(equipped, "equipped");

        List<Worker> employed = new ArrayList<>();

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.hasProfession()) {
                employed.add(worker);
            }
        }

        Set<UUID> demoted = new LinkedHashSet<>();
        Map<ProfessionType, Integer> counts = countByProfession(employed);

        // E a troca: quem ficou com a vaga sem conseguir baú a perde
        // para quem consegue, enquanto houver quem consiga.
        int left = replacements;

        for (Worker worker : employed) {
            if (left <= 0) {
                break;
            }

            if (demoted.contains(worker.villagerId()) || equipped.test(worker.villagerId())) {
                continue;
            }

            ProfessionType role = worker.profession().orElse(null);

            if (preserveFoundation
                    && role != null
                    && ProfessionAssigner.FOUNDATION_ORDER.contains(role)
                    && counts.getOrDefault(role, 0) <= 1) {
                continue;
            }

            worker.unassign();
            demoted.add(worker.villagerId());

            if (role != null) {
                counts.merge(role, -1, Integer::sum);
            }

            left--;
        }

        return demoted;
    }

    static Map<ProfessionType, Integer> countByProfession(
            Collection<Worker> colonyWorkers) {

        Map<ProfessionType, Integer> counts = new EnumMap<>(ProfessionType.class);

        for (ProfessionType type : ProfessionType.values()) {
            counts.put(type, 0);
        }

        for (Worker worker : colonyWorkers) {
            worker.profession()
                    .ifPresent(type -> counts.merge(type, 1, Integer::sum));
        }

        return counts;
    }
}
