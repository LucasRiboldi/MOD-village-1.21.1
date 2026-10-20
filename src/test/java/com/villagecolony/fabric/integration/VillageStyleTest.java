package com.villagecolony.fabric.integration;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O estilo da vila sem vila gerada: o bioma atendido mais comum entre as camas. */
class VillageStyleTest {

    private static final Set<String> SUPPORTED = Set.of("snowy_plains", "plains", "taiga");

    private static Optional<String> vote(String... beds) {
        return VillageStyle.majority(List.of(beds), SUPPORTED::contains, Comparator.naturalOrder());
    }

    @Test
    void theMostCommonSupportedBiomeWins() {
        assertEquals(Optional.of("snowy_plains"), vote("snowy_plains", "forest", "snowy_plains", "plains"));
    }

    @Test
    void unsupportedBiomesDoNotVoteEvenWhenTheyAreTheMostCommon() {
        // A vila de neve da bancada: o centro num pedaço de floresta.
        assertEquals(Optional.of("snowy_plains"), vote("forest", "forest", "forest", "snowy_plains"));
    }

    @Test
    void aTieGoesToTheFirstInTheGivenOrderWhateverTheReadingOrder() {
        assertEquals(Optional.of("plains"), vote("taiga", "plains"));
        assertEquals(Optional.of("plains"), vote("plains", "taiga"));
    }

    @Test
    void noSupportedBiomeGivesNothingAndTheCallerFallsBackToPlains() {
        assertEquals(Optional.empty(), vote("forest", "river"));
    }
}
