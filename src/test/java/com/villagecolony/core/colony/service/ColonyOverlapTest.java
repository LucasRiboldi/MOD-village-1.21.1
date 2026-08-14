package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Duas colônias perto demais uma da outra.
 *
 * <p>ADR-003 §5 manda registrar o aviso e não fundir. Fundir exige nova
 * ADR — e o critério dela já está decidido desde 2026-08-12: um bloco de
 * uma encostando no da outra, o que depende da construção existir.
 */
class ColonyOverlapTest {

    private ColonyService colonies;

    @BeforeEach
    void setUp() {
        colonies = new ColonyService();
    }

    private static ColonyPos at(int x) {
        return new ColonyPos(x, 64, 0);
    }

    @Test
    void aLonelyColonyOverlapsNobody() {
        Colony colony = colonies.createColony(at(0));

        assertTrue(colonies.overlapping(colony).isEmpty());
    }

    @Test
    void aColonyDoesNotOverlapItself() {
        Colony colony = colonies.createColony(at(0));

        assertTrue(colonies.overlapping(colony).isEmpty());
    }

    @Test
    void centersCloserThanTheLimitOverlap() {
        Colony one = colonies.createColony(at(0));
        Colony other = colonies.createColony(at(VillageDetector.OVERLAP_DISTANCE - 1));

        assertEquals(List.of(other), colonies.overlapping(one));
        assertEquals(List.of(one), colonies.overlapping(other));
    }

    /** No limite exato ainda é sobreposição: o aviso erra para o lado de avisar. */
    @Test
    void theLimitItselfCounts() {
        Colony one = colonies.createColony(at(0));
        Colony other = colonies.createColony(at(VillageDetector.OVERLAP_DISTANCE));

        assertEquals(List.of(other), colonies.overlapping(one));
    }

    /**
     * Vizinha conhecida e distante não é sobreposição.
     *
     * <p>Entre {@code OVERLAP_DISTANCE} e {@code DUPLICATE_DISTANCE} está
     * a faixa em que duas colônias se enxergam sem se sobrepor — e é
     * exatamente ali que mora o risco de disputarem trabalhador.
     */
    @Test
    void aColonyBeyondTheLimitIsJustANeighbour() {
        Colony one = colonies.createColony(at(0));
        colonies.createColony(at(VillageDetector.OVERLAP_DISTANCE + 1));

        assertTrue(colonies.overlapping(one).isEmpty());
    }

    /** A altura não separa duas vilas, do mesmo jeito que não separa camas. */
    @Test
    void heightDoesNotSeparateColonies() {
        Colony one = colonies.createColony(new ColonyPos(0, 64, 0));
        Colony other = colonies.createColony(new ColonyPos(5, 200, 0));

        assertEquals(List.of(other), colonies.overlapping(one));
    }
}
