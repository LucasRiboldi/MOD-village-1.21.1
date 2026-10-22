package com.villagecolony.core.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O custo de cavar do mineiro depende da direção — Regra 71: descendo o de sempre, de lado o dobro, subindo o triplo. */
class DetourCostsTest {

    @Test
    void theMinerDigsDownCheapestAndUpDearest() {
        DetourMoves.Costs miner = DetourMoves.Costs.MINER;

        assertEquals(miner.digDown(), miner.dig(-1));
        assertEquals(miner.digLevel(), miner.dig(0));
        assertEquals(miner.digUp(), miner.dig(1));
        assertEquals(2 * miner.digDown(), miner.digLevel());
        assertEquals(3 * miner.digDown(), miner.digUp());
    }
}
