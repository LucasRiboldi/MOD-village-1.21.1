package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Por que cada vaga não saiu — 2026-09-18.
 *
 * <p><b>O que esta instrumentação existe para responder.</b> A vila do
 * deserto parou 28 vezes esperando {@code cut_sandstone}, que é do
 * <b>pedreiro</b>, e não tinha pedreiro nenhum — sendo ele o <b>terceiro</b>
 * da {@code PRODUCER_ORDER}, antes do fundidor, que encheu o log com 109
 * linhas. Nada dizia por quê.
 *
 * <p>Do lado de fora, três estados bem diferentes eram o mesmo nada: <i>a
 * conta da população não abre a vaga</i>, <i>a vaga abre e o candidato está
 * de castigo nela</i>, <i>a colônia inteira está lotada</i>. O primeiro
 * pede mexer na conta; o segundo, no castigo; o terceiro não pede nada.
 *
 * <p><b>É o mesmo movimento que resolveu o P1.0 e o P1.3</b>, e os dois
 * custaram dias antes de alguém instrumentar.
 */
class HiringLogTest {

    private static final UUID COLONY = UUID.randomUUID();

    @BeforeEach
    void forgetEverything() {
        HiringLog.clearAll();
    }

    private static Worker hired(ProfessionType profession) {
        return Worker.restore(UUID.randomUUID(), COLONY, profession);
    }

    private static Worker idle() {
        return Worker.register(UUID.randomUUID(), COLONY);
    }

    /**
     * <b>A espera entre ofícios chega ao relatório</b> — 2026-09-19.
     *
     * <p>Sem esta linha o rodízio continuaria invisível pelo mesmo
     * motivo que fez o {@code HiringLog} existir: do lado de fora
     * <i>"ninguém foi contratado"</i> parece colônia lotada, e o
     * conserto iria para a conta da população. O que o log precisa
     * dizer é que <b>havia vaga e o candidato acabou de largar um
     * ofício</b>.
     */
    @Test
    void theCandidateBetweenTradesIsSaidSo() {
        Worker candidate = idle();

        candidate.assign(ProfessionType.MINER);
        candidate.giveUpProfession();

        ProfessionAssigner.vacancyFor(candidate, List.of(candidate), 7);

        assertEquals(
                1,
                HiringLog.countOf(
                        COLONY, ProfessionType.MINER, HiringLog.Outcome.BETWEEN_TRADES),
                "a espera entre ofícios não foi registrada");

        assertTrue(
                HiringLog.report(COLONY).contains("just left a trade"),
                "o rodízio não chegou ao relatório: " + HiringLog.report(COLONY));
    }

    /**
     * Larga o ofício e deixa passar a espera entre ofícios — 2026-09-19.
     *
     * <p>Sem isto o candidato é recusado por {@code BETWEEN_TRADES}
     * antes de a vaga ser perguntada, e os testes daqui param de medir o
     * que afirmam: eles são sobre o <b>castigo do ofício</b>, e não
     * sobre a espera que o precede. Ver
     * {@code Worker.BETWEEN_TRADES_CYCLES}.
     */
    private static void gaveUpAndWaited(Worker worker, ProfessionType type) {
        worker.assign(type);
        worker.giveUpProfession();

        for (int i = 0; i < Worker.BETWEEN_TRADES_CYCLES; i++) {
            worker.aCycleWentBy();
        }
    }

    /**
     * A vaga preenchida é dita, e ela sai do relatório.
     *
     * <p>Quem conseguiu gente não é o assunto: listá-la afogaria a
     * profissão que falta, que é a única coisa que o relatório precisa
     * mostrar.
     */
    @Test
    void theFilledVacancyLeavesTheReport() {
        Worker candidate = idle();

        ProfessionAssigner.vacancyFor(candidate, List.of(candidate), 7);

        assertEquals(
                1,
                HiringLog.countOf(COLONY, ProfessionType.MINER, HiringLog.Outcome.FILLED),
                "a primeira vaga da ordem não foi registrada como preenchida");

        assertTrue(
                HiringLog.report(COLONY).isEmpty(),
                "profissão preenchida apareceu no relatório: " + HiringLog.report(COLONY));
    }

    /**
     * Profissão no alvo é dita como <b>no alvo</b>, e não como silêncio.
     *
     * <p>É a metade que explica o deserto: se a conta da população já deu
     * ao pedreiro o que ela paga, a vaga não abre — e o conserto é a
     * conta, não o castigo.
     */
    @Test
    void theProfessionAtTargetSaysSo() {
        Worker miner = hired(ProfessionType.MINER);

        Worker candidate = idle();

        // Um adulto: a conta abre uma vaga só, e o mineiro já a tem.
        ProfessionAssigner.vacancyFor(candidate, List.of(miner, candidate), 1);

        assertEquals(
                1,
                HiringLog.countOf(COLONY, ProfessionType.MINER, HiringLog.Outcome.AT_TARGET),
                "o mineiro estava no alvo e o relatório não disse");

        assertTrue(
                HiringLog.report(COLONY).contains("MINER"),
                "o relatório não citou a profissão que não abriu vaga: "
                        + HiringLog.report(COLONY));
    }

    /**
     * Castigo do candidato é dito como castigo, e não como "sem vaga".
     *
     * <p>É a distinção que decide o conserto. Com a vaga aberta e o
     * candidato de molho, mexer na conta da população não resolveria
     * nada — e era para lá que o diagnóstico apontaria sem esta linha.
     */
    @Test
    void theShunnedCandidateIsToldApartFromAFullColony() {
        Worker candidate = idle();

        gaveUpAndWaited(candidate, ProfessionType.MINER);

        ProfessionAssigner.vacancyFor(candidate, List.of(candidate), 7);

        assertEquals(
                1,
                HiringLog.countOf(COLONY, ProfessionType.MINER, HiringLog.Outcome.SHUNNED),
                "o castigo do candidato não foi registrado");

        assertEquals(
                0,
                HiringLog.countOf(COLONY, ProfessionType.MINER, HiringLog.Outcome.AT_TARGET),
                "castigo foi contado como 'no alvo', e os dois pedem consertos opostos");
    }

    /**
     * O mesmo cenário nunca produz os dois desfechos — 2026-09-18.
     *
     * <p><b>A primeira versão deste teste era fraca</b>, e vale registrar
     * porque só a mutação a revelou: ela montava dois cenários
     * <i>diferentes</i> e exigia textos diferentes. Trocando
     * {@code SHUNNED} por {@code AT_TARGET} no código, ela continuava
     * verde — os textos diferiam por outro motivo, o número de adultos,
     * e não pela distinção que o teste afirma proteger.
     *
     * <p>Esta versão fixa o cenário: <b>um</b> candidato de castigo, com
     * vaga aberta. Aí os dois desfechos são mutuamente exclusivos por
     * construção, e trocar um pelo outro no código quebra a afirmação.
     */
    @Test
    void theSameCaseNeverYieldsBothOutcomes() {
        Worker shunned = idle();

        gaveUpAndWaited(shunned, ProfessionType.MINER);

        ProfessionAssigner.vacancyFor(shunned, List.of(shunned), 7);

        int punished =
                HiringLog.countOf(COLONY, ProfessionType.MINER, HiringLog.Outcome.SHUNNED);

        int atTarget =
                HiringLog.countOf(COLONY, ProfessionType.MINER, HiringLog.Outcome.AT_TARGET);

        assertEquals(1, punished, "o candidato de castigo não foi contado como tal");

        // A vaga do mineiro ESTÁ aberta — sete adultos, ninguém empregado.
        // Contá-la como "no alvo" é a colisão que este teste existe para
        // impedir, e mandaria o conserto para a conta da população em vez
        // de para o castigo.
        assertEquals(
                0,
                atTarget,
                "com a vaga aberta, o castigo foi contado como 'no alvo' — o"
                        + " diagnóstico apontaria para a conta da população");
    }

    /**
     * Contratada uma vez não é contratada sempre — 2026-09-19.
     *
     * <p><b>O defeito que este teste tranca, e ele me enganou de
     * verdade.</b> O filtro tirava a profissão do relatório para sempre
     * depois de um único {@code FILLED}, e o contador é acumulativo. Numa
     * sessão de 61 passagens, o mineiro contratado na primeira sumia das
     * outras sessenta, e a linha saía como
     * <i>"MASON/SMELTER/CARPENTER at target"</i> sem citar {@code MINER}.
     *
     * <p>Eu li isso como <b>"a vaga do pedreiro não abre"</b> e quase fui
     * mexer na conta da população. O certo era o contrário: o pedreiro
     * <b>existia</b> — {@code MASON b06ae217 claimed the chest} —, e o que
     * faltava era <b>tarefa</b> para ele, que é outro defeito inteiro.
     *
     * <p>Um relatório que some com a informação no momento em que ela fica
     * interessante é pior que nenhum: ele não cala, <b>mente por
     * omissão</b>.
     */
    @Test
    void theVacancyFilledOnceStillShowsWhenItLaterCloses() {
        Worker candidate = idle();

        // Primeira passagem: a vaga do mineiro abre e é ocupada.
        ProfessionAssigner.vacancyFor(candidate, List.of(candidate), 7);

        assertEquals(
                1,
                HiringLog.countOf(COLONY, ProfessionType.MINER, HiringLog.Outcome.FILLED),
                "a vaga não foi contada como preenchida");

        // Passagens seguintes: já há mineiro, e a vaga não abre mais.
        Worker miner = hired(ProfessionType.MINER);

        Worker later = idle();

        ProfessionAssigner.vacancyFor(later, List.of(miner, later), 1);
        ProfessionAssigner.vacancyFor(later, List.of(miner, later), 1);

        assertTrue(
                HiringLog.report(COLONY).contains("MINER"),
                "o mineiro foi contratado uma vez e sumiu do relatório para sempre —"
                        + " o relatório mente por omissão: " + HiringLog.report(COLONY));
    }

    // --- o laço dos produtores, 2026-09-25 (sobreviventes do PIT) ---
    //
    // Os testes acima caem todos no laço da fundação, que vem antes: com a
    // fundação vazia, o castigo e a vaga são decididos lá. O laço dos
    // produtores só roda com os seis titulares no lugar, e nada o media.

    /** Os seis titulares da casa fundacional, todos no posto. */
    private static List<Worker> foundationFilled() {
        return ProfessionAssigner.FOUNDATION_ORDER.stream().map(HiringLogTest::hired).toList();
    }

    /**
     * Fundação completa, sete adultos: um por produtor. Carpinteiro e
     * fazendeiro são os que faltam; o candidato largou a carpintaria.
     */
    @Test
    void pastTheFoundationTheShunIsSaidAndTheNextTradeIsFilled() {
        Worker candidate = idle();

        gaveUpAndWaited(candidate, ProfessionType.CARPENTER);

        List<Worker> colony = new ArrayList<>(foundationFilled());
        colony.add(candidate);

        assertEquals(
                Optional.of(ProfessionType.FARMER),
                ProfessionAssigner.vacancyFor(candidate, colony, 7));

        assertEquals(1, HiringLog.countOf(
                COLONY, ProfessionType.CARPENTER, HiringLog.Outcome.SHUNNED));
        assertEquals(1, HiringLog.countOf(
                COLONY, ProfessionType.FARMER, HiringLog.Outcome.FILLED));
    }

    /** Todos no alvo: o "sem vaga" é dito, e não calado. */
    @Test
    void aFullColonySaysThereIsNoVacancy() {
        List<Worker> colony = new ArrayList<>(foundationFilled());
        colony.add(hired(ProfessionType.CARPENTER));
        colony.add(hired(ProfessionType.FARMER));

        Worker candidate = idle();
        colony.add(candidate);

        assertEquals(Optional.empty(),
                ProfessionAssigner.vacancyFor(candidate, colony, 8));

        assertEquals(1, HiringLog.countOf(
                COLONY, ProfessionType.MINER, HiringLog.Outcome.NO_VACANCY));
    }

    /** Colônia sem adulto não é erro: não há vaga, e pronto. */
    @Test
    void noAdultsMeansNoVacancyAndNoError() {
        Worker candidate = idle();

        assertEquals(Optional.empty(),
                ProfessionAssigner.vacancyFor(candidate, List.of(candidate), 0));

        assertThrows(IllegalArgumentException.class,
                () -> ProfessionAssigner.vacancyFor(candidate, List.of(candidate), -1));
    }

    // --- o teto e o formato do relatório, 2026-09-25 (sobreviventes do PIT) ---

    /**
     * O registro guarda até 64 colônias; a 65ª esvazia tudo e começa de
     * novo. É o teto que impede o mapa de crescer sem limite num servidor
     * que carrega e descarrega vilas a noite toda.
     */
    @Test
    void theSixtyFifthColonyStartsTheLogOver() {
        UUID first = UUID.randomUUID();
        HiringLog.record(first, ProfessionType.MINER, HiringLog.Outcome.AT_TARGET);

        for (int i = 1; i < 64; i++) {
            HiringLog.record(UUID.randomUUID(), ProfessionType.MINER, HiringLog.Outcome.AT_TARGET);
        }

        assertEquals(1, HiringLog.countOf(first, ProfessionType.MINER, HiringLog.Outcome.AT_TARGET),
                "com 64 colônias ninguém devia ter sido esquecido");

        UUID sixtyFifth = UUID.randomUUID();
        HiringLog.record(sixtyFifth, ProfessionType.MINER, HiringLog.Outcome.AT_TARGET);

        assertEquals(0, HiringLog.countOf(first, ProfessionType.MINER, HiringLog.Outcome.AT_TARGET));
        assertEquals(1, HiringLog.countOf(sixtyFifth, ProfessionType.MINER, HiringLog.Outcome.AT_TARGET));
    }

    /** Dois desfechos saem separados por "; ", e nenhum começa com o separador. */
    @Test
    void theReportSeparatesItsItemsAndNeverStartsWithTheSeparator() {
        HiringLog.record(COLONY, ProfessionType.MINER, HiringLog.Outcome.AT_TARGET);
        HiringLog.record(COLONY, ProfessionType.MINER, HiringLog.Outcome.SHUNNED);

        String report = HiringLog.report(COLONY);

        assertFalse(report.startsWith(";"), "o relatório abriu com o separador: " + report);
        assertEquals(2, report.split("; ").length, "esperava dois itens: " + report);
    }
}
