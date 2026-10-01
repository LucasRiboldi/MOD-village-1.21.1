package com.villagecolony.fabric.work;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Os movimentos de quem sai do curral: andar pelo caminho e pular a cerca —
 * E52, 2026-10-01. Quem decide para onde e quando é o {@link PenEscape}.
 *
 * <p><b>Por que não o {@code WALK_TARGET}.</b> Na bateria de 01-10, o destino
 * Vanilla para um bloco a dois passos, dentro do curral, voltava "não
 * alcança" e o aldeão ficava parado os 400 tiques da saída — às vezes sim, às
 * vezes não. O caminho aqui é o da própria busca do {@link FencedIn}, e o
 * aldeão anda pelo controle de movimento, bloco a bloco, como um jogador.
 */
final class PenMoves {

    /** O passo dele: o multiplicador da velocidade de andar, como o das tasks Vanilla. */
    private static final double WALK_SPEED = 0.6;

    /** A que distância do meio de um bloco do caminho ele conta como chegado nele. */
    private static final double AT_STEP = 0.4;

    /** Tiques sem avançar um passo do caminho antes de refazer o caminho de onde está. */
    private static final int REROUTE_AFTER = 60;

    /** O impulso para cima: a cabeça do pulo fica a 2,7 blocos, sobra 1,2 acima da cerca. */
    static final double JUMP_UP = 0.65;

    /** Quanto ele precisa ter subido antes de ir para a frente: a cerca tem 1,5. */
    private static final double CLEAR_OF_FENCE = 1.55;

    /** O passo para a frente, por tique, lá em cima. */
    private static final double JUMP_AHEAD = 0.3;

    enum Walk { WALKING, ARRIVED, NO_WAY }

    /** O caminho que ele segue agora e o pulo em curso, de um aldeão. */
    static final class Motion {

        private List<BlockPos> route;

        private BlockPos routeTo;

        private int step;

        private long stepSince;

        private long jumpedAt;

        private double jumpedFromY;

        /** Se este pulo já passou da altura da cerca: dali em diante ele vai para a frente. */
        private boolean cleared;

        /** Esquece o caminho: o próximo passo o refaz de onde ele está. */
        void reroute() {
            route = null;
        }

        /** Segue este caminho, já sabido, até {@code to}. */
        void follow(List<BlockPos> path, BlockPos to, long now) {
            route = path;
            routeTo = to;
            step = 0;
            stepSince = now;
        }
    }

    private PenMoves() {
    }

    /**
     * Um passo no caminho até {@code target} pelo chão {@code floor}. A
     * navegação Vanilla fica parada enquanto isso: ela e este caminho
     * disputariam o mesmo controle de movimento.
     */
    static Walk walk(ServerWorld world, VillagerEntity villager, Motion motion, BlockPos target,
            Set<BlockPos> floor, long now) {

        boolean stale = motion.route != null && now - motion.stepSince > REROUTE_AFTER;

        if (motion.route == null || !target.equals(motion.routeTo) || stale) {
            Optional<List<BlockPos>> route = FencedIn.route(world, villager.getBlockPos(), target, floor);

            if (route.isEmpty()) {
                return Walk.NO_WAY;
            }

            motion.follow(route.get(), target, now);
        }

        while (motion.step < motion.route.size() && isAt(villager, motion.route.get(motion.step))) {
            motion.step++;
            motion.stepSince = now;
        }

        stopWalking(villager);

        if (motion.step >= motion.route.size()) {
            return Walk.ARRIVED;
        }

        BlockPos next = motion.route.get(motion.step);

        villager.getMoveControl().moveTo(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, WALK_SPEED);
        villager.getLookControl().lookAt(next.getX() + 0.5, next.getY() + 1.5, next.getZ() + 0.5);

        return Walk.WALKING;
    }

    /** O pulo: só para cima agora; para a frente, quando passar da cerca. */
    static void jump(VillagerEntity villager, Motion motion, long now) {
        stopWalking(villager);
        villager.setVelocity(0.0, JUMP_UP, 0.0);
        villager.velocityModified = true;

        motion.jumpedAt = now;
        motion.jumpedFromY = villager.getY();
        motion.cleared = false;
    }

    /**
     * Um tique no ar sobre a cerca.
     *
     * @return se ele já pousou — do lado de fora, ou de volta do lado de dentro
     */
    static boolean fly(VillagerEntity villager, Motion motion, FencedIn.Exit exit, long now) {
        if (villager.isOnGround() && now - motion.jumpedAt > 4) {
            motion.reroute();

            return true;
        }

        Vec3d barrier = Vec3d.ofBottomCenter(exit.barrier());
        Vec3d way = Vec3d.of(exit.outside().subtract(exit.inside())).multiply(1, 0, 1).normalize();
        Vec3d pos = villager.getPos();

        // Passou quando o corpo inteiro saiu do bloco da cerca: meio bloco
        // dela, mais meia largura dele.
        boolean past = pos.subtract(barrier).dotProduct(way) >= 0.5 + villager.getWidth() / 2 + 0.05;
        Vec3d velocity = villager.getVelocity();

        motion.cleared |= villager.getY() - motion.jumpedFromY >= CLEAR_OF_FENCE;

        if (past) {
            // Do lado de fora: cai reto no bloco logo depois da cerca.
            villager.setVelocity(0.0, velocity.y, 0.0);
        } else if (motion.cleared) {
            Vec3d toOutside = Vec3d.ofBottomCenter(exit.outside()).subtract(pos).multiply(1, 0, 1);
            Vec3d push = toOutside.lengthSquared() < 1.0E-4 ? way : toOutside.normalize();

            villager.setVelocity(push.x * JUMP_AHEAD, velocity.y, push.z * JUMP_AHEAD);
        } else {
            // Ainda subindo: nada para a frente, ou ele bate no poste.
            villager.setVelocity(0.0, velocity.y, 0.0);
        }

        villager.velocityModified = true;

        return false;
    }

    static double horizontalDistance(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;

        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean isAt(VillagerEntity villager, BlockPos step) {
        return horizontalDistance(villager.getPos(), Vec3d.ofBottomCenter(step)) <= AT_STEP
                && Math.abs(villager.getY() - step.getY()) < 0.6;
    }

    private static void stopWalking(VillagerEntity villager) {
        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
        villager.getNavigation().stop();
    }
}
