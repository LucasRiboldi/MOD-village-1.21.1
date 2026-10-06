package com.villagecolony.core.storage.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Teto de compartimentos do mesmo item num baú da colônia (ADR-036 9). */
class ChestSlotCapTest {

    @Test
    void anEmptyChestOpensAtMostThreeSlotsForOneItem() {
        assertEquals(3, ChestSlotCap.slotsOpenFor(27, 0, false));
    }

    @Test
    void slotsAlreadyHoldingTheItemCountAgainstTheCap() {
        assertEquals(1, ChestSlotCap.slotsOpenFor(20, 2, false));
        assertEquals(0, ChestSlotCap.slotsOpenFor(20, 3, false));
    }

    @Test
    void aChestOverTheCapOpensNothingAndNeverANegative() {
        assertEquals(0, ChestSlotCap.slotsOpenFor(20, 7, false));
    }

    @Test
    void fewerEmptySlotsThanTheCapAreAllThereIs() {
        assertEquals(2, ChestSlotCap.slotsOpenFor(2, 0, false));
    }

    @Test
    void aNamedChestHasNoCap() {
        assertEquals(20, ChestSlotCap.slotsOpenFor(20, 5, true));
    }
}
