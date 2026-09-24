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
            ProfessionType.SHEPHERD);

    /**
     * Piso da casa fundacional: uma função ativa de cada tipo alojado nela.
     *
     * <p>{@link ProfessionType#CARPENTER} e {@link ProfessionType#FARMER} seguem
     * disponíveis no registro, na atribuição, nas tarefas e no crescimento
     * normal. A única regra desta lista é que eles não são titulares nem
     * recebem cama ou baú reservados na {@code BigHouseMOD}.
     */
    public static final List<ProfessionType> FOUNDATION_ORDER = List.of(
            ProfessionType.MINER,
            ProfessionType.LUMBERJACK,
            ProfessionType.MASON,
            ProfessionType.SMELTER,
            ProfessionType.SHEPHERD,
            ProfessionType.BUILDER);

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
     * primeiro e o Pastor por último.
     */
    public static ProfessionType mostNeeded(Collection<Worker> colonyWorkers) {
        Objects.requireNonNull(colonyWorkers, "colonyWorkers");

        Map<ProfessionType, Integer> counts = new EnumMap<>(ProfessionType.class);

        for (ProfessionType type : PRODUCER_ORDER) {
            counts.put(type, 0);
        }

        for (Worker worker : colonyWorkers) {
            worker.profession()
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

        Map<ProfessionType, Integer> counts = VacancyEnforcer.countByProfession(colonyWorkers);

        // <b>Por que cada vaga não saiu</b> — 2026-09-18. As duas recusas
        // abaixo eram silenciosas, e do lado de fora "a conta não abre a
        // vaga" e "a vaga abre e o candidato está de castigo" são o mesmo
        // nada: a vila simplesmente não tem pedreiro. Foi o que custou a
        // investigação do deserto — 28 paradas esperando cut_sandstone,
        // que é do pedreiro, com o pedreiro sendo o TERCEIRO da ordem e o
        // fundidor, que é o quarto, com 109 linhas no log.
        //
        // Só conta quando há candidato: a pergunta sem dono é a contagem
        // da colônia, e ela roda a cada passagem por motivo próprio —
        // registrá-la encheria o relatório de ruído que não é decisão de
        // contratação. Ver HiringLog.
        UUID colonyId = candidate == null ? null : candidate.colonyId();

        // <b>Quem acabou de largar um ofício não pega outro agora</b> —
        // 2026-09-19. O castigo do ofício abaixo é por ofício, e a
        // colônia tem sete: numa vila em que tudo trava, largar um é
        // receber o seguinte da ordem na mesma passagem, e o trabalhador
        // atravessa a lista inteira antes de o primeiro castigo vencer.
        // Não é a linha de reserva, é rodízio — ver
        // Worker.BETWEEN_TRADES_CYCLES e a sessão de jogo de 09-19.
        if (candidate != null && candidate.isBetweenTrades()) {
            HiringLog.record(colonyId, PRODUCER_ORDER.get(0), HiringLog.Outcome.BETWEEN_TRADES);

            return Optional.empty();
        }

        // A fundação vem antes do crescimento: enquanto houver adultos
        // suficientes para a próxima função, nenhuma função ativa pode
        // ficar vazia. A camada Fabric cria os adultos e as camas que
        // faltarem; este trecho garante a parte determinística da regra.
        for (int index = 0; index < FOUNDATION_ORDER.size()
                && index < adultPopulation; index++) {
            ProfessionType type = FOUNDATION_ORDER.get(index);

            if (counts.get(type) >= 1) {
                continue;
            }

            if (candidate != null && candidate.isShunning(type)) {
                HiringLog.record(colonyId, type, HiringLog.Outcome.SHUNNED);

                continue;
            }

            if (colonyId != null) {
                HiringLog.record(colonyId, type, HiringLog.Outcome.FILLED);
            }

            return Optional.of(type);
        }

        for (ProfessionType type : PRODUCER_ORDER) {
            if (counts.get(type) >= targetCount(type, adultPopulation)) {
                if (colonyId != null) {
                    HiringLog.record(colonyId, type, HiringLog.Outcome.AT_TARGET);
                }

                continue;
            }

            if (candidate != null && candidate.isShunning(type)) {
                HiringLog.record(colonyId, type, HiringLog.Outcome.SHUNNED);

                continue;
            }

            if (colonyId != null) {
                HiringLog.record(colonyId, type, HiringLog.Outcome.FILLED);
            }

            return Optional.of(type);
        }

        if (colonyId != null) {
            // Nenhuma profissão serviu a este candidato. A conta por
            // profissão acima já disse quais e por quê; esta linha existe
            // para o caso de a ordem ficar vazia, que seria outro defeito.
            HiringLog.record(colonyId, PRODUCER_ORDER.get(0), HiringLog.Outcome.NO_VACANCY);
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
