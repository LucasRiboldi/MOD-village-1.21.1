package com.villagecolony.core.construction.model;

/**
 * O chão da vila e em volta dela, na geração — autor, 2026-10-29, depois do jogo da 0.3.108 ("quebrou uma montanha
 * no meio e deixou partes voando"; escolhas: mistura suave larga, faixa de 4 vezes o desnível de {@link #MIN_WIDTH}
 * a {@link #MAX_WIDTH}). Estudo: {@code docs/research/2026-10-29-solo-fora-da-vila.md}.
 *
 * <ul>
 *   <li><b>Na vila</b> (o miolo): a superfície de degrau 1 ({@link VillageSurface}) — buraco tampado, calombo
 *   cortado, casa no próprio piso.</li>
 *   <li><b>Em volta</b>: {@code (1 − w)·natural + w·nível da vila}, com o nível da vila tirado da coluna do miolo
 *   mais perto. O peso {@code w} cai em curva suave (smoothstep) de 1, colado na vila, a 0, à distância de 4 vezes o
 *   desnível daquela coluna; e o campo de pesos passa por um desfoque gaussiano, para a faixa não mudar de largura
 *   aos saltos. O relevo natural continua lá, só achatado na proporção do peso — a montanha vira encosta larga,
 *   não escada nem paredão.</li>
 *   <li><b>Água</b> não se move e a margem dela fica natural ({@link #WATER_FADE}); o que a vila não alcança na
 *   geração fica natural, e a mistura amacia até essa beira em {@link #EDGE_FADE} blocos.</li>
 *   <li>A conta parte do <b>topo de verdade</b> de cada coluna ({@link Shape#target}): a altura natural da grade é
 *   lida de longe em longe pelo gerador e só serve à vila e à largura da faixa.</li>
 * </ul>
 *
 * <p>Puro e determinístico: a mesma vila dá a mesma conta em todos os chunks dela.
 */
public final class VillageBlend {

    /** A faixa mais estreita em volta da vila. */
    public static final int MIN_WIDTH = 16;

    /** A faixa mais larga em volta da vila: o quanto a geração alcança além das peças. */
    public static final int MAX_WIDTH = 64;

    /** Blocos de faixa por bloco de desnível. */
    static final int PER_STEP = 4;

    /** Em quantos blocos a mistura amacia até a beira do que a vila alcança. */
    static final int EDGE_FADE = 8;

    /** Em quantos blocos a mistura amacia até a água: a margem natural fica. */
    static final int WATER_FADE = 8;

    /** O raio do desfoque gaussiano do campo de pesos. */
    static final int BLUR = 2;

    private static final double[][] KERNEL = kernel();

    /** Um retângulo de colunas, inclusivo, nas coordenadas da grade. */
    public record Area(int minX, int minZ, int maxX, int maxZ) {

        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        /** A distância em linha reta da coluna até o retângulo; zero dentro dele. */
        double distance(int x, int z) {
            int dx = Math.max(0, Math.max(minX - x, x - maxX));
            int dz = Math.max(0, Math.max(minZ - z, z - maxZ));

            return Math.sqrt((double) dx * dx + (double) dz * dz);
        }

        /** Quantas colunas a coluna de dentro está da beira do retângulo. */
        int inset(int x, int z) {
            return Math.min(Math.min(x - minX, maxX - x), Math.min(z - minZ, maxZ - z));
        }
    }

    private VillageBlend() {
    }

    /** A conta de uma vila: a superfície do miolo e o peso de cada coluna em volta. */
    public static final class Shape {
        private final Area core;
        private final int[][] village;
        private final double[][] weight;

        Shape(Area core, int[][] village, double[][] weight) {
            this.core = core;
            this.village = village;
            this.weight = weight;
        }

        /** Se a coluna muda: o miolo, ou em volta com peso. */
        public boolean shapes(int x, int z) {
            return core.contains(x, z) || weight[x][z] > 0;
        }

        /**
         * A altura nova da coluna, partindo do topo de verdade: no miolo, a superfície de degrau 1; em volta,
         * {@code (1 − w)·topo + w·nível da vila} — colada na vila encontra o nível dela, longe não muda.
         */
        public int target(int x, int z, int top) {
            int nearX = Math.max(core.minX(), Math.min(core.maxX(), x));
            int nearZ = Math.max(core.minZ(), Math.min(core.maxZ(), z));
            int level = village[nearX - core.minX()][nearZ - core.minZ()];

            if (core.contains(x, z)) {
                return level;
            }

            return (int) Math.round((1 - weight[x][z]) * top + weight[x][z] * level);
        }
    }

    /**
     * A conta da vila sobre a altura natural {@code natural} ({@code [x][z]}).
     *
     * @param core a vila — o miolo de degrau 1, dentro da grade
     * @param reach o que a geração alcança; fora dele nada muda
     */
    public static Shape shape(int[][] natural, VillageSurface.Kind[][] kinds, Area core, Area reach) {
        int width = natural.length;
        int depth = natural[0].length;
        int[][] village = coreSurface(natural, kinds, core);
        int[][] water = waterDistance(kinds);
        double[][] raw = new double[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                raw[x][z] = core.contains(x, z) ? 1
                        : reach.contains(x, z) ? rawWeight(natural, village, core, reach, x, z)
                                * smoothstep((double) water[x][z] / WATER_FADE) : 0;
            }
        }

        double[][] weight = new double[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean free = reach.contains(x, z) && kinds[x][z] != VillageSurface.Kind.WATER;
                weight[x][z] = core.contains(x, z) ? 1 : free ? blurred(raw, x, z) : 0;
            }
        }

        return new Shape(core, village, weight);
    }

    /** Quanto cada coluna sobe (positivo) ou desce, se o topo de verdade for o natural. */
    public static int[][] offsets(int[][] natural, VillageSurface.Kind[][] kinds, Area core, Area reach) {
        Shape shape = shape(natural, kinds, core, reach);
        int[][] offsets = new int[natural.length][natural[0].length];

        for (int x = 0; x < natural.length; x++) {
            for (int z = 0; z < natural[0].length; z++) {
                offsets[x][z] = shape.target(x, z, natural[x][z]) - natural[x][z];
            }
        }

        return offsets;
    }

    /** A distância, em colunas (8 vizinhas), até a água mais perto; longe sem água, a largura da grade. */
    static int[][] waterDistance(VillageSurface.Kind[][] kinds) {
        int width = kinds.length;
        int depth = kinds[0].length;
        int far = width + depth;
        int[][] distance = new int[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                distance[x][z] = kinds[x][z] == VillageSurface.Kind.WATER ? 0 : far;
            }
        }

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                for (int dx = -1; dx <= 0; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if ((dx < 0 || dz < 0) && x + dx >= 0 && z + dz >= 0 && z + dz < depth) {
                            distance[x][z] = Math.min(distance[x][z], distance[x + dx][z + dz] + 1);
                        }
                    }
                }
            }
        }

        for (int x = width - 1; x >= 0; x--) {
            for (int z = depth - 1; z >= 0; z--) {
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if ((dx > 0 || dz > 0) && x + dx < width && z + dz >= 0 && z + dz < depth) {
                            distance[x][z] = Math.min(distance[x][z], distance[x + dx][z + dz] + 1);
                        }
                    }
                }
            }
        }

        return distance;
    }

    /** A superfície de degrau 1 do miolo, nas coordenadas do miolo. */
    private static int[][] coreSurface(int[][] natural, VillageSurface.Kind[][] kinds, Area core) {
        int width = core.maxX() - core.minX() + 1;
        int depth = core.maxZ() - core.minZ() + 1;
        int[][] subNatural = new int[width][depth];
        VillageSurface.Kind[][] subKinds = new VillageSurface.Kind[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                subNatural[x][z] = natural[core.minX() + x][core.minZ() + z];
                subKinds[x][z] = kinds[core.minX() + x][core.minZ() + z];
            }
        }

        return VillageSurface.smooth(subNatural, subKinds);
    }

    /** O nível da vila na coluna do miolo mais perto, menos o natural desta coluna. */
    private static int delta(int[][] natural, int[][] village, Area core, int x, int z) {
        int nearX = Math.max(core.minX(), Math.min(core.maxX(), x));
        int nearZ = Math.max(core.minZ(), Math.min(core.maxZ(), z));

        return village[nearX - core.minX()][nearZ - core.minZ()] - natural[x][z];
    }

    /** O peso desta coluna antes do desfoque: a curva da faixa vezes o amaciamento até a beira do alcance. */
    private static double rawWeight(int[][] natural, int[][] village, Area core, Area reach, int x, int z) {
        int band = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, PER_STEP * Math.abs(delta(natural, village, core, x, z))));

        return (1 - smoothstep(core.distance(x, z) / band)) * smoothstep((double) reach.inset(x, z) / EDGE_FADE);
    }

    /** A curva suave de 0 a 1, achatada nas duas pontas. */
    static double smoothstep(double t) {
        double clamped = Math.max(0, Math.min(1, t));

        return clamped * clamped * (3 - 2 * clamped);
    }

    /** O peso da coluna depois do desfoque gaussiano, renormalizado onde a grade acaba. */
    private static double blurred(double[][] weight, int x, int z) {
        double sum = 0;
        double total = 0;

        for (int dx = -BLUR; dx <= BLUR; dx++) {
            for (int dz = -BLUR; dz <= BLUR; dz++) {
                int nx = x + dx;
                int nz = z + dz;

                if (nx >= 0 && nz >= 0 && nx < weight.length && nz < weight[0].length) {
                    double k = KERNEL[dx + BLUR][dz + BLUR];
                    sum += k * weight[nx][nz];
                    total += k;
                }
            }
        }

        return sum / total;
    }

    private static double[][] kernel() {
        double[][] kernel = new double[2 * BLUR + 1][2 * BLUR + 1];

        for (int dx = -BLUR; dx <= BLUR; dx++) {
            for (int dz = -BLUR; dz <= BLUR; dz++) {
                kernel[dx + BLUR][dz + BLUR] = Math.exp(-(dx * dx + dz * dz) / 2.0);
            }
        }

        return kernel;
    }
}
