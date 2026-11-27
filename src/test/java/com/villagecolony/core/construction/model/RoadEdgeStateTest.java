package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 388 (ADR-065 §6, etapa 1): cada ligação do grafo planejado tem traçado e estado. Ela trava depois de 3 montagens
 * iguais, fica calçada quando o mundo tem caminho em toda coluna, e a que falha fica guardada com o motivo, sem ser
 * tentada de novo até um bloco mudar perto dela.
 */
class RoadEdgeStateTest {

    private static final int Y = 64;

    private static final int[] NO_GROWTH = {0, 0, -1, -1};

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

    private static List<Long> pair(ColonyPos a, ColonyPos b) {
        long ca = ColonyRoads.column(a.x(), a.z());
        long cb = ColonyRoads.column(b.x(), b.z());

        return List.of(Math.min(ca, cb), Math.max(ca, cb));
    }

    private static RoadLines.Result build(Set<ColonyPos> cells, Set<ColonyPos> fronts, PathTerrain terrain,
                                          NeighborhoodRoutes routes, Set<Long> dirty) {
        return NeighborhoodRoads.build(cells, at(0, 0), fronts, terrain, routes, dirty, NO_GROWTH, 0);
    }

    @Test
    @DisplayName("Regra 388: a ligação trava na terceira montagem igual e fica calçada com caminho em toda coluna")
    void aLinkLocksAfterThreeEqualBuildsAndIsPavedByTheWorld() {
        NeighborhoodRoutes routes = new NeighborhoodRoutes();
        Set<ColonyPos> fronts = Set.of(at(30, 0));
        PathTerrain flat = terrain(Set.of());

        build(Set.of(), fronts, flat, routes, Set.of());
        assertEquals(RoadEdgeState.PLANNED, routes.links().get(pair(at(0, 0), at(30, 0))).state(), "a ligação nova já travou");

        build(Set.of(), fronts, flat, routes, Set.of());
        build(Set.of(), fronts, flat, routes, Set.of());
        RoadLink locked = routes.links().get(pair(at(0, 0), at(30, 0)));

        assertEquals(RoadEdgeState.LOCKED, locked.state(), "Regra 388: 3 montagens iguais não travaram a ligação");

        build(new HashSet<>(locked.columns()), fronts, flat, routes, Set.of());

        assertEquals(RoadEdgeState.PAVED, routes.links().get(pair(at(0, 0), at(30, 0))).state(),
                "Regra 388: com caminho em toda coluna, a ligação não ficou calçada");
    }

    @Test
    @DisplayName("Regra 388: a ligação que falha fica guardada com o motivo e só é tentada de novo com bloco mudado perto")
    void aFailedLinkWaitsForABlockChangeNearIt() {
        Set<Long> moat = new HashSet<>();

        for (int x = 26; x <= 34; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.max(Math.abs(x - 30), Math.abs(z)) == 3) {
                    moat.add(ColonyRoads.column(x, z));
                }
            }
        }

        NeighborhoodRoutes routes = new NeighborhoodRoutes();
        Set<ColonyPos> fronts = Set.of(at(30, 0));
        RoadLines.Result walled = build(Set.of(), fronts, terrain(moat), routes, Set.of());
        RoadLink failed = routes.links().get(pair(at(0, 0), at(30, 0)));

        assertTrue(failed != null && failed.state() == RoadEdgeState.FAILED,
                "Regra 388: a ligação sem passagem não ficou guardada como falha: " + failed);
        assertEquals("closed", failed.reason(), "a falha não disse o motivo");
        assertTrue(walled.loose().stream().anyMatch(entry -> entry.endsWith(":closed")),
                "o log não trouxe o motivo da porta solta: " + walled.loose());

        build(Set.of(), fronts, terrain(Set.of()), routes, Set.of());

        assertEquals(RoadEdgeState.FAILED, routes.links().get(pair(at(0, 0), at(30, 0))).state(),
                "Regra 388: a falha foi tentada de novo sem nenhum bloco mudado");

        build(Set.of(), fronts, terrain(Set.of()), routes, Set.of(RoadRegions.regionOf(30, 3)));

        assertEquals(RoadEdgeState.PLANNED, routes.links().get(pair(at(0, 0), at(30, 0))).state(),
                "Regra 388: com o bloco mudado perto, a ligação não foi traçada de novo");
    }

    @Test
    @DisplayName("Regra 388: a ligação que espera a vez entra na fila, e não como falha")
    void aQueuedLinkIsNotAFailure() {
        NeighborhoodRoutes routes = new NeighborhoodRoutes();
        Set<ColonyPos> fronts = new HashSet<>();

        for (int i = 1; i <= 40; i++) {
            fronts.add(at(i * 12, (i % 2) * 12));
        }

        RoadLines.Result first = build(Set.of(), fronts, terrain(Set.of()), routes, Set.of());

        assertTrue(routes.queued() > 0, "com mais ligações que a vez de uma montagem, nenhuma ficou na fila");
        assertTrue(first.loose().isEmpty(), "Regra 388: a ligação na fila saiu como porta solta: " + first.loose());
        assertTrue(routes.links().values().stream().noneMatch(link -> link.state() == RoadEdgeState.FAILED),
                "Regra 388: a ligação na fila foi guardada como falha");
    }
}
