package com.villagecolony.fabric.work;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Onde um aldeão pode ser posto de pé sem ficar com a cabeça dentro de um bloco — Regra 114.
 *
 * <p>Quem é levado para a cama ou tirado de onde está preso cai num lugar onde o corpo inteiro cabe:
 * chão firme debaixo, nada sólido na altura do corpo e nenhum líquido. Em cima da cama só quando há
 * espaço até a cabeça; senão, o lugar livre mais perto em volta dela.
 */
final class SafeLanding {

    /** Até quantos blocos em volta do alvo se procura. */
    static final int REACH = 2;

    /** Chão mais alto que um bloco (cerca, muro) não é chão: pousar ali é pousar em cima da cerca. */
    private static final double TALLEST_FLOOR = 1.0;

    private SafeLanding() {
    }

    /** O lugar mais perto de {@code floor} onde ele fica de pé em cima do bloco, ou nada. */
    static Optional<Vec3d> near(ServerWorld world, Entity mob, BlockPos floor) {
        Vec3d aim = Vec3d.ofBottomCenter(floor);

        return Stream.iterate(-1, dy -> dy <= 1, dy -> dy + 1)
                .flatMap(dy -> BlockPos.stream(floor.add(-REACH, dy, -REACH), floor.add(REACH, dy, REACH))
                        .map(BlockPos::toImmutable))
                .sorted(Comparator.comparingDouble((BlockPos at) -> at.getSquaredDistance(floor)))
                .map(at -> on(world, mob, at))
                .flatMap(Optional::stream)
                .min(Comparator.comparingDouble(spot -> spot.squaredDistanceTo(aim)));
    }

    /** De pé em cima de {@code floor}, se o corpo cabe ali. */
    static Optional<Vec3d> on(ServerWorld world, Entity mob, BlockPos floor) {
        if (!world.isInBuildLimit(floor) || !world.isChunkLoaded(floor)) {
            return Optional.empty();
        }

        BlockState state = world.getBlockState(floor);
        VoxelShape shape = state.getCollisionShape(world, floor);

        if (shape.isEmpty() || !state.getFluidState().isEmpty()) {
            return Optional.empty();
        }

        double top = shape.getMax(Direction.Axis.Y);

        if (top <= 0 || top > TALLEST_FLOOR) {
            return Optional.empty();
        }

        Vec3d feet = new Vec3d(floor.getX() + 0.5, floor.getY() + top, floor.getZ() + 0.5);

        return fits(world, mob, feet) ? Optional.of(feet) : Optional.empty();
    }

    /** O corpo de pé em {@code feet} não encosta em bloco nem em líquido. */
    static boolean fits(ServerWorld world, Entity mob, Vec3d feet) {
        Box body = mob.getDimensions(EntityPose.STANDING).getBoxAt(feet);

        return world.isSpaceEmpty(mob, body) && !world.containsFluid(body);
    }
}
