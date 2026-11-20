package com.villagecolony.fabric.work;

/**
 * A parada da oficina que é barreira, e não pedido cumprido — Regra 339: a tora que acabou (a tora guardada saiu com a
 * Regra 340). O mesmo
 * pedido pararia no mesmo lugar, então ele não se reabre na hora ({@link TaskChain}) e conta para o guarda de laço
 * ({@link TaskOutcome}), mesmo com uma peça feita.
 */
final class CraftBarrier {

    static final String NO_LOGS = "no logs left in the colony chests";

    private CraftBarrier() {
    }

    /** Se a oficina parou por uma barreira. */
    static boolean stopped(String why) {
        return NO_LOGS.equals(why);
    }
}
