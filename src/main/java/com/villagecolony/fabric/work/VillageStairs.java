package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.LotGround;
import com.villagecolony.fabric.world.MineFill;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A escada fixa entre as alturas da vila — decisão do autor, 2026-10-21.
 *
 * <p>Na vila do deserto jogada, a parte baixa (y63) e a vila de cima (y80+) não tinham caminho
 * que a navegação do jogo achasse: o construtor, o lenhador e o mineiro que moravam embaixo
 * encalhavam a cada viagem, subiam por pilar e a escada de fuga era tapada depois. Agora, quando um
 * encalhado abaixo do caminho chega à altura dele, a colônia assenta uma escada que fica: um degrau
 * por bloco, do lugar onde ele encalhou rumo ao destino, com o piso de pedra do bioma
 * ({@link MineFill}) e dois de ar em cima.
 *
 * <p>Só se abre chão natural; peça da vila, obra da colônia, bloco do jogador e água param a escada
 * ali (Regra 3). Uma escada por vizinhança: perto de outra, não se faz.
 */
final class VillageStairs {

    static {
        ServerMemory.register(VillageStairs.class, VillageStairs::clearAll);
    }

    /** Raio em que uma escada já feita serve, e não se faz outra. */
    static final int NEARBY = 16;

    /** O maior desnível que uma escada vence. */
    static final int TALLEST = 32;

    private static final Map<UUID, List<BlockPos>> BUILT = new HashMap<>();

    private VillageStairs() {
    }

    /**
     * Assenta a escada de {@code bottom} (os pés de quem encalhou) até a altura de {@code top}, rumo a
     * {@code way}.
     *
     * @return quantos degraus ficaram
     */
    static int connect(ServerWorld world, UUID colonyId, BlockPos bottom, BlockPos top, BlockPos way) {
        int rise = Math.min(top.getY() - bottom.getY(), TALLEST);

        if (rise < MinerSteps.CLIMB_OUT_BELOW || hasOneNear(colonyId, bottom)) {
            return 0;
        }

        Direction towards = towards(bottom, way);
        int steps = 0;

        for (int step = 1; step <= rise; step++) {
            BlockPos feet = bottom.offset(towards, step).up(step);
            BlockPos previousHead = bottom.offset(towards, step - 1).up(step + 1);

            if (!opens(world, feet) || !opens(world, feet.up()) || !opens(world, previousHead)
                    || !floors(world, feet.down())) {
                break;
            }

            steps++;
        }

        if (steps == 0) {
            return 0;
        }

        for (int step = 1; step <= steps; step++) {
            BlockPos feet = bottom.offset(towards, step).up(step);

            clear(world, feet);
            clear(world, feet.up());
            clear(world, bottom.offset(towards, step - 1).up(step + 1));

            if (!world.getBlockState(feet.down()).isSolidBlock(world, feet.down())) {
                world.setBlockState(feet.down(), MineFill.fillAt(world, feet.down()), Block.NOTIFY_ALL);
            }

            // Areia solta em cima do vão cairia na escada: vira a pedra do bioma (o escoramento da mina).
            shoreUp(world, feet.up(2));
            shoreUp(world, bottom.offset(towards, step - 1).up(step + 2));
        }

        BUILT.computeIfAbsent(colonyId, id -> new ArrayList<>()).add(bottom.toImmutable());
        VillageColonyMod.LOGGER.info("Colony {} laid a stair of {} steps {} from {} — {} up of {}",
                colonyId.toString().substring(0, 8), steps, towards.asString(), bottom.toShortString(), steps, rise);

        return steps;
    }

    /** Se já há escada desta colônia perto deste ponto de baixo. */
    static boolean hasOneNear(UUID colonyId, BlockPos bottom) {
        return BUILT.getOrDefault(colonyId, List.of()).stream()
                .anyMatch(built -> built.isWithinDistance(bottom, NEARBY));
    }

    /** O lado de {@code from} para {@code to}, pelo eixo de maior distância. */
    private static Direction towards(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();

        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }

        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Pode virar vão: já é vazio, ou é chão natural que ninguém pôs. */
    private static boolean opens(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        return LotGround.isNothing(state) || isNatural(world, pos);
    }

    /** Pode ser piso: já é firme, ou é vazio/natural onde a pedra do bioma entra. */
    private static boolean floors(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        return state.getFluidState().isEmpty()
                && ((state.isSolidBlock(world, pos) && !BlockProtection.isPlayerPlaced(state))
                        || LotGround.isNothing(state) || isNatural(world, pos));
    }

    private static boolean isNatural(ServerWorld world, BlockPos pos) {
        return LotGround.isBiomeGround(world, pos)
                && world.getBlockEntity(pos) == null
                && !BlockProtection.isColonyBuilt(pos)
                && !BlockProtection.isVillageOriginal(world, pos);
    }

    private static void shoreUp(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() instanceof net.minecraft.block.FallingBlock
                && isNatural(world, pos)) {
            world.setBlockState(pos, MineFill.fillAt(world, pos), Block.NOTIFY_ALL);
        }
    }

    private static void clear(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos).isAir()) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    static void clearAll() {
        BUILT.clear();
    }
}
