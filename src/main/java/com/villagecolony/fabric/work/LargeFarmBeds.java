package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Os canteiros da plantação grande — Regra 325: são as colunas de terra arada do molde {@code colony/large_farm}, e não
 * só a terra arada que está no mundo. Canteiro do molde que virou terra ou grama, mesmo com mato em cima, volta a ser
 * arado. O molde é simétrico em quarto de volta (borda de tora, água nas quatro casas de (4, 4)), então o canto mínimo da
 * caixa da obra basta para pôr as colunas no mundo.
 */
final class LargeFarmBeds {

    static {
        ServerMemory.register(LargeFarmBeds.class, LargeFarmBeds::clearAll);
    }

    private static final ResourceId FARMLAND = new ResourceId("minecraft", "farmland");

    /** As colunas de canteiro de cada plantação grande lembrada, como {@code BlockPos.asLong(x, 0, z)}. */
    private static final Map<BlockBox, Set<Long>> BEDS = new HashMap<>();

    private LargeFarmBeds() {
    }

    /** Lembra a plantação grande desta caixa; devolve as colunas de canteiro do molde. */
    static Set<Long> remember(ServerWorld world, BlockBox box) {
        return BEDS.computeIfAbsent(box, missing -> read(world, missing));
    }

    /** Se esta coluna é canteiro do molde numa plantação grande lembrada. */
    static boolean isBed(BlockPos at) {
        long column = BlockPos.asLong(at.getX(), 0, at.getZ());

        for (Map.Entry<BlockBox, Set<Long>> farm : BEDS.entrySet()) {
            BlockBox box = farm.getKey();

            if (at.getX() >= box.getMinX() && at.getX() <= box.getMaxX() && at.getZ() >= box.getMinZ()
                    && at.getZ() <= box.getMaxZ() && farm.getValue().contains(column)) {
                return true;
            }
        }

        return false;
    }

    /** Terra ou grama de canteiro do molde, com nada ou só mato em cima. */
    static boolean isFallowBed(ServerWorld world, BlockPos at) {
        BlockState ground = world.getBlockState(at);

        return (ground.isOf(Blocks.DIRT) || ground.isOf(Blocks.GRASS_BLOCK)) && isBed(at)
                && isClearOrWeed(world.getBlockState(at.up()));
    }

    /** Nada em cima, ou só mato que sai com a enxada: grama, samambaia, flor pequena. */
    static boolean isClearOrWeed(BlockState above) {
        return above.isAir() || (above.getFluidState().isEmpty()
                && (above.isReplaceable() || above.isIn(BlockTags.SMALL_FLOWERS)));
    }

    /**
     * Ara todo canteiro do molde que está como terra ou grama nesta caixa.
     *
     * @return quantos arou
     */
    static int tillAll(ServerWorld world, BlockBox box) {
        int tilled = 0;

        for (long column : remember(world, box)) {
            for (int y = box.getMaxY(); y >= box.getMinY() - 1; y--) {
                BlockPos at = new BlockPos(BlockPos.unpackLongX(column), y, BlockPos.unpackLongZ(column));

                if (OwnFarm.tillIfDirt(world, at)) {
                    tilled++;

                    break;
                }
            }
        }

        return tilled;
    }

    private static Set<Long> read(ServerWorld world, BlockBox box) {
        Optional<Blueprint> plan = HousePlans.READ.computeIfAbsent(LargeFarmPlans.MODEL,
                missing -> StructureBlueprintReader.read(world, missing));
        Set<Long> beds = new HashSet<>();

        plan.ifPresent(found -> {
            for (BlueprintBlock block : found.blocks()) {
                if (FARMLAND.equals(block.block())) {
                    beds.add(BlockPos.asLong(box.getMinX() + block.offset().x(), 0, box.getMinZ() + block.offset().z()));
                }
            }
        });

        return Set.copyOf(beds);
    }

    static void clearAll() {
        BEDS.clear();
    }
}
