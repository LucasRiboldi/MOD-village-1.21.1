package com.villagecolony.core.storage.model;

/**
 * Nenhum baú da colônia guarda mais de {@link #MAX_SLOTS_PER_ITEM}
 * compartimentos do mesmo item — ADR-036 item 9.
 *
 * <p>Pilha que já existe continua sendo completada; o teto só impede abrir
 * compartimento novo. Baú nomeado pelo jogador ({@link VillageChestRule}) é
 * dele e não tem teto.
 */
public final class ChestSlotCap {

    public static final int MAX_SLOTS_PER_ITEM = 3;

    private ChestSlotCap() {
    }

    /**
     * Quantos compartimentos vazios este item ainda pode ocupar.
     *
     * @param emptySlots compartimentos vazios no baú
     * @param slotsOfItem compartimentos que já guardam este item
     * @param named se o jogador deu nome ao baú
     */
    public static int slotsOpenFor(int emptySlots, int slotsOfItem, boolean named) {
        if (named) {
            return emptySlots;
        }

        return Math.min(emptySlots, Math.max(0, MAX_SLOTS_PER_ITEM - slotsOfItem));
    }
}
