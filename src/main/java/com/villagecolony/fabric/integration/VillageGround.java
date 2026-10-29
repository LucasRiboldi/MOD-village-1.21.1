package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
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
 * O solo da vila na geração do chunk — autor, 2026-10-29 (escolhas: na geração, encosta suave, água fica, borda de
 * {@link #MARGIN} até o terreno natural). Roda no começo da colocação de estruturas e features de cada chunk
 * ({@code ChunkGenerator.generateFeatures}), com o terreno pronto e antes de as peças da vila serem postas: as ruas do
 * Minecraft seguem o chão que acharem, então nascem sobre o chão suave.
 *
 * <p>A superfície de cada vila ({@link VillageSurface}) é calculada uma vez, com a planta inteira dela (as peças já
 * existem antes do terreno de cada chunk) e a altura natural do gerador, e guardada: todos os chunks da vila veem a
 * mesma. Cada chunk só mexe nas próprias colunas — corta ou aterra até a superfície com o bloco de cima e o de baixo
 * da própria coluna, e tampa os vãos de caverna logo abaixo. Água não é tocada.
 */
public final class VillageGround {

    static {
        ServerMemory.register(VillageGround.class, VillageGround::clearAll);
    }

    /** A faixa em volta da vila em que o chão vai do nível da vila ao terreno natural. */
    public static final int MARGIN = 16;

    /** De quantos em quantos blocos se lê a altura natural do gerador (o meio é interpolado). */
    static final int SAMPLE = 4;

    /** Quantos blocos acima do mapa de altura (ou do alvo) a varredura do topo começa. */
    static final int SCAN_ABOVE = 16;

    /** Quantos blocos abaixo do chão novo os vãos de caverna são tampados. */
    static final int CAP = 8;

    /** Vilas guardadas ao mesmo tempo; a geração vai e volta entre poucas. */
    static final int CACHED = 32;

    /** A superfície de uma vila: o canto de menor x e z e as alturas e papéis das colunas. */
    static final class Plan {
        private final int minX;
        private final int minZ;
        private final int[][] surface;
        private final VillageSurface.Kind[][] kinds;

        Plan(int minX, int minZ, int[][] surface, VillageSurface.Kind[][] kinds) {
            this.minX = minX;
            this.minZ = minZ;
            this.surface = surface;
            this.kinds = kinds;
        }

        boolean covers(int x, int z) {
            return x >= minX && z >= minZ && x < minX + surface.length && z < minZ + surface[0].length;
        }

        /** Se a coluna é chão que a vila molda (nem casa, nem água, nem o anel da borda). */
        boolean shapes(int x, int z) {
            return covers(x, z) && kinds[x - minX][z - minZ] == VillageSurface.Kind.FREE;
        }

        int surfaceAt(int x, int z) {
            return surface[x - minX][z - minZ];
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
                        column(chunk, x, z, plan.surfaceAt(x, z));
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
        BlockBox pieces = null;

        for (StructurePiece piece : start.getChildren()) {
            pieces = pieces == null ? piece.getBoundingBox() : encompass(pieces, piece.getBoundingBox());
        }

        int minX = pieces.getMinX() - MARGIN;
        int minZ = pieces.getMinZ() - MARGIN;
        int width = pieces.getMaxX() - pieces.getMinX() + 1 + 2 * MARGIN;
        int depth = pieces.getMaxZ() - pieces.getMinZ() + 1 + 2 * MARGIN;
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
                boolean ring = x == 0 || z == 0 || x == width - 1 || z == depth - 1;

                natural[x][z] = wet ? water[nearI][nearJ] : (int) Math.round(floor);
                kinds[x][z] = wet ? VillageSurface.Kind.WATER : ring ? VillageSurface.Kind.FIXED : VillageSurface.Kind.FREE;
            }
        }

        // O piso das peças rígidas (as casas): o bloco de baixo do nível de chão da peça.
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
        }

        StringBuilder fixed = new StringBuilder();

        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof PoolStructurePiece pool
                    && pool.getPoolElement().getProjection() == StructurePool.Projection.RIGID) {
                BlockBox box = piece.getBoundingBox();
                fixed.append(fixed.length() == 0 ? "" : ",").append('[').append(box.getMinX()).append(',')
                        .append(box.getMinZ()).append(',').append(box.getMaxX()).append(',').append(box.getMaxZ())
                        .append(']');
            }
        }

        // A área moldada e as peças fixas, para medir o chão depois (scripts/ground_report.py).
        VillageColonyMod.LOGGER.info("VC_GROUND {\"minX\":{},\"minZ\":{},\"width\":{},\"depth\":{},\"fixed\":[{}]}",
                minX, minZ, width, depth, fixed);

        return new Plan(minX, minZ, VillageSurface.smooth(natural, kinds), kinds);
    }

    /**
     * Corta ou aterra a coluna até {@code target} (o bloco sólido de cima), com o bloco de cima e o de baixo que a
     * coluna já tinha, e tampa os vãos logo abaixo. Coluna com água em cima fica como está.
     */
    static void column(Chunk chunk, int x, int z, int target) {
        // O topo de verdade, varrendo de cima: o mapa de altura do chunk nesta etapa pode ficar um bloco abaixo, e o
        // corte deixava a camada de cima solta no ar.
        int top = topOf(chunk, x, z, Math.max(target,
                chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR_WG, x & 15, z & 15)) + SCAN_ABOVE);
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
