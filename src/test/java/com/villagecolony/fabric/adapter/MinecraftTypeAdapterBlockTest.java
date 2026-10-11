package com.villagecolony.fabric.adapter;

import com.villagecolony.core.type.ResourceId;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O nome de bloco vindo do save nunca derruba quem o lê — AUD-02. */
class MinecraftTypeAdapterBlockTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void aKnownBlockIsFound() {
        assertEquals(Optional.of(Blocks.STONE), MinecraftTypeAdapter.toBlock(ResourceId.vanilla("stone")));
    }

    @Test
    void anUnknownBlockIsEmpty() {
        assertEquals(Optional.empty(), MinecraftTypeAdapter.toBlock(ResourceId.vanilla("no_such_block")));
    }

    /** Maiúscula e espaço não são aceitos pelo jogo: vazio, como bloco desconhecido. */
    @Test
    void aNameTheGameRejectsIsEmptyInsteadOfThrowing() {
        assertEquals(Optional.empty(), MinecraftTypeAdapter.toBlock(new ResourceId("minecraft", "Stone Bricks")));
        assertEquals(Optional.empty(), MinecraftTypeAdapter.toBlock(new ResourceId("Village Colony", "stone")));
    }
}
