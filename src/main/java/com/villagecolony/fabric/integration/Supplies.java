package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.type.ServerMemory;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O registro do suprimento excepcional — Regra 111. Toda vez que algo surge pela lista declarada
 * ({@link SupplyException}) fica uma linha {@code VC_SUPPLY} no log, e a contagem da sessão por colônia e
 * exceção; {@code scripts/supply_report.py} a resume.
 */
public final class Supplies {

    static {
        ServerMemory.register(Supplies.class, Supplies::clearAll);
    }

    /** A colônia sem id (a receita da bancada não sabe de quem é o baú): conta à parte. */
    private static final UUID UNKNOWN = new UUID(0L, 0L);

    private static final Map<UUID, Map<SupplyException, Integer>> SESSION = new HashMap<>();

    private Supplies() {
    }

    /**
     * Uma vez em que algo surgiu.
     *
     * @param what o item ou bloco que surgiu
     * @param count quantos
     */
    public static void record(SupplyException exception, @Nullable UUID colonyId, String what, int count) {
        if (count <= 0) {
            return;
        }

        UUID colony = colonyId == null ? UNKNOWN : colonyId;

        SESSION.computeIfAbsent(colony, id -> new EnumMap<>(SupplyException.class)).merge(exception, count, Integer::sum);
        VillageColonyMod.LOGGER.info("VC_SUPPLY version=1 exception={} rule={} colony={} item={} count={}",
                exception, exception.rule(), colony.toString().substring(0, 8), what, count);
    }

    /** Quanto surgiu nesta colônia, nesta sessão, por esta exceção. */
    public static int sessionCount(UUID colonyId, SupplyException exception) {
        return SESSION.getOrDefault(colonyId, Map.of()).getOrDefault(exception, 0);
    }

    public static void clearAll() {
        SESSION.clear();
    }
}
