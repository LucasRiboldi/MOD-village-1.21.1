package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Árvore ou planta que o jogador tirou não volta naquele lugar — regra do autor,
 * 2026-10-11. A coluna em que o jogador quebrou tronco, muda ou flor fica
 * marcada, e o plantio da colônia ({@code TreeNursery}) a recusa. A marca vai ao
 * save ({@code WorkMarksSavedData}). O replantio depois do corte do lenhador não
 * passa por aqui: quem cortou foi a colônia.
 */
public final class PlayerClearedPlants {

    static {
        ServerMemory.register(PlayerClearedPlants.class, PlayerClearedPlants::clearAll);
    }

    /** Teto do conjunto, para o servidor que vive meses; a marca mais velha sai primeiro. */
    static final int MAX_COLUMNS = 16_384;

    private static final LinkedHashSet<Long> COLUMNS = new LinkedHashSet<>();

    private PlayerClearedPlants() {
    }

    /** O jogador quebrou {@code before} em {@code pos}. */
    public static void observe(BlockPos pos, BlockState before) {
        if (before.isIn(BlockTags.LOGS) || before.isIn(BlockTags.SAPLINGS) || before.isIn(BlockTags.FLOWERS)) {
            add(column(pos.getX(), pos.getZ()));
        }
    }

    /** Se o jogador já tirou planta desta coluna. */
    public static boolean isCleared(BlockPos pos) {
        return COLUMNS.contains(column(pos.getX(), pos.getZ()));
    }

    /** As colunas marcadas, para o save. */
    public static List<Long> snapshot() {
        return List.copyOf(COLUMNS);
    }

    /** As colunas do save. */
    public static void restore(Collection<Long> columns) {
        COLUMNS.clear();
        columns.forEach(PlayerClearedPlants::add);
    }

    public static void clearAll() {
        COLUMNS.clear();
    }

    private static void add(long column) {
        COLUMNS.remove(column);
        COLUMNS.add(column);

        if (COLUMNS.size() > MAX_COLUMNS) {
            Iterator<Long> oldest = COLUMNS.iterator();
            oldest.next();
            oldest.remove();
        }
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
