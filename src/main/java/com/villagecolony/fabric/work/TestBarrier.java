package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
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
        laid = 0;
    }
}
