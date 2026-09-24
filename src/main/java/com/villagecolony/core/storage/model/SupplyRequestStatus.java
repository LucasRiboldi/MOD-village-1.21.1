package com.villagecolony.core.storage.model;

/**
 * O desfecho de um pedido de suprimento — decisão 4B, 2026-09-24.
 *
 * <p>Ver {@code docs/decisions/ADR-024-physical-warehouse-index.md}.
 */
public enum SupplyRequestStatus {
    /** Ainda não passou por {@code reserve}. */
    OPEN,

    /** O estoque físico foi comprometido para este pedido, nesta fotografia. */
    RESERVED,

    /** O material físico chegou às mãos de quem pediu. */
    FULFILLED,

    /** Não foi possível reservar; ver {@link SupplyBlockReason}. */
    BLOCKED
}
