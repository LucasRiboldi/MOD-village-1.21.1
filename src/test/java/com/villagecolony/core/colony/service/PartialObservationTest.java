package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nenhuma detecção enxerga a vila inteira: o raio é 64 e a vila é maior.
 *
 * <p>Estes testes travam a regra que impede uma observação parcial de
 * arrastar o centro. O oscilar foi observado em jogo antes da regra
 * existir, alternando entre uma visão de 12 camas e outra de 3.
 */
class PartialObservationTest {

    private ColonyService service;

    @BeforeEach
    void setUp() {
        service = new ColonyService();
    }

    private static VillageCandidate seen(int x, int z, int beds) {
        return new VillageCandidate(new ColonyPos(x, 64, z), beds);
    }

    @Test
    void partialViewDoesNotMoveTheCenter() {
        service.adopt(seen(1109, 730, 12));

        Colony colony = service.adopt(seen(1080, 733, 3));

        assertEquals(new ColonyPos(1109, 64, 730), colony.center());
        assertEquals(12, colony.observedBeds());
    }

    /** A sequência exata observada no log de 2026-08-06. */
    @Test
    void centerStopsOscillating() {
        service.adopt(seen(1109, 730, 12));

        for (int i = 0; i < 5; i++) {
            service.adopt(seen(1080, 733, 3));
            service.adopt(seen(1109, 730, 12));
        }

        Colony colony = service.all().iterator().next();

        assertEquals(1, service.count());
        assertEquals(new ColonyPos(1109, 64, 730), colony.center());
    }

    @Test
    void betterViewMovesTheCenter() {
        service.adopt(seen(1080, 733, 3));

        Colony colony = service.adopt(seen(1109, 730, 12));

        assertEquals(new ColonyPos(1109, 64, 730), colony.center());
        assertEquals(12, colony.observedBeds());
    }

    /** A vila pode se mover mantendo o mesmo número de camas. */
    @Test
    void equallyCompleteViewMovesTheCenter() {
        service.adopt(seen(0, 0, 5));

        Colony colony = service.adopt(seen(20, 0, 5));

        assertEquals(new ColonyPos(20, 64, 0), colony.center());
    }

    @Test
    void firstObservationAlwaysCounts() {
        Colony colony = service.adopt(seen(0, 0, 3));

        assertEquals(new ColonyPos(0, 64, 0), colony.center());
        assertEquals(3, colony.observedBeds());
    }

    @Test
    void observeReportsWhetherItMoved() {
        Colony colony = service.adopt(seen(0, 0, 5));

        assertFalse(colony.observe(new ColonyPos(0, 64, 0), 5), "mesmo centro");
        assertTrue(colony.observe(new ColonyPos(9, 64, 0), 5), "centro novo");
        assertFalse(colony.observe(new ColonyPos(50, 64, 0), 2), "visão pior");
    }

    /**
     * O pior desfecho da deriva, observado em jogo em 2026-08-06.
     *
     * <p>Sem a regra de completude, a colônia perseguia visões parciais
     * até se afastar mais que {@link VillageDetector#DUPLICATE_DISTANCE}
     * da vila real. A detecção seguinte não achava colônia por perto e
     * criava outra — a vila trocava de UUID, quebrando a promessa da
     * ADR-003 §4.
     *
     * <p>O que se garante aqui é a identidade da vila, não a contagem de
     * colônias: o cluster de 1126,663 está a 69 blocos, além do limite de
     * duplicata, e virar colônia própria é a decisão correta da §6.
     */
    @Test
    void driftMustNotCostTheVillageItsIdentity() {
        Colony original = service.adopt(seen(1109, 730, 12));

        service.adopt(seen(1116, 669, 5));
        service.adopt(seen(1126, 663, 3));

        Colony afterwards = service.adopt(seen(1109, 730, 12));

        assertEquals(original.id(), afterwards.id(), "o UUID da vila deve permanecer");
        assertEquals(new ColonyPos(1109, 64, 730), afterwards.center(), "o centro não deriva");
    }

    /** Observação a 61 blocos é a mesma vila e não pode mover o centro. */
    @Test
    void nearbyPartialViewStaysInTheSameColony() {
        Colony original = service.adopt(seen(1109, 730, 12));

        Colony same = service.adopt(seen(1116, 669, 5));

        assertEquals(1, service.count());
        assertEquals(original.id(), same.id());
        assertEquals(new ColonyPos(1109, 64, 730), same.center());
    }

    @Test
    void worseViewKeepsTheStoredBedCount() {
        Colony colony = service.adopt(seen(0, 0, 10));

        colony.observe(new ColonyPos(5, 64, 5), 2);

        assertEquals(10, colony.observedBeds());
    }

    // ----------------------------------------------------------------
    // A colônia pode encolher — decisão do autor em 2026-08-07.
    //
    // Até aqui observedBeds só crescia, e uma vila que perdesse camas
    // ficava com o centro congelado para sempre: nenhuma observação
    // futura alcançaria a marca antiga.
    //
    // Quem tem autoridade para dizer que a vila encolheu é a observação
    // completa — aquela que provadamente não pode ter cortado cama
    // nenhuma. Ver VillageDetectorTest#completeness.
    // ----------------------------------------------------------------

    private static VillageCandidate fullySeen(int x, int z, int beds) {
        return new VillageCandidate(new ColonyPos(x, 64, z), beds, true);
    }

    /** Observação com âncora: o ponto de onde a varredura partiu. */
    private static VillageCandidate seenFrom(ColonyPos anchor, int x, int z, int beds) {
        return new VillageCandidate(new ColonyPos(x, 64, z), beds, false, anchor);
    }

    /**
     * O caso que falhou em jogo em 2026-08-07.
     *
     * <p>Vila de 38 camas, cinco camas destruídas, cinco observações
     * seguidas de 32 e 33 camas — todas recusadas pela prova geométrica,
     * porque a vila é maior que a margem de 32 blocos. A colônia ficou
     * presa em 38.
     *
     * <p>Com a âncora, a segunda observação vinda do mesmo ponto tem
     * autoridade: a mesma janela não encolhe sozinha.
     */
    @Test
    void sameAnchorSeeingFewerBedsShrinksTheColony() {
        ColonyPos anchor = new ColonyPos(1109, 64, 730);

        service.adopt(seenFrom(anchor, 1109, 730, 38));

        Colony colony = service.adopt(seenFrom(anchor, 1109, 730, 33));

        assertEquals(33, colony.observedBeds(), "a mesma janela viu menos: a vila encolheu");
    }

    /** Âncora diferente não prova nada — é a visão de borda de sempre. */
    @Test
    void anotherAnchorSeeingFewerBedsProvesNothing() {
        ColonyPos anchor = new ColonyPos(1109, 64, 730);
        ColonyPos elsewhere = new ColonyPos(1080, 64, 733);

        service.adopt(seenFrom(anchor, 1109, 730, 38));

        Colony colony = service.adopt(seenFrom(elsewhere, 1080, 733, 3));

        assertEquals(38, colony.observedBeds());
        assertEquals(new ColonyPos(1109, 64, 730), colony.center());
    }

    /**
     * A âncora acompanha a melhor observação, não a última.
     *
     * <p>Se a âncora fosse sobrescrita por qualquer observação, uma visão
     * de borda viraria a nova referência e a próxima visão de borda dali
     * encolheria a colônia — a deriva do §11 por outro caminho.
     */
    @Test
    void aWorseViewDoesNotBecomeTheNewAnchor() {
        ColonyPos good = new ColonyPos(1109, 64, 730);
        ColonyPos edge = new ColonyPos(1080, 64, 733);

        service.adopt(seenFrom(good, 1109, 730, 38));
        service.adopt(seenFrom(edge, 1080, 733, 3));

        Colony colony = service.adopt(seenFrom(edge, 1080, 733, 2));

        assertEquals(38, colony.observedBeds(), "a borda nunca virou referência");
    }

    /** Crescer continua sem precisar de âncora nenhuma. */
    @Test
    void growingStillNeedsNoAnchor() {
        ColonyPos anchor = new ColonyPos(0, 64, 0);

        service.adopt(seenFrom(anchor, 0, 0, 5));

        Colony colony = service.adopt(seen(20, 0, 9));

        assertEquals(9, colony.observedBeds());
    }

    /** Uma observação melhor de outro ponto muda a âncora junto. */
    @Test
    void aBetterViewMovesTheAnchorToo() {
        ColonyPos first = new ColonyPos(0, 64, 0);
        ColonyPos better = new ColonyPos(30, 64, 0);

        service.adopt(seenFrom(first, 0, 0, 5));
        service.adopt(seenFrom(better, 30, 0, 12));

        Colony colony = service.adopt(seenFrom(better, 30, 0, 8));

        assertEquals(8, colony.observedBeds(), "a nova âncora já tem autoridade");
    }

    /** Sem âncora, nada encolhe — é o padrão seguro de quem não sabe de onde olhou. */
    @Test
    void anObservationWithoutAnAnchorNeverShrinks() {
        ColonyPos anchor = new ColonyPos(0, 64, 0);

        service.adopt(seenFrom(anchor, 0, 0, 10));

        Colony colony = service.adopt(seen(0, 0, 4));

        assertEquals(10, colony.observedBeds());
    }

    /**
     * A prova geométrica continua valendo por si.
     *
     * <p>Ela é rara em vila grande, mas é a única que funciona na
     * primeira observação, quando ainda não há âncora com que comparar.
     */
    @Test
    void completeViewStillShrinksWithoutMatchingAnchor() {
        service.adopt(seenFrom(new ColonyPos(0, 64, 0), 0, 0, 10));

        Colony colony = service.adopt(fullySeen(50, 50, 4));

        assertEquals(4, colony.observedBeds());
    }

    @Test
    void completeViewMayShrinkTheColony() {
        service.adopt(fullySeen(1109, 730, 12));

        Colony colony = service.adopt(fullySeen(1109, 730, 4));

        assertEquals(4, colony.observedBeds(), "a vila encolheu e a colônia acompanha");
    }

    @Test
    void completeViewMayShrinkAndMoveTheCenter() {
        service.adopt(fullySeen(1109, 730, 12));

        Colony colony = service.adopt(fullySeen(1080, 733, 4));

        assertEquals(new ColonyPos(1080, 64, 733), colony.center());
        assertEquals(4, colony.observedBeds());
    }

    /**
     * A regra antiga continua valendo para quem não viu tudo.
     *
     * <p>É o que impede a decisão de hoje de reabrir a oscilação: o
     * jogador andando pela vila produz visões parciais o tempo todo, e
     * nenhuma delas encolhe coisa alguma.
     */
    @Test
    void partialViewStillCannotShrinkTheColony() {
        service.adopt(fullySeen(1109, 730, 12));

        Colony colony = service.adopt(seen(1080, 733, 4));

        assertEquals(new ColonyPos(1109, 64, 730), colony.center());
        assertEquals(12, colony.observedBeds());
    }

    /** A oscilação original, agora com uma visão completa no meio dela. */
    @Test
    void completeViewDoesNotReopenTheOscillation() {
        service.adopt(fullySeen(1109, 730, 12));

        for (int i = 0; i < 5; i++) {
            service.adopt(seen(1080, 733, 3));
            service.adopt(seen(1109, 730, 12));
        }

        Colony colony = service.all().iterator().next();

        assertEquals(new ColonyPos(1109, 64, 730), colony.center());
        assertEquals(12, colony.observedBeds());
    }

    /** Crescer nunca precisou de autoridade, e continua não precisando. */
    @Test
    void partialViewMayStillGrowTheColony() {
        service.adopt(fullySeen(0, 0, 5));

        Colony colony = service.adopt(seen(20, 0, 9));

        assertEquals(new ColonyPos(20, 64, 0), colony.center());
        assertEquals(9, colony.observedBeds());
    }

    @Test
    void observeReportsMovementWhenShrinking() {
        Colony colony = service.adopt(fullySeen(0, 0, 10));

        assertFalse(colony.observe(new ColonyPos(0, 64, 0), 3, true), "mesmo centro");
        assertTrue(colony.observe(new ColonyPos(9, 64, 0), 2, true), "centro novo");
        assertFalse(colony.observe(new ColonyPos(50, 64, 0), 1, false), "visão parcial");
    }
}
