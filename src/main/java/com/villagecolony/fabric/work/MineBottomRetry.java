package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ritmo da recuperacao de uma mina que chegou ao fundo sem nova boca.
 *
 * <p>A geometria da mina permanece em {@code Mine}; isto so impede que o
 * adaptador Fabric procure a mesma boca oposta em todos os tiques. O estado
 * e deliberadamente da sessao: recarregar o mundo permite uma tentativa nova,
 * e uma mudanca do jogador pode ser percebida no proximo ciclo sem gravar um
 * relogio derivado do mundo.
 */
final class MineBottomRetry {

    /** Um ciclo da colonia: tempo suficiente para o mundo ou jogador mudar. */
    static final long RETRY_TICKS = 600L;

    static {
        ServerMemory.register(MineBottomRetry.class, MineBottomRetry::clearAll);
    }

    private static final Map<UUID, Long> nextAttempt = new HashMap<>();

    private MineBottomRetry() {
    }

    static boolean isDue(UUID colonyId, long now) {
        return now >= nextAttempt.getOrDefault(colonyId, Long.MIN_VALUE);
    }

    static void defer(UUID colonyId, long now) {
        nextAttempt.put(colonyId, now + RETRY_TICKS);
    }

    static void clear(UUID colonyId) {
        nextAttempt.remove(colonyId);
    }

    static void clearAll() {
        nextAttempt.clear();
    }
}
