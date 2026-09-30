package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Areia de beira d'água ao alcance da colônia — decisão do autor, 2026-09-30:
 * <i>"nos biomas as areias podem ser encontradas próximo dos rios e mares, de
 * lagos, da água"</i>.
 *
 * <p>A rota da areia só existia no deserto, e por isso a vidraça da planície
 * não tinha cadeia: ninguém ia buscar areia para o vidro. Esta sonda procura,
 * na superfície carregada, areia exposta com água ao lado. Sem ela, a rota
 * continua fechada — areia é natural do bioma e não aparece no baú, então
 * dizer que há rota sem haver areia deixaria a obra esperando para sempre.
 *
 * <p>O resultado vale {@link #RECHECK_TICKS} por colônia: a margem do rio não
 * muda de um ciclo para o outro, e a busca percorre milhares de colunas.
 */
public final class SandNearWater {

    static {
        ServerMemory.register(SandNearWater.class, SandNearWater::clearAll);
    }

    /** O mesmo alcance da coleta de areia ({@code SandGathering}). */
    public static final int RADIUS = 48;

    static final int RECHECK_TICKS = 6_000;

    private static final Map<UUID, Answer> ANSWERS = new HashMap<>();

    private record Answer(boolean found, long at) {
    }

    private SandNearWater() {
    }

    /** Se há areia de beira d'água ao alcance, guardando a resposta. */
    public static boolean around(ServerWorld world, UUID colonyId, BlockPos center, int radius) {
        Answer answer = ANSWERS.get(colonyId);

        if (answer != null && world.getTime() - answer.at() < RECHECK_TICKS) {
            return answer.found();
        }

        boolean found = within(world, center, radius);

        ANSWERS.put(colonyId, new Answer(found, world.getTime()));

        return found;
    }

    /** A mesma pergunta, sem guardar: para a bateria. */
    public static boolean within(ServerWorld world, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;

                if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
                    continue;
                }

                BlockPos surface = surfaceAt(world, x, z);

                if (surface != null
                        && world.getBlockState(surface).isIn(BlockTags.SAND)
                        && besideWater(world, surface)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** Quantos blocos descer do topo, passando por ar, planta e barreira. */
    private static final int LOOK_DOWN = 12;

    /** O primeiro bloco de verdade da coluna, descendo do topo. */
    private static BlockPos surfaceAt(ServerWorld world, int x, int z) {
        int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;

        for (int y = top; y > top - LOOK_DOWN; y--) {
            BlockPos at = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(at);

            if (!LotGround.isNothing(state) && !state.isOf(net.minecraft.block.Blocks.BARRIER)) {
                return at;
            }
        }

        return null;
    }

    /** Água ao lado da areia, no mesmo nível ou um abaixo. */
    private static boolean besideWater(ServerWorld world, BlockPos sand) {
        for (Direction side : Direction.Type.HORIZONTAL) {
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos at = sand.offset(side).up(dy);

                if (world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4) == null) {
                    continue;
                }

                BlockState state = world.getBlockState(at);

                if (state.getFluidState().isIn(FluidTags.WATER)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void clearAll() {
        ANSWERS.clear();
    }
}
