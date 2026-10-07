package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * O adiantamento — peça que nenhuma obra pediu — ADR-038 P5b: no máximo
 * {@link #BATCH} de uma peça por vez; feitas, troca para a seguinte, num ciclo
 * sem fim de um pouco de cada. Peça que não avança em {@link #PATIENCE} ciclos
 * (falta de material) cede a vez, para o ciclo não parar nela.
 */
public final class AdvanceStock {

    static {
        ServerMemory.register(AdvanceStock.class, AdvanceStock::clearAll);
    }

    /** Quantas de uma peça se adiantam de cada vez. */
    public static final int BATCH = 5;

    /** Ciclos sem progresso antes de a peça ceder a vez. */
    public static final int PATIENCE = 10;

    private static final class Cursor {
        int index;
        int target;
        int stalled;
    }

    private static final Map<UUID, Cursor> CURSORS = new HashMap<>();

    private AdvanceStock() {
    }

    public static void clearAll() {
        CURSORS.clear();
    }

    /**
     * Põe nas metas a peça da vez desta colônia.
     *
     * @param pieces as peças que a vila adianta, em ordem
     * @param stock quanto a colônia tem de cada uma
     * @return a peça da vez, ou nenhuma quando a lista é vazia
     */
    public static java.util.Optional<ResourceType> addTo(
            Map<ResourceType, Integer> goals, UUID colonyId, List<ResourceType> pieces,
            ToIntFunction<ResourceType> stock) {
        if (pieces.isEmpty()) {
            return java.util.Optional.empty();
        }

        Cursor cursor = CURSORS.get(colonyId);

        if (cursor == null) {
            cursor = new Cursor();
            cursor.index = 0;
            cursor.target = stock.applyAsInt(pieces.get(0)) + BATCH;
            CURSORS.put(colonyId, cursor);
        }

        cursor.index = Math.floorMod(cursor.index, pieces.size());
        ResourceType piece = pieces.get(cursor.index);
        int have = stock.applyAsInt(piece);

        if (have >= cursor.target || ++cursor.stalled > PATIENCE) {
            cursor.index = Math.floorMod(cursor.index + 1, pieces.size());
            piece = pieces.get(cursor.index);
            have = stock.applyAsInt(piece);
            cursor.target = have + BATCH;
            cursor.stalled = 0;
        }

        goals.merge(piece, cursor.target, Math::max);

        return java.util.Optional.of(piece);
    }
}
