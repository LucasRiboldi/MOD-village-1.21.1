package com.villagecolony.fabric.brain;

import com.villagecolony.core.type.ServerMemory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aldeões cujo andar está, por ora, nas mãos de outro — E52, 2026-10-01.
 *
 * <p>Quem sai de um curral anda até o portão ou até a cerca que vai pular,
 * e o destino do ofício dele, do outro lado da cerca, não pode disputar o
 * {@code WALK_TARGET} a cada tique: cada troca é um A* novo, e o aldeão
 * ficaria parado recalculando. Enquanto ele estiver aqui, a
 * {@link GoToWorkTargetTask} não corre.
 *
 * <p>Mora no pacote do {@code Brain}, e não no do trabalho, porque é o
 * {@code Brain} que lê; o trabalho só escreve.
 */
public final class WalkOverride {

    static {
        ServerMemory.register(WalkOverride.class, WalkOverride::clearAll);
    }

    private static final Set<UUID> HELD = ConcurrentHashMap.newKeySet();

    private WalkOverride() {
    }

    /** O andar deste aldeão passa a ser de quem chamou. */
    public static void hold(UUID villagerId) {
        HELD.add(villagerId);
    }

    /** Devolve o andar ao destino do ofício. */
    public static void release(UUID villagerId) {
        HELD.remove(villagerId);
    }

    /** Se o andar dele está segurado. */
    public static boolean isHeld(UUID villagerId) {
        return HELD.contains(villagerId);
    }

    /** Esquece tudo. Chamado ao abrir e ao parar o servidor. */
    public static void clearAll() {
        HELD.clear();
    }
}
