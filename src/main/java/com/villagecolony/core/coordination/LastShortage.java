package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * O que faltava a cada colônia no último ciclo — F-2, 2026-10-02.
 *
 * <p>O ciclo da colônia (a cada 30 s) mede a falta e abre os pedidos. Quem
 * concluía um pedido no meio do intervalo ficava parado até o ciclo seguinte
 * abrir o próximo, mesmo com a falta igual. Guardar a falta medida deixa quem
 * conclui abrir o pedido seguinte na hora; o ciclo seguinte continua sendo
 * quem cancela o que perdeu o motivo ({@code ColonyCycle.cancelSatisfied}).
 */
public final class LastShortage {

    static {
        ServerMemory.register(LastShortage.class, LastShortage::clearAll);
    }

    private static final Map<UUID, Set<ResourceType>> MISSING = new ConcurrentHashMap<>();

    private LastShortage() {
    }

    /** A falta que o ciclo acabou de medir. Público para o teste da corrente. */
    public static void measured(UUID colonyId, Set<ResourceType> missing) {
        MISSING.put(colonyId, Set.copyOf(missing));
    }

    /** Se este recurso faltava no último ciclo da colônia. */
    public static boolean stillMissing(UUID colonyId, ResourceType resource) {
        Set<ResourceType> missing = MISSING.get(colonyId);

        return missing != null && missing.contains(resource);
    }

    public static void clearAll() {
        MISSING.clear();
    }
}
