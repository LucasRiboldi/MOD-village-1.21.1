package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Travessia física do pastor e entrega pela porteira, Regra 299 e ADR-058. */
final class ShepherdDelivery {
    private final PenMoves.Motion motion = new PenMoves.Motion();
    private FencedIn.Exit crossing;
    private boolean airborne;
    private boolean animalAligned;
    private BlockPos center;
    private long searchAt;
    private int attempt;

    void reset() {
        crossing = null;
        airborne = false;
        animalAligned = false;
        attempt = 0;
        searchAt = 0;
        motion.reroute();
    }

    void advance(ServerWorld world, VillagerEntity shepherd, AnimalEntity animal, ShepherdHerding.Herd herd) {
        var pen = herd.pen;
        boolean readyToClose = herd.phase == ShepherdHerding.Phase.ENTER && ShepherdPens.isInside(pen, animal);
        if (!readyToClose) FencedIn.holdForDelivery(world, pen.gate());
        if (herd.phase == ShepherdHerding.Phase.JUMP_IN) {
            ShepherdLead.alignAtGate(animal, pen.outside());
            if (cross(world, shepherd, pen, true)) herd.phase = ShepherdHerding.Phase.ALIGN;
            return;
        }
        if (herd.phase == ShepherdHerding.Phase.ALIGN) {
            ShepherdLead.alignAtGate(animal, pen.outside());
            if (walkInside(world, shepherd, pen, pen.inside())) herd.phase = ShepherdHerding.Phase.ENTER;
            return;
        }

        if (center == null) {
            double x = pen.reach().stream().mapToInt(BlockPos::getX).average().orElseThrow();
            double z = pen.reach().stream().mapToInt(BlockPos::getZ).average().orElseThrow();
            center = pen.reach().stream().min(Comparator.comparingDouble(
                    at -> Math.pow(at.getX() - x, 2) + Math.pow(at.getZ() - z, 2))).orElseThrow();
        }
        if (!ShepherdLead.isTooFar(shepherd, animal)) walkInside(world, shepherd, pen, center);
        if (readyToClose) {
            if (FencedIn.closeForDelivery(world, pen.gate())) {
                PenTether.release(animal);
                animal.detachLeash(true, false);
                if (animal instanceof SheepEntity) PostGrass.inPen(world, animal.getBlockPos());
                VillageColonyMod.LOGGER.info("Shepherd {} brought a {} into the pen at {}",
                        shepherd.getUuidAsString().substring(0, 8), animal.getType().getUntranslatedName(),
                        pen.gate().toShortString());
                herd.phase = ShepherdHerding.Phase.JUMP_OUT;
                reset();
            }
            return;
        }
        animalAligned |= animal.squaredDistanceTo(Vec3d.ofBottomCenter(pen.outside())) <= 0.75 * 0.75
                || animal.getBoundingBox().intersects(new net.minecraft.util.math.Box(pen.gate()));
        ShepherdPens.walkThrough(world, pen, animal, animalAligned ? ShepherdPens.deeper(pen) : pen.outside());
    }

    private boolean walkInside(ServerWorld world, VillagerEntity shepherd, ShepherdPens.Pen pen, BlockPos target) {
        if (shepherd.getBlockPos().equals(target)) {
            PenMoves.stopWalking(shepherd);
            if (shepherd.squaredDistanceTo(Vec3d.ofBottomCenter(target)) < 0.16) return true;
            approach(shepherd, target);
            return false;
        }
        return PenMoves.walk(world, shepherd, motion, target, pen.reach(), world.getTime()) == PenMoves.Walk.ARRIVED;
    }

    /** Usa o mesmo impulso e a mesma física nos dois sentidos. Só confirma a chegada depois do pouso. */
    boolean cross(ServerWorld world, VillagerEntity shepherd, ShepherdPens.Pen pen, boolean entering) {
        long now = world.getTime();
        if (airborne) {
            if (!PenMoves.fly(shepherd, motion, crossing, now)) return false;
            airborne = false;
        }
        if (shepherd.isOnGround() && pen.reach().contains(shepherd.getBlockPos()) == entering
                && !FencedIn.isBarrier(world.getBlockState(shepherd.getBlockPos().down()))) {
            reset();
            return true;
        }
        if (crossing == null || now >= searchAt) {
            if (now < searchAt) return false;
            searchAt = now + 80;
            var exits = fenceCrossings(world, pen, entering);
            crossing = null;
            if (exits.isEmpty()) return false;
            var exit = exits.get(Math.floorMod(attempt++, exits.size()));
            crossing = entering ? new FencedIn.Exit(false, /* inside= */ exit.outside(), exit.barrier(), /* outside= */ exit.inside()) : exit;
            motion.reroute();
        }
        var launch = crossing.inside();
        if (shepherd.squaredDistanceTo(Vec3d.ofBottomCenter(launch)) <= 0.16 && shepherd.isOnGround()) {
            PenMoves.jump(shepherd, motion, now);
            airborne = true;
        } else if (entering) {
            if (shepherd.squaredDistanceTo(Vec3d.ofBottomCenter(launch)) <= 2.25) {
                PenMoves.stopWalking(shepherd);
                approach(shepherd, launch);
            } else if (shepherd.getNavigation().isIdle() || !launch.equals(shepherd.getNavigation().getTargetPos())) {
                shepherd.getNavigation().startMovingTo(launch.getX() + 0.5, launch.getY(), launch.getZ() + 0.5, 0.6);
            }
        } else {
            walkInside(world, shepherd, pen, launch);
        }
        return false;
    }

    private List<FencedIn.Exit> fenceCrossings(ServerWorld world, ShepherdPens.Pen pen, boolean entering) {
        List<FencedIn.Exit> exits = new ArrayList<>();
        for (BlockPos inside : pen.reach()) {
            for (Direction way : Direction.Type.HORIZONTAL) {
                BlockPos barrier = inside.offset(way);
                if (!world.getBlockState(barrier).isIn(BlockTags.FENCES)
                        || !FencedIn.isVaultable(world, inside, barrier)) continue;
                FencedIn.landing(world, barrier.offset(way), pen.reach())
                        .filter(outside -> !entering || outside.getY() >= inside.getY())
                        .filter(outside -> FencedIn.isVaultable(world, outside, barrier))
                        .ifPresent(outside -> exits.add(new FencedIn.Exit(false, inside, barrier, outside)));
            }
        }
        exits.sort(Comparator.comparingDouble(exit -> exit.barrier().getSquaredDistance(pen.gate())));
        return exits;
    }

    private static void approach(VillagerEntity shepherd, BlockPos at) {
        shepherd.getMoveControl().moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.6);
    }
}
