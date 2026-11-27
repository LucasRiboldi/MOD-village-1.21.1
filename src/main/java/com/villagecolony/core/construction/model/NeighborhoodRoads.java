package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * O modo 3 do grafo, plano primeiro — ADR-057, ADR-065. Os nós são o centro e as entradas, ligados pela malha de cidade
 * ({@link CityStreets}, Regra 389): braços retos nos eixos e ramais retos de cada entrada à linha mais perto. Cada ligação é
 * traçada por A* sobre o chão ({@link StreetRouter}): água, construção e degrau de mais de 1 bloco não passam. A ligação
 * traçada fica guardada por par de nós, com estado ({@link RoadLink}, Regra 388), e só é refeita quando um quadrado sujo
 * ({@link RoadRegions}) a cruza. Puro: o chão vem por {@link PathTerrain}.
 */
public final class NeighborhoodRoads {

    /** Quantas ligações novas ou sujas se traçam por montagem; as outras esperam a próxima, na fila. */
    public static final int ROUTES_PER_BUILD = 16;

    /** A folga da busca em volta dos dois pontos, na ligação avulsa. */
    static final int SEARCH_MARGIN = 16;

    private NeighborhoodRoads() {
    }

    /** O grafo do modo 3, sem volta sem lote nesta montagem. */
    public static RoadLines.Result build(Set<ColonyPos> cells, ColonyPos center, Set<ColonyPos> fronts, PathTerrain terrain,
                                         NeighborhoodRoutes routes, Set<Long> dirty, int[] limit, long today) {
        return build(cells, center, fronts, terrain, routes, dirty, limit, today, new boolean[CityStreets.SIDES.length]);
    }

    /**
     * O grafo do modo 3. {@code grow} diz, por lado (leste, oeste, sul, norte), se a busca de lote fechou a volta sem lote
     * daquele lado: o braço cresce, até {@code limit} ({minX, minZ, maxX, maxZ}). A entrada que ficou na fila não é porta
     * solta; a que falhou sai em {@code loose} com o motivo.
     */
    public static RoadLines.Result build(Set<ColonyPos> cells, ColonyPos center, Set<ColonyPos> fronts, PathTerrain terrain,
                                         NeighborhoodRoutes routes, Set<Long> dirty, int[] limit, long today, boolean[] grow) {
        Map<Long, Integer> physical = new HashMap<>();

        cells.forEach(at -> physical.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));

        int hubY = terrain.groundY(center.x(), center.z(), center.y()).orElse(center.y());
        CityStreets.Grid grid = CityStreets.build(new ColonyPos(center.x(), hubY, center.z()), fronts, physical, terrain,
                routes, dirty, limit, today, grow);
        Map<Long, Integer> columns = grid.columns();
        Map<Long, Integer> toLay = new HashMap<>();
        Set<Long> doors = new HashSet<>();

        fronts.forEach(at -> {
            long column = ColonyRoads.column(at.x(), at.z());

            toLay.putIfAbsent(column, at.y());
            doors.add(column);
        });

        List<String> loose = new ArrayList<>();

        for (ColonyPos front : fronts) {
            long column = ColonyRoads.column(front.x(), front.z());

            if (!columns.containsKey(column) && !grid.queued().contains(column)) {
                loose.add("door@" + front.x() + "," + front.y() + "," + front.z() + ":" + whyLoose(routes, column));
            }
        }

        if (columns.size() <= 1) {
            return new RoadLines.Result(Optional.empty(), List.of(), 0, List.of(), List.copyOf(loose));
        }

        return new RoadLines.Result(Optional.of(RoadLinesGraph.of(columns, toLay, center, doors)),
                RoadLines.toPave(columns, toLay, physical), 0, List.of(), List.copyOf(loose));
    }

    /** O lado do eixo mais perto deste deslocamento do centro: 0 leste, 1 oeste, 2 sul, 3 norte; -1 no centro (Regra 389). */
    public static int sideOf(int dx, int dz) {
        return CityStreets.sideOf(dx, dz);
    }

    /** O motivo da porta solta: o da ligação dela que falhou, ou {@code no-route} sem nenhuma tentada. */
    private static String whyLoose(NeighborhoodRoutes routes, long column) {
        return routes.byPair.entrySet().stream()
                .filter(entry -> entry.getKey().contains(column) && entry.getValue().state() == RoadEdgeState.FAILED)
                .map(entry -> entry.getValue().reason()).findFirst().orElse("no-route");
    }

    /** A ligação avulsa por A*, de {@code from} a {@code to}, sem rede, ou vazio sem passagem dentro do teto. */
    static Optional<List<ColonyPos>> route(ColonyPos from, ColonyPos to, Map<Long, Integer> physical, PathTerrain terrain) {
        return StreetRouter.route(from, to, physical, terrain, Map.of(), StreetRouter.Box.around(from, to, SEARCH_MARGIN),
                new String[1]);
    }
}
