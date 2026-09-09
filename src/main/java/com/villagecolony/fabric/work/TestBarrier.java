package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ResourceId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A barreira de teste da Regra 28, e a única coisa que a implementa.
 *
 * <p><b>Regra 28, do autor em 2026-08-20, e ele a declarou provisória:</b>
 * enquanto o projeto não estiver formalmente acabado, a obra não espera
 * por peça que não esteja num baú da vila — hoje porta, baú, tronco
 * descascado, tocha e vidraça. O bloco é riscado, e a casa fica sem
 * ele.
 *
 * <p><b>A razão dela caducou em 2026-08-21.</b> A justificativa escrita
 * era "peças que dependem de cadeia que a colônia ainda não fecha". As
 * cadeias fecharam — o pastor tosquia, a mina traz carvão e ferro, a
 * fornalha funde, o fabricante descasca e monta. Hoje a colônia sabe
 * fazer <b>todas</b> elas.
 *
 * <p>Decisão do autor em 2026-08-21: a barreira <b>fica</b> até a
 * primeira sessão de jogo, porque nenhuma dessas cadeias rodou de
 * verdade e é ela que impede a sessão de morrer se um elo quebrar. Mas
 * ela deixa de ser silenciosa: cada peça riscada sai como {@code WARN}
 * dizendo qual cadeia deveria tê-la produzido, e a sessão termina com a
 * soma. Casa que sobe sem uma linha destas subiu com a cadeia inteira;
 * casa que sobe com elas diz, peça por peça, onde procurar.
 *
 * <p><b>Cama e lampião saíram da lista no mesmo dia</b>, e por outra
 * decisão: a Regra 21 morreu, e com ela a passagem que repunha mobília
 * depois. Riscar a cama deixaria a casa sem cama <b>para sempre</b>, e a
 * demanda de lã sumiria junto — a conta sai da obra aberta agora. Para
 * esses dois vale a Regra 27 pura: o construtor aguarda, e o
 * PatienceClock é quem impede a colônia de morrer esperando.
 *
 * <p><b>Para desligar:</b> apague esta classe e as três chamadas a ela
 * em {@link BuilderWork} e {@link
 * com.villagecolony.fabric.event.ServerLifecycleHandler}. A Regra 27
 * volta a valer sem exceção.
 */
public final class TestBarrier {

    /**
     * As cinco peças, e a cadeia que deveria ter posto cada uma no baú.
     *
     * <p>Ordenado: a chave é o sufixo do nome do bloco, e a primeira que
     * casar vence. {@code stripped_} é prefixo e está tratado à parte.
     */
    private static final Map<String, String> CHAINS = new LinkedHashMap<>();

    static {
        CHAINS.put("_door", "the manufacturer's planks");
        CHAINS.put("chest", "the manufacturer's planks");
        CHAINS.put("torch", "the miner's coal and the manufacturer's sticks");
        CHAINS.put("_pane", "the miner's sand and the smelter's glass");
    }

    /** Quantas vezes cada peça foi riscada nesta sessão. */
    private static final Map<String, Integer> SKIPPED = new LinkedHashMap<>();

    /**
     * Desde quando cada peça de cada obra está faltando no baú.
     *
     * <p>A chave é a obra <b>e</b> o bloco: a mesma peça faltando em
     * duas casas são duas esperas, e a carência de uma não conta para a
     * outra.
     */
    private static final Map<String, Long> FIRST_MISSED = new LinkedHashMap<>();

    /**
     * Quanto tempo a barreira espera antes de riscar — 2026-09-09.
     *
     * <p><b>Ela riscava na primeira falta</b>, e a sessão de 09-06
     * mostrou o que isso esconde: 24 {@code stripped_oak_log} riscados
     * com <b>cinquenta toras de carvalho no baú</b> da mesma colônia.
     * O fabricante sabe descascar — {@code ManufacturerWork.strip} roda
     * antes da guarda de conversão e escolhe a espécie que a colônia
     * tem, desde 09-05 —, mas o construtor chega ao bloco, não acha a
     * peça pronta e risca no mesmo tique. Na sessão inteira o fabricante
     * descascou <b>um</b> tronco.
     *
     * <p>A cadeia não deixou de entregar: ela nunca teve o ciclo para
     * entregar. E o grito da barreira dizia o contrário, que é pior que
     * o buraco na parede — manda o autor procurar defeito numa cadeia
     * inteira.
     *
     * <p>Cinco ciclos da colônia. Bastante para o fabricante pegar a
     * tarefa, chegar ao baú e descascar; e bem dentro dos vinte ciclos
     * do {@code PatienceClock}, que é quem tira a obra da frente se a
     * espera não terminar. Passado o prazo a Regra 28 vale como sempre
     * valeu: risca, e grita — só que agora o grito é verdadeiro.
     */
    private static final int GRACE_TICKS = 5 * VillageDetector.CYCLE_TICKS;

    /**
     * Quantas peças a obra assentou de verdade nesta sessão.
     *
     * <p><b>Existe por causa do E31</b>, visto em jogo na sessão de
     * 2026-08-26, 23:06: zero obras, zero projetos, nenhum bloco posto —
     * e o servidor parou dizendo <i>"covered for nothing — Rule 28 can
     * go"</i>. A frase é uma conclusão sobre a Regra 28, e a soma que a
     * sustentava só sabia contar o que <b>foi</b> riscado. Numa sessão
     * sem obra a barreira não é exercitada uma vez, e o silêncio dela
     * não prova nada.
     */
    private static int laid;

    private TestBarrier() {
    }

    /**
     * O que a sessão autoriza dizer sobre a Regra 28.
     *
     * <p>Três estados, e a diferença entre os dois primeiros é o E31:
     * absolver a barreira e não ter tido o que medir <b>não</b> são a
     * mesma coisa.
     */
    public enum Verdict {

        /**
         * Nenhuma peça foi assentada: a barreira não foi exercitada.
         *
         * <p>Não é notícia boa nem má — é ausência de notícia, e é o que
         * a sessão das 23:06 deveria ter dito.
         */
        NOTHING_BUILT,

        /**
         * A obra assentou, e nada precisou ser riscado.
         *
         * <p>É a única forma da notícia boa: a colônia produziu tudo o
         * que a casa pediu, e a Regra 28 pode sair.
         */
        COVERED_FOR_NOTHING,

        /** A barreira riscou peça, e a lista diz qual e quantas. */
        COVERED
    }

    /**
     * Uma peça assentada pela obra, seja ela da barreira ou não.
     *
     * <p>Conta a peça, e não o bloco: a porta que ocupa duas posições
     * passa por aqui uma vez só.
     */
    public static void laidOne() {
        laid++;
    }

    /**
     * O veredito da sessão, sem escrever nada.
     *
     * <p>Peça riscada vence: ela é medida por si, e ter construído não
     * apaga o que a cadeia deixou de entregar.
     */
    public static Verdict verdict() {
        if (!SKIPPED.isEmpty()) {
            return Verdict.COVERED;
        }

        return laid == 0 ? Verdict.NOTHING_BUILT : Verdict.COVERED_FOR_NOTHING;
    }

    /**
     * Se este bloco é uma das sete peças que a barreira deixa passar.
     *
     * @return a cadeia que deveria tê-lo produzido, ou vazio se a
     *     barreira não cobre este bloco — e nesse caso vale a Regra 27,
     *     e o construtor aguarda. Cama e lampião caem aqui desde
     *     2026-08-21
     */
    public static Optional<String> chainFor(ResourceId block) {
        String name = block.path();

        if (name.startsWith("stripped_")) {
            return Optional.of("the manufacturer's stripping");
        }

        for (Map.Entry<String, String> chain : CHAINS.entrySet()) {
            if (name.endsWith(chain.getKey())) {
                return Optional.of(chain.getValue());
            }
        }

        return Optional.empty();
    }

    /**
     * A obra bateu na falta desta peça: começa a contar, e diz se a
     * carência já venceu.
     *
     * <p>Quem chama é o construtor, e só ele — é ele que de fato tentou
     * tirar a peça do baú. O despertador da obra usa
     * {@link #willStrike}, que lê sem começar a contar: carência que
     * corresse sem ninguém ter tentado venceria antes da primeira
     * tentativa, e a espera não teria existido.
     *
     * @return {@code true} quando a peça já esperou {@link #GRACE_TICKS}
     *     e a Regra 28 pode riscá-la
     */
    public static boolean graceExpired(long now, UUID projectId, ResourceId block) {
        long since = FIRST_MISSED.computeIfAbsent(key(projectId, block), any -> now);

        return now - since >= GRACE_TICKS;
    }

    /**
     * Se a barreira já desistiu desta peça, sem mexer no relógio dela.
     *
     * <p>É a pergunta que {@code BuilderWork.hasMaterialForNextBlock}
     * faz, e as duas respostas têm de casar com o que o construtor vai
     * fazer — senão a obra acorda, tenta, falha e dorme, todo ciclo,
     * que é o laço que aquele método já evitava por outro caminho.
     *
     * <p>Peça que a barreira ainda espera <b>segura a obra como
     * qualquer outra</b>: o despertador cai no teste de material de
     * verdade, a obra dorme enquanto a colônia não tiver a peça, e
     * acorda quando o fabricante a puser no baú. Peça de que a barreira
     * já desistiu não segura nada, porque o construtor vai passar por
     * cima dela — e aí dizer "tem" é dizer a verdade sobre o que vai
     * acontecer, que é o que esta pergunta sempre respondeu.
     */
    public static boolean willStrike(long now, UUID projectId, ResourceId block) {
        Long since = FIRST_MISSED.get(key(projectId, block));

        return since != null && now - since >= GRACE_TICKS;
    }

    private static String key(UUID projectId, ResourceId block) {
        return projectId + "/" + block.path();
    }

    /**
     * Uma peça riscada, e o grito que a acompanha.
     *
     * <p>{@code WARN} de propósito: a linha existe para ser achada num
     * log de sessão de vinte minutos, e {@code INFO} some no meio do
     * relatório de ciclo.
     */
    public static void skip(UUID projectId, ResourceId block, String chain) {
        SKIPPED.merge(block.path(), 1, Integer::sum);

        VillageColonyMod.LOGGER.warn(
                "TEST BARRIER skipped {} in project {} — the colony should be able to make"
                        + " this by now ({}), so that chain did not deliver",
                block.path(),
                projectId,
                chain);
    }

    /**
     * A soma da sessão, uma linha só, ao parar o servidor.
     *
     * <p><b>Ela diz o que mediu, e só isso</b> — o E31 era o contrário.
     * Sessão que não assentou peça nenhuma sai com
     * {@link Verdict#NOTHING_BUILT} e não absolve a Regra 28: a barreira
     * não teve chance de trabalhar, e chamar isso de notícia boa mandou
     * o autor riscar do {@code TODO} uma pendência que continuava
     * aberta.
     */
    public static void report() {
        Verdict verdict = verdict();

        if (verdict == Verdict.NOTHING_BUILT) {
            VillageColonyMod.LOGGER.info(
                    "TEST BARRIER has nothing to say this session — no piece was laid, so it was"
                            + " never asked to cover for anything. Rule 28 stands untested.");

            return;
        }

        if (verdict == Verdict.COVERED_FOR_NOTHING) {
            VillageColonyMod.LOGGER.info(
                    "TEST BARRIER covered for nothing this session — {} pieces were laid and every"
                            + " one came from the colony's own chests. Rule 28 can go.",
                    laid);

            return;
        }

        VillageColonyMod.LOGGER.warn(
                "TEST BARRIER covered for {} of the {} pieces laid this session:", total(), laid);

        SKIPPED.forEach(
                (block, count) ->
                        VillageColonyMod.LOGGER.warn(
                                "    {}x {} — {}",
                                count,
                                block,
                                chainFor(ResourceId.vanilla(block)).orElse("no chain")));
    }

    private static int total() {
        return SKIPPED.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Esquece a soma inteira. Chamado ao parar o servidor, depois do relatório. */
    public static void clearAll() {
        SKIPPED.clear();
        FIRST_MISSED.clear();
        laid = 0;
    }
}
