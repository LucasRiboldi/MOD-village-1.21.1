package com.villagecolony.fabric.integration;

/** Origem auditavel de uma peca pedida pela construcao. */
public enum SupplySource {
    BIOME_RESOURCE,
    AUTOMATIC_DROP,
    CRAFTING,
    SMELTING,
    NO_ROUTE,
    NO_COLONY,
    CYCLE,
    DEPTH_LIMIT
}
