package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.VillageBlend;
import com.villagecolony.core.construction.model.VillageSurface;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * O solo da vila na geração do chunk — autor, 2026-10-29. Roda no começo da colocação de estruturas e features de
 * cada chunk ({@code ChunkGenerator.generateFeatures}), com o terreno pronto e antes de as peças da vila serem
 * postas: as ruas do Minecraft seguem a altura de geração da coluna, que o corte leva junto (Regra 250), então nascem
 * sobre o chão novo.
 *
 * <p>A conta é do {@link VillageBlend}: na vila (as peças + {@link #CORE_PAD}), degrau de 1 e buraco tampado; em
 * volta, até {@link #REACH}, a mistura suave do topo de verdade com o nível da vila, numa faixa de 4 vezes o desnível. Calculada uma
 * vez por vila, com a planta inteira dela e a altura natural do gerador, e guardada: todos os chunks da vila veem a
 * mesma. Cada chunk só mexe nas próprias colunas — corta ou aterra com o bloco de cima e o de baixo da própria
 * coluna, tampa os vãos de caverna logo abaixo, e a árvore em pé sobre o chão cortado sai inteira. A água fora da
 * vila não é tocada; a de dentro vira chão, e a peça fixa sobre água ganha base até o piso (Regra 288).
 */
public final class VillageGround {

    static {
        ServerMemory.register(VillageGround.class, VillageGround::clearAll);
    }

    /** Até onde, além das peças, o chão da vila mistura com o terreno natural — e a vila referencia os chunks. */
    public static final int REACH = VillageBlend.MAX_WIDTH;

    /** O miolo de degrau 1: as peças e mais esta folga. */
    static final int CORE_PAD = 4;

    /** Até quantos chunks do início da vila o jogo procura a referência ({@code addStructureReferences}). */
    static final int REFERENCE_CHUNKS = 8;

    /**
     * De quantos em quantos blocos se lê a altura natural do gerador (o meio é interpolado). Cada leitura custa o
     * ruído de uma coluna inteira: com 4, uma vila da taiga levou 20 s; a conta em volta parte do topo de verdade.
     */
    static final int SAMPLE = 8;

    /** Quantos blocos acima do mapa de altura a varredura do topo começa. */
    static final int SCAN_ABOVE = 16;

    /** Quantos blocos abaixo do chão novo os vãos de caverna são tampados. */
    static final int CAP = 8;

    /** Quantos blocos de tronco e folha uma árvore tirada tem no máximo. */
    static final int TREE_BLOCKS = 600;

    /** O raio da árvore tirada em volta do tronco. */
    static final int TREE_RADIUS = 8;

    /** Vilas guardadas ao mesmo tempo; a geração vai e volta entre poucas. */
    static final int CACHED = 32;

    /** A conta de uma vila: o canto de menor x e z, o papel de cada coluna e a mistura ({@link VillageBlend.Shape}). */
    static final class Plan {
        private final int minX;
        private final int minZ;
        private final VillageSurface.Kind[][] kinds;
        private final boolean[][] drainWater;
        private final VillageBlend.Shape shape;
        private final Map<Long, Integer> doorFronts;

        Plan(int minX, int minZ, VillageSurface.Kind[][] kinds, boolean[][] drainWater, VillageBlend.Shape shape,
             Map<Long, Integer> doorFronts) {
            this.minX = minX;
            this.minZ = minZ;
            this.kinds = kinds;
            this.drainWater = drainWater;
            this.shape = shape;
            this.doorFronts = Map.copyOf(doorFronts);
        }

        /** O piso da porta, se a coluna é a frente de uma porta da vila (Regra 303). */
        Integer doorFloor(int x, int z) {
            return doorFronts.get(com.villagecolony.core.construction.model.ColonyRoads.column(x, z));
        }

        boolean covers(int x, int z) {
            return x >= minX && z >= minZ && x < minX + kinds.length && z < minZ + kinds[0].length;
        }

        /** Se a coluna muda: chão (nem casa, nem água) da vila, ou em volta com peso. */
        boolean shapes(int x, int z) {
            return covers(x, z) && kinds[x - minX][z - minZ] == VillageSurface.Kind.FREE
                    && shape.shapes(x - minX, z - minZ);
        }

        /** A altura nova da coluna, partindo do topo de verdade. */
        int targetFor(int x, int z, int top) {
            return shape.target(x - minX, z - minZ, top);
        }

        boolean drainsWater(int x, int z) {
            return drainWater[x - minX][z - minZ];
        }

        /** Se a coluna é de peça fixa (casa, lampião, enfeite), que ganha base quando está sobre água (Regra 288). */
        boolean founds(int x, int z) {
            return covers(x, z) && kinds[x - minX][z - minZ] == VillageSurface.Kind.FIXED;
        }
    }

    /** Regra 288: a água dentro da vila (as peças + {@link #CORE_PAD}) é chão a moldar; só a de fora fica. */
    static VillageSurface.Kind waterKind(boolean wet, boolean inCore) {
        return wet && !inCore ? VillageSurface.Kind.WATER : VillageSurface.Kind.FREE;
    }

    private static final Map<String, Plan> PLANS = new LinkedHashMap<>(CACHED, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Plan> eldest) {
            return size() > CACHED;
        }
    };

    private VillageGround() {
    }

    /**
     * A caixa com que a vila referencia os chunks: as peças mais {@link #REACH}, para a faixa em volta ser moldada;
     * a de qualquer outra estrutura fica a do jogo.
     */
    public static BlockBox referenceBox(StructureWorldAccess world, StructureStart start, BlockBox vanilla) {
        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

        if (!start.hasChildren() || !registry.getEntry(start.getStructure()).isIn(StructureTags.VILLAGE)) {
            return vanilla;
        }

        return encompass(vanilla, piecesOf(start).expand(REACH));
    }

    /** Molda as colunas deste chunk que ficam na área de uma vila. */
    public static void shape(StructureWorldAccess world, Chunk chunk, StructureAccessor structures,
                             ChunkGenerator generator) {
        ChunkPos pos = chunk.getPos();
        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

        for (StructureStart start : structures.getStructureStarts(pos,
                structure -> registry.getEntry(structure).isIn(StructureTags.VILLAGE))) {
            if (!start.hasChildren()) {
                continue;
            }

            Plan plan = planOf(world, start, generator);
            VillageSoil soil = VillageSoil.of(chunk);

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int x = pos.getStartX() + lx;
                    int z = pos.getStartZ() + lz;
                    boolean founds = plan.founds(x, z);
                    Integer door = plan.doorFloor(x, z);

                    // A frente da porta fica no piso dela, com o bloco de caminho (Regra 303).
                    if (door != null) {
                        DoorFronts.shape(world, chunk, x, z, topOf(chunk, x, z,
                                chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR_WG, x & 15, z & 15) + SCAN_ABOVE), door, soil);
                    } else if (plan.shapes(x, z) || founds) {
                        int top = topOf(chunk, x, z,
                                chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR_WG, x & 15, z & 15) + SCAN_ABOVE);

                        if (!founds) {
                            column(world, chunk, x, z, top, plan.targetFor(x, z, top), plan.drainsWater(x, z), soil);
                        } else if (VillageSoil.isWet(chunk, x, z, top)) {
                            soil.fill(chunk, x, z, top, plan.targetFor(x, z, top));
                        }
                    }
                }
            }
        }
    }

    /** A superfície da vila, calculada na primeira vez que um chunk dela é gerado. */
    static Plan planOf(StructureWorldAccess world, StructureStart start, ChunkGenerator generator) {
        String key = world.getSeed() + "/" + start.getPos() + "/" + start.getStructure().getType();

        synchronized (PLANS) {
            Plan plan = PLANS.get(key);

            if (plan == null) {
                plan = plan(world, start, generator);
                PLANS.put(key, plan);
            }

            return plan;
        }
    }

    private static Plan plan(StructureWorldAccess world, StructureStart start, ChunkGenerator generator) {
        long began = System.nanoTime();
        BlockBox pieces = piecesOf(start);
        int minX = pieces.getMinX() - REACH;
        int minZ = pieces.getMinZ() - REACH;
        int width = pieces.getMaxX() - pieces.getMinX() + 1 + 2 * REACH;
        int depth = pieces.getMaxZ() - pieces.getMinZ() + 1 + 2 * REACH;
        NoiseConfig noise = world.toServerWorld().getChunkManager().getNoiseConfig();

        int[][] natural = new int[width][depth];
        VillageSurface.Kind[][] kinds = new VillageSurface.Kind[width][depth];
        boolean[][] wet = new boolean[width][depth];
        boolean[][] drainWater = new boolean[width][depth];
        VillageBlend.Area core = new VillageBlend.Area(REACH - CORE_PAD, REACH - CORE_PAD,
                width - 1 - REACH + CORE_PAD, depth - 1 - REACH + CORE_PAD);
        int samplesX = (width - 1) / SAMPLE + 2;
        int samplesZ = (depth - 1) / SAMPLE + 2;
        int[][] ground = new int[samplesX][samplesZ];
        int[][] water = new int[samplesX][samplesZ];

        for (int i = 0; i < samplesX; i++) {
            for (int j = 0; j < samplesZ; j++) {
                int x = minX + i * SAMPLE;
                int z = minZ + j * SAMPLE;
                ground[i][j] = generator.getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, world, noise) - 1;
                water[i][j] = generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, noise) - 1;
            }
        }

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                int i = x / SAMPLE;
                int j = z / SAMPLE;
                double fx = (x % SAMPLE) / (double) SAMPLE;
                double fz = (z % SAMPLE) / (double) SAMPLE;
                int nearI = fx < 0.5 ? i : i + 1;
                int nearJ = fz < 0.5 ? j : j + 1;
                wet[x][z] = water[nearI][nearJ] > ground[nearI][nearJ];
            }
        }

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                int i = x / SAMPLE;
                int j = z / SAMPLE;
                double fx = (x % SAMPLE) / (double) SAMPLE;
                double fz = (z % SAMPLE) / (double) SAMPLE;
                double floor = ground[i][j] * (1 - fx) * (1 - fz) + ground[i + 1][j] * fx * (1 - fz)
                        + ground[i][j + 1] * (1 - fx) * fz + ground[i + 1][j + 1] * fx * fz;
                int nearI = fx < 0.5 ? i : i + 1;
                int nearJ = fz < 0.5 ? j : j + 1;
                boolean inCore = core.contains(x, z);

                // Regra 288: a água conta na altura da superfície dela; a de dentro da vila vira chão nessa altura.
                natural[x][z] = wet[x][z] ? water[nearI][nearJ] : (int) Math.round(floor);
                kinds[x][z] = waterKind(wet[x][z], inCore);
                drainWater[x][z] = inCore;
            }
        }

        // O jogo procura a referência a até 8 chunks do início da vila: além disso a geração não alcança.
        ChunkPos origin = start.getPos();
        VillageBlend.Area reach = new VillageBlend.Area(
                (origin.x - REFERENCE_CHUNKS) * 16 - minX, (origin.z - REFERENCE_CHUNKS) * 16 - minZ,
                (origin.x + REFERENCE_CHUNKS) * 16 + 15 - minX, (origin.z + REFERENCE_CHUNKS) * 16 + 15 - minZ);

        // As peças rígidas (as casas) descem ou sobem ao solo suavizado e viram o piso fixo (Regra 296).
        Map<Long, Integer> doorFronts = new java.util.HashMap<>();
        String placed = VillagePieces.settle(start, natural, kinds, core, reach, minX, minZ,
                world.toServerWorld().getStructureTemplateManager(), world.getRegistryManager(), doorFronts);
        VillageBlend.Shape shape = VillageBlend.shape(natural, kinds, core, reach);

        // A área moldada, o miolo, as peças fixas e quanto andaram, para medir o chão depois (scripts/ground_report.py).
        VillageColonyMod.LOGGER.info("VC_GROUND {\"minX\":{},\"minZ\":{},\"width\":{},\"depth\":{},\"reach\":{},"
                        + "\"core\":{},\"ms\":{},{}}", minX, minZ, width, depth, REACH, CORE_PAD,
                (System.nanoTime() - began) / 1_000_000, placed);

        return new Plan(minX, minZ, kinds, drainWater, shape, doorFronts);
    }

    /** A caixa de todas as peças da vila. */
    private static BlockBox piecesOf(StructureStart start) {
        BlockBox pieces = null;

        for (StructurePiece piece : start.getChildren()) {
            pieces = pieces == null ? piece.getBoundingBox() : encompass(pieces, piece.getBoundingBox());
        }

        return java.util.Objects.requireNonNull(pieces, "a vila sem peças não é moldada");
    }

    /** {@link #column(StructureWorldAccess, Chunk, int, int, int, int, boolean, VillageSoil)} com o solo do chunk. */
    static void column(StructureWorldAccess world, Chunk chunk, int x, int z, int top, int target, boolean drainWater) {
        column(world, chunk, x, z, top, target, drainWater, VillageSoil.of(chunk));
    }

    /**
     * Corta ou aterra a coluna de {@code top} até {@code target}, com o bloco de cima e o de baixo que a coluna já
     * tinha, e tampa os vãos logo abaixo. A coluna com água fica como está fora da vila e, dentro dela
     * ({@code drainWater}), vira chão ({@link VillageSoil#fill}, Regra 288). A árvore em pé sobre o chão cortado sai
     * inteira (autor, 2026-10-29).
     */
    static void column(StructureWorldAccess world, Chunk chunk, int x, int z, int top, int target, boolean drainWater,
                       VillageSoil soil) {
        BlockPos.Mutable at = new BlockPos.Mutable(x, top + 1, z);

        if (top <= chunk.getBottomY()) {
            return;
        }

        if (VillageSoil.isWet(chunk, x, z, top)) {
            if (drainWater) {
                soil.fill(chunk, x, z, top, target);
            }

            return;
        }

        BlockState surface = chunk.getBlockState(at.setY(top));
        BlockState below = chunk.getBlockState(at.setY(top - 1));
        BlockState fill = SiteSurroundings.isNaturalGround(below)
                && !(below.getBlock() instanceof net.minecraft.block.FallingBlock)
                ? below : Blocks.DIRT.getDefaultState();

        if (surface.isAir() || !surface.getFluidState().isEmpty()
                || !safeColumn(chunk, x, z, top, target, drainWater)) {
            return;
        }

        if (top > target && chunk.getBlockState(at.setY(top + 1)).isIn(BlockTags.LOGS)) {
            removeTree(world, at.toImmutable());
        }

        for (int y = top; y > target; y--) {
            chunk.setBlockState(at.setY(y), Blocks.AIR.getDefaultState(), false);
        }

        for (int y = top; y < target; y++) {
            chunk.setBlockState(at.setY(y), fill, false);
        }

        chunk.setBlockState(at.setY(target), surface, false);

        for (int y = target - 1; y >= target - CAP && y > chunk.getBottomY(); y--) {
            BlockState state = chunk.getBlockState(at.setY(y));

            if (state.isAir()) {
                chunk.setBlockState(at, y >= target - 2 ? fill : Blocks.STONE.getDefaultState(), false);
            } else if (y >= target - 2 && top > target && SiteSurroundings.isNaturalGround(state)) {
                // O chão cortado ganha o mesmo subsolo do de antes, e não a pedra crua.
                chunk.setBlockState(at, fill, false);
            }
        }

        clearUnsupportedGrass(world, chunk, at, Math.min(top, target) + 1, Math.max(top, target) + 2);
        GenerationHeights.retrack(chunk, x, z, Math.max(top, target) + 2);
    }

    /** Remove flora sem apoio depois de mudar a altura da coluna. */
    private static void clearUnsupportedGrass(StructureWorldAccess world, Chunk chunk, BlockPos.Mutable at,
                                              int fromY, int toY) {
        for (int y = fromY; y <= toY; y++) {
            BlockState state = chunk.getBlockState(at.setY(y));
            if (isFlora(state) && !state.canPlaceAt(world, at)) {
                chunk.setBlockState(at, Blocks.AIR.getDefaultState(), false);
            }
        }
    }

    /**
     * Plantas soltas não sobrevivem ao nivelamento; lavoura não é flora espontânea. Flor e muda no topo não são o chão:
     * a varredura passa por elas, ou a coluna fica de pé (Regra 251).
     */
    private static boolean isFlora(BlockState state) {
        return state.isOf(Blocks.SHORT_GRASS) || state.isOf(Blocks.TALL_GRASS)
                || state.isOf(Blocks.FERN) || state.isOf(Blocks.LARGE_FERN) || state.isIn(BlockTags.FLOWERS)
                || state.isIn(BlockTags.SAPLINGS);
    }

    /**
     * Confere o corte e o aterro antes de escrever: blocos de estrutura e líquidos ficam intactos. O minério é chão:
     * a coluna com ele desce como as vizinhas, e não fica de torre (Regra 292).
     */
    static boolean safeColumn(Chunk chunk, int x, int z, int top, int target, boolean drainWater) {
        BlockPos.Mutable at = new BlockPos.Mutable(x, top, z);
        for (int y = Math.min(top, target); y <= Math.max(top, target); y++) {
            BlockState state = chunk.getBlockState(at.setY(y));
            boolean fluid = !state.getFluidState().isEmpty();
            if ((!drainWater && fluid)
                    || (!fluid && !state.isAir() && !state.isReplaceable()
                    && !SiteSurroundings.isNaturalOrOre(state))) {
                return false;
            }
        }
        return true;
    }

    /**
     * A árvore cujo tronco nasce em {@code trunk}: os troncos e folhas ligados a ele, até {@link #TREE_BLOCKS} e a
     * {@link #TREE_RADIUS} do tronco, saem — o chão embaixo vai ser cortado, e ela ficaria no ar.
     *
     * @return quantos blocos saíram
     */
    static int removeTree(net.minecraft.world.WorldAccess world, BlockPos trunk) {
        java.util.ArrayDeque<BlockPos> open = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        open.add(trunk);
        seen.add(trunk);
        int removed = 0;

        while (!open.isEmpty() && removed < TREE_BLOCKS) {
            BlockPos at = open.poll();
            BlockState state = world.getBlockState(at);

            if (!state.isIn(BlockTags.LOGS) && !state.isIn(BlockTags.LEAVES)) {
                continue;
            }

            world.setBlockState(at, Blocks.AIR.getDefaultState(), net.minecraft.block.Block.FORCE_STATE);
            removed++;

            for (net.minecraft.util.math.Direction side : net.minecraft.util.math.Direction.values()) {
                BlockPos next = at.offset(side);

                if (next.getY() >= trunk.getY() && Math.abs(next.getX() - trunk.getX()) <= TREE_RADIUS
                        && Math.abs(next.getZ() - trunk.getZ()) <= TREE_RADIUS && seen.add(next)) {
                    open.add(next);
                }
            }
        }

        return removed;
    }

    static void clearAll() {
        synchronized (PLANS) {
            PLANS.clear();
        }
    }

    /**
     * O bloco de terreno mais alto da coluna, a partir de {@code from} para baixo: pula ar, planta, folha e tronco (a
     * árvore de um chunk vizinho já gerado pode entrar neste) e para na água.
     */
    static int topOf(Chunk chunk, int x, int z, int from) {
        BlockPos.Mutable at = new BlockPos.Mutable(x, Math.min(from, chunk.getTopY() - 1), z);

        while (at.getY() > chunk.getBottomY()) {
            BlockState state = chunk.getBlockState(at);

            if (!state.getFluidState().isEmpty()) {
                return at.getY();
            }

            if (!state.isAir() && !state.isReplaceable() && !state.isIn(BlockTags.LEAVES) && !state.isIn(BlockTags.LOGS)
                    && !isFlora(state)) {
                return at.getY();
            }

            at.move(0, -1, 0);
        }

        return at.getY();
    }

    private static BlockBox encompass(BlockBox a, BlockBox b) {
        return new BlockBox(Math.min(a.getMinX(), b.getMinX()), Math.min(a.getMinY(), b.getMinY()),
                Math.min(a.getMinZ(), b.getMinZ()), Math.max(a.getMaxX(), b.getMaxX()),
                Math.max(a.getMaxY(), b.getMaxY()), Math.max(a.getMaxZ(), b.getMaxZ()));
    }
}
