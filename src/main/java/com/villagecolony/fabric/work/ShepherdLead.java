package com.villagecolony.fabric.work;

import com.villagecolony.fabric.brain.WorkRest;
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
        walkTo(world, shepherd, goal, arrival, false);
    }

    /** Usa a navegação externa durante a condução (ADR-004). */
    static void leadTo(ServerWorld world, VillagerEntity shepherd, BlockPos goal, int arrival) {
        walkTo(world, shepherd, goal, arrival, true);
    }

    private static void walkTo(ServerWorld world, VillagerEntity shepherd, BlockPos goal,
            int arrival, boolean leadingAnimal) {
        Optional<BlockPos> leg = WalkLegs.towards(world, shepherd.getBlockPos(), goal);
        BlockPos destination = leg.orElse(goal);
        int tolerance = leg.isPresent() ? 2 : arrival;

        if (leadingAnimal) {
            if (!WorkTargets.isLeadingAnimal(shepherd.getUuid())) {
                // A navegação Vanilla pode reutilizar o caminho feito antes de prender a guia (ADR-004).
                WorkRest.release(shepherd);
            }
            WorkTargets.setLeadingAnimal(shepherd.getUuid(), destination, tolerance);
        } else {
            WorkTargets.set(shepherd.getUuid(), destination, tolerance);
        }
    }

    static boolean reachedOutsideAnimal(ServerWorld world, VillagerEntity shepherd, AnimalEntity animal) {
        int reach = ShepherdHerding.REACH;
        if (shepherd.squaredDistanceTo(animal) > reach * reach) {
            return false;
        }
        if (HerdCandidates.sheltersAnimal(world, shepherd.getBlockPos())) {
            WorkTargets.set(shepherd.getUuid(), animal.getBlockPos(), 0);
            return false;
        }
        return true;
    }

    static void takeLeash(VillagerEntity shepherd, AnimalEntity animal) {
        WorkTargets.clear(shepherd.getUuid());
        WorkRest.release(shepherd);
        animal.attachLeash(shepherd, true);
    }

    /**
     * Na aproximação lateral, o pastor atravessa a frente da porteira para que a guia
     * mantenha o animal andando até a passagem, mesmo ao descer um degrau (Regra 299).
     */
    static BlockPos gateWaitingSpot(ServerWorld world, ShepherdPens.Pen pen, AnimalEntity animal) {
        BlockPos outside = pen.outside();
        BlockPos animalPos = animal.getBlockPos();
        boolean gateRunsAlongX = outside.getZ() != pen.gate().getZ();
        int offset = gateRunsAlongX ? animalPos.getX() - outside.getX() : animalPos.getZ() - outside.getZ();
        if (Math.abs(offset) < ShepherdHerding.REACH) {
            return outside;
        }
        int step = -Integer.signum(offset);
        BlockPos across = gateRunsAlongX ? outside.add(step, 0, 0) : outside.add(0, 0, step);
        return BuilderApproach.standable(world, across) ? across : outside;
    }

    /** Guia o animal até a célula externa pela navegação dele, ainda na corda (Regra 299). */
    static void alignAtGate(AnimalEntity animal, BlockPos outside) {
        var navigation = animal.getNavigation();
        if (!navigation.isIdle() && outside.equals(navigation.getTargetPos())) {
            return;
        }

        var path = navigation.findPathTo(outside, 0);
        if (path != null && path.reachesTarget()) {
            navigation.startMovingAlong(path, 1.0);
        }
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
