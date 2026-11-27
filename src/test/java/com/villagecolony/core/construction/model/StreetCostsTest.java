package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os custos e as escolhas da malha de cidade, com o centro fora da origem — Regras 386 e 389 (ADR-065): o preço de cada
 * passo, quem encosta na rede, a ordem das entradas, o braço guardado e o ramal reto. Valores exatos, para o teste de
 * mutação.
 */
class StreetCostsTest {

    private static final int Y = 64;

    private static final ColonyPos HUB = new ColonyPos(100, Y, 100);

    private static ColonyPos at(int x, int z) {
        return new ColonyPos(x, Y, z);
    }

    private static final PathTerrain FLAT = new PathTerrain() {
        @Override
        public OptionalInt groundY(int x, int z, int nearY) {
            return OptionalInt.of(Y);
        }

        @Override
        public boolean blocked(int x, int z) {
            return false;
        }
    };

    private static long column(int x, int z) {
        return ColonyRoads.column(x, z);
    }

    @Test
    @DisplayName("Regra 389: o preço de cada passo")
    void eachStepHasItsPrice() {
        Map<Long, Integer> network = Map.of(column(10, 10), Y);
        Map<Long, Integer> physical = Map.of(column(20, 20), Y, column(10, 10), Y);
        long far = column(500, 500);

        assertEquals(1.0, StreetRouter.step(column(0, 0), 0, 0, false, Map.of(), Map.of(), far, far), 1e-9, "plano");
        assertEquals(3.0, StreetRouter.step(column(0, 0), 0, 0, true, Map.of(), Map.of(), far, far), 1e-9, "degrau");
        assertEquals(0.1, StreetRouter.step(column(10, 10), 10, 10, false, physical, network, far, far), 1e-9, "rede");
        assertEquals(0.3, StreetRouter.step(column(20, 20), 20, 20, false, physical, network, far, far), 1e-9, "caminho");
        assertEquals(5.0, StreetRouter.step(column(11, 10), 11, 10, false, Map.of(), network, far, far), 1e-9,
                "ao lado da rede");
        assertEquals(1.0, StreetRouter.step(column(11, 10), 11, 10, false, Map.of(), network, column(12, 10), far), 1e-9,
                "colada à chegada");
        assertEquals(1.0, StreetRouter.step(column(11, 10), 11, 10, false, Map.of(), network, far, column(11, 11)), 1e-9,
                "colada à saída");
    }

    @Test
    @DisplayName("Regra 389: quem encosta na rede, quem toca a ponta, e a estimativa até o fim")
    void besideTouchAndEstimate() {
        Map<Long, Integer> network = Map.of(column(10, 10), Y);

        assertTrue(StreetRouter.besideTheNetwork(11, 10, network));
        assertTrue(StreetRouter.besideTheNetwork(9, 10, network));
        assertTrue(StreetRouter.besideTheNetwork(10, 11, network));
        assertTrue(StreetRouter.besideTheNetwork(10, 9, network));
        assertFalse(StreetRouter.besideTheNetwork(11, 11, network), "a diagonal não encosta");
        assertFalse(StreetRouter.besideTheNetwork(12, 10, network));

        assertTrue(StreetRouter.touches(10, 10, column(10, 10)));
        assertTrue(StreetRouter.touches(11, 10, column(10, 10)));
        assertTrue(StreetRouter.touches(10, 9, column(10, 10)));
        assertFalse(StreetRouter.touches(11, 11, column(10, 10)));
        assertFalse(StreetRouter.touches(12, 10, column(10, 10)));
        assertFalse(StreetRouter.touches(8, 10, column(10, 10)));

        assertEquals(0.7, StreetRouter.estimate(103, 104, HUB), 1e-9);
        assertEquals(0.7, StreetRouter.estimate(97, 96, HUB), 1e-9);
    }

    @Test
    @DisplayName("Regra 389: a altura da rede vale antes da do caminho e do chão")
    void theNetworkHeightComesFirst() {
        assertEquals(OptionalInt.of(70), StreetRouter.groundOf(Map.of(column(1, 1), 60), Map.of(column(1, 1), 70), FLAT, 1, 1, Y));
        assertEquals(OptionalInt.of(60), StreetRouter.groundOf(Map.of(column(1, 1), 60), Map.of(), FLAT, 1, 1, Y));
        assertEquals(OptionalInt.of(Y), StreetRouter.groundOf(Map.of(), Map.of(), FLAT, 1, 1, Y));
    }

    @Test
    @DisplayName("Regra 389: a entrada mais perto do centro liga primeiro; no empate, pela coluna")
    void theNearestEntranceGoesFirst() {
        assertEquals(List.of(at(103, 100), at(100, 95), at(110, 110)),
                CityStreets.nearestFirst(HUB, List.of(at(110, 110), at(100, 95), at(103, 100))));

        List<ColonyPos> tie = CityStreets.nearestFirst(HUB, List.of(at(102, 100), at(98, 100)));

        assertEquals(column(tie.get(0).x(), tie.get(0).z()) < column(tie.get(1).x(), tie.get(1).z()), true,
                "no empate, a coluna menor não veio primeiro: " + tie);
        assertEquals(25L, CityStreets.squared(at(103, 104), HUB));
        assertEquals(25L, CityStreets.squared(HUB, at(97, 96)));
    }

    private static Set<ColonyPos> build(NeighborhoodRoutes routes, Set<ColonyPos> fronts, int[] limit, boolean[] grow) {
        return RoadRegions.columnsOf(NeighborhoodRoads.build(Set.of(), HUB, fronts, FLAT, routes, Set.of(), limit, 0, grow)
                .graph().orElseThrow());
    }

    @Test
    @DisplayName("Regra 389: com o centro fora da origem, o braço vai até a entrada, cresce 16 e lembra o comprimento")
    void theArmAwayFromTheOrigin() {
        int[] wide = {0, 0, 300, 300};
        NeighborhoodRoutes routes = new NeighborhoodRoutes();
        Set<ColonyPos> first = build(routes, Set.of(at(120, 100)), wide, new boolean[4]);

        assertTrue(first.contains(at(120, 100)));
        assertFalse(first.contains(at(121, 100)));
        assertEquals(at(120, 100), routes.tipEdges().get(0));

        Set<ColonyPos> grown = build(routes, Set.of(at(120, 100)), wide, new boolean[] {true, false, false, false});

        assertTrue(grown.contains(at(136, 100)), "o braço não cresceu 16 a partir da ponta guardada: " + grown);
        assertFalse(grown.contains(at(137, 100)));

        Set<ColonyPos> kept = build(routes, Set.of(at(100, 110)), wide, new boolean[4]);

        assertTrue(kept.contains(at(136, 100)), "o braço guardado sumiu");
        assertTrue(build(new NeighborhoodRoutes(), Set.of(at(100, 80)), wide, new boolean[] {false, false, false, true})
                .contains(at(100, 64)), "o braço norte não cresceu 16");
    }

    @Test
    @DisplayName("Regra 389: o ramal desce reto ao braço, e não ao ponto mais perto da rede")
    void theSpurDropsStraight() {
        Set<ColonyPos> columns = build(new NeighborhoodRoutes(), Set.of(at(140, 100), at(120, 110), at(125, 112)),
                new int[] {0, 0, 300, 300}, new boolean[4]);

        for (int z = 100; z <= 112; z++) {
            assertTrue(columns.contains(at(125, z)), "o ramal de 125 não desceu reto em z=" + z + ": " + columns);
        }

        assertFalse(columns.contains(at(122, 111)), "o ramal foi ao ponto mais perto em vez de descer reto");
    }

    @Test
    @DisplayName("Regra 389: sem entrada, sem braço guardado e sem grafo")
    void noEntranceNoGraph() {
        NeighborhoodRoutes routes = new NeighborhoodRoutes();

        assertTrue(NeighborhoodRoads.build(Set.of(), HUB, Set.of(), FLAT, routes, Set.of(), new int[] {0, 0, 300, 300}, 0)
                .graph().isEmpty());
        assertTrue(routes.tipEdges().isEmpty(), "o lado sem entrada guardou uma ponta");
    }

    @Test
    @DisplayName("Regra 386: o caminho da mina passa por coluna sem chão que se corta, e não pela bloqueada")
    void theMineWayCutsButNeverCrossesABlock() {
        Set<Long> cut = new HashSet<>(List.of(column(1, 0)));
        PathTerrain cuttable = new PathTerrain() {
            @Override
            public OptionalInt groundY(int x, int z, int nearY) {
                return cut.contains(column(x, z)) ? OptionalInt.empty() : OptionalInt.of(Y);
            }

            @Override
            public boolean blocked(int x, int z) {
                return false;
            }

            @Override
            public boolean canCut(int x, int z, int floorY) {
                return true;
            }
        };
        PathTerrain walled = new PathTerrain() {
            @Override
            public OptionalInt groundY(int x, int z, int nearY) {
                return cut.contains(column(x, z)) ? OptionalInt.empty() : OptionalInt.of(Y);
            }

            @Override
            public boolean blocked(int x, int z) {
                return cut.contains(column(x, z));
            }

            @Override
            public boolean canCut(int x, int z, int floorY) {
                return true;
            }
        };

        assertEquals(2, MineHomeWay.between(at(0, 0), at(2, 0), cuttable).size(), "a coluna que se corta não passou");
        assertTrue(MineHomeWay.between(at(0, 0), at(2, 0), walled).isEmpty(), "a coluna bloqueada passou");
    }
}
