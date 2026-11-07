package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.VillageRoad;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Caminho não fica voando — nota do autor no jogo da 0.4.18.
 *
 * <p>Percorre as colunas da rede de ruas aos poucos ({@value #COLUMNS} por segundo, por colônia). Coluna de caminho
 * com vão de até {@value #MAX_GAP} blocos embaixo ganha terra até o chão. Vão maior ou água fica como está, que é
 * ponte. Só em vila nova ({@link NewVillage}). A terra surge sem origem física: cada coluna fica no log ("dirt under the path").
 */
public final class PathSupport {

    static {
        ServerMemory.register(PathSupport.class, PathSupport::clearAll);
    }

    private static final int EVERY = 20;

    private static final int COLUMNS = 64;

    static final int MAX_GAP = 6;

    private static final Map<UUID, Integer> CURSORS = new HashMap<>();

    private PathSupport() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey().equals(World.OVERWORLD) && world.getTime() % EVERY == 0) {
                for (Colony colony : VillageColonyMod.COLONIES.all()) {
                    RoadIndex.roadsOf(colony.id()).ifPresent(roads -> pass(world, colony.id(), roads));
                }
            }
        });
    }

    private static void pass(ServerWorld world, UUID colonyId, ColonyRoads roads) {
        List<Long> columns = roads.columns();

        if (columns.isEmpty()) {
            return;
        }

        int at = CURSORS.getOrDefault(colonyId, 0);

        for (int done = 0; done < Math.min(COLUMNS, columns.size()); done++, at++) {
            long column = columns.get(at % columns.size());

            support(world, colonyId, ColonyRoads.xOf(column), ColonyRoads.zOf(column));
        }

        CURSORS.put(colonyId, at % columns.size());
    }

    /** Põe terra embaixo do caminho desta coluna, se ele voa; devolve quantos blocos. */
    static int support(ServerWorld world, UUID colonyId, int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return 0;
        }

        BlockPos path = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);

        if (!VillageRoad.isPathBlock(world.getBlockState(path))) {
            return 0;
        }

        int gap = 0;

        while (gap <= MAX_GAP && isEmpty(world, path.down(gap + 1))) {
            gap++;
        }

        // Sem chão ao alcance, ou com água no vão: é ponte. Conferido antes de escrever.
        if (gap == 0 || gap > MAX_GAP || !world.getFluidState(path.down(gap + 1)).isEmpty()) {
            return 0;
        }

        for (int depth = 1; depth <= gap; depth++) {
            world.setBlockState(path.down(depth), Blocks.DIRT.getDefaultState());
        }

        VillageColonyMod.LOGGER.info("Colony {} put {} dirt under the path at {}",
                colonyId.toString().substring(0, 8), gap, path.toShortString());

        return gap;
    }

    private static boolean isEmpty(ServerWorld world, BlockPos at) {
        return world.getBlockState(at).getCollisionShape(world, at).isEmpty()
                && world.getFluidState(at).isEmpty();
    }

    private static void clearAll() {
        CURSORS.clear();
    }
}
