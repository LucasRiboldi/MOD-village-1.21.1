package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O rastro de volta pela mina — 2026-10-02 (retorno e F-3). */
class MineReturnTest {

    private static final UUID MINER = UUID.randomUUID();

    @BeforeEach
    void forget() {
        MineReturn.clearAll();
    }

    @Test
    void theTrailStartsAgainAtTheSurfaceAndDropsLoops() {
        MineReturn.walked(MINER, new BlockPos(0, 64, 0), true);
        MineReturn.walked(MINER, new BlockPos(1, 63, 0), false);
        MineReturn.walked(MINER, new BlockPos(2, 62, 0), false);
        MineReturn.walked(MINER, new BlockPos(1, 63, 0), false);

        long[] cells = MineReturn.snapshot().get(MINER);

        assertEquals(2, cells.length, "voltar a um lugar pisado tira o laço do rastro");
        assertEquals(new BlockPos(0, 64, 0), BlockPos.fromLong(cells[0]), "o rastro começa na boca da mina");

        MineReturn.walked(MINER, new BlockPos(5, 64, 5), true);

        assertTrue(MineReturn.snapshot().isEmpty(), "na superfície o rastro recomeça: uma pisada só não vai ao save");
    }

    @Test
    void theTrailComesBackFromTheSaveInTheSameOrder() {
        MineReturn.walked(MINER, new BlockPos(0, 64, 0), true);
        MineReturn.walked(MINER, new BlockPos(1, 63, 0), false);
        MineReturn.walked(MINER, new BlockPos(2, 62, 0), false);

        Map<UUID, long[]> saved = MineReturn.snapshot();

        MineReturn.clearAll();
        MineReturn.restore(saved);

        long[] back = MineReturn.snapshot().get(MINER);

        assertEquals(3, back.length);
        assertEquals(new BlockPos(2, 62, 0), BlockPos.fromLong(back[2]), "a ordem da descida se perdeu");
    }
}
