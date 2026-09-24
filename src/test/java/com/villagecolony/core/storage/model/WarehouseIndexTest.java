package com.villagecolony.core.storage.model;

import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O contrato do armazém físico — decisão 4B, 2026-09-24. Ver
 * {@code docs/decisions/ADR-024-physical-warehouse-index.md}.
 */
class WarehouseIndexTest {

    private static final ResourceId IRON = ResourceId.vanilla("iron_ingot");
    private static final ResourceId WOOD = ResourceId.vanilla("oak_log");

    private static SupplyRequest request(ResourceId resourceId, int amount, SupplyPriority priority) {
        return new SupplyRequest(resourceId, amount, priority);
    }

    @Test
    void reservationPreventsSameCycleDoubleConsumption() {
        WarehouseIndex index = WarehouseIndex.complete(Map.of(IRON, 1));

        assertEquals(SupplyRequestStatus.RESERVED,
                index.reserve(request(IRON, 1, SupplyPriority.ACTIVE_CONSTRUCTION)).status());
        assertEquals(SupplyRequestStatus.BLOCKED,
                index.reserve(request(IRON, 1, SupplyPriority.ACTIVE_CONSTRUCTION)).status());
    }

    @Test
    void reservationBlockedByStockGivesTheInsufficientReason() {
        WarehouseIndex index = WarehouseIndex.complete(Map.of(IRON, 2));

        WarehouseIndex.Reservation blocked =
                index.reserve(request(IRON, 3, SupplyPriority.ACTIVE_CONSTRUCTION));

        assertEquals(SupplyRequestStatus.BLOCKED, blocked.status());
        assertEquals(SupplyBlockReason.INSUFFICIENT_PHYSICAL_STOCK, blocked.reason().orElseThrow());
    }

    @Test
    void anIncompleteSnapshotBlocksEveryReservation() {
        WarehouseIndex index = WarehouseIndex.incomplete(Map.of(IRON, 100));

        WarehouseIndex.Reservation blocked =
                index.reserve(request(IRON, 1, SupplyPriority.ACTIVE_CONSTRUCTION));

        assertEquals(SupplyRequestStatus.BLOCKED, blocked.status());
        assertEquals(SupplyBlockReason.SNAPSHOT_INCOMPLETE, blocked.reason().orElseThrow());
    }

    @Test
    void aResourceNeverSeenIsZeroStockNotAnError() {
        WarehouseIndex index = WarehouseIndex.complete(Map.of(IRON, 5));

        WarehouseIndex.Reservation blocked =
                index.reserve(request(WOOD, 1, SupplyPriority.ACTIVE_CONSTRUCTION));

        assertEquals(SupplyRequestStatus.BLOCKED, blocked.status());
        assertEquals(SupplyBlockReason.INSUFFICIENT_PHYSICAL_STOCK, blocked.reason().orElseThrow());
    }

    @Test
    void invalidationClearsReservationsWithoutChangingStock() {
        WarehouseIndex index = WarehouseIndex.complete(Map.of(IRON, 1));

        index.reserve(request(IRON, 1, SupplyPriority.ACTIVE_CONSTRUCTION));
        index.invalidate();

        assertEquals(SupplyRequestStatus.RESERVED,
                index.reserve(request(IRON, 1, SupplyPriority.ACTIVE_CONSTRUCTION)).status());
    }

    /**
     * A prioridade é responsabilidade de quem monta a fila do ciclo, e
     * não do índice: {@code reserve} é <em>greedy</em> por chamada — a
     * primeira requisição que chega reserva primeiro, sempre. Ver
     * {@code reserveBatch}: é ele quem ordena por
     * {@link SupplyPriority} antes de reservar, e é essa ordenação que
     * garante que "prioridade mais baixa não consome estoque escasso
     * primeiro" — a régua do plano.
     */
    @Test
    void reserveBatchOrdersByPriorityBeforeReserving() {
        WarehouseIndex index = WarehouseIndex.complete(Map.of(IRON, 1));

        // A prioridade baixa entra primeiro na LISTA, mas reserveBatch
        // ordena antes de reservar — a alta deve vencer o estoque único.
        SupplyRequest low = request(IRON, 1, SupplyPriority.STOCK_OBJECTIVE);
        SupplyRequest high = request(IRON, 1, SupplyPriority.ACTIVE_CONSTRUCTION);

        Map<SupplyRequest, WarehouseIndex.Reservation> results =
                index.reserveBatch(List.of(low, high));

        assertEquals(SupplyRequestStatus.RESERVED, results.get(high).status());
        assertEquals(SupplyRequestStatus.BLOCKED, results.get(low).status());
    }

    @Test
    void reserveIsGreedyByCallOrderRegardlessOfPriority() {
        WarehouseIndex index = WarehouseIndex.complete(Map.of(IRON, 1));

        SupplyRequest low = request(IRON, 1, SupplyPriority.STOCK_OBJECTIVE);
        SupplyRequest high = request(IRON, 1, SupplyPriority.ACTIVE_CONSTRUCTION);

        // reserve() sozinho não reordena: quem chama primeiro reserva
        // primeiro, mesmo com prioridade menor. É reserveBatch quem
        // aplica a ordem — a diferença é intencional e testada acima.
        assertEquals(SupplyRequestStatus.RESERVED, index.reserve(low).status());
        assertEquals(SupplyRequestStatus.BLOCKED, index.reserve(high).status());
    }

    @Test
    void priorityOrderIsExplicit() {
        assertTrue(SupplyPriority.ACTIVE_CONSTRUCTION.ordinal()
                < SupplyPriority.ACTIVE_PROFESSION_WORK.ordinal());
        assertTrue(SupplyPriority.ACTIVE_PROFESSION_WORK.ordinal()
                < SupplyPriority.CAPACITY_RELIEF.ordinal());
        assertTrue(SupplyPriority.CAPACITY_RELIEF.ordinal()
                < SupplyPriority.STOCK_OBJECTIVE.ordinal());
    }

    @Test
    void zeroOrNegativeAmountIsRejected() {
        WarehouseIndex index = WarehouseIndex.complete(Map.of(IRON, 5));

        try {
            index.reserve(request(IRON, 0, SupplyPriority.ACTIVE_CONSTRUCTION));
            throw new AssertionError("deveria ter recusado quantidade zero");
        } catch (IllegalArgumentException expected) {
            // esperado
        }
    }
}
