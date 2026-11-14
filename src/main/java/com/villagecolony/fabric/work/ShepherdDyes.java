package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CraftingLookup;
import com.villagecolony.fabric.integration.Supplies;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O corante das flores — Regra 272 (autor, 2026-11-13, ADR-055 item 11). No ócio do ofício, o pastor colhe uma flor em
 * volta da vila que dá uma cor que falta nos baús e a transforma em corante pela receita do jogo. A cor sem flor ao
 * alcance surge; a que ele não colhe em {@value #GIVE_UP} tiques, ou cuja flor some, também surge. Tudo o que surge
 * entra no {@code VC_SUPPLY} como {@code FLOWER_DYE}.
 */
final class ShepherdDyes {

    /** Até onde do centro da vila ele procura flor. */
    static final int RADIUS = 24;

    /** Prazo de uma colheita, em tiques: passou, o corante surge. */
    static final int GIVE_UP = 2_400;

    /** Entre uma colheita e a próxima procura. */
    static final int EVERY = 600;

    /** Quanto abaixo do topo da coluna a flor ainda é procurada (debaixo de copa). */
    private static final int SCAN_DOWN = 16;

    private record Job(UUID shepherd, DyeColor color, BlockPos flower, long since) {
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    private static final Map<UUID, Long> NEXT = new HashMap<>();

    static {
        ServerMemory.register(ShepherdDyes.class, ShepherdDyes::clearAll);
    }

    private ShepherdDyes() {
    }

    static void clearAll() {
        JOBS.clear();
        NEXT.clear();
    }

    /** Esquece a colheita e o descanso desta colônia. */
    static void forget(UUID colonyId) {
        JOBS.remove(colonyId);
        NEXT.remove(colonyId);
    }

    /**
     * Um passo da colheita deste pastor ocioso.
     *
     * @return se ele está nisto agora (anda até a flor ou acabou de colhê-la)
     */
    static boolean step(ServerWorld world, Colony colony, VillagerEntity shepherd, List<ColonyPos> chests) {
        long now = world.getTime();
        Job job = JOBS.get(colony.id());

        if (job != null && !job.shepherd().equals(shepherd.getUuid())) {
            return false;
        }

        if (job == null) {
            if (now < NEXT.getOrDefault(colony.id(), Long.MIN_VALUE)) {
                return false;
            }

            NEXT.put(colony.id(), now + EVERY);

            Map<DyeColor, BlockPos> flowers = flowersAround(world, MinecraftTypeAdapter.toBlockPos(colony.center()));
            Optional<DyeColor> picked = flowers.isEmpty() ? Optional.empty() : missing(world, chests, flowers);

            if (picked.isPresent()) {
                job = new Job(shepherd.getUuid(), picked.get(), flowers.get(picked.get()), now);
                JOBS.put(colony.id(), job);
            } else {
                missing(world, chests, Map.of()).ifPresent(color -> appear(world, colony, chests, color));
                return false;
            }
        }

        if (now - job.since() > GIVE_UP || colorOf(world, job.flower()).filter(job.color()::equals).isEmpty()) {
            JOBS.remove(colony.id());
            appear(world, colony, chests, job.color());
            return false;
        }

        if (!shepherd.getBlockPos().isWithinDistance(job.flower(), 2.5)) {
            WorkTargets.set(shepherd.getUuid(), job.flower(), 1);
            return true;
        }

        Item dye = DyeItem.byColor(job.color());

        world.setBlockState(job.flower(), Blocks.AIR.getDefaultState());
        ColonyChests.deposit(world, chests, dye, 1);
        shepherd.swingHand(Hand.MAIN_HAND);
        ActionJournal.action(shepherd.getUuid(), "CRAFTED", ActionJournal.idOf(dye), 1, job.flower(), "dye from a flower");
        JOBS.remove(colony.id());

        return true;
    }

    /** A primeira cor sem corante nos baús entre as que {@code flowers} dão, ou entre todas quando vazio. */
    private static Optional<DyeColor> missing(ServerWorld world, List<ColonyPos> chests, Map<DyeColor, BlockPos> flowers) {
        for (DyeColor color : DyeColor.values()) {
            if ((flowers.isEmpty() || flowers.containsKey(color))
                    && ColonyChests.countIn(world, chests, DyeItem.byColor(color)) == 0) {
                return Optional.of(color);
            }
        }

        return Optional.empty();
    }

    /** A flor mais perto de cada cor, no topo do terreno, a até {@value #RADIUS} blocos do centro. */
    private static Map<DyeColor, BlockPos> flowersAround(ServerWorld world, BlockPos center) {
        Map<DyeColor, BlockPos> nearest = new EnumMap<>(DyeColor.class);

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;

                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                BlockPos at = flowerInColumn(world, x, z);

                if (at == null) {
                    continue;
                }

                colorOf(world, at).ifPresent(color -> {
                    BlockPos known = nearest.get(color);

                    if (known == null || at.getSquaredDistance(center) < known.getSquaredDistance(center)) {
                        nearest.put(color, at);
                    }
                });
            }
        }

        return nearest;
    }

    /** Do topo da coluna para baixo, através de ar e folha, a flor pequena antes do primeiro chão; ou nulo. */
    private static @org.jspecify.annotations.Nullable BlockPos flowerInColumn(ServerWorld world, int x, int z) {
        int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;

        for (int y = top; y > top - SCAN_DOWN; y--) {
            BlockPos at = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(at);

            if (state.isIn(BlockTags.SMALL_FLOWERS)) {
                return at;
            }

            if (!state.isAir() && !state.isIn(BlockTags.LEAVES) && !state.isReplaceable()) {
                return null;
            }
        }

        return null;
    }

    /** A cor do corante desta flor, se for flor pequena que se pode colher e a receita do jogo a fizer corante. */
    private static Optional<DyeColor> colorOf(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        if (!state.isIn(BlockTags.SMALL_FLOWERS) || !BlockProtection.mayDigOut(world, at, state)) {
            return Optional.empty();
        }

        return CraftingLookup.resultOfOne(world, new ItemStack(state.getBlock().asItem()))
                .map(ItemStack::getItem)
                .filter(DyeItem.class::isInstance)
                .map(item -> ((DyeItem) item).getColor());
    }

    private static void appear(ServerWorld world, Colony colony, List<ColonyPos> chests, DyeColor color) {
        Item dye = DyeItem.byColor(color);

        ColonyChests.deposit(world, chests, dye, 1);
        Supplies.record(SupplyException.FLOWER_DYE, colony.id(), ActionJournal.idOf(dye), 1);
    }
}
