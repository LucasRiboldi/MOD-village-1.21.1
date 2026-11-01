package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockProtection;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Regra 48: quem caminha pela vila abre aos poucos a passagem obstruída por folhas naturais. */
final class LeafPaths {
    private LeafPaths() {
    }

    static void pass(ServerWorld world) {
        for (var colony : VillageColonyMod.COLONIES.all()) {
            for (var worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
                if (world.getEntity(worker.villagerId()) instanceof VillagerEntity villager && villager.isAlive()) {
                    clearOne(world, villager);
                }
            }
        }
    }

    /** Um bloco por aldeão e por segundo, no próximo nó real da rota. */
    static boolean clearOne(ServerWorld world, VillagerEntity villager) {
        BlockPos target = WorkTargets.of(villager.getUuid()).orElse(null);
        var path = villager.getNavigation().getCurrentPath();
        if (target == null && (path == null || path.isFinished())) {
            return false;
        }
        BlockPos current = villager.getBlockPos();
        BlockPos next = path != null && !path.isFinished() ? path.getCurrentNodePos() : target;
        int dx;
        int dz;
        if (next == null) {
            dx = villager.getHorizontalFacing().getOffsetX();
            dz = villager.getHorizontalFacing().getOffsetZ();
        } else {
            dx = Integer.compare(next.getX(), current.getX());
            dz = Integer.compare(next.getZ(), current.getZ());
            if (Math.abs(next.getX() - current.getX()) >= Math.abs(next.getZ() - current.getZ())) {
                dz = 0;
            } else {
                dx = 0;
            }
        }
        if (dx == 0 && dz == 0) {
            return false;
        }
        for (int height = 0; height <= 1; height++) {
            BlockPos at = current.add(dx, height, dz);
            var state = world.getBlockState(at);
            if (state.isIn(BlockTags.LEAVES) && !BlockProtection.isPlayerPlaced(state)
                    && !BlockProtection.isColonyBuilt(at)
                    && !BlockProtection.isVillageOriginal(world, at)) {
                world.breakBlock(at, true);
                villager.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                ActionJournal.action(villager.getUuid(), "CLEARED_LEAVES", ActionJournal.idOf(state.getBlock().asItem()),
                        1, at, "path blocked by leaves");
                return true;
            }
        }
        return false;
    }
}
