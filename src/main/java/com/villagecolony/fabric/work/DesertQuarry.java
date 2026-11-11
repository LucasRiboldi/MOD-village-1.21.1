package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.VillageRoad;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A pedreira de superfície do deserto — Regra 236 (autor, 2026-11-11, opção M1): na vila de deserto, o mineiro sem ramal
 * livre tira areia e arenito da superfície, fora da caixa da vila e a até {@value #BAND} blocos dela, em vez de esperar a
 * escada. Só raspa a coluna que não fica abaixo das vizinhas, uma camada por vez, até {@value #DEPTH} blocos abaixo do
 * centro da vila: o chão baixa por igual, sem poço onde o aldeão fique preso. Nunca toca vila gerada, construção da
 * colônia, caminho ou bloco junto de água.
 */
final class DesertQuarry {

    /** Até quantos blocos fora da caixa a pedreira vai. */
    static final int BAND = 32;

    /** Quanto abaixo do centro da vila o chão pode baixar. */
    static final int DEPTH = 4;

    /** Colunas olhadas por pedido, para a busca não pesar no tique. */
    static final int BUDGET = 512;

    private static final Set<Block> SAND = Set.of(Blocks.SAND, Blocks.SANDSTONE, Blocks.RED_SAND, Blocks.RED_SANDSTONE);

    /** Onde a busca parou, por colônia: a seguinte continua dali. */
    private static final Map<UUID, Integer> CURSOR = new HashMap<>();

    static {
        ServerMemory.register(DesertQuarry.class, CURSOR::clear);
    }

    private DesertQuarry() {
    }

    /** O próximo bloco da pedreira desta vila; vazio fora do deserto, sem caixa ou sem bloco que sirva. */
    static Optional<BlockPos> next(ServerWorld world, Colony colony) {
        if (colony.bounds().isEmpty() || !VillageGrove.isDesert(world, colony)) {
            return Optional.empty();
        }

        return pick(world, colony.id(), colony.bounds().get(), colony.center().y());
    }

    /** A busca em anéis em volta da caixa, de dentro para fora, retomando do cursor da colônia. */
    static Optional<BlockPos> pick(ServerWorld world, UUID colonyId, VillageBounds box, int centerY) {
        int total = 0;

        for (int ring = 1; ring <= BAND; ring++) {
            total += perimeter(box, ring);
        }

        int start = CURSOR.getOrDefault(colonyId, 0) % total;

        for (int step = 0; step < Math.min(BUDGET, total); step++) {
            int index = (start + step) % total;
            BlockPos column = columnAt(box, index);
            Optional<BlockPos> found = shavable(world, column, centerY);

            if (found.isPresent()) {
                CURSOR.put(colonyId, index);

                return found;
            }
        }

        CURSOR.put(colonyId, (start + BUDGET) % total);

        return Optional.empty();
    }

    /** O bloco do topo desta coluna, se ele pode sair sem deixar buraco nem tocar o que é da vila. */
    static Optional<BlockPos> shavable(ServerWorld world, BlockPos column, int centerY) {
        if (!world.isChunkLoaded(column.getX() >> 4, column.getZ() >> 4)) {
            return Optional.empty();
        }

        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()) - 1;
        BlockPos at = new BlockPos(column.getX(), top, column.getZ());
        BlockState state = world.getBlockState(at);

        if (!SAND.contains(state.getBlock()) || top - 1 < centerY - DEPTH || !world.getFluidState(at.up()).isEmpty()
                || VillageRoad.isPathBlock(state) || BlockProtection.isVillageOriginal(world, at) || BlockProtection.isColonyBuilt(at)) {
            return Optional.empty();
        }

        for (Direction side : Direction.Type.HORIZONTAL) {
            BlockPos next = at.offset(side);

            if (!world.isChunkLoaded(next.getX() >> 4, next.getZ() >> 4) || !world.getFluidState(next).isEmpty()
                    || !world.getFluidState(next.down()).isEmpty()
                    || world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, next.getX(), next.getZ()) - 1 > top) {
                return Optional.empty();
            }
        }

        return Optional.of(at);
    }

    private static int perimeter(VillageBounds box, int ring) {
        int width = box.maxX() - box.minX() + 1 + 2 * ring;
        int depth = box.maxZ() - box.minZ() + 1 + 2 * ring;

        return 2 * width + 2 * depth - 4;
    }

    /** A coluna de número {@code index}, contando os anéis de dentro para fora e cada anel em volta. */
    static BlockPos columnAt(VillageBounds box, int index) {
        int ring = 1;
        int left = index;

        while (left >= perimeter(box, ring)) {
            left -= perimeter(box, ring);
            ring++;
        }

        int minX = box.minX() - ring;
        int minZ = box.minZ() - ring;
        int width = box.maxX() - box.minX() + 1 + 2 * ring;
        int depth = box.maxZ() - box.minZ() + 1 + 2 * ring;

        if (left < width) {
            return new BlockPos(minX + left, 0, minZ);
        }

        left -= width;

        if (left < depth - 1) {
            return new BlockPos(minX + width - 1, 0, minZ + 1 + left);
        }

        left -= depth - 1;

        if (left < width - 1) {
            return new BlockPos(minX + width - 2 - left, 0, minZ + depth - 1);
        }

        left -= width - 1;

        return new BlockPos(minX, 0, minZ + depth - 2 - left);
    }
}
