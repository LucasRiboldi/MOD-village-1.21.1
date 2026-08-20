package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.service.VillageDetector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A paciência da obra com o material que falta.
 *
 * <p>Fora do jogo de propósito: o defeito que ela corrige é de escala de
 * minutos — dez, no valor de hoje —, e uma bateria que roda em vinte e
 * cinco segundos nunca poderia afirmá-lo por dentro. É a mesma razão do
 * {@link WorkClock}.
 */
class PatienceClockTest {

    @Test
    void aFreshWaitHasNotRunOut() {
        assertFalse(PatienceClock.ranOut(1_000, 1_000));
    }

    @Test
    void oneCycleShortIsStillWaiting() {
        long since = 1_000;

        assertFalse(PatienceClock.ranOut(
                since, since + PatienceClock.TICKS - VillageDetector.CYCLE_TICKS));
    }

    @Test
    void theExactMomentCounts() {
        long since = 1_000;

        assertTrue(PatienceClock.ranOut(since, since + PatienceClock.TICKS));
    }

    /**
     * A espera precisa ser maior que uma varredura de lote inteira.
     *
     * <p>É o apoio do número, e por isso está preso em teste: desistir
     * custa dezessete ciclos de varredura, e uma paciência menor faria a
     * colônia largar uma obra antes de ter terminado de procurar onde
     * pôr a próxima.
     */
    @Test
    void thePatienceOutlastsAFullLotSweep() {
        int sweep = 17;

        assertTrue(PatienceClock.CYCLES > sweep);
    }

    /** O relógio do mundo só cresce, mas o mod nunca depende disso. */
    @Test
    void aClockThatWentBackwardsDoesNotGiveUp() {
        assertFalse(PatienceClock.ranOut(9_000, 1_000));
    }
}
