package com.villagecolony.core.worker.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O raio de busca da profissão: o do jogador quando ele configurou, senão o
 * automático da profissão — ADR-030, decisão movida para o core pela ADR-035 §4.
 */
class ProfessionPolicyTest {

    @Test
    void anAutomaticRadiusFallsBackToTheProfessionDefault() {
        assertEquals(32, ProfessionPolicy.defaults().searchRadiusOr(32));
    }

    @Test
    void aConfiguredRadiusWinsOverTheProfessionDefault() {
        ProfessionPolicy configured = new ProfessionPolicy(true, ProfessionPolicy.UNLIMITED, 48);

        assertEquals(48, configured.searchRadiusOr(32));
    }
}
