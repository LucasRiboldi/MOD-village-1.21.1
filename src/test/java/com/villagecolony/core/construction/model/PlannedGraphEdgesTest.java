package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * As bordas do grafo planejado — Regras 386, 388 e 389 (ADR-065): o lado de cada entrada, o estado da ligação, a caixa e
 * os custos do A*, o braço que cresce e encurta, e o caminho da mina. Valores exatos, para o teste de mutação.
 */
class PlannedGraphEdgesTest {

    private static final int Y = 64;

    private static ColonyPos at(int x, int z) {
        return new ColonyPos(x, Y, z);
    }

    private static PathTerrain terrain(Set<Long> water, Map<Long, Integer> heights) {
        return new PathTerrain() {
            @Override
            public OptionalInt groundY(int x, int z, int nearY) {
                long column = ColonyRoads.column(x, z);

                return water.contains(column) ? OptionalInt.empty() : OptionalInt.of(heights.getOrDefault(column, Y));
            }

            @Override
            public boolean blocked(int x, int z) {
                return false;
            }
        };
    }

    private static final PathTerrain FLAT = terrain(Set.of(), Map.of());

    @Test
    @DisplayName("Regra 389: o lado do eixo mais perto de cada deslocamento")
    void eachOffsetHasItsSide() {
        assertEquals(-1, CityStreets.sideOf(0, 0));
        assertEquals(0, CityStreets.sideOf(5, 0));
        assertEquals(1, CityStreets.sideOf(-5, 0));
        assertEquals(2, CityStreets.sideOf(0, 5));
        assertEquals(3, CityStreets.sideOf(0, -5));
        assertEquals(0, CityStreets.sideOf(3, 3), "no empate, o eixo x");
        assertEquals(1, CityStreets.sideOf(-3, -3), "no empate, o eixo x");
        assertEquals(2, CityStreets.sideOf(2, 3));
        assertEquals(3, CityStreets.sideOf(-2, -3));
        assertEquals(2, NeighborhoodRoads.sideOf(1, 9));
    }

    @Test
    @DisplayName("Regra 388: as ligações guardadas voltam iguais, e a diferença em qualquer parte aparece")
    void storedRoutesCompareByEveryPart() {
        Map<List<Long>, RoadLink> links = Map.of(List.of(1L, 2L), new RoadLink(List.of(at(0, 0)), RoadEdgeState.LOCKED, 3, ""));
        NeighborhoodRoutes routes = NeighborhoodRoutes.restore(links, Map.of(0, at(9, 0)), Map.of(0, 4L));

        assertEquals(1, routes.size());
        assertEquals(links, routes.links());
        assertEquals(Map.of(0, at(9, 0)), routes.tipEdges());
        assertEquals(Map.of(0, 4L), routes.tipDays());
        assertEquals(0, routes.queued());
        assertTrue(routes.sameAs(NeighborhoodRoutes.restore(links, Map.of(0, at(9, 0)), Map.of(0, 4L))));
        assertFalse(routes.sameAs(null));
        assertFalse(routes.sameAs(NeighborhoodRoutes.restore(Map.of(), Map.of(0, at(9, 0)), Map.of(0, 4L))));
        assertFalse(routes.sameAs(NeighborhoodRoutes.restore(links, Map.of(0, at(8, 0)), Map.of(0, 4L))));
        assertFalse(routes.sameAs(NeighborhoodRoutes.restore(links, Map.of(0, at(9, 0)), Map.of(0, 5L))));
    }

    @Test
    @DisplayName("Regra 388: a ligação trava na terceira montagem igual, e o mundo diz se está calçada")
    void aLinkGoesFromPlannedToPaved() {
        RoadLink planned = RoadLink.planned(List.of(at(0, 0)));
        RoadLink second = planned.reused();
        RoadLink third = second.reused();

        assertEquals(RoadEdgeState.PLANNED, planned.state());
        assertEquals(1, planned.sameReads());
        assertEquals(RoadEdgeState.PLANNED, second.state());
        assertEquals(2, second.sameReads());
        assertEquals(RoadEdgeState.LOCKED, third.state());
        assertEquals(RoadStability.READS, third.reused().sameReads(), "a conta passou do teto");

        RoadLink failed = RoadLink.failed("closed");

        assertSame(failed, failed.reused(), "a falha contou montagem igual");
        assertSame(planned, planned.paved(true), "a planejada virou calçada sem travar");

        RoadLink paved = third.paved(true);

        assertEquals(RoadEdgeState.PAVED, paved.state());
        assertSame(third, third.paved(false));
        assertEquals(RoadEdgeState.LOCKED, paved.paved(false).state(), "a calçada que perdeu caminho não voltou a travada");
        assertSame(paved, paved.paved(true));
    }

    @Test
    @DisplayName("Regra 389: a caixa da busca inclui as bordas e nada além delas")
    void theSearchBoxHasInclusiveEdges() {
        StreetRouter.Box box = StreetRouter.Box.around(at(0, 0), at(4, 6), 1);

        assertTrue(box.contains(-1, -1));
        assertTrue(box.contains(5, 7));
        assertFalse(box.contains(-2, 0));
        assertFalse(box.contains(0, -2));
        assertFalse(box.contains(6, 0));
        assertFalse(box.contains(0, 8));
    }

    @Test
    @DisplayName("Regra 389: a rua reta no corredor justo, o degrau de 1 passa e o de 2 não")
    void theRouteIsStraightAndClimbsOneAtMost() {
        String[] why = {""};
        List<ColonyPos> straight = StreetRouter.route(at(0, 0), at(5, 0), Map.of(), FLAT, Map.of(),
                StreetRouter.Box.around(at(0, 0), at(5, 0), 0), why).orElseThrow();

        assertEquals(List.of(at(0, 0), at(1, 0), at(2, 0), at(3, 0), at(4, 0), at(5, 0)), straight);

        Map<Long, Integer> step = new java.util.HashMap<>();

        for (int x = 3; x <= 5; x++) {
            step.put(ColonyRoads.column(x, 0), Y + 1);
        }

        List<ColonyPos> climb = StreetRouter.route(at(0, 0), new ColonyPos(5, Y + 1, 0), Map.of(), terrain(Set.of(), step),
                Map.of(), StreetRouter.Box.around(at(0, 0), at(5, 0), 0), why).orElseThrow();

        assertEquals(Y + 1, climb.get(3).y(), "o degrau de 1 não subiu");

        for (int x = 3; x <= 5; x++) {
            step.put(ColonyRoads.column(x, 0), Y + 2);
        }

        assertTrue(StreetRouter.route(at(0, 0), new ColonyPos(5, Y + 2, 0), Map.of(), terrain(Set.of(), step), Map.of(),
                StreetRouter.Box.around(at(0, 0), at(5, 0), 0), why).isEmpty(), "o degrau de 2 passou");
        assertEquals("closed", why[0]);
    }

    @Test
    @DisplayName("Regra 389: a busca que passa do teto diz exhausted")
    void aSearchPastTheCeilingSaysExhausted() {
        Set<Long> moat = new HashSet<>();

        for (int x = 97; x <= 103; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.max(Math.abs(x - 100), Math.abs(z)) == 3) {
                    moat.add(ColonyRoads.column(x, z));
                }
            }
        }

        String[] why = {""};

        assertTrue(StreetRouter.route(at(0, 0), at(100, 0), Map.of(), terrain(moat, Map.of()), Map.of(),
                StreetRouter.Box.around(at(0, 0), at(100, 0), 60), why).isEmpty());
        assertEquals("exhausted", why[0]);
    }

    @Test
    @DisplayName("Regra 389: a rua não corre colada à linha da rede; entra nela")
    void theRouteRidesTheNetworkInsteadOfRunningBesideIt() {
        Map<Long, Integer> network = new java.util.HashMap<>();

        for (int x = -2; x <= 22; x++) {
            network.put(ColonyRoads.column(x, 0), Y);
        }

        List<ColonyPos> path = StreetRouter.route(at(0, 1), at(20, 1), Map.of(), FLAT, network,
                StreetRouter.Box.around(at(0, 1), at(20, 1), 3), new String[1]).orElseThrow();

        assertTrue(path.contains(at(10, 0)), "a rua não entrou na linha da rede: " + path);
        assertFalse(path.contains(at(10, 1)), "a rua correu colada à linha: " + path);
    }

    @Test
    @DisplayName("Regra 389: a curva custa, e a rua em L dobra uma vez só")
    void anLTurnsOnce() {
        List<ColonyPos> path = StreetRouter.route(at(0, 0), at(6, 6), Map.of(), FLAT, Map.of(),
                StreetRouter.Box.around(at(0, 0), at(6, 6), 0), new String[1]).orElseThrow();
        int turns = 0;

        for (int i = 2; i < path.size(); i++) {
            boolean before = path.get(i - 1).x() != path.get(i - 2).x();
            boolean now = path.get(i).x() != path.get(i - 1).x();

            turns += before != now ? 1 : 0;
        }

        assertEquals(13, path.size());
        assertEquals(1, turns, "a rua dobrou mais de uma vez: " + path);
    }

    private static Set<ColonyPos> build(NeighborhoodRoutes routes, Set<ColonyPos> fronts, PathTerrain terrain, int[] limit,
                                        boolean[] grow, long today) {
        RoadLines.Result result = NeighborhoodRoads.build(Set.of(), at(0, 0), fronts, terrain, routes, Set.of(), limit, today,
                grow);

        return RoadRegions.columnsOf(result.graph().orElseThrow());
    }

    @Test
    @DisplayName("Regra 389: o braço vai até a entrada e nem um bloco além; o outro lado fica sem braço")
    void theArmEndsAtTheEntrance() {
        Set<ColonyPos> columns = build(new NeighborhoodRoutes(), Set.of(at(20, 0)), FLAT, new int[] {-99, -99, 99, 99},
                new boolean[4], 0);

        assertTrue(columns.contains(at(20, 0)));
        assertFalse(columns.contains(at(21, 0)));
        assertFalse(columns.contains(at(-1, 0)));
        assertFalse(columns.contains(at(0, 1)));
    }

    @Test
    @DisplayName("Regra 389: o braço cresce até o limite exato, para os dois lados do eixo x")
    void theArmGrowsUpToTheExactLimit() {
        boolean[] east = {true, false, false, false};
        boolean[] west = {false, true, false, false};

        assertTrue(build(new NeighborhoodRoutes(), Set.of(at(20, 0)), FLAT, new int[] {-99, -99, 36, 99}, east, 0)
                .contains(at(36, 0)), "o braço leste não chegou ao limite exato");
        assertFalse(build(new NeighborhoodRoutes(), Set.of(at(20, 0)), FLAT, new int[] {-99, -99, 35, 99}, east, 0)
                .contains(at(21, 0)), "o braço leste passou do limite");
        assertTrue(build(new NeighborhoodRoutes(), Set.of(at(-20, 0)), FLAT, new int[] {-36, -99, 99, 99}, west, 0)
                .contains(at(-36, 0)), "o braço oeste não chegou ao limite exato");
        assertFalse(build(new NeighborhoodRoutes(), Set.of(at(-20, 0)), FLAT, new int[] {-35, -99, 99, 99}, west, 0)
                .contains(at(-21, 0)), "o braço oeste passou do limite");
        assertTrue(build(new NeighborhoodRoutes(), Set.of(at(0, 20)), FLAT, new int[] {-99, -99, 99, 36},
                new boolean[] {false, false, true, false}, 0).contains(at(0, 36)), "o braço sul não cresceu");
        assertTrue(build(new NeighborhoodRoutes(), Set.of(at(0, -20)), FLAT, new int[] {-99, -36, 99, 99},
                new boolean[] {false, false, false, true}, 0).contains(at(0, -36)), "o braço norte não cresceu");
    }

    @Test
    @DisplayName("Regra 389: o braço que não passa encurta de 8 em 8, e guarda o comprimento pedido")
    void theBlockedArmShrinksByEight() {
        Set<Long> water = new HashSet<>();

        for (int z = -3; z <= 3; z++) {
            water.add(ColonyRoads.column(15, z));
        }

        NeighborhoodRoutes routes = NeighborhoodRoutes.restore(Map.of(), Map.of(0, at(20, 0)), Map.of());
        Set<ColonyPos> columns = build(routes, Set.of(), terrain(water, Map.of()), new int[] {-99, -99, 99, 99},
                new boolean[4], 0);

        assertTrue(columns.contains(at(12, 0)), "o braço não encurtou para 12: " + columns);
        assertFalse(columns.contains(at(13, 0)), "o braço passou de 12: " + columns);
        assertEquals(at(20, 0), routes.tipEdges().get(0), "o braço esqueceu o comprimento pedido");
    }

    @Test
    @DisplayName("Regra 389: a entrada longe de toda reta liga ao ponto mais perto da rede")
    void aFarEntranceLinksToTheNearestPoint() {
        RoadLines.Result result = NeighborhoodRoads.build(Set.of(), at(0, 0), Set.of(at(100, 100)), FLAT, new NeighborhoodRoutes(),
                Set.of(), new int[] {-200, -200, 200, 200}, 0);
        Set<ColonyPos> columns = RoadRegions.columnsOf(result.graph().orElseThrow());

        assertTrue(result.loose().isEmpty(), "a entrada ficou solta: " + result.loose());
        assertTrue(columns.contains(at(100, 50)), "a entrada não desceu reto até a ponta do braço: " + columns);
    }

    @Test
    @DisplayName("Regra 386: o caminho da mina sobe 1, nunca 2, e chega a até 1 de altura")
    void theMineWayClimbsOneAtMost() {
        Map<Long, Integer> heights = new java.util.HashMap<>();

        heights.put(ColonyRoads.column(2, 0), Y + 1);
        heights.put(ColonyRoads.column(3, 0), Y + 1);

        List<ColonyPos> way = MineHomeWay.between(at(0, 0), new ColonyPos(3, Y + 1, 0), terrain(Set.of(), heights));

        assertEquals(List.of(at(1, 0), new ColonyPos(2, Y + 1, 0), new ColonyPos(3, Y + 1, 0)), way);
        assertEquals(at(1, 0), MineHomeWay.between(at(0, 0), at(3, 3), FLAT).getFirst(), "a dobra não começou pelo eixo x");

        heights.put(ColonyRoads.column(2, 0), Y + 2);
        heights.put(ColonyRoads.column(3, 0), Y + 2);

        assertTrue(MineHomeWay.between(at(0, 0), new ColonyPos(3, Y + 2, 0), terrain(Set.of(), Map.of(
                ColonyRoads.column(1, 0), Y + 2, ColonyRoads.column(2, 0), Y + 2, ColonyRoads.column(3, 0), Y + 2,
                ColonyRoads.column(0, 1), Y + 2, ColonyRoads.column(0, 2), Y + 2, ColonyRoads.column(0, 3), Y + 2))).isEmpty(),
                "o caminho subiu 2 de uma vez");
        assertTrue(MineHomeWay.between(at(0, 0), new ColonyPos(3, Y + 2, 0), FLAT).isEmpty(),
                "o caminho chegou à entrada 2 blocos acima");
        assertEquals(3, MineHomeWay.between(at(0, 0), new ColonyPos(3, Y + 1, 0), FLAT).size(),
                "o caminho não chegou à entrada 1 bloco acima");
        assertEquals(Optional.empty(), Optional.<ColonyPos>empty());
    }
}
