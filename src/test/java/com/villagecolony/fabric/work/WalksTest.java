package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.BedPlan;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A caminhada real pelo mapa a pé — Regra 74 e R-148 do estudo v2. */
class WalksTest {

    /** Um corredor em U: a cama fica a 6 blocos em linha reta do trabalho, e a 22 passos pelo caminho. */
    private static Set<BlockPos> corridor() {
        Set<BlockPos> cells = new HashSet<>();

        for (int z = 0; z <= 10; z++) {
            cells.add(new BlockPos(0, 64, z));
            cells.add(new BlockPos(4, 64, z));
        }

        for (int x = 1; x <= 3; x++) {
            cells.add(new BlockPos(x, 64, 10));
        }

        return cells;
    }

    @Test
    void theWalkCountsTheWayRoundAndNotTheStraightLine() {
        Walks walks = new Walks(corridor());
        ColonyPos work = new ColonyPos(0, 64, 0);
        ColonyPos bed = new ColonyPos(6, 64, 0);

        // A cama vale a até 2 blocos da célula: a mais perto pelo caminho é (4, 64, 2), a 22 passos.
        assertEquals(22.0, walks.between(work, bed), 1e-9);
        assertEquals(6.0, BedPlan.distance(work, bed), 1e-9, "a linha reta seria 6");
    }

    @Test
    void aBedNobodyWalksToIsInfinitelyFar() {
        Walks walks = new Walks(corridor());

        assertEquals(Double.POSITIVE_INFINITY, walks.between(new ColonyPos(0, 64, 0), new ColonyPos(40, 64, 40)));
    }

    @Test
    void withoutTheMapOrAwayFromItTheStraightLineCounts() {
        ColonyPos work = new ColonyPos(0, 64, 0);
        ColonyPos bed = new ColonyPos(3, 68, 0);

        assertEquals(5.0, new Walks(Set.of()).between(work, bed), 1e-9, "sem mapa");
        assertEquals(BedPlan.distance(new ColonyPos(0, 30, 0), bed),
                new Walks(corridor()).between(new ColonyPos(0, 30, 0), bed), 1e-9, "trabalho fora do mapa (a mina)");
    }
}
