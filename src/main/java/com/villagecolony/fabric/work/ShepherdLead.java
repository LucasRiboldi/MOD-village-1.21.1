package com.villagecolony.fabric.work;

import com.villagecolony.fabric.brain.WorkTargets;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/** Como o pastor anda com o animal na corda até um curral longe — Regra 196. */
final class ShepherdLead {

    /** A corda arrebenta aos 10 blocos e só puxa de 6 em diante: a esta distância ele volta para o animal. */
    private static final int SLACK = 9;

    private ShepherdLead() {
    }

    /** Longe, por pernas: a navegação não traça o caminho inteiro de uma vez. */
    static void walkTo(ServerWorld world, VillagerEntity shepherd, BlockPos goal, int arrival) {
        Optional<BlockPos> leg = WalkLegs.towards(world, shepherd.getBlockPos(), goal);

        WorkTargets.set(shepherd.getUuid(), leg.orElse(goal), leg.isPresent() ? 2 : arrival);
    }

    static boolean isTooFar(VillagerEntity shepherd, AnimalEntity animal) {
        return shepherd.squaredDistanceTo(animal) > SLACK * SLACK;
    }

    /** O nó que ficou sem nenhum animal sai da cerca. */
    static void removeIfEmpty(ServerWorld world, LeashKnotEntity knot) {
        if (world.getEntitiesByClass(AnimalEntity.class, knot.getBoundingBox().expand(SLACK * 2),
                animal -> animal.getLeashHolder() == knot).isEmpty()) {
            knot.discard();
        }
    }
}
