package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Limite diário da frente autônoma da mina.
 *
 * <p>Sem demanda de material da obra, a mina só repõe uma pilha por dia. O
 * maior estoque visto no dia conta o que já chegou ao baú, portanto retirar
 * pedra depois não abre uma segunda pilha. A frente e a proteção física continuam em
 * {@link MineDigging}; esta classe somente reduz a meta de estoque.
 */
public final class MineSupply {

    public static final int DAILY_STONE_LIMIT = 64;

    private static final Map<UUID, DailyStock> DAILY = new HashMap<>();

    static {
        ServerMemory.register(MineSupply.class, MineSupply::clearAll);
    }

    private MineSupply() {
    }

    /** Espaço que a meta autônoma ainda pode pedir hoje. */
    public static int room(UUID colonyId, long timeOfDay, int stored, int physicalRoom) {
        if (stored < 0 || physicalRoom < 0) {
            throw new IllegalArgumentException("Stored and room must not be negative");
        }

        long day = Math.floorDiv(timeOfDay, 24_000L);
        DailyStock stock = DAILY.get(colonyId);

        if (stock == null || stock.day() != day) {
            stock = new DailyStock(day, stored, stored);
            DAILY.put(colonyId, stock);
        }

        if (stored > stock.highestStored()) {
            stock = new DailyStock(day, stock.openingStored(), stored);
            DAILY.put(colonyId, stock);
        }

        int deposited = stock.highestStored() - stock.openingStored();

        return Math.min(physicalRoom, Math.max(0, DAILY_STONE_LIMIT - deposited));
    }

    static void clearAll() {
        DAILY.clear();
    }

    private record DailyStock(long day, int openingStored, int highestStored) {
    }
}
