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
 * Dá função aos trabalhadores que ainda não têm nenhuma.
 *
 * <p>Responde à pergunta "de quem esta colônia mais precisa agora?" e
 * atribui. Ver Profession-System.md §"Seleção de Profissão" e
 * §"Nascimento de Novos Aldeões".
 *
 * <p>Lógica pura: recebe o registro, não lê o mundo. Quem descobre que há
 * aldeão novo é {@code fabric.integration.VillagerScanner}.
 *
 * <p>Não decide prioridade de tarefa nem realoca quem já trabalha —
 * apenas preenche vaga. Realocação conforme a necessidade muda é da
 * colônia, e não pertence ao MVP.
 */
public final class ProfessionAssigner {

    private ProfessionAssigner() {
    }

    /**
     * De qual profissão a colônia mais precisa, dado quem ela já tem.
     *
     * <p>Escolhe a de menor contagem, o que cobre as quatro funções antes
     * de duplicar qualquer uma — a necessidade mínima do
     * Profession-System.md: seis aldeões, um de cada.
     *
     * <p>Empate resolvido pela ordem de declaração de
     * {@link ProfessionType}, que é a ordem da cadeia produtiva do MVP:
     * lenhador antes de fabricante, fabricante antes de construtor. Numa
     * colônia recém-detectada todas as contagens são zero, então é essa
     * ordem que decide as primeiras quatro atribuições — e começar pelo
     * construtor, sem madeira nem material, daria um trabalhador sem o
     * que fazer.
     */
    public static ProfessionType mostNeeded(Collection<Worker> colonyWorkers) {
        Objects.requireNonNull(colonyWorkers, "colonyWorkers");

        Map<ProfessionType, Integer> counts = new EnumMap<>(ProfessionType.class);

        for (ProfessionType type : ProfessionType.values()) {
            counts.put(type, 0);
        }

        for (Worker worker : colonyWorkers) {
            worker.profession().ifPresent(type -> counts.merge(type, 1, Integer::sum));
        }

        ProfessionType scarcest = ProfessionType.values()[0];

        for (ProfessionType type : ProfessionType.values()) {
            if (counts.get(type) < counts.get(scarcest)) {
                scarcest = type;
            }
        }

        return scarcest;
    }

    /**
     * Quantos trabalhadores de cada profissão uma colônia tem.
     *
     * <p>Um. Decisão do autor em 2026-08-12: uma vila tem um lenhador,
     * um fabricante, um fazendeiro e um construtor, e mais ninguém
     * trabalha.
     *
     * <p>Antes disto a vaga era ilimitada e {@link #mostNeeded} apenas
     * equilibrava as contagens: a vila de 43 aldeões do autor acabou com
     * seis lenhadores, e o servidor mostrou os seis disputando tarefa a
     * cada ciclo. Uma vila não precisa de seis lenhadores para encher os
     * baús que tem.
     */
    public static final int MAX_PER_PROFESSION = 1;

    /**
     * Qual profissão ainda tem vaga nesta colônia.
     *
     * <p>Vazio quando as quatro já estão preenchidas, e é isso que faz o
     * quinto aldeão continuar Vanilla em vez de virar o segundo lenhador.
     *
     * <p>A ordem é a de {@link ProfessionType}, que é a da cadeia
     * produtiva: lenhador antes de fabricante, fabricante antes de
     * construtor. Numa colônia recém-detectada é ela que decide quem vem
     * primeiro — e começar pelo construtor, sem madeira nem material,
     * daria um trabalhador sem o que fazer.
     */
    public static Optional<ProfessionType> vacancy(Collection<Worker> colonyWorkers) {
        Objects.requireNonNull(colonyWorkers, "colonyWorkers");

        Map<ProfessionType, Integer> counts = countByProfession(colonyWorkers);

        for (ProfessionType type : ProfessionType.values()) {
            if (counts.get(type) < MAX_PER_PROFESSION) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }

    /**
     * Tira a função de quem excede a vaga.
     *
     * <p>Existe para os saves anteriores a 2026-08-12: a colônia do autor
     * chegou com seis lenhadores gravados, e uma regra que só valesse para
     * aldeão novo nunca os desfaria.
     *
     * <p>Mantém o primeiro de cada profissão, na ordem em que o registro
     * os devolve — que é a ordem de inserção, e portanto estável entre
     * ciclos. Escolher por critério mais fino (quem tem baú, quem está
     * mais perto) exigiria dados que este pacote não tem.
     *
     * <p>Quem perde a função não é removido: continua trabalhador da
     * colônia, sem profissão, e volta a ser candidato à primeira vaga que
     * abrir — quando o lenhador morrer, por exemplo.
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

        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(equipped, "equipped");

        Map<ProfessionType, Worker> keeping = new EnumMap<>(ProfessionType.class);
        List<Worker> employed = new ArrayList<>();

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.hasProfession()) {
                employed.add(worker);
            }
        }

        // Duas passadas: primeiro quem pode trabalhar fica com a vaga,
        // depois os demais preenchem o que sobrou. Numa passada só, o
        // primeiro da lista ficaria com ela mesmo sem baú.
        for (Worker worker : employed) {
            if (equipped.test(worker.villagerId())) {
                keeping.putIfAbsent(worker.profession().orElseThrow(), worker);
            }
        }

        for (Worker worker : employed) {
            keeping.putIfAbsent(worker.profession().orElseThrow(), worker);
        }

        Set<UUID> demoted = new LinkedHashSet<>();

        for (Worker worker : employed) {
            if (keeping.get(worker.profession().orElseThrow()) != worker) {
                worker.unassign();
                demoted.add(worker.villagerId());
            }
        }

        return demoted;
    }

    private static Map<ProfessionType, Integer> countByProfession(
            Collection<Worker> colonyWorkers) {

        Map<ProfessionType, Integer> counts = new EnumMap<>(ProfessionType.class);

        for (ProfessionType type : ProfessionType.values()) {
            counts.put(type, 0);
        }

        for (Worker worker : colonyWorkers) {
            worker.profession().ifPresent(type -> counts.merge(type, 1, Integer::sum));
        }

        return counts;
    }

    /**
     * Dá função aos trabalhadores sem função que podem trabalhar.
     *
     * <p>Recalcula a cada atribuição, e não uma vez para o lote: atribuir
     * quatro aldeões de uma colônia vazia de uma só vez daria quatro
     * lenhadores, porque a contagem seria a mesma para todos.
     *
     * <p>Chamável a cada ciclo. Quem já tem função é ignorado, então
     * rodar de novo sem aldeão novo não faz nada.
     *
     * @param employable quem pode receber função agora. Quem está fora
     *     é pulado sem virar erro: um bebê ainda vai crescer e um aldeão
     *     que não foi visto neste ciclo continua existindo. Quem decide
     *     isso é a camada fabric, que enxerga a entidade — ver
     *     {@code VillagerScanner}. A contagem de necessidade continua
     *     olhando a colônia inteira, porque um lenhador é um lenhador
     *     esteja ele à vista ou não.
     * @return quantos receberam função agora
     */
    public static int assignMissing(
            WorkerService workers, UUID colonyId, Set<UUID> employable) {

        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(employable, "employable");

        int assigned = 0;

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.hasProfession() || !employable.contains(worker.villagerId())) {
                continue;
            }

            Optional<ProfessionType> vacancy = vacancy(workers.ofColony(colonyId));

            if (vacancy.isEmpty()) {
                // As quatro vagas estão preenchidas. Os demais aldeões
                // continuam Vanilla, que é o que a regra de uma vaga por
                // profissão quer dizer para uma vila de quarenta.
                break;
            }

            worker.assign(vacancy.get());
            assigned++;
        }

        return assigned;
    }
}
