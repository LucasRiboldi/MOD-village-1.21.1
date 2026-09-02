package com.villagecolony.fabric.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quando a ausência de um trabalhador vira notícia — 2026-09-02.
 *
 * <p>A regra existe porque <b>ausência não é morte</b>: um aldeão pode
 * estar fora da área carregada num tique e voltar no seguinte, mesmo numa
 * colônia ativa. Noticiar a primeira falta encheria o log de falso
 * positivo, e é o oposto do que esta instrumentação existe para fazer —
 * ela existe para dizer se o trabalhador fantasma acontece <b>de
 * verdade</b>.
 */
class PhantomWorkerLogTest {

    private static final UUID SOMEONE = UUID.randomUUID();

    @BeforeEach
    void clean() {
        PhantomWorkerLog.clearAll();
    }

    /** Faltar uma vez não é notícia: ele pode só ter saído do alcance. */
    @Test
    void oneMissIsNotNews() {
        assertFalse(PhantomWorkerLog.observe(SOMEONE, false));
    }

    /** Some, some, some — aí sim. */
    @Test
    void theNewsComesOutAtTheThirdMissInARow() {
        assertFalse(PhantomWorkerLog.observe(SOMEONE, false));
        assertFalse(PhantomWorkerLog.observe(SOMEONE, false));

        assertTrue(
                PhantomWorkerLog.observe(SOMEONE, false),
                "três faltas seguidas continuaram em silêncio");
    }

    /**
     * E ela sai <b>uma vez</b>.
     *
     * <p>A condição é permanente: um fantasma some para sempre. Repetir a
     * linha a cada ciclo afogaria o log da sessão, que é justamente o
     * instrumento que precisa ser legível.
     */
    @Test
    void theNewsDoesNotRepeatEveryCycle() {
        PhantomWorkerLog.observe(SOMEONE, false);
        PhantomWorkerLog.observe(SOMEONE, false);
        PhantomWorkerLog.observe(SOMEONE, false);

        assertFalse(
                PhantomWorkerLog.observe(SOMEONE, false),
                "a linha saiu de novo no ciclo seguinte");
    }

    /**
     * Quem volta zera a conta — e volta a poder ser notícia depois.
     *
     * <p>Zerar só a contagem e não o aviso deixaria o aldeão que sumiu,
     * voltou e sumiu de novo em silêncio para sempre: a segunda vez é uma
     * notícia diferente da primeira.
     */
    @Test
    void comingBackResetsTheCount() {
        PhantomWorkerLog.observe(SOMEONE, false);
        PhantomWorkerLog.observe(SOMEONE, false);
        PhantomWorkerLog.observe(SOMEONE, false);

        PhantomWorkerLog.observe(SOMEONE, true);

        assertFalse(PhantomWorkerLog.observe(SOMEONE, false));
        assertFalse(PhantomWorkerLog.observe(SOMEONE, false));

        assertTrue(
                PhantomWorkerLog.observe(SOMEONE, false),
                "depois de voltar e sumir de novo, ele nunca mais vira notícia");
    }

    /** Duas ausências de aldeões diferentes não se somam. */
    @Test
    void theCountIsPerVillager() {
        UUID other = UUID.randomUUID();

        PhantomWorkerLog.observe(SOMEONE, false);
        PhantomWorkerLog.observe(other, false);
        PhantomWorkerLog.observe(SOMEONE, false);

        assertFalse(
                PhantomWorkerLog.observe(other, false),
                "a falta de um contou para o outro");
    }
}
