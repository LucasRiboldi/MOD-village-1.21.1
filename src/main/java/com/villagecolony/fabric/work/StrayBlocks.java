package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.SiteSurroundings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A sobra que os ociosos tiram da vila — Regra 294 (autor 15-11, G6 da ADR-056):
 *
 * <ul>
 *   <li><b>Bloco solto:</b> terra, grama, pedra, areia ou cascalho sem vizinho nos quatro lados nem em cima — calombo de
 *   um bloco ou bloco no ar.</li>
 *   <li><b>Toco:</b> de uma a {@value #STUMP} toras naturais em pé sobre chão natural, sem folha a até {@value #LEAVES}
 *   blocos e sem bloco de construção encostado.</li>
 * </ul>
 *
 * <p>Uma janela da caixa da vila é varrida a cada passada e o achado entra na fila da colônia; o ocioso de qualquer ofício
 * vai até ele, quebra e guarda o que cair no baú mais perto. Peça da vila, obra da colônia e bloco do jogador ficam.
 */
final class StrayBlocks {

    /** Quantas toras um toco tem no máximo. */
    static final int STUMP = 3;

    /** Até onde, em volta do topo do toco, uma folha diz que ele ainda é árvore. */
    static final int LEAVES = 3;

    /** O lado da janela varrida por passada. */
    static final int WINDOW = 16;

    /** A que distância o aldeão quebra a sobra. */
    static final double REACH = 3.0;

    private static final Map<UUID, Deque<BlockPos>> QUEUE = new HashMap<>();
    private static final Map<UUID, Integer> CURSOR = new HashMap<>();
    private static final Map<UUID, BlockPos> TARGET = new HashMap<>();

    static {
        ServerMemory.register(StrayBlocks.class, () -> {
            QUEUE.clear();
            CURSOR.clear();
            TARGET.clear();
        });
    }

    private StrayBlocks() {
    }

    /** Se este é um bloco solto: chão natural sem vizinho dos lados nem em cima, e que se pode tirar. */
    static boolean isLoneBlock(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        if (!SiteSurroundings.isNaturalGround(state) || !isOpen(world, at.up())
                || !BlockProtection.mayBreak(world, at, state)) {
            return false;
        }

        for (Direction side : Direction.Type.HORIZONTAL) {
            if (!isOpen(world, at.offset(side))) {
                return false;
            }
        }

        return true;
    }

    /** As toras do toco que começa em {@code base}, de baixo para cima; vazia quando não é toco. */
    static List<BlockPos> stumpAt(ServerWorld world, BlockPos base) {
        List<BlockPos> logs = new ArrayList<>();

        if (!SiteSurroundings.isNaturalGround(world.getBlockState(base.down()))) {
            return List.of();
        }

        BlockPos at = base;

        while (isUprightNaturalLog(world.getBlockState(at)) && BlockProtection.mayBreak(world, at, world.getBlockState(at))) {
            if (logs.size() == STUMP) {
                return List.of();
            }

            logs.add(at);
            at = at.up();
        }

        if (logs.isEmpty() || !isOpen(world, at)) {
            return List.of();
        }

        for (BlockPos log : logs) {
            for (Direction side : Direction.Type.HORIZONTAL) {
                if (!isOpen(world, log.offset(side))) {
                    return List.of();
                }
            }
        }

        BlockPos top = logs.get(logs.size() - 1);

        for (BlockPos near : BlockPos.iterate(top.add(-LEAVES, -LEAVES, -LEAVES), top.add(LEAVES, LEAVES, LEAVES))) {
            if (world.getBlockState(near).isIn(BlockTags.LEAVES)) {
                return List.of();
            }
        }

        return logs;
    }

    private static boolean isUprightNaturalLog(BlockState state) {
        return state.isIn(BlockTags.LOGS)
                && !net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).getPath().startsWith("stripped_")
                && state.getOrEmpty(PillarBlock.AXIS).map(axis -> axis == Direction.Axis.Y).orElse(true);
    }

    private static boolean isOpen(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        return (state.isAir() || state.isReplaceable()) && state.getFluidState().isEmpty();
    }

    /** Varre a próxima janela da caixa da vila e põe na fila o que achar. */
    static void sweep(ServerWorld world, Colony colony) {
        if (colony.bounds().isEmpty()) {
            return;
        }

        VillageBounds box = colony.bounds().get();
        int across = Math.max(1, (box.maxX() - box.minX()) / WINDOW + 1);
        int down = Math.max(1, (box.maxZ() - box.minZ()) / WINDOW + 1);
        int index = CURSOR.getOrDefault(colony.id(), 0) % (across * down);
        CURSOR.put(colony.id(), index + 1);
        int minX = box.minX() + (index % across) * WINDOW;
        int minZ = box.minZ() + (index / across) * WINDOW;
        Deque<BlockPos> queue = QUEUE.computeIfAbsent(colony.id(), ignored -> new ArrayDeque<>());

        for (int x = minX; x < minX + WINDOW && x <= box.maxX(); x++) {
            for (int z = minZ; z < minZ + WINDOW && z <= box.maxZ(); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                BlockPos top = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                BlockPos base = top;

                while (base.getY() > top.getY() - STUMP && world.getBlockState(base.down()).isIn(BlockTags.LOGS)) {
                    base = base.down();
                }

                if ((isLoneBlock(world, top) || !stumpAt(world, base).isEmpty()) && !queue.contains(base)) {
                    queue.add(isLoneBlock(world, top) ? top : base);
                }
            }
        }
    }

    /**
     * Dá ao ocioso a sobra mais perto da fila, ou quebra a que ele já alcançou.
     *
     * @return se ele está cuidando de uma sobra
     */
    static boolean tend(ServerWorld world, Colony colony, VillagerEntity villager) {
        UUID id = villager.getUuid();
        BlockPos target = TARGET.get(id);

        if (target != null && !isStray(world, target)) {
            TARGET.remove(id);
            target = null;
        }

        if (target == null) {
            target = next(world, colony, villager);

            if (target == null) {
                return false;
            }

            TARGET.put(id, target);
        }

        if (villager.getBlockPos().isWithinDistance(target, REACH)) {
            remove(world, colony.id(), villager, target);
            TARGET.remove(id);
        } else {
            villager.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(target, 0.5F, 1));
        }

        return true;
    }

    private static boolean isStray(ServerWorld world, BlockPos at) {
        return isLoneBlock(world, at) || !stumpAt(world, at).isEmpty();
    }

    private static BlockPos next(ServerWorld world, Colony colony, VillagerEntity villager) {
        Deque<BlockPos> queue = QUEUE.getOrDefault(colony.id(), new ArrayDeque<>());

        while (!queue.isEmpty()) {
            BlockPos candidate = queue.poll();

            if (isStray(world, candidate) && candidate.isWithinDistance(villager.getBlockPos(), IdleRoutine.REACH)
                    && !TARGET.containsValue(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /** Quebra a sobra e guarda o que ela der no baú mais perto da colônia. */
    private static void remove(ServerWorld world, UUID colonyId, VillagerEntity villager, BlockPos at) {
        List<BlockPos> blocks = isLoneBlock(world, at) ? List.of(at) : stumpAt(world, at);
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId,
                com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(at));

        for (BlockPos block : blocks.reversed()) {
            BlockState state = world.getBlockState(block);

            for (ItemStack drop : Block.getDroppedStacks(state, world, block, null)) {
                int left = drop.getCount();

                for (ColonyPos chest : chests) {
                    left = ChestDepositor.deposit(world, chest, drop.getItem(), left);

                    if (left == 0) {
                        break;
                    }
                }

                // O que não coube no baú fica no chão, e não some.
                if (left > 0) {
                    world.spawnEntity(new net.minecraft.entity.ItemEntity(world, block.getX() + 0.5, block.getY() + 0.5,
                            block.getZ() + 0.5, drop.copyWithCount(left)));
                }
            }

            world.setBlockState(block, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }

        villager.swingHand(Hand.MAIN_HAND);
        ActionJournal.action(villager.getUuid(), "CLEARED_STRAY", "block", blocks.size(), at, "Regra 294");
        VillageColonyMod.LOGGER.info("VC_STRAY colony={} at={} blocks={} by={}", colonyId.toString().substring(0, 8),
                at.toShortString(), blocks.size(), villager.getUuid().toString().substring(0, 8));
    }
}
