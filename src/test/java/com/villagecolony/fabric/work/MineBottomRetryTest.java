package com.villagecolony.fabric.work;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O fundo sem nova boca nao pode reexecutar a busca a cada tique.
 *
 * <p>O save real de 27-09 escreveu 23.524 avisos iguais em vinte minutos:
 * a mina ja sabia que todos os ramais tinham acabado, mas tentava a mesma
 * boca oposta para cada mineiro em cada tique. A espera deve deixar o mundo
 * respirar e ainda permitir nova tentativa quando o jogador tiver liberado
 * terreno no lado oposto.
 */
class MineBottomRetryTest {

    private final UUID colonyId = UUID.randomUUID();

    @AfterEach
    void clearRetryState() {
        MineBottomRetry.clearAll();
    }

    @Test
    void aMissingOppositeMouthWaitsOneColonyCycleBeforeTryingAgain() {
        long firstAttempt = 10_000L;

        assertTrue(
                MineBottomRetry.isDue(colonyId, firstAttempt),
                "a mina esgotada precisa poder tentar a recuperacao pela primeira vez");

        MineBottomRetry.defer(colonyId, firstAttempt);

        assertFalse(
                MineBottomRetry.isDue(colonyId, firstAttempt + MineBottomRetry.RETRY_TICKS - 1),
                "a mesma boca nao pode ser procurada em cada tique durante a espera");
        assertTrue(
                MineBottomRetry.isDue(colonyId, firstAttempt + MineBottomRetry.RETRY_TICKS),
                "depois de um ciclo a mina precisa poder notar que o jogador abriu uma boca valida");
    }
}
