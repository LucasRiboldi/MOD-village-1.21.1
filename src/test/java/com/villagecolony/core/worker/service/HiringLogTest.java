package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        candidate.assign(ProfessionType.MINER);
        candidate.giveUpProfession();

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

        shunned.assign(ProfessionType.MINER);
        shunned.giveUpProfession();

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
}
