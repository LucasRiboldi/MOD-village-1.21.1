package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.integration.SiteSurroundings;
import com.villagecolony.fabric.integration.VillageRoad;
import net.minecraft.block.BlockState;
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
 * <p>Telhado é: pé sobre o que não é chão natural nem rua, e ao menos {@link #DROPS} das oito colunas a {@link #RING}
 * blocos com o chão {@link #DROP} ou mais abaixo do pé. Ele se joga do telhado rumo ao lugar de pé sobre chão mais
 * perto, até {@link #SEARCH} blocos (autor, 2026-10-29: "aldeão deve se atirar dos telhados"); só depois de
 * {@link #LEAPS} pulos sem sair é posto no chão, como quem é tirado do curral ({@code PenLift}).
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

    /** Quantos pulos antes de ser posto no chão. */
    static final int LEAPS = 3;

    /** A força do pulo para fora do telhado, para o lado e para cima. */
    static final double LEAP = 0.5;

    static {
        com.villagecolony.core.type.ServerMemory.register(RoofDescent.class, RoofDescent::clearAll);
    }

    private static final java.util.Map<java.util.UUID, Integer> TRIED = new java.util.HashMap<>();

    private RoofDescent() {
    }

    static void clearAll() {
        TRIED.clear();
    }

    /** Se ele está num telhado e foi posto no chão. */
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

        villager.getNavigation().stop();
        int tried = TRIED.merge(villager.getUuid(), 1, Integer::sum);

        if (tried <= LEAPS) {
            double dx = ground.get().getX() + 0.5 - villager.getX();
            double dz = ground.get().getZ() + 0.5 - villager.getZ();
            double length = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));

            villager.setVelocity(dx / length * LEAP, LEAP, dz / length * LEAP);
            villager.velocityModified = true;
            VillageColonyMod.LOGGER.info("Worker {} leaps off the roof at {} towards the ground at {} ({} of {})",
                    villager.getUuid().toString().substring(0, 8), feet.toShortString(), ground.get().toShortString(),
                    tried, LEAPS);
            ActionJournal.action(villager.getUuid(), "LEAPT", "", tried, ground.get(), "off a roof");

            return true;
        }

        TRIED.remove(villager.getUuid());
        villager.requestTeleport(ground.get().getX() + 0.5, ground.get().getY(), ground.get().getZ() + 0.5);
        VillageColonyMod.LOGGER.info("Worker {} was let down from the roof at {} to the ground at {}",
                villager.getUuid().toString().substring(0, 8), feet.toShortString(), ground.get().toShortString());
        ActionJournal.action(villager.getUuid(), "LET_DOWN", "", 1, ground.get(), "from a roof");

        return true;
    }

    /** Se o pé está sobre construção, com o chão longe embaixo em volta. */
    static boolean onARoof(ServerWorld world, BlockPos feet) {
        BlockState under = world.getBlockState(feet.down());

        if (under.isAir() || !under.getFluidState().isEmpty() || isGround(under)) {
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

    private static boolean isGround(BlockState state) {
        return SiteSurroundings.isNaturalGround(state) || VillageRoad.isPathBlock(state);
    }
}
