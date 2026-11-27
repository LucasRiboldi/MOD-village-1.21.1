package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Quem mexeu em cada coluna desde que o servidor abriu — ADR-064, para a foto do terreno. O {@link EditLedger} conta: o
 * jogador, um sistema do mod e o mundo (queda, crescimento). Só registra; nada no jogo lê isto.
 */
public final class TerrainEdits {

    public static final int PLAYER = 1;

    public static final int MOD = 2;

    public static final int WORLD = 4;

    private static final Map<Long, Integer> COLUMNS = new HashMap<>();

    static {
        ServerMemory.register(TerrainEdits.class, COLUMNS::clear);
    }

    private TerrainEdits() {
    }

    /** Alguém ({@link #PLAYER}, {@link #MOD} ou {@link #WORLD}) mexeu num bloco desta coluna. */
    public static void record(BlockPos pos, int who) {
        COLUMNS.merge(BlockPos.asLong(pos.getX(), 0, pos.getZ()), who, (before, now) -> before | now);
    }

    /** Quem mexeu nesta coluna, somado em bits; 0 se ninguém. */
    public static int of(int x, int z) {
        return COLUMNS.getOrDefault(BlockPos.asLong(x, 0, z), 0);
    }
}
