package com.villagecolony.fabric.integration;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regra 384: cada bloco mudado vira uma linha do diário, com o tique, quem, onde, antes e depois. */
class BlockJournalTest {

    @Test
    @DisplayName("Regra 384: a linha do diário diz quem mudou o bloco, onde e para quê")
    void aLineSaysWhoWhereAndWhat() {
        assertEquals("{\"t\":120,\"who\":\"mod:nivelamento\",\"x\":5,\"y\":64,\"z\":-7,\"was\":\"minecraft:dirt\","
                        + "\"now\":\"minecraft:air\"}",
                BlockJournal.line(120, "mod:nivelamento", new BlockPos(5, 64, -7), "minecraft:dirt", "minecraft:air"));
        assertEquals("{\"t\":1,\"who\":\"player\",\"x\":0,\"y\":0,\"z\":0,\"now\":\"minecraft:stone\"}",
                BlockJournal.line(1, "player", BlockPos.ORIGIN, "", "minecraft:stone"));
    }
}
