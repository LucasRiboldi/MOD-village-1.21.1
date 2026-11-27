package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A malha de cidade do grafo planejado — Regra 389 (ADR-065, emenda de 27-11, M1 🅲). Quatro braços retos saem do centro
 * nos eixos leste, oeste, sul e norte, até a entrada mais distante daquele lado, e crescem {@value #GROWTH} blocos por dia
 * quando a busca de lote fecha a volta sem lote daquele lado. Cada entrada, da mais perto do centro para a mais longe, liga
 * em reta à linha mais perto na mesma coluna ou linha dela; sem reta, ao ponto mais perto da rede. Cada ligação tem traçado
 * e estado ({@link RoadLink}, Regra 388). Puro.
 */
final class CityStreets {

    /** Quantos blocos o corredor da rua reta tem de cada lado do eixo. */
    static final int CORRIDOR = 2;

    /** Quanto o braço cresce numa volta sem lote. */
    static final int GROWTH = 16;

    /** O ramal reto mais longo que se procura. */
    static final int MAX_SPUR = 64;

    /** A folga da busca em volta dos dois pontos, quando não há reta. */
    static final int SEARCH_MARGIN = 16;

    /** De quanto em quanto o braço que não passa encurta. */
    static final int ARM_STEP = 8;

    /** Leste, oeste, sul e norte. */
    static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final NeighborhoodRoutes routes;

    private final Map<Long, Integer> physical;

    private final PathTerrain terrain;

    private final Set<Long> dirty;

    private final Map<Long, Integer> network = new LinkedHashMap<>();

    private final Set<List<Long>> considered = new HashSet<>();

    private final Set<Long> queuedFronts = new HashSet<>();

    private int routed;

    private CityStreets(NeighborhoodRoutes routes, Map<Long, Integer> physical, PathTerrain terrain, Set<Long> dirty) {
        this.routes = routes;
        this.physical = physical;
        this.terrain = terrain;
        this.dirty = dirty;
    }

    /** A malha montada: as colunas da rede e as entradas que ficaram na fila. */
    record Grid(Map<Long, Integer> columns, Set<Long> queued) {
    }

    static Grid build(ColonyPos hub, Collection<ColonyPos> fronts, Map<Long, Integer> physical, PathTerrain terrain,
                      NeighborhoodRoutes routes, Set<Long> dirty, int[] limit, long today, boolean[] grow) {
        CityStreets city = new CityStreets(routes, physical, terrain, dirty);

        routes.queued = 0;
        city.network.put(ColonyRoads.column(hub.x(), hub.z()), hub.y());

        int[] need = new int[SIDES.length];

        for (ColonyPos front : fronts) {
            int side = sideOf(front.x() - hub.x(), front.z() - hub.z());

            if (side >= 0) {
                need[side] = Math.max(need[side], along(side, front.x() - hub.x(), front.z() - hub.z()));
            }
        }

        for (int side = 0; side < SIDES.length; side++) {
            city.arm(hub, side, need[side], limit, today, grow[side]);
        }

        nearestFirst(hub, fronts).forEach(city::spur);

        // A falha fica guardada enquanto o par for tentado: só um bloco mudado perto a traça de novo (Regra 388).
        routes.byPair.keySet().retainAll(city.considered);
        routes.byPair.replaceAll((pair, link) -> link.paved(
                link.columns().stream().allMatch(at -> physical.containsKey(ColonyRoads.column(at.x(), at.z())))));

        return new Grid(city.network, city.queuedFronts);
    }

    /** O lado do eixo mais perto: leste ou oeste se a entrada está mais longe na x, sul ou norte na z; -1 no centro. */
    static int sideOf(int dx, int dz) {
        if (dx == 0 && dz == 0) {
            return -1;
        }

        if (Math.abs(dz) <= Math.abs(dx)) {
            return dx > 0 ? 0 : 1;
        }

        return dz > 0 ? 2 : 3;
    }

    private static int along(int side, int dx, int dz) {
        return Math.abs(SIDES[side][0] != 0 ? dx : dz);
    }

    /** O braço deste lado, do comprimento pedido ou do guardado, mais o crescimento; o que não passa encurta. */
    private void arm(ColonyPos hub, int side, int need, int[] limit, long today, boolean grows) {
        ColonyPos stored = routes.lastEdge.get(side);
        int length = Math.max(need, stored == null ? 0 : along(side, stored.x() - hub.x(), stored.z() - hub.z()));

        if (grows && today > routes.lastDay.getOrDefault(side, Long.MIN_VALUE)) {
            int grown = length + GROWTH;
            int x = hub.x() + SIDES[side][0] * grown;
            int z = hub.z() + SIDES[side][1] * grown;

            if (x >= limit[0] && z >= limit[1] && x <= limit[2] && z <= limit[3]) {
                length = grown;
                routes.lastDay.put(side, today);
            }
        }

        if (length <= 0) {
            return;
        }

        routes.lastEdge.put(side, endOf(hub, side, length));

        for (int reach = length; reach > 0; reach -= ARM_STEP) {
            ColonyPos end = endOf(hub, side, reach);
            int across = CORRIDOR;
            StreetRouter.Box box = new StreetRouter.Box(Math.min(hub.x(), end.x()) - (SIDES[side][0] == 0 ? across : 0),
                    Math.min(hub.z(), end.z()) - (SIDES[side][1] == 0 ? across : 0),
                    Math.max(hub.x(), end.x()) + (SIDES[side][0] == 0 ? across : 0),
                    Math.max(hub.z(), end.z()) + (SIDES[side][1] == 0 ? across : 0));

            if (link(hub, end, List.of(box), -1)) {
                return;
            }
        }
    }

    private ColonyPos endOf(ColonyPos hub, int side, int length) {
        int x = hub.x() + SIDES[side][0] * length;
        int z = hub.z() + SIDES[side][1] * length;

        return new ColonyPos(x, terrain.groundY(x, z, hub.y()).orElse(hub.y()), z);
    }

    /** O ramal desta entrada: em reta à linha mais perto na mesma coluna ou linha; sem reta, ao ponto mais perto. */
    private void spur(ColonyPos front) {
        long column = ColonyRoads.column(front.x(), front.z());

        if (network.containsKey(column)) {
            return;
        }

        Optional<ColonyPos> straight = straightTarget(front);

        // A reta no corredor primeiro; se ela não passa, o desvio largo até o mesmo ponto, antes de dar a falha.
        if (straight.isPresent() && link(front, straight.get(), List.of(StreetRouter.Box.around(front, straight.get(), CORRIDOR),
                StreetRouter.Box.around(front, straight.get(), SEARCH_MARGIN)), column)) {
            return;
        }

        nearestOnTheNetwork(front).filter(target -> !straight.equals(Optional.of(target))).ifPresent(target ->
                link(front, target, List.of(StreetRouter.Box.around(front, target, SEARCH_MARGIN)), column));
    }

    private Optional<ColonyPos> straightTarget(ColonyPos front) {
        for (int k = 1; k <= MAX_SPUR; k++) {
            for (int[] side : SIDES) {
                long at = ColonyRoads.column(front.x() + side[0] * k, front.z() + side[1] * k);
                Integer y = network.get(at);

                if (y != null) {
                    return Optional.of(new ColonyPos(ColonyRoads.xOf(at), y, ColonyRoads.zOf(at)));
                }
            }
        }

        return Optional.empty();
    }

    private Optional<ColonyPos> nearestOnTheNetwork(ColonyPos front) {
        return network.entrySet().stream()
                .min(Comparator.comparingInt((Map.Entry<Long, Integer> e) -> Math.abs(ColonyRoads.xOf(e.getKey()) - front.x())
                        + Math.abs(ColonyRoads.zOf(e.getKey()) - front.z())).thenComparingLong(Map.Entry::getKey))
                .map(e -> new ColonyPos(ColonyRoads.xOf(e.getKey()), e.getValue(), ColonyRoads.zOf(e.getKey())));
    }

    /**
     * Uma ligação: a guardada, se nenhum quadrado sujo a cruza, ou uma nova pelo A*; diz se ligou. A guardada que se repete
     * conta mais uma montagem igual e trava na terceira; a que falha fica guardada com o motivo e só volta a ser traçada
     * com um quadrado sujo dentro da busca; a que não tem vez nem traçado guardado entra na fila (Regra 388).
     *
     * @param boxes as caixas da busca, da mais estreita para a mais larga: a primeira que passa vale
     * @param front a coluna da entrada que espera esta ligação, ou -1 no braço
     */
    private boolean link(ColonyPos from, ColonyPos to, List<StreetRouter.Box> boxes, long front) {
        long a = ColonyRoads.column(from.x(), from.z());
        long b = ColonyRoads.column(to.x(), to.z());
        List<Long> pair = List.of(Math.min(a, b), Math.max(a, b));

        considered.add(pair);

        RoadLink stored = routes.byPair.get(pair);
        boolean stale = stored == null || (stored.state() == RoadEdgeState.FAILED ? nearAny(from, to)
                : stored.columns().stream().anyMatch(at -> dirty.contains(RoadRegions.regionOf(at.x(), at.z()))));
        RoadLink link;

        if (stale && routed < NeighborhoodRoads.ROUTES_PER_BUILD) {
            routed++;

            String[] why = {""};
            Optional<List<ColonyPos>> traced = Optional.empty();

            for (int i = 0; i < boxes.size() && traced.isEmpty(); i++) {
                traced = StreetRouter.route(from, to, physical, terrain, network, boxes.get(i), why);
            }

            link = traced.map(path -> stored != null && path.equals(stored.columns()) ? stored.reused() : RoadLink.planned(path))
                    .orElseGet(() -> RoadLink.failed(why[0]));
        } else if (stored == null) {
            routes.queued++;

            if (front != -1) {
                queuedFronts.add(front);
            }

            return false;
        } else {
            // Sem vez, a guardada fica como está até a próxima montagem; sem sujo, conta mais uma igual.
            link = stale ? stored : stored.reused();
        }

        routes.byPair.put(pair, link);

        if (link.state() == RoadEdgeState.FAILED) {
            return false;
        }

        link.columns().forEach(at -> network.putIfAbsent(ColonyRoads.column(at.x(), at.z()), at.y()));

        return true;
    }

    /** Se algum quadrado sujo cai na caixa da busca entre os dois pontos. */
    private boolean nearAny(ColonyPos first, ColonyPos second) {
        if (dirty.isEmpty()) {
            return false;
        }

        StreetRouter.Box box = StreetRouter.Box.around(first, second, SEARCH_MARGIN);

        for (int x = box.minX(); x <= box.maxX() + RoadRegions.SIZE - 1; x += RoadRegions.SIZE) {
            for (int z = box.minZ(); z <= box.maxZ() + RoadRegions.SIZE - 1; z += RoadRegions.SIZE) {
                if (dirty.contains(RoadRegions.regionOf(x, z))) {
                    return true;
                }
            }
        }

        return false;
    }

    /** As entradas da mais perto do centro para a mais longe; no empate, pela coluna. */
    static List<ColonyPos> nearestFirst(ColonyPos hub, Collection<ColonyPos> fronts) {
        List<ColonyPos> ordered = new ArrayList<>(fronts);

        ordered.sort(Comparator.comparingLong((ColonyPos at) -> squared(at, hub))
                .thenComparingLong(at -> ColonyRoads.column(at.x(), at.z())));

        return ordered;
    }

    static long squared(ColonyPos a, ColonyPos b) {
        long dx = (long) a.x() - b.x();
        long dz = (long) a.z() - b.z();

        return dx * dx + dz * dz;
    }
}
