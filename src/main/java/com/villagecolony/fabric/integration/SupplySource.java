package com.villagecolony.fabric.integration;

/** Origem auditavel de uma peca pedida pela construcao. */
public enum SupplySource {
    BIOME_RESOURCE,
    AUTOMATIC_DROP,
    CRAFTING,
    SMELTING,
    /** O machado no tronco: descascar não é receita do jogo. */
    STRIPPING,
    NO_ROUTE,
    NO_COLONY,
    CYCLE,
    DEPTH_LIMIT
}
