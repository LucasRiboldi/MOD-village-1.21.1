package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ColonyPos;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uma roça por fazendeiro, a mais perto do baú dele — Regra 52. */
class FarmOwnersTest {

    @BeforeAll
    static void bootMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    private static final UUID ANA = new UUID(0, 1);

    private static final UUID BIA = new UUID(0, 2);

    private static final BlockBox WEST = new BlockBox(-40, 60, 0, -32, 62, 12);

    private static final BlockBox EAST = new BlockBox(32, 60, 0, 40, 62, 12);

    @Test
    void eachFarmerGetsTheFarmNearestHisChest() {
        Map<UUID, BlockBox> owners = FarmOwners.pair(List.of(ANA, BIA),
                Map.of(ANA, new ColonyPos(30, 64, 5), BIA, new ColonyPos(-30, 64, 5)),
                List.of(WEST, EAST), Map.of());

        assertEquals(EAST, owners.get(ANA));
        assertEquals(WEST, owners.get(BIA));
    }

    @Test
    void anOwnerKeepsHisFarmEvenWhenAnotherIsCloser() {
        Map<UUID, BlockBox> owners = FarmOwners.pair(List.of(ANA, BIA),
                Map.of(ANA, new ColonyPos(30, 64, 5), BIA, new ColonyPos(-30, 64, 5)),
                List.of(WEST, EAST), Map.of(BIA, EAST));

        assertEquals(EAST, owners.get(BIA));
        assertEquals(WEST, owners.get(ANA));
    }

    @Test
    void aFarmThatIsGoneFreesItsOwner() {
        Map<UUID, BlockBox> owners = FarmOwners.pair(List.of(ANA),
                Map.of(ANA, new ColonyPos(30, 64, 5)),
                List.of(WEST), Map.of(ANA, EAST));

        assertEquals(WEST, owners.get(ANA));
    }

    @Test
    void moreFarmersThanFarmsLeavesSomeoneWithout() {
        Map<UUID, BlockBox> owners = FarmOwners.pair(List.of(ANA, BIA),
                Map.of(ANA, new ColonyPos(0, 64, 0), BIA, new ColonyPos(0, 64, 0)),
                List.of(EAST), Map.of());

        assertEquals(1, owners.size());
    }

    @Test
    void oneFarmPerFarmer() {
        assertFalse(ColonyFarms.owed(0, 0));
        assertTrue(ColonyFarms.owed(0, 1));
        assertFalse(ColonyFarms.owed(1, 1));
        assertTrue(ColonyFarms.owed(1, 2));
        assertFalse(ColonyFarms.owed(3, 2));
    }

    @Test
    void theOwnerSurvivesTheWorkMemoryRoundTrip() {
        try {
            FarmOwners.clearAll();

            NbtCompound nbt = new NbtCompound();
            nbt.putIntArray(ANA.toString(), new int[] {
                EAST.getMinX(), EAST.getMinY(), EAST.getMinZ(), EAST.getMaxX(), EAST.getMaxY(), EAST.getMaxZ()
            });

            FarmOwners.load(nbt);
            assertEquals(EAST, FarmOwners.farmOf(ANA).orElseThrow());

            NbtCompound saved = FarmOwners.save();
            FarmOwners.clearAll();
            assertTrue(FarmOwners.farmOf(ANA).isEmpty());

            FarmOwners.load(saved);

            assertEquals(EAST, FarmOwners.farmOf(ANA).orElseThrow());
        } finally {
            FarmOwners.clearAll();
        }
    }
}
