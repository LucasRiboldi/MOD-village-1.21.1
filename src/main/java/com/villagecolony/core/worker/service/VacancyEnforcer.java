package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Compatibilidade da antiga reposição por baú — separada de
 * {@link ProfessionAssigner} em 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>O {@code ProfessionAssigner} decide <b>quem recebe</b> uma vaga; esta
 * classe mantinha o lado oposto: tirar a função do trabalhador sem baú quando
 * existia candidato que conseguiria um. Profissões agora são fixas; falta de
 * baú impede trabalho, mas não troca o ofício do aldeão.
 */
public final class VacancyEnforcer {

    private VacancyEnforcer() {
    }

    /**
     * Mantém profissões existentes quando faltam baús.
     *
     * @return sempre vazio: profissão é fixa
     */
    public static Set<UUID> enforceVacancies(WorkerService workers, UUID colonyId) {
        return enforceVacancies(workers, colonyId, villagerId -> true);
    }

    /**
     * O mesmo, dizendo quem tem baú.
     *
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
     * Compatibilidade com chamadores que ainda calculam substitutos.
     */
    public static Set<UUID> enforceVacancies(
            WorkerService workers, UUID colonyId, Predicate<UUID> equipped, int replacements) {

        return enforceVacancies(workers, colonyId, equipped, replacements, false);
    }

    /**
     * Compatibilidade com chamadores que preservam a fundação.
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

        // Regra 2026-11-02: aldeão não troca de profissão. Os argumentos são
        // preservados para compatibilidade dos saves e dos chamadores antigos.
        return Set.of();
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
