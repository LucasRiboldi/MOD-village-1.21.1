package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O rumo do ramal — ADR-039 D3: o mineiro sem ramal pega primeiro o que tem
 * mais do minério que a vila precisa nas posições ainda por cavar. Falta
 * carvão abaixo de {@link #COAL_WANTED} no baú, falta ferro abaixo de
 * {@link #IRON_WANTED}; sem falta nenhuma, qualquer minério conta. Empate
 * mantém a ordem antiga dos ramais.
 */
final class MineOreHeading {

    static {
        ServerMemory.register(MineOreHeading.class, MineOreHeading::clearAll);
    }

    /** Por quanto tempo a ordem calculada vale: ler o ramal custa ~1.300 blocos. */
    static final int KEEP_TICKS = 200;

    private static final class Kept {
        private final long at;
        private final int[] order;

        Kept(long at, int[] order) {
            this.at = at;
            this.order = order;
        }

        long at() {
            return at;
        }

        int[] order() {
            return order;
        }
    }

    private static final Map<UUID, Kept> KEPT = new HashMap<>();

    /** Carvão no baú abaixo do qual a vila o procura. */
    static final int COAL_WANTED = 32;

    /** Ferro (cru mais lingote) abaixo do qual a vila o procura. */
    static final int IRON_WANTED = 16;

    private MineOreHeading() {
    }

    static void clearAll() {
        KEPT.clear();
    }

    /** Os ramais na ordem em que o mineiro os deve pegar. */
    static int[] order(ServerWorld world, Colony colony, Mine mine) {
        Kept kept = KEPT.get(colony.id());

        if (kept != null && world.getTime() - kept.at() < KEEP_TICKS) {
            return kept.order();
        }

        int[] order = freshOrder(world, colony, mine);

        KEPT.put(colony.id(), new Kept(world.getTime(), order));
        return order;
    }

    private static int[] freshOrder(ServerWorld world, Colony colony, Mine mine) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        boolean coal = ColonyChests.countIn(world, chests, Items.COAL) < COAL_WANTED;
        boolean iron = ColonyChests.countIn(world, chests, Items.RAW_IRON)
                + ColonyChests.countIn(world, chests, Items.IRON_INGOT) < IRON_WANTED;

        return order(world, mine, coal || !iron, iron || !coal);
    }

    static int[] order(ServerWorld world, Mine mine, boolean coal, boolean iron) {
        List<Integer> indices = new ArrayList<>();
        int[] score = new int[mine.arms().size()];

        for (int index = 0; index < score.length; index++) {
            indices.add(index);
            score[index] = oreAhead(world, mine.arm(index), coal, iron);
        }

        indices.sort(Comparator.comparingInt((Integer index) -> -score[index]).thenComparingInt(index -> index));

        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Quanto minério procurado há nas posições do ramal que faltam cavar. */
    static int oreAhead(ServerWorld world, MineArm arm, boolean coal, boolean iron) {
        if (arm.isDone()) {
            return 0;
        }

        int found = 0;

        for (int index = Math.max(arm.cut(), MineShaft.SHARED_BLOCKS); !arm.shaft().beyondTheArm(index); index++) {
            var at = MinecraftTypeAdapter.toBlockPos(arm.shaft().positionAt(index));
            WorldChunk chunk = world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);

            if (chunk == null) {
                continue;
            }

            BlockState state = chunk.getBlockState(at);

            if ((coal && state.isIn(BlockTags.COAL_ORES)) || (iron && state.isIn(BlockTags.IRON_ORES))) {
                found++;
            }
        }

        return found;
    }
}
