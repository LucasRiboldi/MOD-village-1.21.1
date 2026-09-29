package com.villagecolony.fabric.work;

import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link MineCuts#sideOf} trocou {@code Side.values()} por uma tabela
 * escrita à mão em 2026-09-29. A tabela antiga fica copiada aqui, de
 * propósito: uma colônia que já existe tem de continuar abrindo a mina
 * para o mesmo lado, mesmo que alguém reordene {@link Side}.
 */
class MineCutsSideTest {

    private static final Side[] BEFORE = {Side.NORTH, Side.SOUTH, Side.EAST, Side.WEST};

    @Test
    void sideOfMatchesTheMappingSavedColoniesWereBuiltWith() {
        Random random = new Random(20260929L);

        for (int i = 0; i < 2_000; i++) {
            UUID colony = new UUID(random.nextLong(), random.nextLong());

            assertEquals(
                    BEFORE[Math.floorMod(colony.hashCode(), BEFORE.length)],
                    MineCuts.sideOf(colony),
                    colony.toString());
        }
    }
}
