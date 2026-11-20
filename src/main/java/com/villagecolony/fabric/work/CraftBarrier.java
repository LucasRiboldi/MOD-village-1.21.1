package com.villagecolony.fabric.work;

import java.util.Set;

/**
 * As paradas da oficina que são barreira, e não pedido cumprido — Regra 339: a tora guardada e a tora que acabou. O mesmo
 * pedido pararia no mesmo lugar, então ele não se reabre na hora ({@link TaskChain}) e conta para o guarda de laço
 * ({@link TaskOutcome}), mesmo com uma peça feita.
 */
final class CraftBarrier {

    static final String LOGS_KEPT = "half of the colony's wood stays in logs";

    static final String NO_LOGS = "no logs left in the colony chests";

    private static final Set<String> BARRIERS = Set.of(LOGS_KEPT, NO_LOGS);

    private CraftBarrier() {
    }

    /** Se a oficina parou por uma barreira. */
    static boolean stopped(String why) {
        return BARRIERS.contains(why);
    }
}
