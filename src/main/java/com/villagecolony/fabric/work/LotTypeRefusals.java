package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O tipo de obra que não achou lote sai da vez — playtest 0.3.41: desde a 0.3.36 a vila
 * só tentava a oficina que faltava, nenhum lote cabia, e em cinco sessões nenhuma obra
 * nasceu (antes, a casa entrava no rodízio e a vila crescia assim mesmo).
 *
 * <p>Uma volta inteira da varredura sem lote para o tipo escolhido o tira do rodízio por
 * {@link #REFUSE_FOR} tiques; a vila tenta o tipo seguinte. Com todos os que faltam
 * recusados, a casa entra no lugar ({@link HousePlans#plansForNext}).
 */
final class LotTypeRefusals {

    static {
        ServerMemory.register(LotTypeRefusals.class, LotTypeRefusals::clearAll);
    }

    /** Quanto tempo o tipo sem lote fica fora da vez: meia hora. */
    static final long REFUSE_FOR = 36_000;

    private static final Map<UUID, Map<String, Long>> REFUSED = new HashMap<>();

    private LotTypeRefusals() {
    }

    /** Uma volta inteira sem lote para este tipo. */
    static void refuse(UUID colonyId, String type, long now) {
        REFUSED.computeIfAbsent(colonyId, id -> new HashMap<>()).put(type, now);
        VillageColonyMod.LOGGER.info("Colony {} found no lot for a {} in a whole sweep — it tries another kind of"
                + " building for {} minutes", colonyId.toString().substring(0, 8), type, REFUSE_FOR / 1_200);
    }

    /** Se este tipo está fora da vez. */
    static boolean isRefused(UUID colonyId, String type, long now) {
        Long since = REFUSED.getOrDefault(colonyId, Map.of()).get(type);

        return since != null && now - since < REFUSE_FOR;
    }

    /** Se algum tipo desta colônia está fora da vez agora. */
    static boolean anyRefused(UUID colonyId, long now) {
        return REFUSED.getOrDefault(colonyId, Map.of()).values().stream().anyMatch(since -> now - since < REFUSE_FOR);
    }

    static void clearAll() {
        REFUSED.clear();
    }
}
