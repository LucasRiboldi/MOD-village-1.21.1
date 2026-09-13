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

    /** Ordem de crescimento definida para as sete profissões produtoras. */
    public static final List<ProfessionType> PRODUCER_ORDER = List.of(
            ProfessionType.MINER,
            ProfessionType.LUMBERJACK,
            ProfessionType.MASON,
            ProfessionType.SMELTER,
            ProfessionType.CARPENTER,
            ProfessionType.FARMER,
            ProfessionType.BREEDER);

    private static final int ADULTS_PER_BATCH = 15;

    private ProfessionAssigner() {
    }

    /**
     * De qual profissão a colônia mais precisa, dado quem ela já tem.
     *
     * <p>Escolhe a de menor contagem, respeitando a ordem dos sete
     * produtores definida pela regra de crescimento da colônia.
     *
     * <p>Empate resolvido por {@link #PRODUCER_ORDER}, que põe o Mineiro
     * primeiro e o Criador por último.
     */
    public static ProfessionType mostNeeded(Collection<Worker> colonyWorkers) {
        Objects.requireNonNull(colonyWorkers, "colonyWorkers");

        Map<ProfessionType, Integer> counts = new EnumMap<>(ProfessionType.class);

        for (ProfessionType type : PRODUCER_ORDER) {
            counts.put(type, 0);
        }

        for (Worker worker : colonyWorkers) {
            worker.profession().map(ProfessionAssigner::quotaType)
                    .ifPresent(type -> counts.merge(type, 1, Integer::sum));
        }

        ProfessionType scarcest = PRODUCER_ORDER.get(0);

        for (ProfessionType type : PRODUCER_ORDER) {
            if (counts.get(type) < counts.get(scarcest)) {
                scarcest = type;
            }
        }

        return scarcest;
    }

    /**
     * Qual profissão ainda tem vaga nesta colônia.
     *
     * <p>Vazio quando todas as vagas abertas pela população adulta estão
     * preenchidas.
     *
     * <p>Os slots crescem em lotes ligados à população adulta: uma vaga
     * inicial de cada produtor e, a cada novo slot, a próxima profissão
     * na ordem declarada. O empate é resolvido por
     * {@link #PRODUCER_ORDER}.
     */
    public static Optional<ProfessionType> vacancy(Collection<Worker> colonyWorkers) {
        return vacancy(colonyWorkers, colonyWorkers.size());
    }

    /** Vaga conforme a população adulta observada no scanner Fabric. */
    public static Optional<ProfessionType> vacancy(
            Collection<Worker> colonyWorkers, int adultPopulation) {
        return vacancyFor(null, colonyWorkers, adultPopulation);
    }

    /**
     * A vaga desta colônia que <b>este</b> candidato pode ocupar.
     *
     * <p><b>É o que separa a linha de reserva de um descanso caro</b> —
     * 2026-09-10. Sem o candidato, a escolha é só "de quem a colônia mais
     * precisa", e a resposta para quem acabou de largar o ofício é o
     * ofício que ele largou: devolver o posto <b>abre a própria vaga</b>,
     * e ele passa a ser o mais escasso. O mineiro voltaria a ser mineiro
     * no ciclo seguinte, para o mesmo ramal, e a reavaliação teria custado
     * uma escrita no save para não mudar nada.
     *
     * <p>Ofício de que ele desistiu fica de fora enquanto o castigo
     * corre — ver {@link Worker#giveUpProfession}. Esgotados os que
     * sobram, devolve vazio: ele fica sem função por algumas passagens,
     * que é o <b>piso</b> desta linha e é seguro por construção — sem
     * função não há alvo distante nem material exigido, e portanto não há
     * como falhar de novo. Os castigos andam mesmo assim, porque a
     * distribuição conta ciclo para quem está ocioso.
     *
     * @param candidate quem vai ocupar a vaga, ou {@code null} para a
     *     pergunta sem dono — que é a da contagem da colônia
     */
    public static Optional<ProfessionType> vacancyFor(
            Worker candidate, Collection<Worker> colonyWorkers) {

        return vacancyFor(candidate, colonyWorkers, colonyWorkers.size());
    }

    /** Vaga que este candidato pode ocupar na população adulta observada. */
    public static Optional<ProfessionType> vacancyFor(
            Worker candidate, Collection<Worker> colonyWorkers, int adultPopulation) {

        Objects.requireNonNull(colonyWorkers, "colonyWorkers");

        if (adultPopulation < 0) {
            throw new IllegalArgumentException("adultPopulation must not be negative");
        }

        Map<ProfessionType, Integer> counts = countByProfession(colonyWorkers);

        for (ProfessionType type : PRODUCER_ORDER) {
            if (counts.get(type) >= targetCount(type, adultPopulation)) {
                continue;
            }

            if (candidate != null && candidate.isShunning(type)) {
                continue;
            }

            return Optional.of(type);
        }

        return Optional.empty();
    }

    private static int targetCount(ProfessionType type, int adults) {
        int slots;

        if (adults < ADULTS_PER_BATCH) {
            slots = Math.min(adults, PRODUCER_ORDER.size());
        } else {
            slots = (adults / ADULTS_PER_BATCH) * PRODUCER_ORDER.size()
                    + Math.min(adults % ADULTS_PER_BATCH, PRODUCER_ORDER.size());
        }

        int perProfession = slots / PRODUCER_ORDER.size();
        int extras = slots % PRODUCER_ORDER.size();
        int position = PRODUCER_ORDER.indexOf(type);

        return perProfession + (position >= 0 && position < extras ? 1 : 0);
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

        List<Worker> employed = new ArrayList<>();

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.hasProfession()) {
                employed.add(worker);
            }
        }

        Set<UUID> demoted = new LinkedHashSet<>();

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

    private static Map<ProfessionType, Integer> countByProfession(
            Collection<Worker> colonyWorkers) {

        Map<ProfessionType, Integer> counts = new EnumMap<>(ProfessionType.class);

        for (ProfessionType type : ProfessionType.values()) {
            counts.put(type, 0);
        }

        for (Worker worker : colonyWorkers) {
            worker.profession().map(ProfessionAssigner::quotaType)
                    .ifPresent(type -> counts.merge(type, 1, Integer::sum));
        }

        return counts;
    }

    private static ProfessionType quotaType(ProfessionType type) {
        return type == ProfessionType.SHEPHERD ? ProfessionType.BREEDER : type;
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

        return assignMissing(workers, colonyId, employable,
                workers.ofColony(colonyId).size(), villagerId -> true);
    }

    /** Dá função usando a população adulta observada pela camada Fabric. */
    public static int assignMissing(
            WorkerService workers, UUID colonyId, Set<UUID> employable, int adultPopulation) {

        return assignMissing(workers, colonyId, employable, adultPopulation, villagerId -> true);
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

        return assignMissing(workers, colonyId, employable,
                workers.ofColony(colonyId).size(), equipped);
    }

    /** Atribui vagas sem ultrapassar a população adulta informada. */
    public static int assignMissing(
            WorkerService workers, UUID colonyId, Set<UUID> employable,
            int adultPopulation, Predicate<UUID> equipped) {

        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(employable, "employable");
        Objects.requireNonNull(equipped, "equipped");

        if (adultPopulation < 0) {
            throw new IllegalArgumentException("adultPopulation must not be negative");
        }

        int assigned = assignPass(workers, colonyId, employable, adultPopulation, equipped);

        return assigned + assignPass(
                workers, colonyId, employable, adultPopulation, villagerId -> true);
    }

    /** Uma passada de atribuição sobre quem o filtro aceitar. */
    private static int assignPass(
            WorkerService workers, UUID colonyId, Set<UUID> employable, int adultPopulation,
            Predicate<UUID> accepts) {

        int assigned = 0;

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.hasProfession() || !employable.contains(worker.villagerId())
                    || !accepts.test(worker.villagerId())) {

                continue;
            }

            // <b>A vaga é perguntada por candidato</b> — 2026-09-10. Ver
            // vacancyFor: quem largou o ofício não pode recebê-lo de
            // volta na passagem seguinte, e é justamente ele o mais
            // escasso depois de abrir a própria vaga.
            Optional<ProfessionType> vacancy = vacancyFor(
                    worker, workers.ofColony(colonyId), adultPopulation);

            if (vacancy.isEmpty()) {
                // <b>Vazio por dois motivos, e eles não se tratam
                // igual</b> — 2026-09-10.
                //
                // Se a colônia inteira está sem vaga, não há o que fazer
                // por ninguém: todos os slots abertos pela população
                // adulta estão preenchidos.
                //
                // Se a vaga existe mas ESTE candidato a está evitando,
                // parar aqui seria deixar sem função todos os que vêm
                // depois dele na fila, por causa do castigo de um. Ele
                // passa a vez, e a vaga fica para o próximo — que é o
                // comportamento que a linha de reserva quer: um
                // trabalhador de molho não pode congelar a contratação da
                // colônia.
                if (vacancy(workers.ofColony(colonyId), adultPopulation).isEmpty()) {
                    break;
                }

                continue;
            }

            worker.assign(vacancy.get());
            assigned++;
        }

        return assigned;
    }
}
