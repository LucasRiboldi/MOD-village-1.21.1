package com.villagecolony.data.save;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A trava de trecho no save do grafo — Regra 225, ADR-050. */
class RoadGraphStoreTest {

    private static final ColonyPos HUB = new ColonyPos(10, 64, 10);

    private static RoadGraph graph(int length) {
        Set<ColonyPos> cells = new java.util.HashSet<>();

        for (int x = 10; x < 10 + length; x++) {
            cells.add(new ColonyPos(x, 64, 10));
        }

        return RoadGraph.from(cells, HUB, Set.of());
    }

    @Test
    void theSettledDaysSurviveANewGraphAndTheSave() {
        UUID id = UUID.randomUUID();
        RoadGraphStore store = new RoadGraphStore(() -> { });
        Map<Long, Long> since = Map.of(ColonyRoads.column(11, 10), 3L, ColonyRoads.column(12, 10), 7L);

        store.update(id, graph(4));
        store.settle(id, since);
        store.update(id, graph(6));

        assertEquals(since, store.settled(id), "o grafo novo apagou a trava");

        NbtCompound root = new NbtCompound();

        store.write(root);

        RoadGraphStore read = new RoadGraphStore(() -> { });

        read.read(root, Set.of(id));

        assertEquals(since, read.settled(id), "a trava não voltou do save");
    }

    @Test
    void aGraphWithoutSettledDaysReadsAsNoLock() {
        UUID id = UUID.randomUUID();
        RoadGraphStore store = new RoadGraphStore(() -> { });

        store.update(id, graph(3));

        assertTrue(store.settled(id).isEmpty());
        assertTrue(new RoadGraphStore(() -> { }).settled(id).isEmpty());
    }
}
