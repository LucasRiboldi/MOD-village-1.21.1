package com.villagecolony.core.coordination;

import com.villagecolony.core.coordination.BlockReserve.StockClass;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** ADR-062, Regra 340: a reserva de cada bloco sai da classe dele, medida nas vilas geradas. */
class BlockReserveTest {

    @Test
    void aPieceTheVillagesUseByTheThousandIsAVillagePieceAndKeepsTwelve() {
        assertEquals(StockClass.VILLAGE, BlockReserve.classOf("minecraft:oak_stairs", true));
        assertEquals(12, BlockReserve.reserveOf("minecraft:oak_stairs", true));
    }

    @Test
    void aWorkbenchTheVillagesHaveUnderTenOfIsSpecialAndKeepsOne() {
        assertEquals(StockClass.SPECIAL, BlockReserve.classOf("minecraft:crafting_table", true));
        assertEquals(StockClass.SPECIAL, BlockReserve.classOf("minecraft:stone_bricks", true));
        assertEquals(1, BlockReserve.reserveOf("minecraft:cartography_table", true));
    }

    @Test
    void aCraftedBlockOutsideTheVillagesKeepsFour() {
        assertEquals(StockClass.CRAFTED, BlockReserve.classOf("minecraft:glass", true));
        assertEquals(4, BlockReserve.reserveOf("minecraft:polished_andesite", true));
    }

    @Test
    void whatTheVillagerTakesFromTheWorldIsRawUpTo256() {
        assertEquals(StockClass.RAW, BlockReserve.classOf("minecraft:oak_log", false));
        assertEquals(StockClass.RAW, BlockReserve.classOf("minecraft:stone", true), "a pedra tem receita, e é natural");
        assertEquals(256, BlockReserve.reserveOf("minecraft:wheat", true));
    }

    @Test
    void theModBenchesAreNeverStocked() {
        assertEquals(StockClass.NEVER, BlockReserve.classOf("villagecolony:carpenter_bench", true));
        assertEquals(0, BlockReserve.reserveOf("villagecolony:miner_bench", true));
    }

    @Test
    void theResourceTypesFollowTheirProduction() {
        assertEquals(12, BlockReserve.reserveOf(ResourceType.OAK_PLANKS));
        assertEquals(4, BlockReserve.reserveOf(ResourceType.GLASS));
        assertEquals(256, BlockReserve.reserveOf(ResourceType.OAK_LOG));
    }
}
