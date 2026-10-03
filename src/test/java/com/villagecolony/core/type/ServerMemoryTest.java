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

    /**
     * Uma limpeza que carrega outra classe no meio do esquecimento —
     * 2026-09-25, visto em jogo.
     *
     * <p>Fechar o mundo derrubou o {@code resetAll} com
     * {@code ConcurrentModificationException}: um {@code clearAll} tocou uma
     * classe ainda não carregada, o bloco estático dela se inscreveu, e o mapa
     * mudou no meio da volta. O resto da memória ficou sem limpar.
     */
    @Test
    void aResetThatLoadsAnotherClassDoesNotBreakTheSweep() {
        int[] late = {0};
        Runnable lateReset = () -> late[0]++;

        // A classe tardia NÃO está inscrita antes: reinscrever uma chave que
        // já existe não muda a estrutura do mapa, e o teste passaria à toa.
        // E ela não pode ser a última: o iterador já sabe que não há próxima
        // e não confere o mapa de novo. No jogo ela estava no meio da lista.
        ServerMemory.register(EarlyOwner.class,
                () -> ServerMemory.register(LateOwner.class, lateReset));
        ServerMemory.register(AfterOwner.class, () -> { });

        ServerMemory.resetAll();

        assertTrue(ServerMemory.registered().contains(LateOwner.class.getName()));
        assertEquals(1, late[0], "a classe que chegou no meio da volta não foi limpa");
    }

    /** Uma classe que só se inscreve quando outra a toca. */
    private static final class LateOwner {
    }

    private static final class EarlyOwner {
    }

    private static final class AfterOwner {
    }
}
