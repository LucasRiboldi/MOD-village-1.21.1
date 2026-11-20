package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.BlockReserve;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

/** A reserva de um item fabricado nos baús, pela classe dele — ADR-062, Regra 340 ({@link BlockReserve}). */
public final class StockReserve {

    private StockReserve() {
    }

    /** Quanto deste fabricado se guarda: 4, 12 ou 1; 0 para a bancada do mod, que as profissões não fazem. */
    public static int of(Item item) {
        return BlockReserve.reserveOf(Registries.ITEM.getId(item).toString(), true);
    }
}
