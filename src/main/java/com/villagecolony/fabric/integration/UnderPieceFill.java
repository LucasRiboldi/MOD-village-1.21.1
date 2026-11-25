package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Casa da vila não fica sem solo — Regra 304 (autor, 2026-11-16, jogo da 0.4.89: "casas não podem nascer sem solo, se
 * precisar preencha-o com blocos de solo do bioma"). No mundo já gerado, o Zelador (Regra 330) pega uma peça rígida da
 * vila ou uma construção pronta da colônia (Regra 356) por vez e preenche o vão logo abaixo da camada mais baixa dela,
 * coluna a coluna e de baixo para cima, com o chão do bioma, até o chão firme (no máximo {@link #DEPTH} blocos). A geração
 * faz o mesmo antes de pôr a peça ({@link PieceGround#found}).
 */
public final class UnderPieceFill {

    /** Até quantos blocos abaixo da peça o vão é preenchido; mais fundo é buraco de verdade, e fica. */
    static final int DEPTH = 8;

    /** Quantos blocos o pedreiro põe por passada. */
    static final int BUDGET = 64;

    static {
        com.villagecolony.core.type.ServerMemory.register(UnderPieceFill.class, UnderPieceFill::clearAll);
    }

    private static final Map<UUID, Integer> NEXT = new HashMap<>();

    private UnderPieceFill() {
    }

    static void clearAll() {
        NEXT.clear();
    }

    /**
     * Uma passada do Zelador: a próxima peça da vila ou construção pronta da colônia (Regra 356); diz quantos blocos pôs.
     */
    public static int pass(ServerWorld world, Colony colony) {
        List<PoolStructurePiece> pieces = rigidPiecesOf(world, colony);
        List<Building> buildings = VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream().filter(Building::finished)
                .toList();
        int total = pieces.size() + buildings.size();

        for (int tries = 0; tries < total; tries++) {
            int next = NEXT.merge(colony.id(), 1, Integer::sum) % total;
            boolean piece = next < pieces.size();
            int filled = piece ? fill(world, pieces.get(next), BUDGET) : fillBuilding(world, buildings.get(next - pieces.size()),
                    BUDGET);

            if (filled > 0) {
                String where = piece ? pieces.get(next).getBoundingBox().getCenter().toShortString()
                        : buildings.get(next - pieces.size()).min().toString();

                Supplies.record(SupplyException.FOUNDATION_GROUND, colony.id(), "under the building", filled);
                VillageColonyMod.LOGGER.info("Colony {} filled {} blocks under the {} at {} (Regras 304 e 356)",
                        colony.id().toString().substring(0, 8), filled, piece ? "village piece" : "colony building", where);

                return filled;
            }
        }

        return 0;
    }

    /** As peças rígidas das vilas que o chunk do centro da colônia referencia. */
    private static List<PoolStructurePiece> rigidPiecesOf(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        var registry = world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.STRUCTURE);
        List<PoolStructurePiece> pieces = new ArrayList<>();

        for (StructureStart start : world.getStructureAccessor().getStructureStarts(new ChunkPos(center),
                structure -> registry.getEntry(structure).isIn(StructureTags.VILLAGE))) {
            for (StructurePiece piece : start.getChildren()) {
                if (piece instanceof PoolStructurePiece pool
                        && pool.getPoolElement().getProjection() == StructurePool.Projection.RIGID) {
                    pieces.add(pool);
                }
            }
        }

        return pieces;
    }

    /**
     * O vão sob a construção pronta da colônia: a fazenda posta em encosta não fica voando (Regra 356). Diz quantos blocos
     * pôs, até {@code budget}.
     */
    public static int fillBuilding(ServerWorld world, Building building, int budget) {
        BlockBox box = BlockBox.create(MinecraftTypeAdapter.toBlockPos(building.min()),
                MinecraftTypeAdapter.toBlockPos(building.max()));

        return fillUnder(world, box, box.getMinY(), budget);
    }

    /**
     * Preenche o vão sob a camada mais baixa desta peça, nas colunas em que ela tem bloco; diz quantos blocos pôs, até
     * {@code budget}.
     */
    static int fill(ServerWorld world, PoolStructurePiece piece, int budget) {
        BlockBox box = piece.getBoundingBox();

        return fillUnder(world, box, box.getMinY() + piece.getGroundLevelDelta() - 1, budget);
    }

    /**
     * Preenche, de baixo para cima, o vão sob a camada {@code floor} da caixa nas colunas em que ela tem bloco: a areia
     * do deserto posta de cima para baixo cairia.
     */
    private static int fillUnder(ServerWorld world, BlockBox box, int floor, int budget) {
        int filled = 0;

        for (int x = box.getMinX(); x <= box.getMaxX() && filled < budget; x++) {
            for (int z = box.getMinZ(); z <= box.getMaxZ() && filled < budget; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4) || world.getBlockState(new BlockPos(x, floor, z)).isAir()) {
                    continue;
                }

                List<BlockPos> gap = new ArrayList<>();

                for (int depth = 1; depth <= DEPTH; depth++) {
                    BlockPos at = new BlockPos(x, floor - depth, z);
                    BlockState state = world.getBlockState(at);

                    if (!(state.isAir() || state.isReplaceable()) || !state.getFluidState().isEmpty()) {
                        break;
                    }

                    gap.add(at);
                }

                // Sem chão firme ao alcance, é buraco de verdade (mina, ravina): fica; o do jogador também (Regra 334).
                if (gap.size() == DEPTH || PlayerGround.isMarked(x, z)) {
                    continue;
                }

                for (BlockPos at : gap.reversed()) {
                    world.setBlockState(at, VillageBiomes.foundationGroundAt(world, at).getDefaultState(),
                            Block.NOTIFY_LISTENERS);
                    filled++;
                }
            }
        }

        return filled;
    }
}
