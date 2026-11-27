package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 389 (ADR-065, emenda de 27-11, M1 🅲 do jogo da 0.4.151: "deixar todos em linhas retas, a menor quantidade de
 * linhas, nunca uma linha próxima da outra ... como as ruas de uma cidade"): quatro braços retos nos eixos, e cada entrada
 * liga em reta à linha mais perto; nunca duas linhas lado a lado.
 */
class CityStreetsTest {

    private static final int Y = 64;

    private static final int[] WIDE = {-500, -500, 500, 500};

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

    private static ColonyPos at(int x, int z) {
        return new ColonyPos(x, Y, z);
    }

    private static Set<ColonyPos> columnsOf(RoadLines.Result result) {
        return RoadRegions.columnsOf(result.graph().orElseThrow());
    }

    private static RoadLines.Result build(Set<ColonyPos> fronts, NeighborhoodRoutes routes, boolean[] grow, long today) {
        return NeighborhoodRoads.build(Set.of(), at(0, 0), fronts, FLAT, routes, Set.of(), WIDE, today, grow);
    }

    private static boolean[] none() {
        return new boolean[4];
    }

    /** Duas linhas lado a lado sempre fecham um quadrado 2 × 2 de colunas da rede. */
    private static boolean hasTwoLinesSideBySide(Set<ColonyPos> columns) {
        Set<Long> all = new HashSet<>();

        columns.forEach(at -> all.add(ColonyRoads.column(at.x(), at.z())));

        for (ColonyPos at : columns) {
            if (all.contains(ColonyRoads.column(at.x() + 1, at.z())) && all.contains(ColonyRoads.column(at.x(), at.z() + 1))
                    && all.contains(ColonyRoads.column(at.x() + 1, at.z() + 1))) {
                return true;
            }
        }

        return false;
    }

    @Test
    @DisplayName("Regra 389: casas vizinhas não abrem linhas lado a lado")
    void neighbouringHousesNeverGetTwoLinesSideBySide() {
        Set<ColonyPos> fronts = Set.of(at(20, 6), at(23, 6), at(26, 5), at(30, 7), at(12, -9), at(14, -10));
        Set<ColonyPos> columns = columnsOf(build(fronts, new NeighborhoodRoutes(), none(), 0));

        assertTrue(columns.containsAll(fronts), "entrada fora do grafo: " + columns);
        assertFalse(hasTwoLinesSideBySide(columns), "Regra 389: duas linhas lado a lado: " + columns);
    }

    @Test
    @DisplayName("Regra 389: o braço leste é reto no eixo, e o ramal da casa cai em reta sobre ele")
    void theArmIsStraightAndTheSpurDropsOnIt() {
        Set<ColonyPos> columns = columnsOf(build(Set.of(at(30, 0), at(18, 7)), new NeighborhoodRoutes(), none(), 0));

        for (int x = 0; x <= 30; x++) {
            assertTrue(columns.contains(at(x, 0)), "Regra 389: o braço leste saiu do eixo em x=" + x + ": " + columns);
        }

        for (int z = 0; z <= 7; z++) {
            assertTrue(columns.contains(at(18, z)), "Regra 389: o ramal não caiu em reta sobre o braço, em z=" + z);
        }
    }

    @Test
    @DisplayName("Regra 389: a malha tem poucas linhas, menos que a vizinhança relativa")
    void theGridUsesFewLines() {
        Set<ColonyPos> fronts = new HashSet<>(List.of(at(20, 0), at(-20, 0), at(0, 20), at(0, -20), at(15, 12), at(-14, 11),
                at(13, -16), at(-12, -15)));
        RoadLines.Result result = build(fronts, new NeighborhoodRoutes(), none(), 0);

        assertTrue(result.loose().isEmpty(), "entrada solta: " + result.loose());
        assertTrue(result.graph().orElseThrow().edges().size() <= fronts.size() + 4,
                "Regra 389: linhas demais: " + result.graph().orElseThrow().edges().size());
    }

    @Test
    @DisplayName("Regra 389: o braço cresce 16 blocos na volta sem lote, uma vez por dia")
    void theArmGrowsWhenNoLotWasFound() {
        NeighborhoodRoutes routes = new NeighborhoodRoutes();
        boolean[] east = {true, false, false, false};

        build(Set.of(at(20, 0)), routes, none(), 0);
        assertFalse(columnsOf(build(Set.of(at(20, 0)), routes, none(), 0)).contains(at(21, 0)),
                "o braço cresceu sem volta sem lote");

        assertTrue(columnsOf(build(Set.of(at(20, 0)), routes, east, 0)).contains(at(36, 0)),
                "Regra 389: o braço leste não cresceu 16 blocos com a volta sem lote");
        assertFalse(columnsOf(build(Set.of(at(20, 0)), routes, east, 0)).contains(at(37, 0)),
                "Regra 389: o braço cresceu duas vezes no mesmo dia");
        assertEquals(at(36, 0), routes.tipEdges().get(0), "a ponta guardada do braço leste não é a de 36");
        assertTrue(columnsOf(build(Set.of(at(20, 0)), routes, east, 1)).contains(at(52, 0)),
                "Regra 389: o braço não cresceu no dia seguinte");
    }

    @Test
    @DisplayName("Regra 389: o braço não cresce além do limite da vila")
    void theArmStopsAtTheLimit() {
        Set<ColonyPos> columns = columnsOf(NeighborhoodRoads.build(Set.of(), at(0, 0), Set.of(at(20, 0)), FLAT,
                new NeighborhoodRoutes(), Set.of(), new int[] {-500, -500, 30, 500}, 0, new boolean[] {true, false, false, false}));

        assertFalse(columns.contains(at(21, 0)), "Regra 389: o braço passou do limite");
    }

    @Test
    @DisplayName("Regra 389: o desvio da água e a rua que já existe não abrem linha ao lado do braço")
    void aDetourNeverRunsBesideTheArm() {
        Set<Long> water = new HashSet<>();

        for (int x = 17; x <= 23; x++) {
            water.add(ColonyRoads.column(x, 3));
        }

        PathTerrain wet = new PathTerrain() {
            @Override
            public OptionalInt groundY(int x, int z, int nearY) {
                return water.contains(ColonyRoads.column(x, z)) ? OptionalInt.empty() : OptionalInt.of(Y);
            }

            @Override
            public boolean blocked(int x, int z) {
                return false;
            }
        };
        Set<ColonyPos> street = new HashSet<>();

        for (int x = 8; x <= 34; x++) {
            street.add(at(x, 1));
        }

        RoadLines.Result result = NeighborhoodRoads.build(street, at(0, 0), Set.of(at(40, 0), at(20, 6)), wet,
                new NeighborhoodRoutes(), Set.of(), WIDE, 0, none());
        Set<ColonyPos> columns = columnsOf(result);

        assertTrue(result.loose().isEmpty(), "entrada solta: " + result.loose());
        assertFalse(hasTwoLinesSideBySide(columns), "Regra 389: o desvio correu ao lado da linha: " + columns);
    }
}
