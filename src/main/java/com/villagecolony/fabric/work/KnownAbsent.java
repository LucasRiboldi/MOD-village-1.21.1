package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O recurso que a vila já provou não ter no alcance — regra do autor, 2026-10-20.
 *
 * <p>Quando as buscas vazias seguidas fazem o material aparecer no baú ({@link LocateFallback}), a
 * ausência fica registrada aqui por {@link #FOR_TICKS}. Enquanto vale, quem precisar dele recebe no baú
 * da profissão direto, sem sair para buscar ({@link SmelterGathers}). Antes, a entrega zerava a
 * contagem de buscas vazias e cada novo lote custava mais quatro viagens inúteis: no playtest da
 * 0.3.64, 82 buscas vazias de argila para 20 entregas.
 *
 * <p>Vencida a janela, a vila volta a procurar uma vez — o mundo pode ter mudado (o jogador trouxe
 * argila, a vila cresceu até um rio). Achar o recurso apaga o registro na hora ({@link #found}).
 */
final class KnownAbsent {

    static {
        ServerMemory.register(KnownAbsent.class, KnownAbsent::clearAll);
    }

    /** Quanto a ausência registrada vale: um dia do jogo (20 min). */
    static final long FOR_TICKS = 24_000;

    private record Key(UUID colonyId, ResourceType resource) {
    }

    private static final Map<Key, Long> SINCE = new HashMap<>();

    private KnownAbsent() {
    }

    /** As buscas provaram que {@code resource} não existe no alcance desta colônia. */
    static void register(UUID colonyId, ResourceType resource, long now) {
        SINCE.put(new Key(colonyId, resource), now);
    }

    /** Se ainda vale o registro de que {@code resource} não existe no alcance desta colônia. */
    static boolean isAbsent(UUID colonyId, ResourceType resource, long now) {
        Long since = SINCE.get(new Key(colonyId, resource));

        return since != null && now - since < FOR_TICKS;
    }

    /** Alguém achou o recurso: deixa de ser ausente. */
    static void found(UUID colonyId, ResourceType resource) {
        SINCE.remove(new Key(colonyId, resource));
    }

    static void clearAll() {
        SINCE.clear();
    }
}
