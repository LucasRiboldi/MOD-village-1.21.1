package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.VillageFocus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Folha no caminho se corta — Regra 197 (autor, 2026-11-05): o caminho do jogo não atravessa folha, e o aldeão que tem
 * destino e parou ao lado dela ficava preso. Todo aldeão da vila corta a folha natural à frente, uma por passada.
 *
 * <p>Folha posta à mão e folha de obra da colônia ficam ({@link BlockProtection}).
 */
public final class LeafPassage {

    private static final int EVERY = 20;

    /** Quantas passadas parado antes de cortar: entre um e dois segundos. */
    private static final int STILL_PASSES = 2;

    /** Perto assim do destino ele já chegou, e a folha ao lado não atrapalha. */
    private static final int ARRIVED = 3;

    /** Até quantos blocos à frente ele alcança. */
    private static final int REACH = 2;

    private static final int FORGET_EVERY = 6_000;

    /** A altura em relação aos pés, nesta ordem: pés, cabeça, degrau, chão. */
    private static final int[] HEIGHTS = {0, 1, 2, -1};

    private static final Map<UUID, BlockPos> LAST = new HashMap<>();

    private static final Map<UUID, Integer> STILL = new HashMap<>();

    static {
        ServerMemory.register(LeafPassage.class, LeafPassage::clearAll);
    }

    private LeafPassage() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey().equals(World.OVERWORLD) && world.getTime() % EVERY == 0) {
                pass(world);
            }
        });
    }

    private static void pass(ServerWorld world) {
        if (world.getTime() % FORGET_EVERY == 0) {
            LAST.keySet().removeIf(id -> world.getEntity(id) == null);
            STILL.keySet().removeIf(id -> world.getEntity(id) == null);
        }

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            if (VillageFocus.isWorking(world, colony.id())) {
                for (VillagerEntity villager : world.getEntitiesByClass(VillagerEntity.class, boxOf(world, colony),
                        candidate -> candidate.isAlive() && !candidate.isSleeping())) {
                    look(world, villager);
                }
            }
        }
    }

    private static Box boxOf(ServerWorld world, Colony colony) {
        VillageBounds bounds = colony.bounds().orElseGet(() ->
                VillageBounds.typicalAround(colony.center(), world.getBottomY(), world.getTopY() - 1));

        return new Box(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1);
    }

    private static void look(ServerWorld world, VillagerEntity villager) {
        UUID id = villager.getUuid();
        BlockPos at = villager.getBlockPos();
        Optional<BlockPos> destination = destinationOf(villager);
        boolean moved = !at.equals(LAST.put(id, at));

        if (destination.isEmpty() || moved || at.isWithinDistance(destination.get(), ARRIVED)) {
            STILL.remove(id);

            return;
        }

        if (STILL.merge(id, 1, Integer::sum) < STILL_PASSES) {
            return;
        }

        leafInTheWay(world, at, destination.get()).ifPresent(leaf -> {
            world.breakBlock(leaf, true, villager);
            villager.swingHand(Hand.MAIN_HAND);
            STILL.remove(id);

            if (VillageColonyMod.WORKERS.find(id).isPresent()) {
                ActionJournal.action(id, "CLEARED_LEAF", "leaves", 1, leaf, "");
            }
        });
    }

    private static Optional<BlockPos> destinationOf(VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET)
                .map(target -> target.getLookTarget().getBlockPos())
                .or(() -> WorkTargets.of(villager.getUuid()));
    }

    /** A folha mais perto na direção do destino: de frente e nos dois lados dela. */
    private static Optional<BlockPos> leafInTheWay(ServerWorld world, BlockPos at, BlockPos to) {
        int stepX = Integer.signum(to.getX() - at.getX());
        int stepZ = Integer.signum(to.getZ() - at.getZ());
        int[][] headings = {{stepX, stepZ}, {stepX, 0}, {0, stepZ}};

        for (int reach = 1; reach <= REACH; reach++) {
            for (int[] heading : headings) {
                if (heading[0] == 0 && heading[1] == 0) {
                    continue;
                }

                BlockPos column = at.add(heading[0] * reach, 0, heading[1] * reach);

                for (int height : HEIGHTS) {
                    if (isCuttable(world, column.up(height))) {
                        return Optional.of(column.up(height));
                    }
                }
            }
        }

        return isCuttable(world, at.up(2)) ? Optional.of(at.up(2)) : Optional.empty();
    }

    private static boolean isCuttable(ServerWorld world, BlockPos pos) {
        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }

        BlockState state = world.getBlockState(pos);

        return state.isIn(BlockTags.LEAVES) && !BlockProtection.isPlayerPlaced(state) && !BlockProtection.isColonyBuilt(pos);
    }

    private static void clearAll() {
        LAST.clear();
        STILL.clear();
    }
}
