package com.villagecolony.core.storage.model;

import com.villagecolony.core.type.ResourceId;

import java.util.Objects;

/**
 * Um pedido de material físico — decisão 4B, 2026-09-24.
 *
 * <p>Só o id do recurso, a quantidade e a prioridade: nenhum
 * {@code Item}, nenhum baú, nenhuma coordenada. Quem resolve a posição
 * física é {@code fabric.integration.WarehouseObserver}, que nunca entra
 * em {@code core}.
 */
public record SupplyRequest(ResourceId resourceId, int amount, SupplyPriority priority) {

    public SupplyRequest {
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(priority, "priority");

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + amount);
        }
    }
}
