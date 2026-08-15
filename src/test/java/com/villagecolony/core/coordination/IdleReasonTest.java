package com.villagecolony.core.coordination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O que este tipo promete, e por que vale testar um {@code enum}.
 *
 * <p>Não é cerimônia. O valor de {@link IdleReason} está inteiro na
 * promessa de que dois silêncios diferentes produzem linhas diferentes —
 * é essa promessa que o E14 do §17 mostrou não existir quando os motivos
 * eram frases soltas escritas no lugar da chamada. Um motivo que
 * repetisse a frase de outro devolveria o projeto ao ponto de partida,
 * e não haveria nada que reclamasse.
 */
@DisplayName("IdleReason")
class IdleReasonTest {

    @Test
    @DisplayName("todo motivo tem frase, e nenhuma é vazia")
    void everyReasonSpeaks() {
        for (IdleReason reason : IdleReason.values()) {
            assertFalse(
                    reason.message().isBlank(),
                    reason + " has no message, and would log as silence");
        }
    }

    /**
     * A propriedade que dá sentido ao tipo.
     *
     * <p>Se dois motivos dissessem a mesma coisa, quem lê o log não teria
     * como distingui-los — que é exatamente o estado anterior a este
     * tipo, com cinco saídas silenciosas dando a mesma resposta.
     */
    @Test
    @DisplayName("dois motivos nunca dizem a mesma frase")
    void everyReasonSoundsDifferent() {
        Set<String> seen = new HashSet<>();

        for (IdleReason reason : IdleReason.values()) {
            assertTrue(
                    seen.add(reason.message()),
                    reason + " repeats a message that another reason already uses");
        }
    }

    /**
     * A distinção que a segunda metade do E14 custou uma sessão.
     *
     * <p>"Não há alvo" e "não terminei de olhar" são respostas
     * diferentes, e o log afirmava a primeira nos dois casos.
     */
    @Test
    @DisplayName("não achar e não ter terminado de olhar são motivos distintos")
    void anUnfinishedSweepIsNotTheSameAsNothingFound() {
        assertNotEquals(IdleReason.NO_TARGET, IdleReason.SWEEP_INCOMPLETE);

        assertNotEquals(
                IdleReason.NO_TARGET.message(),
                IdleReason.SWEEP_INCOMPLETE.message());
    }

    /**
     * A mesma distinção do lado da tarefa: a colônia não quis, contra a
     * colônia quis e ninguém pôde. Uma manda na meta, a outra na
     * atribuição de profissão.
     */
    @Test
    @DisplayName("sem tarefa e sem executor são motivos distintos")
    void noTaskIsNotTheSameAsNoExecutor() {
        assertNotEquals(IdleReason.NO_TASK, IdleReason.NO_EXECUTOR);

        assertNotEquals(
                IdleReason.NO_TASK.message(),
                IdleReason.NO_EXECUTOR.message());
    }

    @Test
    @DisplayName("o detalhe do momento entra depois da frase")
    void detailIsAppended() {
        assertEquals(
                IdleReason.NO_WORKER.message() + " — no builder in the village",
                IdleReason.NO_WORKER.messageWith("no builder in the village"));
    }

    /**
     * Detalhe vazio é o caso comum — {@code IdleLog.record} sem detalhe
     * passa {@code ""} —, e não pode deixar um travessão pendurado no fim
     * da linha.
     */
    @Test
    @DisplayName("sem detalhe, a frase fica como está")
    void blankDetailChangesNothing() {
        assertEquals(IdleReason.NO_TASK.message(), IdleReason.NO_TASK.messageWith(""));
        assertEquals(IdleReason.NO_TASK.message(), IdleReason.NO_TASK.messageWith("   "));
    }

    @Test
    @DisplayName("detalhe nulo é erro de quem chama, não linha torta no log")
    void nullDetailIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> IdleReason.NO_TASK.messageWith(null));
    }
}
