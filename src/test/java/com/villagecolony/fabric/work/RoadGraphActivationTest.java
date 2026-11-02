package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoadGraphActivationTest {
    @Test
    void exposesEveryConfirmedGraphColumn() {
        RoadGraph graph = RoadGraph.from(Set.of(new ColonyPos(0, 64, 0), new ColonyPos(1, 64, 0),
                new ColonyPos(2, 64, 0)), new ColonyPos(0, 64, 0), Set.of());

        assertEquals(Set.of(ColonyRoads.column(0, 0), ColonyRoads.column(1, 0), ColonyRoads.column(2, 0)),
                RoadGraphActivation.columnsOf(graph));
    }
}
