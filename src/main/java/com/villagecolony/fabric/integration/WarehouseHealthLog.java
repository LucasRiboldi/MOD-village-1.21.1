package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Registra a transição de saúde da leitura física do armazém.
 *
 * <p>Um baú em chunk descarregado não pode calar a colônia inteira, mas precisa
 * aparecer no log. O conjunto vive somente neste servidor e evita uma linha a
 * cada ciclo enquanto a mesma indisponibilidade persiste.
 */
public final class WarehouseHealthLog {

    static {
        ServerMemory.register(WarehouseHealthLog.class, WarehouseHealthLog::clearAll);
    }

    private static final Set<UUID> DEGRADED = new HashSet<>();

    private WarehouseHealthLog() {
    }

    /** Atualiza a saúde da fotografia sem alterar estoque ou carregamento de chunk. */
    public static void observe(UUID colonyId, ChestInventoryReader.ChestSurvey survey) {
        if (survey.isDegraded()) {
            if (DEGRADED.add(colonyId)) {
                VillageColonyMod.LOGGER.warn(
                        "Colony {} warehouse is degraded: {}. Continuing with observed physical stock only",
                        colonyId,
                        survey.coverage());
            }
            return;
        }

        if (DEGRADED.remove(colonyId)) {
            VillageColonyMod.LOGGER.info(
                    "Colony {} warehouse recovered: every registered chest is readable again",
                    colonyId);
        }
    }

    static boolean isDegraded(UUID colonyId) {
        return DEGRADED.contains(colonyId);
    }

    /** Esquece a sessão ao fechar o servidor. */
    public static void clearAll() {
        DEGRADED.clear();
    }
}
