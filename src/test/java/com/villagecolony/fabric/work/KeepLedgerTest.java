package com.villagecolony.fabric.work;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regra 343: cada trava pequena de cru conta, por dia, quantas vezes foi consultada, quantas barrou e o menor estoque. */
class KeepLedgerTest {

    @BeforeEach
    @AfterEach
    void forget() {
        KeepLedger.clear();
    }

    @Test
    void eachLockCountsChecksBarsAndTheLowestStock() {
        assertTrue(KeepLedger.below("upkeep", 20, 36), "abaixo do limite, barra");
        assertFalse(KeepLedger.below("upkeep", 40, 36));
        assertTrue(KeepLedger.atMost("wheat", 8, 8), "no limite, o trigo fica");
        assertTrue(KeepLedger.atLeast("ingot", 64, 64), "no teto, o lingote para");

        assertEquals("upkeep=36,2,1,20", KeepLedger.entry("upkeep", KeepLedger.today().get("upkeep")));
        assertEquals("wheat=8,1,1,8", KeepLedger.entry("wheat", KeepLedger.today().get("wheat")));
    }

    @Test
    void theDailyLineListsEveryLockSeen() {
        KeepLedger.below("wool", 30, 16);
        KeepLedger.atMost("seed", 3, 8);

        assertEquals("VC_KEEP version=1 day=4 locks=2 seed=8,1,1,3 wool=16,1,0,30", KeepLedger.line(4));
    }
}
