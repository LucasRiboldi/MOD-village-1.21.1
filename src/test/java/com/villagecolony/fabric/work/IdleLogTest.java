package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.IdleReason;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O registrador de silêncio fala nas transições, e volta a falar depois
 * do {@code clear}.
 *
 * <p><b>Ele não tinha teste nenhum até 2026-09-09</b>, e a promessa dele
 * é sutil: <i>fala na primeira vez, cala enquanto o motivo não mudar, e
 * volta a falar quando mudar</i>. Uma regressão aí não quebra nada — só
 * apaga do log a linha que explica a colônia parada, que é o oposto do
 * que este tipo existe para fazer.
 *
 * <p>O ciclo que o descobriu foi o que ligou o
 * {@code ColonyCycle.requestMissing} a ele: a correção do pulo calado
 * <b>depende</b> do par {@code record}/{@code clear}, e a metade do
 * {@code clear} é a fácil de esquecer — sem ela, uma colônia que perde a
 * profissão, contrata outra e a perde de novo fica muda na segunda vez.
 *
 * <p>Afirma o <b>texto emitido</b>, e não o estado interno. É a lição da
 * mesma semana: um teste sobre os construtores da frase não cai sobre o
 * defeito quando o defeito é o argumento.
 */
class IdleLogTest {

    private static final String SUBJECT = "smelt_material";

    private final UUID colony = UUID.randomUUID();

    private Captured captured;

    @BeforeEach
    void attach() {
        IdleLog.clearAll();

        captured = Captured.attached();
    }

    @AfterEach
    void detach() {
        captured.detach();

        IdleLog.clearAll();
    }

    /** A primeira vez fala, e diz o motivo e o detalhe. */
    @Test
    void theFirstTimeSpeaks() {
        IdleLog.record(colony, SUBJECT, IdleReason.NO_WORKER, "GLASS needs SMELT_ITEMS");

        assertEquals(1, captured.lines.size(), "a primeira vez calou: " + captured.lines);

        String line = captured.lines.get(0);

        assertTrue(
                line.contains("no " + SUBJECT + " work"),
                "a linha não nomeia o assunto: " + line);

        assertTrue(
                line.contains("no worker in the village can do it"),
                "a linha não traz o motivo: " + line);

        assertTrue(
                line.contains("GLASS needs SMELT_ITEMS"),
                "a linha não traz o detalhe, e é ele que diz qual material: " + line);
    }

    /**
     * Repetir o mesmo motivo cala, mesmo com detalhe diferente.
     *
     * <p>É a promessa do tipo, e o detalhe fica fora da comparação de
     * propósito: o ciclo roda a cada trinta segundos, e um detalhe que
     * muda faria a linha voltar toda vez.
     */
    @Test
    void theSameReasonStaysQuiet() {
        IdleLog.record(colony, SUBJECT, IdleReason.NO_WORKER, "GLASS needs SMELT_ITEMS");
        IdleLog.record(colony, SUBJECT, IdleReason.NO_WORKER, "IRON_INGOT needs SMELT_ITEMS");
        IdleLog.record(colony, SUBJECT, IdleReason.NO_WORKER, "SMOOTH_SANDSTONE needs it too");

        assertEquals(
                1,
                captured.lines.size(),
                "o mesmo motivo falou mais de uma vez, e o log de uma sessão de vinte"
                        + " minutos teria oitenta linhas iguais: " + captured.lines);
    }

    /**
     * <b>Depois do {@code clear}, o mesmo motivo volta a falar.</b>
     *
     * <p>É a metade de que a correção do pulo calado depende, e a que
     * cai se alguém tirar o {@code clear} por parecer supérfluo: a
     * colônia perde o fundidor (fala), contrata outro (limpa), e o perde
     * de novo — e é essa terceira que ficaria muda.
     */
    @Test
    void afterClearTheSameReasonSpeaksAgain() {
        IdleLog.record(colony, SUBJECT, IdleReason.NO_WORKER, "GLASS needs SMELT_ITEMS");

        IdleLog.clear(colony, SUBJECT);

        IdleLog.record(colony, SUBJECT, IdleReason.NO_WORKER, "GLASS needs SMELT_ITEMS");

        assertEquals(
                2,
                captured.lines.size(),
                "a segunda vez ficou muda depois do clear — é a colônia que perde a"
                        + " profissão duas vezes e só conta a primeira: " + captured.lines);
    }

    /**
     * Assuntos diferentes não se calam um ao outro.
     *
     * <p>Importa aqui porque a correção do pulo calado usa <b>um assunto
     * por tarefa</b>: uma colônia sem fundidor e sem mineiro tem dois
     * silêncios, e o primeiro não pode engolir o segundo.
     */
    @Test
    void differentSubjectsDoNotSilenceEachOther() {
        IdleLog.record(colony, "smelt_material", IdleReason.NO_WORKER, "GLASS");
        IdleLog.record(colony, "collect_stone", IdleReason.NO_WORKER, "COBBLESTONE");

        assertEquals(
                2,
                captured.lines.size(),
                "um assunto calou o outro, e a colônia sem duas profissões só"
                        + " contaria uma: " + captured.lines);
    }

    /** Colônias diferentes também não. */
    @Test
    void differentColoniesDoNotSilenceEachOther() {
        IdleLog.record(colony, SUBJECT, IdleReason.NO_WORKER, "GLASS");
        IdleLog.record(UUID.randomUUID(), SUBJECT, IdleReason.NO_WORKER, "GLASS");

        assertEquals(
                2,
                captured.lines.size(),
                "uma colônia calou a outra: " + captured.lines);
    }

    /** Motivo diferente no mesmo assunto volta a falar. */
    @Test
    void aDifferentReasonSpeaks() {
        IdleLog.record(colony, SUBJECT, IdleReason.NO_WORKER, "");
        IdleLog.record(colony, SUBJECT, IdleReason.MISSING_MATERIAL, "");

        assertEquals(
                2,
                captured.lines.size(),
                "o motivo mudou e o log não disse — é a transição que este tipo"
                        + " existe para registrar: " + captured.lines);
    }

    /**
     * Pendura-se no logger do mod e sai no {@code AfterEach}.
     *
     * <p>Mesma técnica do {@code LumberjackGameTest}: um appender
     * esquecido acumularia linha de todos os testes seguintes.
     */
    private static final class Captured extends AbstractAppender {

        private final List<String> lines = new ArrayList<>();

        private Captured() {
            super("village-colony-idle-log-test", null, null, true, Property.EMPTY_ARRAY);
        }

        static Captured attached() {
            Captured captured = new Captured();

            captured.start();

            ((org.apache.logging.log4j.core.Logger)
                    LogManager.getLogger(VillageColonyMod.MOD_ID)).addAppender(captured);

            return captured;
        }

        void detach() {
            ((org.apache.logging.log4j.core.Logger)
                    LogManager.getLogger(VillageColonyMod.MOD_ID)).removeAppender(this);

            stop();
        }

        @Override
        public void append(LogEvent event) {
            lines.add(event.getMessage().getFormattedMessage());
        }
    }

    // ------------------------------------------------------------------
    // O amortecedor — 2026-09-11, sessão das 02:03.
    //
    // A regra de transição supõe que quem pergunta é o ciclo da colônia,
    // uma vez a cada trinta segundos. A busca de areia pergunta no laço
    // do mineiro, POR TIQUE, e o motivo dela alterna por construção:
    // toda volta da varredura em anéis termina em NO_TARGET e a seguinte
    // recomeça em SWEEP_INCOMPLETE. Duas transições por volta, uma volta
    // a cada dez tiques: quatro linhas por segundo.
    //
    // Medido naquela sessão: 4.389 linhas de areia num log de 6.117 —
    // 72% da sessão, e a próxima fica cega para qualquer outra coisa.
    // ------------------------------------------------------------------

    /** <b>O caso da sessão:</b> motivo que oscila não vira enxurrada. */
    @Test
    void anOscillatingReasonDoesNotFlood() {
        for (int tick = 0; tick < 600; tick++) {
            IdleLog.recordAt(
                    colony,
                    SUBJECT,
                    tick % 2 == 0 ? IdleReason.SWEEP_INCOMPLETE : IdleReason.NO_TARGET,
                    "sand within 48 blocks",
                    tick);
        }

        assertEquals(
                1,
                captured.lines.size(),
                "seiscentos tiques de motivo alternante deram "
                        + captured.lines.size() + " linhas — é a enxurrada de volta");
    }

    /** E passado o silêncio ele volta a falar: a notícia não se perde. */
    @Test
    void afterTheQuietPeriodItSpeaksAgain() {
        IdleLog.recordAt(colony, SUBJECT, IdleReason.SWEEP_INCOMPLETE, "", 0);

        IdleLog.recordAt(colony, SUBJECT, IdleReason.NO_TARGET, "", 600);

        assertEquals(
                2,
                captured.lines.size(),
                "passado um ciclo inteiro o assunto continuou calado");
    }

    /**
     * <b>E o amortecedor não fala do que não mudou.</b> Ele se soma à
     * regra de transição, não a substitui: motivo igual continua calado
     * mesmo depois do silêncio.
     */
    @Test
    void theQuietPeriodDoesNotResurrectTheSameReason() {
        IdleLog.recordAt(colony, SUBJECT, IdleReason.NO_TARGET, "", 0);

        IdleLog.recordAt(colony, SUBJECT, IdleReason.NO_TARGET, "", 6000);

        assertEquals(
                1,
                captured.lines.size(),
                "o mesmo motivo foi dito duas vezes só porque o tempo passou");
    }

    /**
     * <b>Quem pergunta por ciclo não paga o amortecedor.</b> O
     * {@code record} sem relógio continua falando em toda transição, e é
     * de propósito: lá a transição já é rara, e atrasar uma notícia
     * legítima seria o preço errado.
     */
    @Test
    void theCycleCallerIsNotDamped() {
        IdleLog.record(colony, SUBJECT, IdleReason.SWEEP_INCOMPLETE, "");

        IdleLog.record(colony, SUBJECT, IdleReason.NO_TARGET, "");

        assertEquals(
                2,
                captured.lines.size(),
                "quem pergunta uma vez por ciclo foi calado pelo amortecedor");
    }
}
