package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regra 320 (ADR-057, modo 3): vizinhança relativa entre as entradas e A* com custo de terreno. */
class NeighborhoodRoadsTest {

    private static final int Y = 64;

    private static final PathTerrain FLAT = terrain(Set.of());

    /** Sem crescimento: o limite fica nos próprios nós. */
    private static final int[] NO_GROWTH = {0, 0, -1, -1};

    /** Chão plano, com água (sem chão) nas colunas dadas. */
    private static PathTerrain terrain(Set<Long> water) {
        return new PathTerrain() {
            @Override
            public OptionalInt groundY(int x, int z, int nearY) {
                return water.contains(ColonyRoads.column(x, z)) ? OptionalInt.empty() : OptionalInt.of(Y);
            }

            @Override
            public boolean blocked(int x, int z) {
                return false;
            }
        };
    }

    private static ColonyPos at(int x, int z) {
        return new ColonyPos(x, Y, z);
    }

    private static Set<ColonyPos> columnsOf(RoadLines.Result result) {
        return RoadRegions.columnsOf(result.graph().orElseThrow());
    }

    @Test
    void theNeighborhoodLinksASquareInARingAndSkipsTheDiagonals() {
        Map<Long, ColonyPos> nodes = new TreeMap<>();

        for (ColonyPos node : List.of(at(0, 0), at(20, 0), at(0, 20), at(20, 20))) {
            nodes.put(ColonyRoads.column(node.x(), node.z()), node);
        }

        assertEquals(4, NeighborhoodRoads.neighborhood(nodes).size(), "o quadrado devia ter 4 lados e nenhuma diagonal");
    }

    @Test
    void everyEntranceGetsLinkedToTheCenter() {
        RoadLines.Result result = NeighborhoodRoads.build(Set.of(), at(0, 0), Set.of(at(12, 0), at(0, 15), at(-10, -6)), FLAT,
                new NeighborhoodRoads.Routes(), Set.of(), NO_GROWTH, 0);
        Set<ColonyPos> columns = columnsOf(result);

        assertTrue(result.loose().isEmpty(), "entrada sem ligação: " + result.loose());
        assertTrue(columns.containsAll(Set.of(at(12, 0), at(0, 15), at(-10, -6))), "entrada fora do grafo: " + columns);
        assertFalse(result.toPave().isEmpty(), "nada a calçar num chão sem caminho");
    }

    @Test
    void theRouteGoesAroundWaterAndPrefersTheExistingPath() {
        Set<Long> water = new HashSet<>();

        for (int z = -3; z <= 3; z++) {
            water.add(ColonyRoads.column(5, z));
        }

        List<ColonyPos> around = NeighborhoodRoads.route(at(0, 0), at(10, 0), Map.of(), terrain(water)).orElseThrow();

        assertTrue(around.stream().noneMatch(c -> water.contains(ColonyRoads.column(c.x(), c.z()))), "a ligação pisou na água");

        Map<Long, Integer> path = new java.util.HashMap<>();

        for (int x = 0; x <= 10; x++) {
            path.put(ColonyRoads.column(x, 1), Y);
        }

        List<ColonyPos> reused = NeighborhoodRoads.route(at(0, 0), at(10, 0), path, FLAT).orElseThrow();

        // Ir reto custa 10; pelo caminho a 1 bloco, 1 + 11 × 0,3 + 1 + 2 curvas ≈ 7,3.
        assertTrue(reused.stream().filter(c -> c.z() == 1).count() >= 8,
                "a ligação não reaproveitou o caminho que já existe a 1 bloco: " + reused);
    }

    @Test
    void aStoredRouteIsReusedUntilADirtySquareCrossesIt() {
        NeighborhoodRoads.Routes routes = new NeighborhoodRoads.Routes();
        Set<ColonyPos> fronts = Set.of(at(30, 0));

        NeighborhoodRoads.build(Set.of(), at(0, 0), fronts, FLAT, routes, Set.of(), NO_GROWTH, 0);
        assertEquals(1, routes.size());

        RoadLines.Result again = NeighborhoodRoads.build(Set.of(), at(0, 0), fronts, terrain(Set.of(ColonyRoads.column(15, 0))),
                routes, Set.of(), NO_GROWTH, 0);

        assertTrue(columnsOf(again).contains(at(15, 0)), "sem quadrado sujo, a ligação guardada devia ficar como estava");

        RoadLines.Result dirty = NeighborhoodRoads.build(Set.of(), at(0, 0), fronts, terrain(Set.of(ColonyRoads.column(15, 0))),
                routes, Set.of(RoadRegions.regionOf(15, 0)), NO_GROWTH, 0);

        assertFalse(columnsOf(dirty).contains(at(15, 0)), "com o quadrado sujo, a ligação devia desviar da água nova");
        assertSame(dirty.loose().isEmpty(), true);
    }

    @Test
    void anEntranceBeyondTheNeighborhoodReachStillGetsLinked() {
        RoadLines.Result result = NeighborhoodRoads.build(Set.of(), at(0, 0), Set.of(at(10, 0), at(150, 0)), FLAT,
                new NeighborhoodRoads.Routes(), Set.of(), NO_GROWTH, 0);

        assertTrue(result.loose().isEmpty(), "Regra 320: entrada a 150 blocos ficou solta: " + result.loose());
        assertTrue(columnsOf(result).contains(at(150, 0)), "Regra 320: a entrada distante não entrou no grafo");
    }

    @Test
    void theEastTipGrowsOneBlockADay() {
        NeighborhoodRoads.Routes routes = new NeighborhoodRoads.Routes();
        int[] limit = {-200, -200, 200, 200};
        Set<ColonyPos> street = new HashSet<>();

        for (int x = 0; x <= 20; x++) {
            street.add(at(x, 0));
        }

        assertTrue(columnsOf(NeighborhoodRoads.build(street, at(0, 0), Set.of(), FLAT, routes, Set.of(), limit, 0))
                .contains(at(21, 0)), "Regra 320: a ponta leste não ganhou o bloco de fora");

        street.add(at(21, 0));

        assertFalse(columnsOf(NeighborhoodRoads.build(street, at(0, 0), Set.of(), FLAT, routes, Set.of(), limit, 0))
                .contains(at(22, 0)), "Regra 320: a ponta andou dois blocos no mesmo dia");
        assertTrue(columnsOf(NeighborhoodRoads.build(street, at(0, 0), Set.of(), FLAT, routes, Set.of(), limit, 1))
                .contains(at(22, 0)), "Regra 320: a ponta não andou no dia seguinte");
    }

    @Test
    void theTipStopsAtTheLimit() {
        Set<ColonyPos> street = new HashSet<>();

        for (int x = 0; x <= 20; x++) {
            street.add(at(x, 0));
        }

        assertFalse(columnsOf(NeighborhoodRoads.build(street, at(0, 0), Set.of(), FLAT, new NeighborhoodRoads.Routes(),
                Set.of(), new int[] {-200, -200, 20, 200}, 0)).contains(at(21, 0)), "Regra 320: a ponta passou do limite");
    }
}
