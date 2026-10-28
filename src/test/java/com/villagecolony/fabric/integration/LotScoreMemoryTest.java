package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A nota do último lote (Regra 139, {@code SweepState.LOT_SCORE}) é memória do mundo aberto: descarregar o mundo
 * a esquece, como o resto do estado da varredura — PROJECT_AUDIT.md, R3/T1.
 */
class LotScoreMemoryTest {

    @AfterEach
    void forget() {
        ServerMemory.resetAll();
    }

    @Test
    void theWorldUnloadForgetsTheLotScore() throws ClassNotFoundException {
        // Quem limpa o estado da varredura é o BuildSiteScanner, que se registra no ServerMemory ao carregar.
        Class.forName(BuildSiteScanner.class.getName(), true, getClass().getClassLoader());
        UUID colony = UUID.randomUUID();

        SweepState.LOT_SCORE.put(colony, 42.0);
        ServerMemory.resetAll();

        assertTrue(SweepState.lastLotScore(colony).isEmpty(),
                "a nota do lote sobreviveu ao mundo descarregado: " + SweepState.lastLotScore(colony));
    }
}
