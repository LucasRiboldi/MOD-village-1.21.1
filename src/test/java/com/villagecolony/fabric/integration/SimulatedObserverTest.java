package com.villagecolony.fabric.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import net.minecraft.util.math.ChunkPos;
import org.junit.jupiter.api.Test;

class SimulatedObserverTest {

    @Test
    void oneChunkPutsTheObserverInItsMiddle() {
        assertArrayEquals(new long[] {8, 8}, SimulatedObserver.centerOf(new long[] {ChunkPos.toLong(0, 0)}));
    }

    @Test
    void aSquareOfChunksPutsTheObserverInTheMiddleOfTheSquare() {
        long[] square = new long[9];
        int i = 0;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                square[i++] = ChunkPos.toLong(100 + x, -40 + z);
            }
        }

        assertArrayEquals(new long[] {100 * 16 + 8, -40 * 16 + 8}, SimulatedObserver.centerOf(square));
    }

    @Test
    void theKeyIsOffWithoutTheProperty() {
        // A bateria não liga a chave: o observador nunca existe fora da bancada de simulação.
        assertFalse(SimulatedObserver.enabled());
    }
}
