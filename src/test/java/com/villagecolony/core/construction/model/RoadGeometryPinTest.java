package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A geometria do grafo em muitas formas de rua, fixada e conferida — Regras 177, 183, 184, 186, 206 e 208.
 *
 * <p>Duas provas. As invariantes dizem o que tem de valer sempre. O registro em {@code road_geometry_pin.txt} fixa o resultado de
 * hoje, forma a forma: mudança de traçado reprova aqui e precisa ser olhada; se for de propósito, regrave com
 * a variável de ambiente {@code ROAD_PIN_UPDATE} ligada. O registro não diz que o traçado está certo, só que não mudou sem ninguém ver.
 */
@DisplayName("RoadGeometry")
class RoadGeometryPinTest {

    private static final int Y = 64;

    private static final String PIN = "road_geometry_pin.txt";

    /** O terreno de cada cenário: morro suave, colunas bloqueadas, sem chão e onde dá para cortar. */
    private record Terrain(int slope, int blockedEvery, int holeEvery, int cutEvery) implements PathTerrain {
        private int height(int x, int z) {
            return slope == 0 ? Y : Y + Math.floorMod(x / slope + z / (slope + 1), 3);
        }

        private static boolean hits(int every, int x, int z) {
            return every > 0 && Math.floorMod(x * 7 + z * 13, every) == 0;
        }

        @Override
        public OptionalInt groundY(int x, int z, int nearY) {
            return hits(holeEvery, x, z) ? OptionalInt.empty() : OptionalInt.of(height(x, z));
        }

        @Override
        public boolean blocked(int x, int z) {
            return hits(blockedEvery, x, z);
        }

        @Override
        public boolean canCut(int x, int z, int floorY) {
            return hits(cutEvery, x, z);
        }
    }

    private record Scenario(String name, Set<Long> cells, Terrain terrain, Set<Long> doors, Set<Long> rings, int links) {
    }

    private static long column(int x, int z) {
        return ColonyRoads.column(x, z);
    }

    private static void rect(Set<Long> cells, int x1, int z1, int x2, int z2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                cells.add(column(x, z));
            }
        }
    }

    /** Uma rua que anda ao acaso: {@code width} de largura, com curva, ramal e buraco. */
    private static Set<Long> wander(long seed, int steps, int width, int holes) {
        Random random = new Random(seed);
        Set<Long> cells = new HashSet<>();
        List<int[]> tips = new ArrayList<>(List.of(new int[] {0, 0}));

        for (int step = 0; step < steps; step++) {
            int[] tip = tips.get(random.nextInt(tips.size()));
            int length = 3 + random.nextInt(12);
            boolean alongX = random.nextBoolean();
            int sign = random.nextBoolean() ? 1 : -1;
            int x = tip[0] + (alongX ? sign * length : 0);
            int z = tip[1] + (alongX ? 0 : sign * length);

            rect(cells, tip[0], tip[1], x + (alongX ? 0 : width - 1), z + (alongX ? width - 1 : 0));
            tips.add(new int[] {x, z});
        }

        List<Long> all = new ArrayList<>(new TreeSet<>(cells));

        for (int hole = 0; hole < holes && !all.isEmpty(); hole++) {
            cells.remove(all.get(random.nextInt(all.size())));
        }

        return cells;
    }

    private static List<Scenario> scenarios() {
        List<Scenario> all = new ArrayList<>();
        Terrain flat = new Terrain(0, 0, 0, 0);
        Set<Long> line = new HashSet<>();
        Set<Long> cross = new HashSet<>();
        Set<Long> square = new HashSet<>();
        Set<Long> parts = new HashSet<>();
        Set<Long> gaps = new HashSet<>();
        Set<Long> blob = new HashSet<>();
        Set<Long> ladder = new HashSet<>();
        Set<Long> pocket = new HashSet<>();
        Set<Long> diagonal = new HashSet<>();

        rect(line, 0, 0, 40, 2);
        rect(cross, -20, -1, 20, 1);
        rect(cross, -1, -20, 1, 20);
        rect(square, 0, 0, 24, 0);
        rect(square, 0, 24, 24, 24);
        rect(square, 0, 0, 0, 24);
        rect(square, 24, 0, 24, 24);
        rect(parts, 0, 0, 20, 0);
        rect(parts, 28, 0, 48, 0);
        rect(parts, 0, 30, 20, 30);
        rect(parts, -30, -30, -24, -24);
        rect(gaps, 0, 0, 10, 2);
        rect(gaps, 13, 0, 24, 2);
        rect(gaps, 0, 4, 6, 8);
        rect(blob, 0, 0, 14, 14);
        rect(ladder, 0, 0, 30, 0);
        rect(ladder, 0, 6, 30, 6);
        rect(ladder, 0, 0, 0, 6);
        rect(ladder, 15, 0, 15, 6);
        rect(ladder, 30, 0, 30, 6);
        rect(pocket, 0, 0, 12, 12);
        rect(pocket, 40, 0, 44, 0);

        for (int hole : new int[] {3, 4, 7, 8}) {
            pocket.remove(column(hole, 5));
            pocket.remove(column(5, hole));
        }

        for (int i = 0; i < 30; i++) {
            diagonal.add(column(i, i));
            diagonal.add(column(i + 1, i));
        }

        Map<String, Set<Long>> shapes = new TreeMap<>();

        shapes.put("line", line);
        shapes.put("cross", cross);
        shapes.put("square", square);
        shapes.put("parts", parts);
        shapes.put("gaps", gaps);
        shapes.put("blob", blob);
        shapes.put("ladder", ladder);
        shapes.put("pocket", pocket);
        shapes.put("diagonal", diagonal);

        for (long seed = 1; seed <= 8; seed++) {
            shapes.put("wander" + seed, wander(seed * 31, 6 + (int) seed, 1 + (int) (seed % 3), (int) (seed % 4) * 2));
        }

        List<Terrain> terrains = List.of(flat, new Terrain(5, 0, 0, 0), new Terrain(0, 9, 0, 0), new Terrain(3, 0, 11, 5));

        shapes.forEach((name, cells) -> {
            for (int index = 0; index < terrains.size(); index++) {
                List<Long> sorted = new ArrayList<>(new TreeSet<>(cells));
                Set<Long> doors = new HashSet<>();
                Set<Long> rings = new HashSet<>();

                if (index % 2 == 1) {
                    for (int door = 0; door < sorted.size(); door += Math.max(1, sorted.size() / 3)) {
                        long at = sorted.get(door);

                        doors.add(column(ColonyRoads.xOf(at) + 2, ColonyRoads.zOf(at) - 3));
                    }
                }

                if (index == 2) {
                    rings.addAll(sorted.subList(0, Math.min(sorted.size(), 9)));
                }

                all.add(new Scenario(name + "/" + index, cells, terrains.get(index), doors, rings, index == 0 ? 0 : 1 + index));
            }
        });

        return all;
    }

    private static Map<Long, Integer> heights(Set<Long> cells, PathTerrain terrain) {
        Map<Long, Integer> map = new HashMap<>();

        for (long at : cells) {
            map.put(at, terrain.groundY(ColonyRoads.xOf(at), ColonyRoads.zOf(at), Y).orElse(Y));
        }

        return map;
    }

    private static Set<ColonyPos> positions(Set<Long> cells, PathTerrain terrain) {
        Set<ColonyPos> out = new HashSet<>();

        heights(cells, terrain).forEach((at, y) -> out.add(new ColonyPos(ColonyRoads.xOf(at), y, ColonyRoads.zOf(at))));

        return out;
    }

    private static String sortedMap(Map<Long, Integer> map) {
        return new TreeMap<>(map).toString();
    }

    private static String ofChains(RoadChains.Net net) {
        List<String> chains = new ArrayList<>();

        net.chains().forEach(chain -> chains.add(chain.from.x + "," + chain.from.z + ">" + chain.to.x + "," + chain.to.z + ":" + chain.pixels));

        return net.nodes().stream().map(node -> node.x + "," + node.z + "," + node.fixed).sorted().toList()
                + "|" + chains.stream().sorted().toList();
    }

    private static String ofResult(RoadLines.Result result) {
        String graph = result.graph().map(found -> found.nodes().stream()
                .map(node -> node.position() + "=" + new TreeSet<>(node.roles())).sorted().toList()
                + "|" + found.edges().stream().map(edge -> edge.from() + ">" + edge.to() + ":" + edge.columns() + edge.roadClass())
                .sorted().toList()).orElse("none");

        return graph + "|" + result.toPave().stream().map(Objects::toString).sorted().toList() + "|" + result.isolated()
                + "|" + result.surplus().stream().map(Objects::toString).sorted().toList();
    }

    /** Uma linha por resultado: o nome, o tamanho e o resumo do texto, para o registro caber no repositório. */
    private static void record(Map<String, String> out, String key, String text) {
        out.put(key, text.length() + ":" + Integer.toHexString(text.hashCode()));
    }

    private static Map<String, String> measure() {
        Map<String, String> out = new TreeMap<>();

        for (Scenario scenario : scenarios()) {
            Terrain terrain = scenario.terrain();
            Map<Long, Integer> cells = heights(scenario.cells(), terrain);
            Map<Long, Integer> mask = RoadMask.closed(cells, terrain);
            Set<Long> line = RoadThinning.skeleton(mask.keySet(), scenario.doors());
            RoadChains.Net net = RoadChains.of(line, scenario.doors());

            record(out, scenario.name() + " mask", sortedMap(mask));
            record(out, scenario.name() + " line", new TreeSet<>(line).toString());
            record(out, scenario.name() + " chains", ofChains(net));
            record(out, scenario.name() + " routes", sortedMap(RoadRouting.route(net, mask, terrain)));

            ColonyPos center = new ColonyPos(4, Y, 3);

            for (int turn = 0; turn < 2; turn++) {
                record(out, scenario.name() + " build" + turn, ofResult(RoadLines.build(positions(scenario.cells(), terrain), Set.of(), center,
                        positions(scenario.doors(), terrain), positions(scenario.rings(), terrain), terrain, scenario.links(), turn)));
            }
        }

        return out;
    }

    private static String render(Map<String, String> pins) {
        return pins.entrySet().stream().map(pin -> pin.getKey() + " " + pin.getValue()).collect(Collectors.joining("\n")) + "\n";
    }

    @Test
    @DisplayName("nenhum traçado mudou sem ser olhado")
    void theGeometryIsPinned() throws IOException {
        String now = render(measure());

        if (System.getenv("ROAD_PIN_UPDATE") != null) {
            Files.writeString(Path.of("src/test/resources", PIN), now, StandardCharsets.UTF_8);
            return;
        }

        try (InputStream in = RoadGeometryPinTest.class.getResourceAsStream("/" + PIN)) {
            assertTrue(in != null, "falta o registro " + PIN);
            assertEquals(new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n"), now);
        }
    }

    @Test
    @DisplayName("a máscara guarda o que já é caminho, e o miolo sai dela")
    void theLineStaysInsideTheMask() {
        for (Scenario scenario : scenarios()) {
            Map<Long, Integer> cells = heights(scenario.cells(), scenario.terrain());
            Map<Long, Integer> mask = RoadMask.closed(cells, scenario.terrain());
            Set<Long> line = RoadThinning.skeleton(mask.keySet(), Set.of());

            assertTrue(mask.keySet().containsAll(cells.keySet()), scenario.name() + ": a máscara perdeu caminho");
            assertTrue(mask.keySet().containsAll(line), scenario.name() + ": a linha saiu da máscara");
            assertTrue(line.size() <= mask.size(), scenario.name());
        }
    }

    @Test
    @DisplayName("afinar não parte a rua em pedaços")
    void thinningKeepsTheNetworkInOnePiece() {
        for (Scenario scenario : scenarios()) {
            Map<Long, Integer> cells = heights(scenario.cells(), scenario.terrain());
            Map<Long, Integer> mask = RoadMask.closed(cells, scenario.terrain());
            Set<Long> line = RoadThinning.skeleton(mask.keySet(), Set.of());

            assertTrue(pieces(line) <= pieces(mask.keySet()), scenario.name() + ": afinar partiu a rua");
        }
    }

    /** Quantos pedaços, contando a diagonal: a linha do meio anda nela. */
    private static int pieces(Set<Long> columns) {
        Set<Long> left = new HashSet<>(columns);
        int pieces = 0;

        while (!left.isEmpty()) {
            List<Long> open = new ArrayList<>(List.of(left.iterator().next()));

            left.remove(open.getFirst());
            pieces++;

            while (!open.isEmpty()) {
                long at = open.removeLast();

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        long next = column(ColonyRoads.xOf(at) + dx, ColonyRoads.zOf(at) + dz);

                        if (left.remove(next)) {
                            open.add(next);
                        }
                    }
                }
            }
        }

        return pieces;
    }

    /** O grafo oscilava a cada releitura: a ordem dos nós vinha da identidade do objeto. */
    @Test
    @DisplayName("a mesma rua dá o mesmo traçado em toda leitura")
    void readingTwiceGivesTheSameGeometry() {
        Map<String, String> first = measure();

        for (int read = 0; read < 4; read++) {
            assertEquals(first, measure(), "o traçado mudou sem a rua mudar");
        }
    }

    @Test
    @DisplayName("frente de porta fica na linha do meio")
    void aDoorIsKeptOnTheLine() {
        Set<Long> cells = new HashSet<>();

        rect(cells, 0, 0, 30, 4);

        long door = column(7, 0);
        Set<Long> line = RoadThinning.skeleton(cells, Set.of(door));

        assertTrue(line.contains(door), "a linha do meio apagou a frente de porta");
    }

    @Test
    @DisplayName("nó e trecho contam o que a linha tem")
    void theChainsCoverTheLine() {
        Set<Long> line = new HashSet<>();

        rect(line, 0, 10, 20, 10);
        rect(line, 10, 0, 10, 20);

        RoadChains.Net net = RoadChains.of(line, Set.of());

        assertEquals(5, net.nodes().size(), "uma cruz tem quatro pontas e um cruzamento: " + ofChains(net));
        assertEquals(4, net.chains().size(), "uma cruz tem quatro trechos: " + ofChains(net));
        assertTrue(RoadThinning.branches(line, column(10, 10)) >= 3, "o cruzamento não se viu cruzamento");
        assertEquals(1, RoadThinning.branches(line, column(0, 10)), "a ponta tem um vizinho");
        assertEquals(2, RoadThinning.neighbours(line, column(5, 10)).size());
    }
}
