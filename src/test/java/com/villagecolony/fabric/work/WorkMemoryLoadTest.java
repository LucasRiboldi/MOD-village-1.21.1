package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * A memória de trabalho malformada não derruba o início do servidor — AUD-02.
 *
 * <p>{@link WorkMemory#load} roda no {@code SERVER_STARTED}: uma exceção ali
 * não perde só a memória, impede o mundo de abrir. Os leitores são defensivos
 * ({@code containsUuid} antes de {@code getUuid}, UUID inválido descartado);
 * este teste segura isso.
 */
class WorkMemoryLoadTest {

    @AfterEach
    void forget() {
        ServerMemory.resetAll();
    }

    @Test
    void garbageInEverySectionLoadsWithoutThrowing() {
        String colony = UUID.randomUUID().toString();
        NbtCompound memory = new NbtCompound();

        for (String section : new String[] {"trees", "surface", "veins", "arms", "herds", "farms"}) {
            NbtCompound bad = new NbtCompound();
            bad.putString(colony, "not a compound");
            bad.putInt("not-a-uuid", 7);
            NbtCompound wrongTypes = new NbtCompound();
            wrongTypes.putString("animal", "not a uuid");
            wrongTypes.putString("fence", "not a long");
            wrongTypes.putInt("phase", 3);
            wrongTypes.putString("0", "not a uuid");
            bad.put(UUID.randomUUID().toString(), wrongTypes);
            memory.put(section, bad);
        }

        NbtList advance = new NbtList();
        NbtCompound turn = new NbtCompound();
        turn.putString("colony", "not-a-uuid");
        turn.putString("lane", "STONE");
        advance.add(turn);
        memory.put("advance", advance);

        assertDoesNotThrow(() -> WorkMemory.load(memory));
    }

    @Test
    void anEmptyMemoryLoads() {
        assertDoesNotThrow(() -> WorkMemory.load(new NbtCompound()));
    }
}
