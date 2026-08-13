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
     * <p>Dois. Decisão do autor em 2026-08-13: a vila começa com dois
     * lenhadores, dois fabricantes, dois fazendeiros e dois construtores.
     * Os demais aldeões continuam os que já eram — o mod não os emprega,
     * e eles seguem a rotina Vanilla.
     *
     * <p>Era um até 2026-08-13, e antes disso a vaga era ilimitada: a
     * vila de 43 aldeões do autor acabou com seis lenhadores, e o
     * servidor mostrou os seis disputando tarefa a cada ciclo. O teto
     * existe por isso; o valor dele é do autor.
     *
     * <p>Oito trabalhadores numa vila de quarenta continua sendo uma
     * minoria empregada, que é o ponto: a vila continua sendo a vila do
     * jogador, com a colônia dentro dela.
     */
    public static final int MAX_PER_PROFESSION = 2;

    /**
     * Qual profissão ainda tem vaga nesta colônia.
     *
     * <p>Vazio quando as quatro estão no teto, e é isso que faz o nono
     * aldeão continuar Vanilla em vez de virar o terceiro lenhador.
     *
     * <p>Devolve a profissão mais escassa que ainda tem vaga, e não a
     * primeira da lista: com teto de dois, ir por ordem daria dois
     * lenhadores antes do primeiro fabricante. Uma vila com dois
     * lenhadores e nenhum construtor é pior do que uma com um de cada,
     * então a colônia cobre as quatro funções antes de dobrar qualquer
     * uma.
     *
     * <p>Empate resolvido pela ordem de {@link ProfessionType}, que é a
     * da cadeia produtiva: lenhador antes de fabricante, fabricante antes
     * de construtor. Numa colônia recém-detectada todas as contagens são
     * zero, então é essa ordem que decide as primeiras quatro — e começar
     * pelo construtor, sem madeira nem material, daria um trabalhador sem
     * o que fazer.
     */
    public static Optional<ProfessionType> vacancy(Collection<Worker> colonyWorkers) {
        Objects.requireNonNull(colonyWorkers, "colonyWorkers");

        Map<ProfessionType, Integer> counts = countByProfession(colonyWorkers);

        ProfessionType scarcest = null;

        for (ProfessionType type : ProfessionType.values()) {
            if (counts.get(type) >= MAX_PER_PROFESSION) {
                continue;
            }

            if (scarcest == null || counts.get(type) < counts.get(scarcest)) {
                scarcest = type;
            }
        }

        return Optional.ofNullable(scarcest);
    }

    /**
     * Tira a função de quem excede a vaga.
     *
     * <p>Existe para os saves anteriores a 2026-08-12: a colônia do autor
     * chegou com seis lenhadores gravados, e uma regra que só valesse para
     * aldeão novo nunca os desfaria.
     *
     * <p>Mantém os {@value #MAX_PER_PROFESSION} primeiros de cada
     * profissão, na ordem em que o registro os devolve — que é a ordem de
     * inserção, e portanto estável entre ciclos. Escolher por critério
     * mais fino (quem está mais perto, quem trabalhou mais) exigiria
     * dados que este pacote não tem.
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

        return enforceVacancies(workers, colonyId, equipped, 0);
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

        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(equipped, "equipped");

        Map<ProfessionType, Set<Worker>> keeping = new EnumMap<>(ProfessionType.class);
        List<Worker> employed = new ArrayList<>();

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.hasProfession()) {
                employed.add(worker);
            }
        }

        // Duas passadas: primeiro quem pode trabalhar fica com as vagas,
        // depois os demais preenchem o que sobrou. Numa passada só, os
        // primeiros da lista ficariam com elas mesmo sem baú.
        for (Worker worker : employed) {
            if (equipped.test(worker.villagerId())) {
                keep(keeping, worker);
            }
        }

        for (Worker worker : employed) {
            keep(keeping, worker);
        }

        Set<UUID> demoted = new LinkedHashSet<>();

        for (Worker worker : employed) {
            if (!keeping.get(worker.profession().orElseThrow()).contains(worker)) {
                worker.unassign();
                demoted.add(worker.villagerId());
            }
        }

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

            worker.unassign();
            demoted.add(worker.villagerId());

            left--;
        }

        return demoted;
    }

    /** Guarda este trabalhador na vaga da profissão dele, se ainda couber. */
    private static void keep(Map<ProfessionType, Set<Worker>> keeping, Worker worker) {
        Set<Worker> kept = keeping.computeIfAbsent(
                worker.profession().orElseThrow(), type -> new LinkedHashSet<>());

        if (kept.size() < MAX_PER_PROFESSION) {
            kept.add(worker);
        }
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

        return assignMissing(workers, colonyId, employable, villagerId -> true);
    }

    /**
     * @param equipped diz se este aldeão conseguiria um baú. A vaga vai
     *     primeiro para quem consegue: um trabalhador sem baú pega a
     *     tarefa e a devolve à fila a cada ciclo, para sempre, e do lado
     *     de fora isso se parece com trabalho acontecendo. O log de
     *     2026-08-13 mostrou dois lenhadores assim numa vila que tinha
     *     baú livre — a vaga tinha ido para a cama errada.
     *
     *     <p>É preferência, não exigência: esgotados os candidatos com
     *     baú possível, a vaga vai para quem sobrar. Uma vaga vazia não
     *     é melhor que um trabalhador que ainda não tem onde guardar —
     *     o jogador pode construir o baú depois, e aí ele o reivindica
     *     no ciclo seguinte
     */
    public static int assignMissing(
            WorkerService workers, UUID colonyId, Set<UUID> employable,
            Predicate<UUID> equipped) {

        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(employable, "employable");
        Objects.requireNonNull(equipped, "equipped");

        int assigned = assignPass(workers, colonyId, employable, equipped);

        return assigned + assignPass(workers, colonyId, employable, villagerId -> true);
    }

    /** Uma passada de atribuição sobre quem o filtro aceitar. */
    private static int assignPass(
            WorkerService workers, UUID colonyId, Set<UUID> employable,
            Predicate<UUID> accepts) {

        int assigned = 0;

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.hasProfession() || !employable.contains(worker.villagerId())
                    || !accepts.test(worker.villagerId())) {

                continue;
            }

            Optional<ProfessionType> vacancy = vacancy(workers.ofColony(colonyId));

            if (vacancy.isEmpty()) {
                // As oito vagas estão preenchidas. Os demais aldeões
                // continuam sendo o que já eram — é o que a regra de
                // duas vagas por profissão quer dizer numa vila de
                // quarenta.
                break;
            }

            worker.assign(vacancy.get());
            assigned++;
        }

        return assigned;
    }
}
