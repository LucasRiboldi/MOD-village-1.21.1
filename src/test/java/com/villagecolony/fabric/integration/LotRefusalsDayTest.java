package com.villagecolony.fabric.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 375 (notas do jogo da 0.4.145, J2 🅲: "por que este espaço todo livre da vila não teve construção?"): uma vez
 * por dia, o log diz por que os lotes foram recusados, com a conta e até {@value LotRefusals#SAMPLES} lugares de exemplo
 * de cada motivo.
 */
class LotRefusalsDayTest {

    @Test
    @DisplayName("Regra 375: a linha do dia diz o motivo, a conta e onde, e recomeça")
    void theDailyLineSaysWhyAndWhere() {
        UUID colony = UUID.randomUUID();

        for (int i = 0; i < 12; i++) {
            LotRefusals.refusedAt(colony, LotRefusals.Reason.TOO_CLOSE, 16 * i, 40);
        }
        LotRefusals.refusedAt(colony, LotRefusals.Reason.NO_GROUND, 5, -7);

        String line = LotRefusals.dayLine(colony);

        assertTrue(line.contains("TOO_CLOSE=12@0,40;16,40;32,40;48,40;64,40 "), line);
        assertTrue(line.contains("NO_GROUND=1@5,-7"), line);
        assertEquals("", LotRefusals.dayLine(colony), "a linha do dia não recomeçou");
    }
}
