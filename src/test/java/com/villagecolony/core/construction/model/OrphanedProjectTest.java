package com.villagecolony.core.construction.model;

import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A obra que o centro da vila deixou para trás — 2026-09-15.
 *
 * <p><b>O que o autor viu em jogo, às 21:02:</b> nenhuma construção
 * nascendo. O log mostrou uma obra aberta às 20:54:35 —
 * {@code plains_butcher_shop_2} em {@code x=638, y=65, z=-2793} — parada
 * em <i>"382 blocks left"</i> por sete minutos e meio, <b>sem um único
 * bloco assentado</b>, e segurando a vaga única da colônia:
 * <i>"no building work: one is already open"</i>.
 *
 * <p><b>A causa está na aritmética do próprio log.</b> O centro da vila
 * não é estável — a mesma sessão registrou oito centros diferentes, de
 * {@code 616,-2863} a {@code 640,-2891} —, e a obra foi planejada num
 * instante em que o centro era {@code 625,-2854}, a 62,4 blocos dela:
 * dentro do raio de {@value VillageDetector#SEARCH_RADIUS}. O centro que
 * prevaleceu foi {@code 637,-2871}, que a deixa a <b>78 blocos</b> — fora
 * do raio, e portanto fora do alcance de qualquer trabalhador.
 *
 * <p><b>Por que ninguém reclamava.</b> O relógio de paciência só conta
 * para obra em {@code WAITING_RESOURCES}; esta estava em
 * {@code BUILDING}, e o {@code BuilderWork.step} sai em silêncio quando
 * o aldeão não está em chunk carregado. Obra viva, inalcançável, calada,
 * e ocupando a única vaga — os quatro ao mesmo tempo.
 */
class OrphanedProjectTest {

    private static final int RADIUS = VillageDetector.SEARCH_RADIUS;

    /** O caso do log, com os números que ele registrou. */
    @Test
    void theButcherShopOfTheLogIsOutOfReachOfTheCentreThatPrevailed() {
        ColonyPos work = new ColonyPos(638, 65, -2793);
        ColonyPos centre = new ColonyPos(637, 72, -2871);

        assertTrue(
                ConstructionReach.isOutOfReach(work, centre, RADIUS),
                "a obra a 78 blocos do centro passou por alcançável, e é a que travou a vila");
    }

    /**
     * E ela era alcançável do centro em que foi planejada.
     *
     * <p>É o que prova que o defeito é a <b>deriva</b>, e não uma escolha
     * errada de lote: no instante do planejamento a conta fechava.
     */
    @Test
    void theSameWorkWasWithinReachOfTheCentreThatPlannedIt() {
        ColonyPos work = new ColonyPos(638, 65, -2793);
        ColonyPos whenPlanned = new ColonyPos(625, 72, -2854);

        assertFalse(
                ConstructionReach.isOutOfReach(work, whenPlanned, RADIUS),
                "a obra estava a 62 blocos quando nasceu — recusá-la ali seria recusar"
                        + " um lote bom");
    }

    /** A altura não entra na conta: o raio da vila é horizontal. */
    @Test
    void heightDoesNotCountTowardsTheRadius() {
        ColonyPos deep = new ColonyPos(10, -40, 10);
        ColonyPos centre = new ColonyPos(10, 70, 10);

        assertFalse(
                ConstructionReach.isOutOfReach(deep, centre, RADIUS),
                "a mina fundo da vila passou por fora do raio — a conta pegou o eixo Y");
    }

    /** Na borda exata ainda vale: o raio é inclusivo, como a detecção. */
    @Test
    void theEdgeOfTheRadiusStillCounts() {
        ColonyPos edge = new ColonyPos(RADIUS, 70, 0);
        ColonyPos centre = new ColonyPos(0, 70, 0);

        assertFalse(
                ConstructionReach.isOutOfReach(edge, centre, RADIUS),
                "a obra na borda exata do raio foi abandonada");

        assertTrue(
                ConstructionReach.isOutOfReach(
                        new ColonyPos(RADIUS + 1, 70, 0), centre, RADIUS),
                "um bloco além da borda continuou passando por alcançável");
    }
}
