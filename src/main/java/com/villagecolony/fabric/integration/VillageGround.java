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
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool;
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
 * postas: as ruas do Minecraft seguem o chão que acharem, então nascem sobre o chão novo.
 *
 * <p>A conta é do {@link VillageBlend}: na vila (as peças + {@link #CORE_PAD}), degrau de 1 e buraco tampado; em
 * volta, até {@link #REACH}, a mistura suave do topo de verdade com o nível da vila, numa faixa de 4 vezes o desnível. Calculada uma
 * vez por vila, com a planta inteira dela e a altura natural do gerador, e guardada: todos os chunks da vila veem a
 * mesma. Cada chunk só mexe nas próprias colunas — corta ou aterra com o bloco de cima e o de baixo da própria
 * coluna, tampa os vãos de caverna logo abaixo, e a árvore em pé sobre o chão cortado sai inteira. Água não é tocada.
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
        private final VillageBlend.Shape shape;

        Plan(int minX, int minZ, VillageSurface.Kind[][] kinds, VillageBlend.Shape shape) {
            this.minX = minX;
            this.minZ = minZ;
            this.kinds = kinds;
            this.shape = shape;
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

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int x = pos.getStartX() + lx;
                    int z = pos.getStartZ() + lz;

                    if (plan.shapes(x, z)) {
                        int top = topOf(chunk, x, z,
                                chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR_WG, x & 15, z & 15) + SCAN_ABOVE);
                        column(world, chunk, x, z, top, plan.targetFor(x, z, top));
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
                double floor = ground[i][j] * (1 - fx) * (1 - fz) + ground[i + 1][j] * fx * (1 - fz)
                        + ground[i][j + 1] * (1 - fx) * fz + ground[i + 1][j + 1] * fx * fz;
                int nearI = fx < 0.5 ? i : i + 1;
                int nearJ = fz < 0.5 ? j : j + 1;
                boolean wet = water[nearI][nearJ] > ground[nearI][nearJ];

                natural[x][z] = wet ? water[nearI][nearJ] : (int) Math.round(floor);
                kinds[x][z] = wet ? VillageSurface.Kind.WATER : VillageSurface.Kind.FREE;
            }
        }

        // O piso das peças rígidas (as casas): o bloco de baixo do nível de chão da peça.
        StringBuilder fixed = new StringBuilder();

        for (StructurePiece piece : start.getChildren()) {
            if (!(piece instanceof PoolStructurePiece pool)
                    || pool.getPoolElement().getProjection() != StructurePool.Projection.RIGID) {
                continue;
            }

            BlockBox box = piece.getBoundingBox();
            int floor = box.getMinY() + pool.getGroundLevelDelta() - 1;

            for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                    natural[x - minX][z - minZ] = floor;
                    kinds[x - minX][z - minZ] = VillageSurface.Kind.FIXED;
                }
            }

            fixed.append(fixed.length() == 0 ? "" : ",").append('[').append(box.getMinX()).append(',')
                    .append(box.getMinZ()).append(',').append(box.getMaxX()).append(',').append(box.getMaxZ())
                    .append(']');
        }

        VillageBlend.Area core = new VillageBlend.Area(REACH - CORE_PAD, REACH - CORE_PAD,
                width - 1 - REACH + CORE_PAD, depth - 1 - REACH + CORE_PAD);
        // O jogo procura a referência a até 8 chunks do início da vila: além disso a geração não alcança.
        ChunkPos origin = start.getPos();
        VillageBlend.Area reach = new VillageBlend.Area(
                (origin.x - REFERENCE_CHUNKS) * 16 - minX, (origin.z - REFERENCE_CHUNKS) * 16 - minZ,
                (origin.x + REFERENCE_CHUNKS) * 16 + 15 - minX, (origin.z + REFERENCE_CHUNKS) * 16 + 15 - minZ);
        VillageBlend.Shape shape = VillageBlend.shape(natural, kinds, core, reach);

        // A área moldada, o miolo e as peças fixas, para medir o chão depois (scripts/ground_report.py).
        VillageColonyMod.LOGGER.info("VC_GROUND {\"minX\":{},\"minZ\":{},\"width\":{},\"depth\":{},\"reach\":{},"
                        + "\"core\":{},\"ms\":{},\"fixed\":[{}]}", minX, minZ, width, depth, REACH, CORE_PAD,
                (System.nanoTime() - began) / 1_000_000, fixed);

        return new Plan(minX, minZ, kinds, shape);
    }

    /** A caixa de todas as peças da vila. */
    private static BlockBox piecesOf(StructureStart start) {
        BlockBox pieces = null;

        for (StructurePiece piece : start.getChildren()) {
            pieces = pieces == null ? piece.getBoundingBox() : encompass(pieces, piece.getBoundingBox());
        }

        return java.util.Objects.requireNonNull(pieces, "a vila sem peças não é moldada");
    }

    /**
     * Corta ou aterra a coluna de {@code top} até {@code target}, com o bloco de cima e o de baixo que a coluna já
     * tinha, e tampa os vãos logo abaixo. Coluna com água em cima fica como está; a árvore em pé sobre o chão
     * cortado sai inteira (autor, 2026-10-29).
     */
    static void column(StructureWorldAccess world, Chunk chunk, int x, int z, int top, int target) {
        BlockPos.Mutable at = new BlockPos.Mutable(x, top + 1, z);

        if (top <= chunk.getBottomY() || !chunk.getFluidState(at.setY(top)).isEmpty()
                || !chunk.getFluidState(at.setY(top + 1)).isEmpty()) {
            return;
        }

        BlockState surface = chunk.getBlockState(at.setY(top));
        BlockState below = chunk.getBlockState(at.setY(top - 1));
        BlockState fill = below.isAir() || !below.getFluidState().isEmpty() ? Blocks.DIRT.getDefaultState() : below;

        if (surface.isAir() || !surface.getFluidState().isEmpty()) {
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
            } else if (y >= target - 2 && top > target) {
                // O chão cortado ganha o mesmo subsolo do de antes, e não a pedra crua.
                chunk.setBlockState(at, fill, false);
            }
        }
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

            if (!state.isAir() && !state.isReplaceable() && !state.isIn(BlockTags.LEAVES) && !state.isIn(BlockTags.LOGS)) {
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
