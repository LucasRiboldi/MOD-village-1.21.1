package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.integration.SiteSurroundings;
import com.villagecolony.fabric.integration.VillageRoad;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Optional;

/**
 * O encalhado em cima de um telhado desce ao chão — jogo da 0.3.109 (autor, 2026-10-29: "o construtor não precisa
 * subir ou escalar"): o construtor ficou 3 vezes preso no telhado de uma casa, a queda até o chão era de 5 blocos, e a
 * fuga o dava por solto "after 0 steps" — ela só pergunta se o terreno em volta não está acima dele.
 *
 * <p>Regra 160: resgata para o chão natural ou rua mais próxima que permita ficar de pé. Em um desnível
 * curto, tenta até {@link #LEAPS} pulos. Se o desnível excede a distância segura de queda do aldeão,
 * já com a margem do pulo, ou se as tentativas se esgotam, coloca o aldeão no chão.
 *
 * <p>Regra 362: bloco de trabalho não é telhado, e a terceira descida seguida do mesmo lugar põe o aldeão no chão e solta
 * a tarefa.
 */
public final class RoofDescent {

    /** A distância das oito colunas que dizem se o chão está longe embaixo. */
    static final int RING = 4;

    /** Quantos blocos abaixo do pé o chão precisa estar para ser queda (jogo da 0.3.110: o telhado preso era de 2). */
    static final int DROP = 2;

    /** Quantas das oito colunas precisam ser queda. */
    static final int DROPS = 5;

    /** Até quantos blocos em volta se procura o chão. */
    static final int SEARCH = 8;

    /** Quantas descidas seguidas do mesmo lugar antes de ser posto no chão de vez, sem a tarefa (Regra 362). */
    static final int LOOPS = 3;

    /** Quantos pulos antes de ser posto no chão. */
    static final int LEAPS = 3;

    /** Impulsos do pulo curto. Com impulso vertical de 0,3 o pulo sobe menos de um bloco. */
    private static final double LEAP_OUTWARD = 0.5;
    private static final double LEAP_UPWARD = 0.3;

    static {
        com.villagecolony.core.type.ServerMemory.register(RoofDescent.class, RoofDescent::clearAll);
    }

    private static final java.util.Map<java.util.UUID, Integer> TRIED = new java.util.HashMap<>();

    /** De onde cada aldeão desceu da última vez e quantas descidas seguidas de lá (Regra 362). */
    private static final java.util.Map<java.util.UUID, long[]> LOOPED = new java.util.HashMap<>();

    private RoofDescent() {
    }

    static void clearAll() {
        TRIED.clear();
        LOOPED.clear();
    }

    /** Se iniciou um pulo curto ou colocou o aldeão no chão. */
    static boolean check(ServerWorld world, VillagerEntity villager) {
        BlockPos feet = villager.getBlockPos();

        if (!onARoof(world, feet)) {
            TRIED.remove(villager.getUuid());
            return false;
        }

        Optional<BlockPos> ground = groundNear(world, feet);

        if (ground.isEmpty()) {
            return false;
        }

        BlockPos landing = ground.get();
        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
        villager.getNavigation().stop();
        int tried = TRIED.getOrDefault(villager.getUuid(), 0) + 1;

        if (tried == 1 && loops(villager.getUuid(), feet) >= LOOPS) {
            // Desceu e subiu de novo ao mesmo lugar: o pulo não resolve, e a tarefa que o leva lá em cima sai.
            LOOPED.remove(villager.getUuid());
            java.util.List.copyOf(VillageColonyMod.TASKS.assignedTo(villager.getUuid()))
                    .forEach(task -> task.release("climbed the same roof " + LOOPS + " times"));
            VillageColonyMod.LOGGER.info("VC_ROOF_LOOP worker={} at={} times={} rule=362",
                    villager.getUuid().toString().substring(0, 8), feet.toShortString(), LOOPS);
            return letDown(villager, feet, landing);
        }
        double fallWithLeap = villager.getY() - landing.getY() + 1.0;
        double safeFall = villager.getAttributeValue(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE);

        if (tried <= LEAPS && fallWithLeap <= safeFall) {
            TRIED.put(villager.getUuid(), tried);
            double dx = landing.getX() + 0.5 - villager.getX();
            double dz = landing.getZ() + 0.5 - villager.getZ();
            double length = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));

            villager.setVelocity(dx / length * LEAP_OUTWARD, LEAP_UPWARD, dz / length * LEAP_OUTWARD);
            villager.velocityModified = true;
            VillageColonyMod.LOGGER.info("Worker {} leaps off the roof at {} towards the ground at {} ({} of {})",
                    villager.getUuid().toString().substring(0, 8), feet.toShortString(), landing.toShortString(),
                    tried, LEAPS);
            ActionJournal.action(villager.getUuid(), "LEAPT", "", tried, landing, "off a roof");

            return true;
        }

        return letDown(villager, feet, landing);
    }

    /** Quantas descidas seguidas deste lugar, contando esta. */
    private static int loops(java.util.UUID id, BlockPos feet) {
        long[] last = LOOPED.get(id);
        int count = last != null && last[0] == feet.asLong() ? (int) last[1] + 1 : 1;

        LOOPED.put(id, new long[] {feet.asLong(), count});

        return count;
    }

    private static boolean letDown(VillagerEntity villager, BlockPos feet, BlockPos landing) {
        TRIED.remove(villager.getUuid());
        villager.requestTeleport(landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5);
        villager.setVelocity(0.0, 0.0, 0.0);
        villager.velocityModified = true;
        villager.onLanding();
        VillageColonyMod.LOGGER.info("Worker {} was let down from the roof at {} to the ground at {}",
                villager.getUuid().toString().substring(0, 8), feet.toShortString(), landing.toShortString());
        ActionJournal.action(villager.getUuid(), "LET_DOWN", "", 1, landing, "from a roof");

        return true;
    }

    /** Se o pé está sobre construção, com o chão longe embaixo em volta. */
    static boolean onARoof(ServerWorld world, BlockPos feet) {
        BlockState under = world.getBlockState(feet.down());

        if (under.isAir() || !under.getFluidState().isEmpty() || isGround(under) || isWorkBlock(under)) {
            return false;
        }

        int drops = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                int x = feet.getX() + dx * RING;
                int z = feet.getZ() + dz * RING;

                if (world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)
                        && world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) <= feet.getY() - DROP) {
                    drops++;
                }
            }
        }

        return drops >= DROPS;
    }

    /** O lugar de pé sobre chão natural ou rua mais perto, abaixo do pé. */
    static Optional<BlockPos> groundNear(ServerWorld world, BlockPos feet) {
        for (int radius = 1; radius <= SEARCH; radius++) {
            BlockPos best = null;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int x = feet.getX() + dx;
                    int z = feet.getZ() + dz;

                    if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }

                    BlockPos stand = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);

                    if (stand.getY() < feet.getY() && isGround(world.getBlockState(stand.down()))
                            && BuilderApproach.standable(world, stand)
                            && (best == null || stand.getSquaredDistance(feet) < best.getSquaredDistance(feet))) {
                        best = stand;
                    }
                }
            }

            if (best != null) {
                return Optional.of(best);
            }
        }

        return Optional.empty();
    }

    /** Bloco de trabalho (composter, barril, bancada e afins) não é telhado: o aldeão pisa nele no ofício (Regra 362). */
    static boolean isWorkBlock(BlockState state) {
        return net.minecraft.world.poi.PointOfInterestTypes.getTypeForState(state).isPresent();
    }

    private static boolean isGround(BlockState state) {
        return SiteSurroundings.isNaturalGround(state) || VillageRoad.isPathBlock(state);
    }
}
