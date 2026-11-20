package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.BlockReserve.StockClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Regra 342: o registro de blocos conta, por dia, o que a obra pôs, o que esperou e o que surgiu. */
class BlockLedgerTest {

    private static final UUID COLONY = UUID.randomUUID();

    private static final UUID BUILDER = UUID.randomUUID();

    @BeforeEach
    @AfterEach
    void forget() {
        BlockLedger.clear(COLONY);
        VillageColonyMod.WORKERS.remove(BUILDER);
    }

    @Test
    void theBuildPlacingWaitingAndTheSupplyAreCountedByBlock() {
        VillageColonyMod.WORKERS.register(BUILDER, COLONY);

        BlockLedger.action(BUILDER, "PLACED", "oak_planks", 1);
        BlockLedger.action(BUILDER, "PLACED", "oak_planks", 1);
        BlockLedger.action(BUILDER, "SET_ASIDE", "oak_planks", 1);
        BlockLedger.action(BUILDER, "MINED", "stone", 1);
        BlockLedger.surged(COLONY, "minecraft:cobblestone", 36);

        assertArrayEquals(new int[] {2, 1, 0}, BlockLedger.today(COLONY).get("oak_planks"));
        assertArrayEquals(new int[] {0, 0, 36}, BlockLedger.today(COLONY).get("cobblestone"));
        assertFalse(BlockLedger.today(COLONY).containsKey("stone"), "minerar não é gasto da obra");
    }

    @Test
    void aSupplyThatIsNotABlockIsLeftOut() {
        BlockLedger.surged(COLONY, "under the building", 13);

        assertFalse(BlockLedger.today(COLONY).containsKey("under the building"));
    }

    @Test
    void theEntrySaysClassReserveStockAndTheThreeCounts() {
        assertEquals("oak_planks=V,12,40,2,1,0",
                BlockLedger.entry("oak_planks", StockClass.VILLAGE, 12, 40, new int[] {2, 1, 0}));
        assertEquals("crafting_table=S,1,0,0,0,0",
                BlockLedger.entry("crafting_table", StockClass.SPECIAL, 1, 0, new int[] {0, 0, 0}));
    }
}
