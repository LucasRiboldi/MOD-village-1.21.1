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
    // Quem tem autoridade para dizer que a vila encolheu é a sonda: a
    // varredura ancorada no centro da colônia, repetida a cada ciclo.
    // Duas leituras seguidas dela são comparáveis entre si, porque
    // partem do mesmo ponto. Ver §15 de Project-State.md.
    // ----------------------------------------------------------------

    private static VillageCandidate fullySeen(int x, int z, int beds) {
        return new VillageCandidate(new ColonyPos(x, 64, z), beds, true);
    }

    /** Leitura da sonda: a varredura ancorada no centro da colônia. */
    private static VillageCandidate probe(ColonyPos anchor, int x, int z, int beds) {
        return new VillageCandidate(new ColonyPos(x, 64, z), beds, false, anchor);
    }

    /**
     * O caso que falhou em jogo em 2026-08-07, duas vezes.
     *
     * <p>Vila de 38 camas, cinco destruídas. A sonda lê 33 e não pode
     * encolher sozinha — 33 tanto pode ser a vila menor quanto uma visão
     * parcial. Quando a leitura seguinte, do mesmo ponto, repete 33, ela
     * deixa de ser acidente de posição: a vila tem 33.
     */
    @Test
    void aRepeatedProbeReadingShrinksTheColony() {
        ColonyPos anchor = new ColonyPos(1109, 64, 730);

        service.adopt(probe(anchor, 1109, 730, 38));
        service.adopt(probe(anchor, 1109, 730, 33));

        Colony colony = service.adopt(probe(anchor, 1109, 730, 33));

        assertEquals(33, colony.observedBeds(), "a sonda confirmou: a vila encolheu");
    }

    /** Uma leitura só nunca basta: pode ser a posição, não a vila. */
    @Test
    void aSingleProbeReadingProvesNothing() {
        ColonyPos anchor = new ColonyPos(1109, 64, 730);

        service.adopt(probe(anchor, 1109, 730, 38));

        Colony colony = service.adopt(probe(anchor, 1109, 730, 33));

        assertEquals(38, colony.observedBeds());
    }

    /**
     * A colônia que veio do save também encolhe.
     *
     * <p>Foi exatamente isto que travou em jogo: a âncora nascia só numa
     * observação aceita, e nenhuma vinha enquanto a colônia estivesse
     * grande demais. A sonda registra a âncora mesmo quando é recusada.
     */
    @Test
    void aColonyLoadedFromSaveCanStillShrink() {
        ColonyPos anchor = new ColonyPos(1109, 64, 730);

        Colony colony = service.adopt(seen(1109, 730, 38));

        service.adopt(probe(anchor, 1109, 730, 33));
        service.adopt(probe(anchor, 1109, 730, 33));

        assertEquals(33, colony.observedBeds());
    }

    /**
     * A varredura do jogador não é sonda, e repetir não lhe dá
     * autoridade.
     *
     * <p>É o que impede a deriva do §11 de voltar: jogador parado na
     * borda repete a mesma visão pobre ciclo após ciclo.
     */
    @Test
    void repeatingAPlayerViewNeverShrinksTheColony() {
        service.adopt(seen(1109, 730, 38));

        for (int i = 0; i < 5; i++) {
            service.adopt(seen(1080, 733, 3));
        }

        Colony colony = service.all().iterator().next();

        assertEquals(38, colony.observedBeds());
        assertEquals(new ColonyPos(1109, 64, 730), colony.center());
    }

    /** Sonda de outro ponto não confirma leitura de ponto nenhum. */
    @Test
    void probesFromDifferentAnchorsDoNotConfirmEachOther() {
        ColonyPos here = new ColonyPos(1109, 64, 730);
        ColonyPos there = new ColonyPos(1080, 64, 733);

        service.adopt(probe(here, 1109, 730, 38));
        service.adopt(probe(here, 1109, 730, 33));

        Colony colony = service.adopt(probe(there, 1080, 733, 3));

        assertEquals(38, colony.observedBeds());
    }

    /**
     * A prova geométrica continua valendo por si.
     *
     * <p>Sozinha ela não bastou — vila real é maior que a margem, e foi
     * isso que travou em jogo. Mas ela não depende de repetição, e é a
     * única que encolhe já na primeira observação.
     */
    @Test
    void aCompleteViewShrinksWithoutAnyProbe() {
        service.adopt(seen(0, 0, 10));

        Colony colony = service.adopt(fullySeen(50, 50, 4));

        assertEquals(4, colony.observedBeds());
    }

    /** Crescer continua sem precisar de sonda nenhuma. */
    @Test
    void growingStillNeedsNoProbe() {
        service.adopt(seen(0, 0, 5));

        Colony colony = service.adopt(seen(20, 0, 9));

        assertEquals(9, colony.observedBeds());
    }

    /** A vila volta a crescer depois de encolher. */
    @Test
    void theColonyGrowsAgainAfterShrinking() {
        ColonyPos anchor = new ColonyPos(0, 64, 0);

        service.adopt(probe(anchor, 0, 0, 10));
        service.adopt(probe(anchor, 0, 0, 4));
        service.adopt(probe(anchor, 0, 0, 4));

        Colony colony = service.adopt(seen(0, 0, 12));

        assertEquals(12, colony.observedBeds());
    }
}
