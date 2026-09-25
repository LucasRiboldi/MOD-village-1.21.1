package com.villagecolony.core.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O próprio {@link ServerMemory}, sem o jogo carregado.
 *
 * <p>Separado do {@code ServerMemoryRegistrationTest} em 2026-09-25: aquele
 * sobe o Minecraft para percorrer as classes do mod, e o PIT roda sem o
 * {@code fabric-loader-junit}. Juntos, a falha do bootstrap derrubava também
 * este teste, e o PIT inteiro parava antes da primeira mutação.
 */
class ServerMemoryTest {

    @Test
    void resetAllRunsEveryRegisteredReset() {
        int[] calls = {0};
        ServerMemory.register(ServerMemoryTest.class, () -> calls[0]++);

        int total = ServerMemory.resetAll();

        assertEquals(1, calls[0]);
        assertTrue(total >= 1);
    }
}
