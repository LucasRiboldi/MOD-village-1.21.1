package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * O chão que o jogador mexeu fica como ele deixou — Regra 334. A coluna em que o jogador quebrou chão natural (terra,
 * grama, pedra, areia e afins) ou pôs chão natural fica marcada para sempre. O Zelador e todo nivelamento da vila (a
 * fila do terreno, o vão entre construções, o solo sob a peça da vila, a volta das casas e o apoio da calçada) não
 * cortam nem aterram essa coluna, e a calçada passa por ela no nível em que está. A marca vai ao save
 * ({@code WorkMarksSavedData}), como a da planta que o jogador tirou (Regra 104, {@link PlayerClearedPlants}).
 */
public final class PlayerGround {

    static {
        ServerMemory.register(PlayerGround.class, PlayerGround::clearAll);
    }

    /** Teto do conjunto, para o servidor que vive meses; a marca mais velha sai primeiro. */
    static final int MAX_COLUMNS = 16_384;

    private static final LinkedHashSet<Long> COLUMNS = new LinkedHashSet<>();

    private PlayerGround() {
    }

    /** O jogador quebrou {@code before} em {@code pos}. */
    public static void broke(BlockPos pos, BlockState before) {
        if (SiteSurroundings.isNaturalGround(before)) {
            add(column(pos.getX(), pos.getZ()));
        }
    }

    /** O bloco em {@code pos} mudou de {@code before} para {@code after} pela mão do jogador. */
    public static void placed(BlockPos pos, BlockState before, BlockState after) {
        if (SiteSurroundings.isNaturalGround(after) && !SiteSurroundings.isNaturalGround(before)) {
            add(column(pos.getX(), pos.getZ()));
        }
    }

    /** Se o jogador mexeu no chão desta coluna. */
    public static boolean isMarked(int x, int z) {
        return COLUMNS.contains(column(x, z));
    }

    /** As colunas marcadas, para o save. */
    public static List<Long> snapshot() {
        return List.copyOf(COLUMNS);
    }

    /** As colunas do save. */
    public static void restore(Collection<Long> columns) {
        COLUMNS.clear();
        columns.forEach(PlayerGround::add);
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
