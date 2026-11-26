package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.GradePlan;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineMouths;
import com.villagecolony.fabric.integration.PlayerGround;
import com.villagecolony.fabric.integration.StreetSurface;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A leitura do mundo para o plano de alturas — ADR-063, fase 2. Monta o {@link GradePlan} da caixa da vila e diz o que
 * ele mexeria, sem pôr nem tirar bloco nenhum. Fora da caixa nada muda nem puxa o plano: a ribanceira ao lado da vila não
 * a arrasta para baixo.
 *
 * <ul>
 *   <li><b>fora:</b> a pegada das construções da colônia e das peças da vila gerada, a folga da boca da mina, o chão do
 *   jogador, a água e o que não é chão (árvore, telhado);</li>
 *   <li><b>âncora:</b> a frente de cada porta, na altura do piso dela, e a faixa de {@value #RING} bloco em volta de
 *   cada estrutura ({@value #LARGE_PEN_RING} no curral grande), na altura da entrada dela;</li>
 *   <li><b>caminho:</b> o bloco de caminho e o material de rua no topo da coluna.</li>
 * </ul>
 *
 * A linha diária {@code VC_GRADE} sai no fim do expediente, junto da {@code VC_TERRAIN} ({@link TerrainReport}).
 */
public final class GradeSurvey {

    /** A faixa em volta de cada estrutura. */
    static final int RING = 1;

    /** A faixa em volta do curral grande (autor, 2026-11-25). */
    static final int LARGE_PEN_RING = 2;

    /** O maior lado lido; a vila maior que isso fica sem plano e diz por quê. */
    static final int MAX_SIDE = 400;

    private static final String LARGE_PEN = "colony/large_animal_pen";

    private GradeSurvey() {
    }

    /** O que a leitura achou e o plano que sai dela. */
    public static final class Survey {
        private final int minX;
        private final int minZ;
        private final GradePlan.Kind[][] kinds;
        private final int[][] anchors;
        private final int[][] natural;
        private final boolean[][] fronts;
        private final GradePlan.Result result;
        private final int doors;
        private final long millis;

        Survey(int minX, int minZ, GradePlan.Kind[][] kinds, int[][] anchors, int[][] natural, boolean[][] fronts,
               GradePlan.Result result, int doors, long millis) {
            this.minX = minX;
            this.minZ = minZ;
            this.kinds = kinds;
            this.anchors = anchors;
            this.natural = natural;
            this.fronts = fronts;
            this.result = result;
            this.doors = doors;
            this.millis = millis;
        }

        /**
         * As colunas fora do alvo, na ordem do Zelador (ADR-063 §3): frente de porta, caminho, faixa das estruturas,
         * calombo e buraco pequenos, o resto. Cada item é {x, z, alvo, ordem}.
         */
        public List<int[]> work() {
            int[][] target = result.target();
            List<int[]> work = new ArrayList<>();

            for (int x = 0; x < kinds.length; x++) {
                for (int z = 0; z < kinds[0].length; z++) {
                    if (kinds[x][z] != GradePlan.Kind.OUT && target[x][z] != natural[x][z]) {
                        work.add(new int[] {minX + x, minZ + z, target[x][z], rank(x, z)});
                    }
                }
            }

            work.sort(java.util.Comparator.comparingInt((int[] item) -> item[3]));

            return work;
        }

        private int rank(int x, int z) {
            if (fronts[x][z]) {
                return 0;
            }

            return switch (kinds[x][z]) {
                case PATH -> 1;
                case ANCHOR -> 2;
                default -> result.isSmallFeature(x, z) ? 3 : 4;
            };
        }

        /** A altura-alvo de cada coluna da caixa, {@code [x - minX][z - minZ]} (ADR-064: a foto do terreno). */
        public int[][] targets() {
            return result.target();
        }

        public int minX() {
            return minX;
        }

        public int minZ() {
            return minZ;
        }

        public GradePlan.Kind kind(int x, int z) {
            return inside(x, z) ? kinds[x - minX][z - minZ] : GradePlan.Kind.OUT;
        }

        public int anchor(int x, int z) {
            return inside(x, z) ? anchors[x - minX][z - minZ] : 0;
        }

        public int pending() {
            return result.pending();
        }

        public int doors() {
            return doors;
        }

        public List<GradePlan.Conflict> conflicts() {
            return result.conflicts();
        }

        /** Pares de vizinhas dentro do plano com degrau maior que 1 no chão de hoje. */
        public int steps() {
            int steps = 0;

            for (int x = 0; x < kinds.length; x++) {
                for (int z = 0; z < kinds[0].length; z++) {
                    if (kinds[x][z] == GradePlan.Kind.OUT) {
                        continue;
                    }
                    if (x + 1 < kinds.length && kinds[x + 1][z] != GradePlan.Kind.OUT
                            && Math.abs(natural[x + 1][z] - natural[x][z]) > 1) {
                        steps++;
                    }
                    if (z + 1 < kinds[0].length && kinds[x][z + 1] != GradePlan.Kind.OUT
                            && Math.abs(natural[x][z + 1] - natural[x][z]) > 1) {
                        steps++;
                    }
                }
            }

            return steps;
        }

        /** A linha diária {@code VC_GRADE}. */
        public String line(UUID colonyId, long day) {
            java.util.Map<GradePlan.Kind, Integer> count = new java.util.EnumMap<>(GradePlan.Kind.class);

            for (GradePlan.Kind[] row : kinds) {
                for (GradePlan.Kind kind : row) {
                    count.merge(kind, 1, Integer::sum);
                }
            }

            return String.format(java.util.Locale.ROOT,
                    "VC_GRADE version=1 colony=%s day=%d columns=%d anchors=%d doors=%d out=%d path=%d steps=%d "
                            + "pending=%d conflicts=%d ms=%d",
                    colonyId.toString().substring(0, 8), day, kinds.length * kinds[0].length,
                    count.getOrDefault(GradePlan.Kind.ANCHOR, 0), doors, count.getOrDefault(GradePlan.Kind.OUT, 0),
                    count.getOrDefault(GradePlan.Kind.PATH, 0), steps(), pending(), conflicts().size(), millis);
        }

        private boolean inside(int x, int z) {
            return x >= minX && z >= minZ && x - minX < kinds.length && z - minZ < kinds[0].length;
        }
    }

    /** A leitura desta colônia; vazia sem caixa ou com a caixa maior que {@value #MAX_SIDE}. */
    public static Optional<Survey> read(ServerWorld world, Colony colony) {
        Optional<VillageBounds> bounds = colony.bounds();

        if (bounds.isEmpty()) {
            return Optional.empty();
        }

        long began = System.nanoTime();
        VillageBounds box = bounds.get();
        int minX = box.minX();
        int minZ = box.minZ();
        int width = box.maxX() - minX + 1;
        int depth = box.maxZ() - minZ + 1;

        if (width > MAX_SIDE || depth > MAX_SIDE) {
            VillageColonyMod.LOGGER.info("VC_GRADE version=1 colony={} skipped=too_large width={} depth={}",
                    colony.id().toString().substring(0, 8), width, depth);
            return Optional.empty();
        }

        GradePlan.Kind[][] kinds = new GradePlan.Kind[width][depth];
        int[][] anchors = new int[width][depth];
        int[][] natural = new int[width][depth];
        List<Structure> structures = structuresOf(world, colony);

        readColumns(world, colony, minX, minZ, kinds, natural, structures);

        int doors = 0;
        boolean[][] fronts = new boolean[width][depth];

        for (Structure structure : structures) {
            doors += anchorStructure(world, structure, minX, minZ, kinds, anchors, natural, fronts);
        }

        GradeHoles.fill(kinds, anchors, natural, minX, minZ,
                structures.stream().map(Structure::box).toList());

        GradePlan plan = new GradePlan(width, depth);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                plan.ground(x, z, natural[x][z]);

                switch (kinds[x][z]) {
                    case ANCHOR -> plan.anchor(x, z, anchors[x][z]);
                    // O caminho fica na altura em que está: o Zelador só mexe no chão natural (Regra 364).
                    case PATH -> plan.anchor(x, z, natural[x][z]);
                    case OUT -> plan.out(x, z);
                    default -> { }
                }
            }
        }

        return Optional.of(new Survey(minX, minZ, kinds, anchors, natural, fronts, plan.plan(), doors,
                (System.nanoTime() - began) / 1_000_000));
    }

    /** Uma estrutura do plano: a caixa, se é da colônia e se é o curral grande. */
    private record Structure(BlockBox box, boolean colonyBuilt, boolean largePen, boolean open) {

        Structure(BlockBox box, boolean colonyBuilt, boolean largePen) {
            this(box, colonyBuilt, largePen, false);
        }
    }

    private static List<Structure> structuresOf(ServerWorld world, Colony colony) {
        List<Structure> structures = new ArrayList<>();
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            structures.add(new Structure(BlockBox.create(MinecraftTypeAdapter.toBlockPos(building.min()),
                    MinecraftTypeAdapter.toBlockPos(building.max())), true, building.blueprint().path().equals(LARGE_PEN)));
        }

        VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).ifPresent(open -> structures.add(new Structure(BlockBox.create(
                MinecraftTypeAdapter.toBlockPos(open.origin()),
                MinecraftTypeAdapter.toBlockPos(open.origin()).add(open.blueprint().size().x() - 1,
                        open.blueprint().size().y() - 1, open.blueprint().size().z() - 1)), true, false, true)));

        for (BlockBox piece : GeneratedVillages.structuresAt(world, hub.getX(), hub.getZ())) {
            structures.add(new Structure(piece, false, false));
        }

        return structures;
    }

    private static void readColumns(ServerWorld world, Colony colony, int minX, int minZ, GradePlan.Kind[][] kinds,
                                    int[][] natural, List<Structure> structures) {
        Optional<BlockPos> mine = VillageColonyMod.MINES.of(colony.id())
                .map(found -> MinecraftTypeAdapter.toBlockPos(found.shaft().entry()));

        for (int dx = 0; dx < kinds.length; dx++) {
            for (int dz = 0; dz < kinds[0].length; dz++) {
                int x = minX + dx;
                int z = minZ + dz;

                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    kinds[dx][dz] = GradePlan.Kind.OUT;
                    continue;
                }

                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos at = new BlockPos(x, top, z);
                BlockState state = world.getBlockState(at);

                natural[dx][dz] = top;
                kinds[dx][dz] = kindOf(world, colony.id(), at, state, structures, mine);
            }
        }
    }

    private static GradePlan.Kind kindOf(ServerWorld world, UUID colonyId, BlockPos at, BlockState state,
                                         List<Structure> structures, Optional<BlockPos> mine) {
        int x = at.getX();
        int z = at.getZ();
        boolean nearMine = mine.filter(entry -> Math.abs(entry.getX() - x) <= GroundSmoothing.MINE_GUARD
                && Math.abs(entry.getZ() - z) <= GroundSmoothing.MINE_GUARD).isPresent()
                || MineMouths.isNear(colonyId, x, z);

        if (!state.getFluidState().isEmpty() || nearMine || PlayerGround.isMarked(x, z)
                || structures.stream().anyMatch(structure -> covers(structure.box(), x, z, 0))) {
            return GradePlan.Kind.OUT;
        }

        if (VillageRoad.isPathBlock(state) || StreetSurface.isStreetMaterial(state)) {
            return GradePlan.Kind.PATH;
        }

        return GroundSmoothing.isGround(world, at, state) ? GradePlan.Kind.FREE : GradePlan.Kind.OUT;
    }

    /**
     * A faixa em volta da estrutura, na altura da entrada dela, e a frente de cada porta. A entrada é o piso mais baixo
     * das portas; sem porta, a base da construção da colônia pronta, ou a mediana do chão da faixa na peça da vila gerada
     * e na obra aberta, cuja base fica abaixo do chão com a fundação (Regra 368).
     *
     * @return quantas portas a estrutura tem
     */
    private static int anchorStructure(ServerWorld world, Structure structure, int minX, int minZ, GradePlan.Kind[][] kinds,
                                       int[][] anchors, int[][] natural, boolean[][] fronts) {
        BlockBox box = structure.box();
        List<DoorFronts.Entrance> entrances = DoorFronts.entrances(world, new BlockPos(box.getMinX(), box.getMinY(),
                box.getMinZ()), new BlockPos(box.getMaxX(), box.getMaxY(), box.getMaxZ()));
        int ring = structure.largePen() ? LARGE_PEN_RING : RING;
        List<int[]> band = new ArrayList<>();

        for (int x = box.getMinX() - ring; x <= box.getMaxX() + ring; x++) {
            for (int z = box.getMinZ() - ring; z <= box.getMaxZ() + ring; z++) {
                int dx = x - minX;
                int dz = z - minZ;

                if (!covers(box, x, z, 0) && dx >= 0 && dz >= 0 && dx < kinds.length && dz < kinds[0].length
                        && kinds[dx][dz] != GradePlan.Kind.OUT && kinds[dx][dz] != GradePlan.Kind.ANCHOR
                        && kinds[dx][dz] != GradePlan.Kind.PATH) {
                    band.add(new int[] {dx, dz});
                }
            }
        }

        int level = entrances.stream().mapToInt(entrance -> entrance.front().getY()).min()
                .orElseGet(() -> structure.colonyBuilt() && !structure.open() ? box.getMinY() : median(band, natural));

        for (int[] cell : band) {
            kinds[cell[0]][cell[1]] = GradePlan.Kind.ANCHOR;
            anchors[cell[0]][cell[1]] = level;
        }

        for (DoorFronts.Entrance entrance : entrances) {
            int dx = entrance.front().getX() - minX;
            int dz = entrance.front().getZ() - minZ;

            if (dx >= 0 && dz >= 0 && dx < kinds.length && dz < kinds[0].length
                    && kinds[dx][dz] != GradePlan.Kind.PATH) {
                kinds[dx][dz] = GradePlan.Kind.ANCHOR;
                anchors[dx][dz] = entrance.front().getY();
                fronts[dx][dz] = true;
            }
        }

        return entrances.size();
    }

    private static int median(List<int[]> band, int[][] natural) {
        int[] heights = band.stream().mapToInt(cell -> natural[cell[0]][cell[1]]).sorted().toArray();

        return heights.length == 0 ? 0 : heights[heights.length / 2];
    }

    private static boolean covers(BlockBox box, int x, int z, int grow) {
        return x >= box.getMinX() - grow && x <= box.getMaxX() + grow && z >= box.getMinZ() - grow
                && z <= box.getMaxZ() + grow;
    }
}
