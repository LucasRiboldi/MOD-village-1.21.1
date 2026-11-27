package com.villagecolony.core.construction.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

/**
 * O plano de alturas da vila — ADR-063 (autor, 2026-11-25, G5 do jogo da 0.4.136): a altura-alvo de cada coluna, calculada
 * de uma vez, para todo aldeão andar pela vila inteira com degrau de no máximo 1 e chegar a toda estrutura. Puro e
 * determinístico, sem Minecraft; quem lê o mundo e quem executa ficam no {@code fabric/}.
 *
 * <ol>
 *   <li><b>Envelope das âncoras:</b> em cada coluna, {@code L = max(âncora − passos)} e {@code U = min(âncora + passos)},
 *   com os passos contornando o que fica fora do plano. Onde {@code L > U}, duas âncoras estão mais longe em altura do que
 *   em passos: é um {@link Conflict}, para a rampa ou a escada.</li>
 *   <li><b>Calombo e buraco pequenos:</b> o grupo de até {@link VillageSurface#SMALL_BUMP} colunas que se destaca do
 *   terreno em volta, para cima ou para baixo, vai ao nível em volta (Regra 323: nada de buraco ou bloco isolado).</li>
 *   <li><b>Partida:</b> a coluna livre fica nesse chão se ele cabe no envelope; senão vai à borda dele.</li>
 *   <li><b>Degrau 1:</b> a média entre o maior relevo de degrau 1 abaixo da partida (o corte) e o menor acima dela (o
 *   aterro). Os dois têm degrau 1, então a média também tem.</li>
 *   <li><b>Corte mínimo:</b> partindo da média, cada coluna anda até o chão sem sair de vizinha mais alta − 1 e mais
 *   baixa + 1 nem do envelope; o paredão vira encosta e o chão que já anda de 1 em 1 fica onde está.</li>
 *   <li><b>Caminho plano:</b> a coluna de caminho toma a altura da vizinha de caminho ou âncora quando isso não abre
 *   degrau maior que 1 com nenhuma vizinha e cabe no envelope. O caminho só sobe onde precisa.</li>
 * </ol>
 */
public final class GradePlan {

    /** O papel de cada coluna. */
    public enum Kind {
        /** Chão que o plano molda. */
        FREE,
        /** Coluna do grafo: moldada, e plana onde couber. */
        PATH,
        /** Altura que não se discute: frente de porta, faixa das estruturas, borda, margem da água. */
        ANCHOR,
        /** Fora do plano e da contagem de passos: pegada de estrutura, obra aberta, boca da mina, chão do jogador. */
        OUT
    }

    /** Coluna onde as âncoras não cabem em degrau 1: precisa de pelo menos {@code low} e cabe no máximo em {@code high}. */
    public record Conflict(int x, int z, int low, int high) {
    }

    /** O plano calculado. */
    public static final class Result {
        private final int[][] target;
        private final boolean[][] small;
        private final List<Conflict> conflicts;
        private final int pending;

        Result(int[][] target, boolean[][] small, List<Conflict> conflicts, int pending) {
            this.target = target;
            this.small = small;
            this.conflicts = List.copyOf(conflicts);
            this.pending = pending;
        }

        /** Se a coluna é de um calombo ou buraco pequeno: o Zelador começa por eles, depois das âncoras e do caminho. */
        public boolean isSmallFeature(int x, int z) {
            return small[x][z];
        }

        /** A altura-alvo de cada coluna ({@code [x][z]}); a de fora do plano é a natural. */
        public int[][] target() {
            int[][] copy = new int[target.length][];

            for (int x = 0; x < target.length; x++) {
                copy[x] = target[x].clone();
            }

            return copy;
        }

        public List<Conflict> conflicts() {
            return conflicts;
        }

        /** Quantas colunas do plano estão fora do alvo. */
        public int pending() {
            return pending;
        }
    }

    /** Quantas passadas o caminho plano faz, no máximo. */
    static final int PATH_PASSES = 8;

    /** Quantas passadas o corte mínimo faz, no máximo. */
    static final int DESCENT_PASSES = 64;

    private static final int FAR = 1 << 24;

    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final int width;
    private final int depth;
    private final int[][] natural;
    private final Kind[][] kinds;
    private final int[][] anchors;

    public GradePlan(int width, int depth) {
        if (width < 1 || depth < 1) {
            throw new IllegalArgumentException("Empty grid: " + width + "x" + depth);
        }

        this.width = width;
        this.depth = depth;
        natural = new int[width][depth];
        kinds = new Kind[width][depth];
        anchors = new int[width][depth];

        for (Kind[] row : kinds) {
            Arrays.fill(row, Kind.FREE);
        }
    }

    /** A altura natural da coluna: o {@code y} do chão de cima. */
    public GradePlan ground(int x, int z, int y) {
        natural[x][z] = y;
        return this;
    }

    public GradePlan anchor(int x, int z, int y) {
        kinds[x][z] = Kind.ANCHOR;
        anchors[x][z] = y;
        return this;
    }

    public GradePlan path(int x, int z) {
        kinds[x][z] = Kind.PATH;
        return this;
    }

    public GradePlan out(int x, int z) {
        kinds[x][z] = Kind.OUT;
        return this;
    }

    public Result plan() {
        int[][] base = withoutSmallFeatures();
        int[][] low = spread(seedAnchors(-FAR), -1);
        int[][] high = spread(seedAnchors(FAR), 1);
        List<Conflict> conflicts = new ArrayList<>();
        int[][] start = new int[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                start[x][z] = startOf(x, z, base[x][z], low[x][z], high[x][z], conflicts);
            }
        }

        int[][] cut = spread(start, 1);
        int[][] fill = spread(start, -1);
        int[][] target = new int[width][depth];
        boolean[][] small = new boolean[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                small[x][z] = base[x][z] != natural[x][z];
                target[x][z] = switch (kinds[x][z]) {
                    case OUT -> natural[x][z];
                    case ANCHOR -> anchors[x][z];
                    default -> Math.floorDiv(cut[x][z] + fill[x][z], 2);
                };
            }
        }

        descend(target, base, low, high);
        flattenPaths(target, low, high);

        return new Result(target, small, conflicts, pending(target));
    }

    /** O chão natural com os calombos e buracos de até {@link VillageSurface#SMALL_BUMP} colunas no nível em volta. */
    private int[][] withoutSmallFeatures() {
        VillageSurface.Kind[][] roles = new VillageSurface.Kind[width][depth];
        int[][] ground = new int[width][depth];
        int[][] sunk = new int[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                roles[x][z] = switch (kinds[x][z]) {
                    case OUT -> VillageSurface.Kind.SKIP;
                    case ANCHOR -> VillageSurface.Kind.FIXED;
                    default -> VillageSurface.Kind.FREE;
                };
                ground[x][z] = kinds[x][z] == Kind.ANCHOR ? anchors[x][z] : natural[x][z];
            }
        }

        int[][] cut = VillageSurface.cutSmallBumps(ground, roles);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                sunk[x][z] = -cut[x][z];
            }
        }

        int[][] filled = VillageSurface.cutSmallBumps(sunk, roles);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                filled[x][z] = kinds[x][z] == Kind.OUT ? natural[x][z] : -filled[x][z];
            }
        }

        keepWhatLeansOnTheOut(filled, ground);

        return filled;
    }

    /**
     * O grupo corrigido que encosta no que fica fora do plano volta ao chão: a coluna entre a pegada de uma casa e a
     * encosta não é calombo, só não se vê o chão do outro lado.
     */
    private void keepWhatLeansOnTheOut(int[][] corrected, int[][] ground) {
        boolean[][] seen = new boolean[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                if (seen[x][z] || corrected[x][z] == ground[x][z]) {
                    continue;
                }

                List<int[]> group = new ArrayList<>();
                Deque<int[]> open = new ArrayDeque<>();
                boolean leans = false;

                seen[x][z] = true;
                open.add(new int[] {x, z});

                while (!open.isEmpty()) {
                    int[] at = open.poll();
                    group.add(at);

                    for (int[] side : SIDES) {
                        int nx = at[0] + side[0];
                        int nz = at[1] + side[1];

                        if (nx < 0 || nz < 0 || nx >= width || nz >= depth) {
                            continue;
                        }
                        if (kinds[nx][nz] == Kind.OUT) {
                            leans = true;
                        } else if (!seen[nx][nz] && corrected[nx][nz] != ground[nx][nz]) {
                            seen[nx][nz] = true;
                            open.add(new int[] {nx, nz});
                        }
                    }
                }

                if (leans) {
                    for (int[] at : group) {
                        corrected[at[0]][at[1]] = ground[at[0]][at[1]];
                    }
                }
            }
        }
    }

    /** O corte mínimo: cada coluna anda até o chão sem abrir degrau maior que 1 nem sair do envelope. */
    private void descend(int[][] target, int[][] base, int[][] low, int[][] high) {
        for (int pass = 0; pass < DESCENT_PASSES; pass++) {
            boolean changed = false;

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    if (kinds[x][z] == Kind.OUT || kinds[x][z] == Kind.ANCHOR || low[x][z] > high[x][z]) {
                        continue;
                    }

                    int floor = low[x][z];
                    int ceiling = high[x][z];

                    for (int[] side : SIDES) {
                        int nx = x + side[0];
                        int nz = z + side[1];

                        if (nx >= 0 && nz >= 0 && nx < width && nz < depth && kinds[nx][nz] != Kind.OUT) {
                            floor = Math.max(floor, target[nx][nz] - 1);
                            ceiling = Math.min(ceiling, target[nx][nz] + 1);
                        }
                    }

                    int want = Math.max(floor, Math.min(ceiling, base[x][z]));

                    if (floor <= ceiling && want != target[x][z]) {
                        target[x][z] = want;
                        changed = true;
                    }
                }
            }

            if (!changed) {
                return;
            }
        }
    }

    private int startOf(int x, int z, int ground, int low, int high, List<Conflict> conflicts) {
        if (kinds[x][z] == Kind.OUT) {
            return natural[x][z];
        }

        if (kinds[x][z] == Kind.ANCHOR) {
            return anchors[x][z];
        }

        // Só a coluna que o plano molda: duas âncoras vizinhas fora do degrau 1 não têm onde pôr rampa.
        if (low > high) {
            conflicts.add(new Conflict(x, z, low, high));
        }

        if (low > high) {
            return Math.floorDiv(low + high, 2);
        }

        return Math.max(low, Math.min(high, ground));
    }

    /** A altura das âncoras, e {@code empty} no resto. */
    private int[][] seedAnchors(int empty) {
        int[][] seed = new int[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                seed[x][z] = kinds[x][z] == Kind.ANCHOR ? anchors[x][z] : empty;
            }
        }

        return seed;
    }

    /**
     * Espalha {@code seed} pelos passos dentro do plano: com {@code step = 1}, o menor de {@code seed + passos} (o maior
     * relevo de degrau 1 abaixo da semente); com {@code step = -1}, o maior de {@code seed − passos}. Dijkstra de passo
     * unitário, que contorna o que fica fora.
     */
    private int[][] spread(int[][] seed, int step) {
        int[][] value = new int[width][depth];
        PriorityQueue<int[]> open = new PriorityQueue<>((a, b) -> step > 0 ? Integer.compare(a[2], b[2])
                : Integer.compare(b[2], a[2]));

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                value[x][z] = seed[x][z];

                if (kinds[x][z] != Kind.OUT && Math.abs(seed[x][z]) < FAR) {
                    open.add(new int[] {x, z, seed[x][z]});
                }
            }
        }

        while (!open.isEmpty()) {
            int[] cell = open.poll();

            if (cell[2] != value[cell[0]][cell[1]]) {
                continue;
            }

            for (int[] side : SIDES) {
                int x = cell[0] + side[0];
                int z = cell[1] + side[1];

                if (x < 0 || z < 0 || x >= width || z >= depth || kinds[x][z] == Kind.OUT) {
                    continue;
                }

                int next = cell[2] + step;

                if (step > 0 ? next < value[x][z] : next > value[x][z]) {
                    value[x][z] = next;
                    open.add(new int[] {x, z, next});
                }
            }
        }

        return value;
    }

    /** O caminho plano onde couber (ADR-063 §1): a partir das âncoras e do caminho, em ondas. */
    private void flattenPaths(int[][] target, int[][] low, int[][] high) {
        for (int pass = 0; pass < PATH_PASSES; pass++) {
            boolean changed = false;
            Deque<int[]> wave = new ArrayDeque<>();
            boolean[][] seen = new boolean[width][depth];

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    if (kinds[x][z] == Kind.ANCHOR) {
                        wave.add(new int[] {x, z});
                        seen[x][z] = true;
                    }
                }
            }

            while (!wave.isEmpty()) {
                int[] from = wave.poll();

                for (int[] side : SIDES) {
                    int x = from[0] + side[0];
                    int z = from[1] + side[1];

                    if (x < 0 || z < 0 || x >= width || z >= depth || seen[x][z] || kinds[x][z] != Kind.PATH) {
                        continue;
                    }

                    seen[x][z] = true;
                    wave.add(new int[] {x, z});

                    int level = target[from[0]][from[1]];

                    if (target[x][z] != level && fits(target, x, z, level, low[x][z], high[x][z])) {
                        target[x][z] = level;
                        changed = true;
                    }
                }
            }

            if (!changed) {
                return;
            }
        }
    }

    private boolean fits(int[][] target, int x, int z, int level, int low, int high) {
        if (low <= high && (level < low || level > high)) {
            return false;
        }

        for (int[] side : SIDES) {
            int nx = x + side[0];
            int nz = z + side[1];

            if (nx >= 0 && nz >= 0 && nx < width && nz < depth && kinds[nx][nz] != Kind.OUT
                    && Math.abs(target[nx][nz] - level) > 1) {
                return false;
            }
        }

        return true;
    }

    private int pending(int[][] target) {
        int pending = 0;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                if (kinds[x][z] != Kind.OUT && target[x][z] != natural[x][z]) {
                    pending++;
                }
            }
        }

        return pending;
    }
}
